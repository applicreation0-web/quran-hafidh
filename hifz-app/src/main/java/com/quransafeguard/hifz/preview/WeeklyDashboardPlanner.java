package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Read-only weekly projection. Dashboard and runtime share HifzSchedule.planFor(). */
final class WeeklyDashboardPlanner {
    static final class Row {
        final LocalDate date;
        final String day;
        final String morning;
        final String evening;
        final String state;
        Row(LocalDate date,String day,String morning,String evening,String state){
            this.date=date;this.day=day;this.morning=morning;this.evening=evening;this.state=state;
        }
    }

    private static final class Projection {
        final String label;
        final VerseRef next;
        Projection(String label, VerseRef next){this.label=label;this.next=next;}
    }

    private final HifzPrefs prefs;
    private final GeometryRepository geometry;
    private final DashboardLedger ledger;

    WeeklyDashboardPlanner(HifzPrefs prefs,GeometryRepository geometry,DashboardLedger ledger){
        this.prefs=prefs;this.geometry=geometry;this.ledger=ledger;
    }

    List<Row> week(LocalDate today){
        ArrayList<Row> out=new ArrayList<>();
        LocalDate monday=today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int sabqiCursor=currentSabqiCursor();
        VerseRef itqanCursor=prefs.itqanCursor();
        VerseRef murajaahCursor=prefs.murajaahCursor();
        EligibleCorpus itqanCorpus=prefs.itqanWorkCorpus();
        EligibleCorpus murajaahCorpus=prefs.murajaahCorpus();
        ArrayList<HifzPrefs.RecentSabqi> projectedRecent=new ArrayList<>(prefs.recentSabqi());

        for(int i=0;i<7;i++){
            LocalDate date=monday.plusDays(i);
            if(date.isBefore(prefs.programStartDate())){
                out.add(new Row(date,day(date),"—","—","Parcours non démarré"));
                continue;
            }
            DailyPlan plan=HifzSchedule.INSTANCE.planFor(date.getDayOfWeek());
            String morningMode=modeFor(plan.getMorning().getKind());
            String eveningMode=modeFor(plan.getEvening().getKind());
            DashboardLedger.Record morningActual=ledger.find(date,morningMode);
            DashboardLedger.Record eveningActual=ledger.find(date,eveningMode);

            String morning;
            String projectedMorningRange="";
            switch(plan.getMorning().getKind()){
                case SABQI_NEW: {
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    if(morningActual!=null) morning="✓ "+compact(morningActual.label);
                    else if(block==null) morning="Sabqi · plage à vérifier";
                    else {
                        projectedMorningRange=range(block.startVerse,block.endVerse);
                        morning="Sabqi · "+projectedMorningRange+" · 5 lignes";
                        projectedRecent.add(new HifzPrefs.RecentSabqi(block.startLineIndex,block.endLineIndex));
                        sabqiCursor=block.endLineIndex+1;
                    }
                    break;
                }
                case ITQAN: {
                    if(morningActual!=null) morning="✓ "+compact(morningActual.label);
                    else if(!itqanCorpus.contains(itqanCursor)) morning="Itqān · curseur hors corpus";
                    else {
                        GeometryRepository.VerseUnit unit=geometry.eligiblePageUnit(itqanCursor,itqanCorpus);
                        morning="Itqān ×"+PreviewConfig.ITQAN_TOTAL_REPS+" · "+range(unit.start,unit.end);
                        itqanCursor=itqanCorpus.nextAnchored(unit.end,prefs.itqanRotationStart());
                    }
                    break;
                }
                case RECENT_SABQI_REVIEW:
                    morning=morningActual!=null?"✓ "+compact(morningActual.label)
                        :"Sabqi récent · "+recentRange(projectedRecent)+" · "+plan.getMorning().getTargetMinutes()+" min";
                    break;
                default:
                    morning="—";
            }

            String evening;
            switch(plan.getEvening().getKind()){
                case SABQI_TODAY_REVIEW:
                    if(eveningActual!=null) evening="✓ "+compact(eveningActual.label);
                    else {
                        String exact=date.equals(today)&&date.toString().equals(prefs.sabqiTodayReviewDate())
                            ? rangeForLines(prefs.sabqiTodayReviewStartLine(),prefs.sabqiTodayReviewEndLine())
                            : projectedMorningRange;
                        evening="Sabqi du jour · "+(exact.isEmpty()?"mêmes 5 lignes":exact)+" · "+plan.getEvening().getTargetMinutes()+" min";
                    }
                    break;
                case OLD_ITQAN_MURAJAAH:
                    if(eveningActual!=null) evening="✓ "+compact(eveningActual.label);
                    else {
                        Projection p=projectMurajaah(murajaahCursor,murajaahCorpus,plan.getEvening().getTargetMinutes());
                        evening="Murājaʿah · "+p.label;
                        murajaahCursor=p.next;
                    }
                    break;
                default:
                    evening="—";
            }

            boolean morningDone=morningActual!=null;
            boolean eveningDone=eveningActual!=null;
            String state=morningDone&&eveningDone?"Validées":morningDone?"Soir à faire":"À faire";
            if(date.isBefore(today)&&(!morningDone||!eveningDone)) state="Non validée";
            out.add(new Row(date,day(date),morning,evening,state));
        }
        return out;
    }

    private Projection projectMurajaah(VerseRef cursor,EligibleCorpus corpus,int minutes){
        if(!corpus.contains(cursor)) return new Projection("curseur à vérifier",cursor);
        int lines=Math.max(1,(int)Math.floor(minutes*60.0/prefs.murajaahSecondsPerLine()));
        GeometryRepository.EligibleLinePlan plan=geometry.planEligibleLines(cursor,lines,corpus);
        return new Projection(range(plan.start,plan.actualPlannedEnd)+" · "+minutes+" min",
            corpus.next(plan.actualPlannedEnd));
    }

    private int currentSabqiCursor(){
        int cursor=prefs.sabqiLineCursor();
        return cursor<0?geometry.firstLineIndex(prefs.sabqiStart()):cursor;
    }

    private GeometryRepository.FiveLineBlock safeSabqi(int cursor){
        try{
            if(cursor<geometry.firstLineIndex(prefs.sabqiStart()))return null;
            if(cursor+PreviewConfig.SABQI_LINES-1>geometry.lastLineIndex(prefs.sabqiEnd()))return null;
            return geometry.fiveLineBlock(cursor);
        }catch(RuntimeException e){return null;}
    }

    private String rangeForLines(int start,int end){
        if(start<0||end<start)return "";
        try{
            GeometryRepository.FiveLineBlock block=geometry.fiveLineBlock(start);
            return range(block.startVerse,block.endVerse);
        }catch(RuntimeException e){return "";}
    }

    private String recentRange(List<HifzPrefs.RecentSabqi> recent){
        if(recent.isEmpty())return "aucun passage";
        try{
            GeometryRepository.FiveLineBlock first=geometry.fiveLineBlock(recent.get(0).startLine);
            GeometryRepository.FiveLineBlock last=geometry.fiveLineBlock(recent.get(recent.size()-1).startLine);
            return range(first.startVerse,last.endVerse);
        }catch(RuntimeException e){return "fenêtre à vérifier";}
    }

    private static String range(VerseRef a,VerseRef b){
        if(a.getSurah()==b.getSurah())return "Sourate "+a.getSurah()+" · v."+a.getAyah()+"–"+b.getAyah();
        return "Sourate "+a.getSurah()+" v."+a.getAyah()+" → Sourate "+b.getSurah()+" v."+b.getAyah();
    }

    private static String compact(String label){
        if(label==null||label.isEmpty())return "Séance enregistrée";
        return label.replace(" · révélations 0","");
    }

    private static String modeFor(SessionKind kind){
        switch(kind){
            case SABQI_NEW:return HifzSessionActivity.SABQI;
            case SABQI_TODAY_REVIEW:return HifzSessionActivity.SABQI_TODAY_REVIEW;
            case ITQAN:return HifzSessionActivity.ITQAN;
            case RECENT_SABQI_REVIEW:return HifzSessionActivity.RECENT_SABQI_REVIEW;
            case OLD_ITQAN_MURAJAAH:return HifzSessionActivity.MURAJAAH;
            default:throw new IllegalArgumentException("Unsupported session kind: "+kind);
        }
    }

    private static String day(LocalDate date){
        switch(date.getDayOfWeek()){
            case MONDAY:return "Lun";
            case TUESDAY:return "Mar";
            case WEDNESDAY:return "Mer";
            case THURSDAY:return "Jeu";
            case FRIDAY:return "Ven";
            case SATURDAY:return "Sam";
            case SUNDAY:return "Dim";
            default:return date.getDayOfWeek().toString();
        }
    }
}

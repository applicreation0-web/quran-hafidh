package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.DailyPlan;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.SessionKind;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only sliding seven-day projection. Dashboard and runtime share HifzSchedule.planFor(). */
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

    static List<LocalDate> window(LocalDate today){
        if(today==null)return Collections.emptyList();
        ArrayList<LocalDate> out=new ArrayList<>(7);
        for(int i=0;i<7;i++)out.add(today.plusDays(i));
        return Collections.unmodifiableList(out);
    }

    List<Row> week(LocalDate today){
        ArrayList<Row> out=new ArrayList<>();
        int sabqiCursor=currentSabqiCursor();
        VerseRef murajaahCursor=prefs.murajaahCursor();
        EligibleCorpus murajaahCorpus=prefs.murajaahCorpus();
        ArrayList<HifzPrefs.RecentSabqi> projectedRecent=new ArrayList<>(prefs.recentSabqi());
        boolean consolidationActivated=prefs.recentConsolidationActivatedOn()!=null;

        prefs.currentAnchoringEntry(geometry);
        List<AnchoringQueue.Entry> projectedAnchoring=AnchoringQueue.visitOrder(
            prefs.anchoringQueue(),prefs.anchoringQueueIndex());
        int projectedAnchoringIndex=0;
        int projectedItqanBlockIndex=prefs.itqanBlockIndex();

        for(LocalDate date:window(today)){
            if(date.isBefore(prefs.programStartDate())){
                out.add(new Row(date,day(date,today),"—","—","Parcours non démarré"));
                continue;
            }
            DailyPlan plan=HifzSchedule.INSTANCE.planFor(date.getDayOfWeek(),projectedRecent.size(),consolidationActivated);
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
                    else if(block==null) morning="Leçon neuve · plage à vérifier";
                    else {
                        projectedMorningRange=range(block.startVerse,block.endVerse);
                        morning="Leçon neuve · "+projectedMorningRange+" · 5 lignes";
                        projectedRecent.add(new HifzPrefs.RecentSabqi(block.startLineIndex,block.endLineIndex));
                        sabqiCursor=block.endLineIndex+1;
                    }
                    break;
                }
                case ITQAN: {
                    if(morningActual!=null) morning="✓ "+compact(morningActual.label);
                    else if(date.equals(today)&&prefs.anchoringDeferredToday()) morning="Ancrage · page reportée";
                    else if(projectedAnchoringIndex>=projectedAnchoring.size()) morning="Ancrage · aucune page en attente";
                    else {
                        AnchoringQueue.Entry entry=projectedAnchoring.get(projectedAnchoringIndex);
                        VerseRef start=GeometryRepository.parseVerse(entry.start);
                        VerseRef end=GeometryRepository.parseVerse(entry.end);
                        List<VerseRef> verses=geometry.versesForRange(start,end);
                        boolean fractionated=prefs.isFractionatedUnit(verses);
                        String fractionLabel="";
                        if(fractionated){
                            int lineCount=geometry.lineIdsForVerseRange(start,end).size();
                            int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(lineCount));
                            int block=Math.max(0,Math.min(projectedItqanBlockIndex,blocks-1));
                            fractionLabel=" · bloc "+(block+1)+"/"+blocks;
                            block++;
                            if(block>=blocks){projectedItqanBlockIndex=0;projectedAnchoringIndex++;}
                            else projectedItqanBlockIndex=block;
                        }else{
                            projectedItqanBlockIndex=0;
                            projectedAnchoringIndex++;
                        }
                        morning="Ancrage · "+range(start,end)+fractionLabel
                            +(entry.origin==AnchoringQueue.Origin.FORCED_PROMOTION?" · promotion de sécurité":"");
                    }
                    break;
                }
                case RECENT_SABQI_REVIEW:
                    morning=morningActual!=null?"✓ "+compact(morningActual.label)
                        :"Consolidation · "+recentRange(projectedRecent)+" · "+plan.getMorning().getTargetMinutes()+" min";
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
                        evening="Reprise du soir · "+(exact.isEmpty()?"mêmes 5 lignes":exact)+" · "+plan.getEvening().getTargetMinutes()+" min";
                    }
                    break;
                case OLD_ITQAN_MURAJAAH:
                    if(eveningActual!=null) evening="✓ "+compact(eveningActual.label);
                    else {
                        Projection p=projectMurajaah(murajaahCursor,murajaahCorpus,plan.getEvening().getTargetMinutes());
                        evening="Entretien · "+p.label;
                        murajaahCursor=p.next;
                    }
                    break;
                default:
                    evening="—";
            }

            boolean morningDone=morningActual!=null;
            boolean eveningDone=eveningActual!=null;
            String state=morningDone&&eveningDone?"Validées":morningDone?"Soir à faire":"À faire";
            out.add(new Row(date,day(date,today),morning,evening,state));
        }
        return out;
    }

    private Projection projectMurajaah(VerseRef cursor,EligibleCorpus corpus,int minutes){
        if(!corpus.contains(cursor)) return new Projection("position à vérifier",cursor);
        int lines=HifzCadence.targetLines(minutes,prefs.murajaahSecondsPerLine());
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
            List<HifzPrefs.RecentSabqi> canonical=HifzPrefs.canonicalRecentOrder(recent);
            GeometryRepository.FiveLineBlock first=geometry.fiveLineBlock(canonical.get(0).startLine);
            GeometryRepository.FiveLineBlock last=geometry.fiveLineBlock(canonical.get(canonical.size()-1).startLine);
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

    static String day(LocalDate date,LocalDate today){
        if(date!=null&&date.equals(today))return "Aujourd’hui";
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
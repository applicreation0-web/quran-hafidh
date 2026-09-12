package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledSession;
import com.quransafeguard.hifz.core.SessionType;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Read-only weekly projection. It never writes or moves a Hifz cursor. */
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

    private static final class MurajaahProjection {
        final String recent;
        final String old;
        final VerseRef nextCursor;
        MurajaahProjection(String recent,String old,VerseRef nextCursor){
            this.recent=recent;this.old=old;this.nextCursor=nextCursor;
        }
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
        EligibleCorpus corpus=prefs.corpus();
        ArrayList<HifzPrefs.RecentSabqi> projectedRecent=new ArrayList<>(prefs.recentSabqi());

        for(int i=0;i<7;i++){
            LocalDate date=monday.plusDays(i);
            ScheduledSession scheduled=HifzSchedule.INSTANCE.scheduled(date,prefs.programStartDate(),date);
            if(scheduled==null){out.add(new Row(date,day(date),"—","—","Parcours non démarré"));continue;}
            String type=modeFor(scheduled.getType());
            boolean hasEveningMurajaah=HifzSchedule.INSTANCE.hasEveningMurajaah(date.getDayOfWeek());
            DashboardLedger.Record actual=ledger.find(date,type);
            DashboardLedger.Record eveningActual=hasEveningMurajaah?ledger.find(date,HifzSessionActivity.MURAJAAH):null;
            if(actual!=null && !hasEveningMurajaah){
                out.add(new Row(date,day(date),"✓ "+compact(actual.label),eveningForActual(type,actual.label),"Validée"));
                continue;
            }
            if(date.isBefore(today)){
                if(hasEveningMurajaah){
                    String morning=actual==null?"—":"✓ "+compact(actual.label);
                    String evening=eveningActual==null?"—":"✓ "+compact(eveningActual.label);
                    String state=actual!=null&&eveningActual!=null?"Validées":"Non validée";
                    out.add(new Row(date,day(date),morning,evening,state));
                }else{
                    String state=date.isBefore(ledger.started())?"Historique pré-0.7 non enregistré":"Non validée";
                    out.add(new Row(date,day(date),"—","—",state));
                }
                continue;
            }

            if(scheduled.getType()==SessionType.SABQI){
                GeometryRepository.FiveLineBlock b=safeSabqi(sabqiCursor);
                if(b==null){out.add(new Row(date,day(date),"Sabqi · plage à vérifier","—","À vérifier"));continue;}
                String range=range(b.startVerse,b.endVerse)+(b.endsInsideVerse?" · fin partielle":"");
                int rep=date.equals(today)?prefs.sabqiRep():0;
                String state=rep>=PreviewConfig.SABQI_TOTAL_REPS?"✓ À valider":rep>0?"En cours "+(rep+1)+"/37":"À faire";
                out.add(new Row(date,day(date),"Sabqi · "+range,"Révision courte · "+range,state));
                projectedRecent.add(new HifzPrefs.RecentSabqi(b.startLineIndex,b.endLineIndex));
                sabqiCursor=b.endLineIndex+1;
            }else if(scheduled.getType()==SessionType.ITQAN){
                String morning;
                String state;
                if(actual!=null){
                    morning="✓ "+compact(actual.label);
                    state=eveningActual!=null?"Validées":"Soir à faire";
                }else{
                    if(!corpus.contains(itqanCursor)){out.add(new Row(date,day(date),"Itqān · curseur hors corpus","—","À vérifier"));continue;}
                    GeometryRepository.VerseUnit unit;
                    int rep=date.equals(today)?prefs.itqanRep():0;
                    VerseRef savedStart=date.equals(today)?prefs.itqanUnitStart():null,savedEnd=date.equals(today)?prefs.itqanUnitEnd():null;
                    if(rep>0&&savedStart!=null&&savedEnd!=null){
                        unit=new GeometryRepository.VerseUnit(geometry.pageForVerse(savedStart),savedStart,savedEnd,
                            geometry.versesForRange(savedStart,savedEnd),geometry.lineIdsForVerseRange(savedStart,savedEnd));
                    }else unit=geometry.eligiblePageUnit(itqanCursor,corpus);
                    state=rep>=PreviewConfig.ITQAN_TOTAL_REPS?"✓ À valider":rep>0?"En cours "+(rep+1)+"/"+PreviewConfig.ITQAN_TOTAL_REPS:"À faire";
                    morning="Itqān ×"+PreviewConfig.ITQAN_TOTAL_REPS+" · "+range(unit.start,unit.end);
                    itqanCursor=corpus.next(unit.end);
                }
                String evening="—";
                if(hasEveningMurajaah){
                    if(eveningActual!=null){
                        evening="✓ "+compact(eveningActual.label);
                    }else{
                        MurajaahProjection projection=projectMurajaah(projectedRecent,murajaahCursor,corpus);
                        evening="Murājaʿah · "+projection.recent+" · "+projection.old;
                        murajaahCursor=projection.nextCursor;
                    }
                }
                out.add(new Row(date,day(date),morning,evening,state));
            }else{
                MurajaahProjection projection=projectMurajaah(projectedRecent,murajaahCursor,corpus);
                murajaahCursor=projection.nextCursor;
                String morning="Murājaʿah A · "+projection.recent;
                String evening="Murājaʿah B · "+projection.old;
                String state=date.equals(today)?("B".equals(prefs.murajaahPhase())?"En cours · Bloc B":"En cours · Bloc A"):"À faire";
                out.add(new Row(date,day(date),morning,evening,state));
            }
        }
        return out;
    }

    private MurajaahProjection projectMurajaah(List<HifzPrefs.RecentSabqi> recent,VerseRef cursor,EligibleCorpus corpus){
        String recentRange=recentRange(recent);
        int recentLines=recentLineCount(recent);
        double secondsA=Math.min(PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60.0,
            recentLines*prefs.recentSecondsPerLine());
        double availableBSeconds=PreviewConfig.MURAJAAH_ITQAN_MINUTES_WORKING*60.0+
            Math.max(0.0,PreviewConfig.MURAJAAH_RECENT_SABQI_MINUTES_WORKING*60.0-secondsA);
        int lines=(int)Math.floor(availableBSeconds/prefs.murajaahSecondsPerLine());lines=Math.max(1,lines);
        String old;
        VerseRef next=cursor;
        if(corpus.contains(cursor)){
            GeometryRepository.EligibleLinePlan plan=geometry.planEligibleLines(cursor,lines,corpus);
            old=range(plan.start,plan.actualPlannedEnd)+" · ~"+Math.round(availableBSeconds/60.0)+" min";
            next=corpus.next(plan.actualPlannedEnd);
        }else old="curseur à repositionner";
        String recentLabel=recentRange.isEmpty()?"aucun Sabqi récent":recentRange+" · ~"+Math.round(secondsA/60.0)+" min";
        return new MurajaahProjection(recentLabel,old,next);
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

    private int recentLineCount(List<HifzPrefs.RecentSabqi> recent){
        int count=0;for(HifzPrefs.RecentSabqi item:recent)count+=Math.max(0,item.endLine-item.startLine+1);return count;
    }

    private String recentRange(List<HifzPrefs.RecentSabqi> recent){
        if(recent.isEmpty())return "";
        try{
            GeometryRepository.FiveLineBlock first=geometry.fiveLineBlock(recent.get(0).startLine);
            GeometryRepository.FiveLineBlock last=geometry.fiveLineBlock(recent.get(recent.size()-1).startLine);
            return range(first.startVerse,last.endVerse);
        }catch(RuntimeException e){return "fenêtre récente à vérifier";}
    }

    private static String range(VerseRef a,VerseRef b){
        if(a.getSurah()==b.getSurah())return "Sourate "+a.getSurah()+" · v."+a.getAyah()+"–"+b.getAyah();
        return "Sourate "+a.getSurah()+" v."+a.getAyah()+" → Sourate "+b.getSurah()+" v."+b.getAyah();
    }

    private static String compact(String label){
        if(label==null||label.isEmpty())return "Séance enregistrée";
        return label.replace(" · révélations 0","");
    }

    private static String eveningForActual(String type,String label){
        if(HifzSessionActivity.SABQI.equals(type))return "Révision courte · "+compact(label);
        return "—";
    }

    private static String modeFor(SessionType type){
        switch(type){
            case SABQI:return HifzSessionActivity.SABQI;
            case ITQAN:return HifzSessionActivity.ITQAN;
            case MURAJAAH:return HifzSessionActivity.MURAJAAH;
            default:throw new IllegalArgumentException("Unsupported session type: "+type);
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

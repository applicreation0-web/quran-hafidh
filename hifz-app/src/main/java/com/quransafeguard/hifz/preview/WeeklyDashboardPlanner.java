package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.VerseRef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only seven-day projection driven by the canonical Apprentissage/Stabilisation/Révision cadence. */
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
    private final J10HostBudgetStore hostBudgetStore;

    WeeklyDashboardPlanner(HifzPrefs prefs,GeometryRepository geometry,DashboardLedger ledger,
                           J10HostBudgetStore hostBudgetStore){
        this.prefs=prefs;this.geometry=geometry;this.ledger=ledger;this.hostBudgetStore=hostBudgetStore;
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
            CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek());
            String morning="—",evening="—",state="À faire";
            switch(action){
                case LEARNING:{
                    DashboardLedger.Record morningActual=ledger.find(date,HifzSessionActivity.SABQI);
                    DashboardLedger.Record eveningActual=ledger.find(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
                    boolean morningJ10=morningActual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.SABQI,date);
                    boolean eveningJ10=eveningActual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.SABQI_TODAY_REVIEW,date);
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    if(morningJ10)morning="J10 · créneau utilisé";
                    else if(morningActual!=null)morning="✓ "+compact(morningActual.label);
                    else if(block==null)morning="Apprentissage · plage à vérifier";
                    else{
                        morning="Apprentissage · "+range(block.startVerse,block.endVerse)+" · 5 lignes";
                        sabqiCursor=block.endLineIndex+1;
                    }
                    if(eveningJ10)evening="J10 · créneau utilisé";
                    else if(eveningActual!=null)evening="✓ "+compact(eveningActual.label);
                    else evening="Apprentissage · reprise · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
                    boolean m=morningActual!=null||morningJ10,e=eveningActual!=null||eveningJ10;
                    state=m&&e?"Validé":m?"Reprise à faire":"À faire";
                    break;
                }
                case STABILIZATION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.ITQAN);
                    boolean j10=actual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.ITQAN,date);
                    if(j10)morning="J10 · créneau utilisé";
                    else if(actual!=null)morning="✓ "+compact(actual.label);
                    else if(date.equals(today)&&prefs.anchoringDeferredToday())morning="Stabilisation · unité reportée";
                    else if(projectedAnchoringIndex>=projectedAnchoring.size())morning="Stabilisation · aucune unité à stabiliser";
                    else{
                        AnchoringQueue.Entry entry=projectedAnchoring.get(projectedAnchoringIndex);
                        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
                        List<VerseRef> verses=geometry.versesForRange(start,end);
                        boolean fractionated=prefs.isFractionatedUnit(verses);
                        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
                        if(fractionated){
                            int[] segments=geometry.surahSegmentLineCounts(start,end);
                            int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(segments));
                            int block=Math.max(0,Math.min(projectedItqanBlockIndex,blocks-1));
                            morning="Stabilisation · "+range(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
                            block++;if(block>=blocks){projectedItqanBlockIndex=0;projectedAnchoringIndex++;}else projectedItqanBlockIndex=block;
                        }else{
                            morning="Stabilisation · "+range(start,end)+" · ×"+reps;
                            projectedItqanBlockIndex=0;projectedAnchoringIndex++;
                        }
                    }
                    state=(actual!=null||j10)?"Validé":"À faire";
                    break;
                }
                case REVISION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                    boolean j10=actual==null&&hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(HifzSessionActivity.MURAJAAH,date);
                    if(j10)evening="J10 · créneau utilisé";
                    else if(actual!=null)evening="✓ "+compact(actual.label);
                    else{
                        Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                        evening="Révision · "+projected.label;
                        murajaahCursor=projected.next;
                    }
                    state=(actual!=null||j10)?"Validé":"À faire";
                    break;
                }
                default:throw new IllegalStateException("Action de cadence inconnue");
            }
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

    private static String range(VerseRef a,VerseRef b){
        if(a.getSurah()==b.getSurah())return "Sourate "+a.getSurah()+" · v."+a.getAyah()+"–"+b.getAyah();
        return "Sourate "+a.getSurah()+" v."+a.getAyah()+" → Sourate "+b.getSurah()+" v."+b.getAyah();
    }

    private static String compact(String label){
        if(label==null||label.isEmpty())return "Séance enregistrée";
        return label.replace(" · révélations 0","");
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

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

            if(action==CadenceAction.LEARNING){
                DashboardLedger.Record learned=ledger.find(date,HifzSessionActivity.SABQI);
                DashboardLedger.Record review=ledger.find(date,HifzSessionActivity.SABQI_TODAY_REVIEW);
                boolean learnedJ10=learned==null&&slotConsumed(HifzSessionActivity.SABQI,date);
                boolean reviewJ10=review==null&&slotConsumed(HifzSessionActivity.SABQI_TODAY_REVIEW,date);
                String projectedRange="";
                if(learnedJ10) morning="J10 · créneau utilisé";
                else if(learned!=null) morning="✓ "+compact(learned.label);
                else {
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    if(block==null) morning="Apprentissage · plage à vérifier";
                    else {
                        projectedRange=range(block.startVerse,block.endVerse);
                        morning="Apprentissage · "+projectedRange+" · 5 lignes";
                        sabqiCursor=block.endLineIndex+1;
                    }
                }
                if(reviewJ10) evening="J10 · créneau utilisé";
                else if(review!=null) evening="✓ "+compact(review.label);
                else evening="Apprentissage · reprise · "+(projectedRange.isEmpty()?"mêmes 5 lignes":projectedRange)
                    +" · "+HifzSchedule.EVENING_REVIEW_MINUTES+" min";
                boolean morningDone=learned!=null||learnedJ10;
                boolean eveningDone=review!=null||reviewJ10;
                state=morningDone&&eveningDone?"Validée":morningDone?"Reprise à faire":"À faire";
            }else if(action==CadenceAction.STABILIZATION){
                DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.ITQAN);
                boolean j10=actual==null&&slotConsumed(HifzSessionActivity.ITQAN,date);
                if(j10) morning="J10 · créneau utilisé";
                else if(actual!=null) morning="✓ "+compact(actual.label);
                else if(date.equals(today)&&prefs.anchoringDeferredToday()) morning="Stabilisation · unité reportée";
                else if(projectedAnchoringIndex>=projectedAnchoring.size()) morning="Stabilisation · aucune unité en attente";
                else {
                    AnchoringQueue.Entry entry=projectedAnchoring.get(projectedAnchoringIndex);
                    VerseRef start=GeometryRepository.parseVerse(entry.start);
                    VerseRef end=GeometryRepository.parseVerse(entry.end);
                    boolean fractionated=prefs.isFractionatedUnit(geometry.versesForRange(start,end));
                    int reps=PreviewConfig.itqanTotalReps(entry.protocol);
                    if(fractionated){
                        int[] segments=geometry.surahSegmentLineCounts(start,end);
                        int blocks=Math.max(1,PreviewConfig.fractionatedBlockCount(segments));
                        int block=Math.max(0,Math.min(projectedItqanBlockIndex,blocks-1));
                        morning="Stabilisation · "+range(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
                        block++;
                        if(block>=blocks){projectedItqanBlockIndex=0;projectedAnchoringIndex++;}
                        else projectedItqanBlockIndex=block;
                    }else{
                        morning="Stabilisation · "+range(start,end)+" · ×"+reps;
                        projectedItqanBlockIndex=0;
                        projectedAnchoringIndex++;
                    }
                }
                state=(actual!=null||j10)?"Validée":"À faire";
            }else{
                DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                boolean j10=actual==null&&slotConsumed(HifzSessionActivity.MURAJAAH,date);
                if(j10) evening="J10 · créneau utilisé";
                else if(actual!=null) evening="✓ "+compact(actual.label);
                else {
                    Projection p=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                    evening="Révision · "+p.label;
                    murajaahCursor=p.next;
                }
                state=(actual!=null||j10)?"Validée":"À faire";
            }
            out.add(new Row(date,day(date,today),morning,evening,state));
        }
        return out;
    }

    private boolean slotConsumed(String mode,LocalDate date){
        return hostBudgetStore!=null&&hostBudgetStore.isSlotConsumed(mode,date);
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

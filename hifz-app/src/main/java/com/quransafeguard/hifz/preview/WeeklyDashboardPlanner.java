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

        prefs.currentAnchoringEntry(geometry);
        List<AnchoringQueue.Entry> projectedAnchoring=new ArrayList<>();
        for(AnchoringQueue.Entry entry:AnchoringQueue.visitOrder(prefs.anchoringQueue(),prefs.anchoringQueueIndex())){
            VerseRef entryStart=GeometryRepository.parseVerse(entry.start),entryEnd=GeometryRepository.parseVerse(entry.end);
            if(!CorpusLinePolicy.ownedLineIdsForRangeOnPage(entryStart,entryEnd,geometry).isEmpty())projectedAnchoring.add(entry);
        }
        int projectedAnchoringIndex=0;
        int projectedItqanBlockIndex=prefs.itqanBlockIndex();

        for(LocalDate date:window(today)){
            if(date.isBefore(prefs.programStartDate())){
                out.add(new Row(date,day(date,today),"—","—","Parcours non démarré"));
                continue;
            }
            CadenceAction action=HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), prefs.learningDaysPerWeek());
            String morning="—",evening="—",state="À faire";
            switch(action){
                case LEARNING:{
                    DashboardLedger.Record morningActual=ledger.find(date,HifzSessionActivity.SABQI);
                    DashboardLedger.Record snowballActual=ledger.find(date,HifzSessionActivity.LEARNING_CONSOLIDATION);
                    DashboardLedger.Record entretienActual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    if(morningActual!=null)morning="✓ "+compact(morningActual.label);
                    else if(block==null)morning="Apprentissage · plage à vérifier";
                    else{
                        morning="Apprentissage · "+range(block.startVerse,block.endVerse)+" · "+block.lineIds.size()+" lignes";
                        sabqiCursor=block.endLineIndex+1;
                    }
                    boolean eveningDone=snowballActual!=null&&entretienActual!=null;
                    if(eveningDone)evening="✓ Renforcement + Entretien";
                    else{
                        Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                        evening="Renforcement + Entretien · "+projected.label;
                        murajaahCursor=projected.next;
                    }
                    boolean m=morningActual!=null;
                    state=m&&eveningDone?"Validé":m?"Soir à faire":"À faire";
                    break;
                }
                case STABILIZATION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.ITQAN);
                    if(actual!=null)morning="✓ "+compact(actual.label);
                    else if(date.equals(today)&&prefs.anchoringDeferredToday())morning="Stabilisation · unité reportée";
                    else if(projectedAnchoringIndex>=projectedAnchoring.size())morning="Stabilisation · aucune unité à stabiliser";
                    else{
                        AnchoringQueue.Entry entry=projectedAnchoring.get(projectedAnchoringIndex);
                        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
                        int reps=PreviewConfig.itqanTotalReps(entry.protocol);
                        List<String> owned=CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
                        List<StabilizationHalfPagePolicy.Unit> planned=StabilizationHalfPagePolicy.planPage(
                            geometry.linesForExactIds(owned));
                        int blocks=Math.max(1,planned.size());
                        int block=Math.max(0,Math.min(projectedItqanBlockIndex,blocks-1));
                        if(blocks>1){
                            morning="Stabilisation · "+range(start,end)+" · bloc "+(block+1)+"/"+blocks+" · ×"+reps;
                        }else{
                            morning="Stabilisation · "+range(start,end)+" · ×"+reps;
                        }
                        block++;
                        if(block>=blocks){projectedItqanBlockIndex=0;projectedAnchoringIndex++;}
                        else projectedItqanBlockIndex=block;
                    }
                    boolean morningDone=actual!=null;
                    DashboardLedger.Record snowballActual=ledger.find(date,HifzSessionActivity.RECENT_SABQI_REVIEW);
                    DashboardLedger.Record entretienActual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                    boolean eveningDone=snowballActual!=null&&entretienActual!=null;
                    if(eveningDone)evening="✓ Consolidation + Entretien";
                    else{
                        Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                        evening="Consolidation + Entretien · "+projected.label;
                        murajaahCursor=projected.next;
                    }
                    state=morningDone&&eveningDone?"Validé":morningDone?"Soir à faire":"À faire";
                    break;
                }
                case REVISION:{
                    DashboardLedger.Record learningFinal=ledger.find(date,HifzSessionActivity.LEARNING_FINAL);
                    DashboardLedger.Record consolidationFinal=ledger.find(date,HifzSessionActivity.CONSOLIDATION_FINAL);
                    boolean learningDone=learningFinal!=null||prefs.learningSnowballFinalUnits(date).isEmpty();
                    boolean consolidationDone=consolidationFinal!=null||prefs.stabilizationSnowballFinalUnits(date).isEmpty();
                    morning=(learningDone&&consolidationDone)?"✓ Révision finale ×5":"Révision finale ×5 · Renforcement + Consolidation";
                    boolean morningDone=learningDone&&consolidationDone;
                    DashboardLedger.Record entretienActual=ledger.find(date,HifzSessionActivity.MURAJAAH);
                    boolean eveningDone=entretienActual!=null;
                    if(eveningDone)evening="✓ Entretien";
                    else{
                        Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,HifzSchedule.MAINTENANCE_MINUTES);
                        evening="Entretien · "+projected.label;
                        murajaahCursor=projected.next;
                    }
                    state=(morningDone&&eveningDone)?"Validé":morningDone?"Soir à faire":"À faire";
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
        return new Projection(segmentedRange(plan)+" · "+minutes+" min",
            corpus.next(plan.actualPlannedEnd));
    }

    /**
     * The acquired corpus can hold several disjoint ranges, so a plan can wrap back through it
     * (or jump between ranges) before reaching actualPlannedEnd; a plain start->end range() would
     * then show a nonsensical reversed span within one surah. Splits the traversal at every
     * non-contiguous step, the same way HifzSessionActivity.murajaahObjectiveLabel() does.
     */
    private static String segmentedRange(GeometryRepository.EligibleLinePlan plan){
        List<VerseRef> traversal=plan.traversalVerses;
        if(traversal.isEmpty())return range(plan.start,plan.actualPlannedEnd);
        StringBuilder out=new StringBuilder();
        VerseRef segmentStart=traversal.get(0);
        VerseRef previous=segmentStart;
        for(int i=1;i<=traversal.size();i++){
            VerseRef current=i<traversal.size()?traversal.get(i):null;
            boolean contiguous=current!=null&&GeometryRepository.ordinal(current)==GeometryRepository.ordinal(previous)+1;
            if(!contiguous){
                if(out.length()>0)out.append(" · puis ");
                out.append(range(segmentStart,previous));
                if(current!=null)segmentStart=current;
            }
            if(current!=null)previous=current;
        }
        return out.toString();
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
        return HifzDisplayVocabulary.canonicalize(label).replace(" · révélations 0","");
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

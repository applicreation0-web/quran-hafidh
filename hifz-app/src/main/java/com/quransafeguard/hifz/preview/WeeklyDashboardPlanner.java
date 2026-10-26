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
    private final HifzSpeedStore speedStore;

    WeeklyDashboardPlanner(HifzPrefs prefs,GeometryRepository geometry,DashboardLedger ledger,HifzSpeedStore speedStore){
        this.prefs=prefs;this.geometry=geometry;this.ledger=ledger;this.speedStore=speedStore;
    }

    /** P4 J-15: Révision passive grows with the real ACQUIRED corpus instead of staying a
     *  flat constant — see MaintenanceCoveragePolicy. */
    private int maintenanceMinutes(){
        return MaintenanceCoveragePolicy.minutes(
            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
    }

    static List<LocalDate> window(LocalDate today){
        if(today==null)return Collections.emptyList();
        ArrayList<LocalDate> out=new ArrayList<>(7);
        for(int i=0;i<7;i++)out.add(today.plusDays(i));
        return Collections.unmodifiableList(out);
    }

    /** A failed forecast is not permission to hide the calendar or invent any sessions. */
    static List<Row> fallbackWeek(LocalDate today){
        ArrayList<Row> rows=new ArrayList<>();
        for(LocalDate date:window(today))
            rows.add(new Row(date,day(date,today),
                "Matin · détails à vérifier","Soir · détails à vérifier","Projection à vérifier"));
        return Collections.unmodifiableList(rows);
    }

    /** Display-only: the three canonical recommended Apprentissage days of each week.
     * Never advances a cursor or infers that a missed day was completed. */
    static String learningSessionCue(java.time.DayOfWeek day,HifzPrefs.WeeklyProgram program){
        if(program!=HifzPrefs.WeeklyProgram.RECOMMENDED || day==null)return "";
        switch(day){
            case MONDAY:return " · séance 1/3";
            case WEDNESDAY:return " · séance 2/3";
            case FRIDAY:return " · séance 3/3";
            default:return "";
        }
    }

    List<Row> week(LocalDate today){return week(today,today);}

    /** Selection does not become the real clock. Advance forecasts from today, never saved state. */
    List<Row> week(LocalDate today,LocalDate first){
        if(first.plusDays(6).isBefore(today))return Collections.emptyList();
        if(first.isAfter(today.plusDays(365)))return fallbackWeek(first);
        ArrayList<Row> out=new ArrayList<>();
        // Treat each read-only forecast as independent. A damaged old Itqān
        // cursor must not prevent actual Sabqi and revision receipts showing.
        int sabqiCursor=-1;
        try { sabqiCursor=currentSabqiCursor(); }
        catch(RuntimeException problem) {
            android.util.Log.e("QuranHifz","Weekly Sabqi cursor unavailable",problem);
        }

        VerseRef murajaahCursor=null;
        EligibleCorpus murajaahCorpus=null;
        int duration=-1;
        try {
            murajaahCursor=prefs.murajaahCursor();
            murajaahCorpus=prefs.murajaahCorpus(geometry);
            duration=maintenanceMinutes();
        } catch(RuntimeException problem) {
            android.util.Log.e("QuranHifz","Weekly revision projection unavailable",problem);
        }
        final int revisionMinutes=duration;

        // The historic OPEN snapshot and the live rotation are independent
        // sources; neither may invalidate the other six visible dates.
        ItqanWeeklyForecast itqanForecast=null;
        try {
            AnchoringQueue.Entry currentItqan=prefs.currentAnchoringEntry(geometry);
            VerseRef sabqiFrontier=prefs.currentSabqiPosition(geometry);
            itqanForecast=new ItqanWeeklyForecast(
                prefs.itqanRotationState(),currentItqan,prefs.itqanBlockIndex(),
                (leg,postNas)->prefs.cachedUnitsInLeg(leg,sabqiFrontier,geometry,
                    ItqanMaintenancePolicy.regimeFor(postNas)));
        } catch(RuntimeException problem) {
            android.util.Log.e("QuranHifz","Weekly Itqan rotation projection unavailable",problem);
        }

        // Legacy V1 four-date snapshots: display only frozen OPEN history.
        ItqanFourSessionPolicy.Plan fourDates=null;
        int nextFourDateSlot=0;
        try {
            ItqanFourSessionSnapshot frozenItqan=prefs.frozenItqanSnapshot(geometry);
            if(frozenItqan!=null){
                fourDates=frozenItqan.plan;
                nextFourDateSlot=frozenItqan.sessionIndex;
            }
        } catch(RuntimeException problem) {
            android.util.Log.e("QuranHifz","Weekly frozen Itqan snapshot unavailable",problem);
        }

        ArrayList<LocalDate> projectionDates=new ArrayList<>();
        for(LocalDate date=today;!date.isAfter(first.plusDays(6));date=date.plusDays(1))projectionDates.add(date);
        for(LocalDate date:projectionDates){
            if(date.isBefore(prefs.programStartDate())){
                out.add(new Row(date,day(date,today),"—","—","Parcours non démarré"));
                continue;
            }
            try {
            CadenceAction action=WeeklyProgramPolicy.actionFor(date.getDayOfWeek(),prefs.weeklyProgramFor(date),prefs.learningDaysPerWeek());
            String morning="—",evening="—",state="À faire";
            switch(action){
                case LEARNING:{
                    DashboardLedger.Record morningActual=ledger.find(date,HifzSessionActivity.SABQI);
                    DashboardLedger.Record snowballActual=ledger.find(date,HifzSessionActivity.LEARNING_CONSOLIDATION);
                    GeometryRepository.FiveLineBlock block=safeSabqi(sabqiCursor);
                    String learningCue=learningSessionCue(date.getDayOfWeek(),prefs.weeklyProgramFor(date));
                    if(morningActual!=null)morning="✓ "+compact(morningActual.label)+learningCue;
                    else if(block==null)morning="Apprentissage"+learningCue+" · plage à vérifier";
                    else{
                        morning="Apprentissage"+learningCue+" · "+range(block.startVerse,block.endVerse)+" · "+block.lineIds.size()+" lignes";
                        sabqiCursor=block.endLineIndex+1;
                    }
                    boolean snowballDone=snowballActual!=null || (date.equals(today)
                        && !prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,date));
                    evening=snowballDone?"✓ Renforcement":"Renforcement · révision cumulative";
                    boolean lessonDone=morningActual!=null;
                    state=lessonDone&&snowballDone?"Validé":lessonDone?"Soir à faire":"À faire";
                    break;
                }
                case STABILIZATION:{
                    DashboardLedger.Record actual=ledger.find(date,HifzSessionActivity.ITQAN);
                    boolean alreadyValidated=date.toString().equals(prefs.lastItqanDate());
                    if(actual!=null)morning="✓ "+compact(actual.label);
                    else if(alreadyValidated)morning="✓ Stabilisation · séance validée";
                    else if(date.equals(today)&&prefs.anchoringDeferredToday())morning="Stabilisation · unité reportée";
                    else if(fourDates!=null&&nextFourDateSlot<4) {
                        ItqanFourSessionPolicy.Session slot=fourDates.sessions.get(nextFourDateSlot);
                        while(slot.blocks.isEmpty()&&nextFourDateSlot<3)
                            slot=fourDates.sessions.get(++nextFourDateSlot);
                        if(slot.blocks.isEmpty())morning="Stabilisation · fenêtre terminée";
                        else {
                            String where=QuranSurahNames.block(geometry,slot.blocks.get(0).lineIds,
                                "pages "+fourDates.firstPage+"–"+fourDates.lastPage);
                            morning="Stabilisation · séance "+(nextFourDateSlot+1)+"/4"
                                +" · "+slot.lineCount+" lignes · "+slot.blocks.size()
                                +" bloc(s) · "+where+" · ×40";
                            nextFourDateSlot++;
                        }
                    }
                    else if(fourDates!=null)morning="Stabilisation · prochaine fenêtre après validation";
                    else if(itqanForecast==null)morning="Stabilisation · projection à vérifier";
                    else if(itqanForecast.entry()==null)morning="Stabilisation · aucune unité à stabiliser";
                    else{
                        AnchoringQueue.Entry entry=itqanForecast.entry();
                        VerseRef start=GeometryRepository.parseVerse(entry.start),end=GeometryRepository.parseVerse(entry.end);
                        // An already-open session keeps its frozen plan; future units use
                        // the regime projected after any An-Nās crossing, not stale prefs.
                        ItqanRegimeStore.UnitPlan plan=itqanForecast.isInitialUnit()
                            ?prefs.itqanUnitPlan(entry)
                            :new ItqanRegimeStore.UnitPlan(itqanForecast.regime(),
                                ItqanMaintenancePolicy.protocolForNewSession());
                        int reps=itqanForecast.isInitialUnit()
                            ?ItqanMaintenancePolicy.totalReps(plan)
                            :ItqanMaintenancePolicy.totalReps(plan.regime,plan.protocol);
                        List<String> owned=itqanForecast.isInitialUnit()
                            ?prefs.ordinaryItqanLineIds(start,end,geometry)
                            :CorpusLinePolicy.ownedLineIdsForRangeOnPage(start,end,geometry);
                        boolean postNas=plan.regime==ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE;
                        List<StabilizationHalfPagePolicy.Unit> planned=postNas||owned.isEmpty()
                            ?Collections.<StabilizationHalfPagePolicy.Unit>emptyList()
                            :StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(owned));
                        int blocks=Math.max(1,planned.size());
                        int block=Math.max(0,Math.min(itqanForecast.blockIndex(),blocks-1));
                        String where=planned.isEmpty()?range(start,end):QuranSurahNames.block(geometry,planned.get(block).lineIds,range(start,end));
                        if(blocks>1){
                            morning="Stabilisation · bloc "+(block+1)+"/"+blocks+" · "+where+" · ×"+reps;
                        }else{
                            morning="Stabilisation · "+where+" · ×"+reps;
                        }
                        // This advances the *forecast only*, never the user's saved state.
                        itqanForecast.completedBlock(blocks);
                    }
                    boolean morningDone=actual!=null||alreadyValidated;
                    if(date.getDayOfWeek()==java.time.DayOfWeek.SUNDAY
                            && prefs.weeklyProgramFor(date)!=HifzPrefs.WeeklyProgram.CLASSIC) {
                        DashboardLedger.Record learningFinal=ledger.find(date,HifzSessionActivity.LEARNING_FINAL);
                        DashboardLedger.Record stabilizationFinal=ledger.find(date,HifzSessionActivity.CONSOLIDATION_FINAL);
                        boolean learningDone=learningFinal!=null ||
                            (date.equals(today)&&prefs.learningSnowballFinalUnits(date).isEmpty());
                        boolean stabilizationDone=stabilizationFinal!=null ||
                            (date.equals(today)&&prefs.stabilizationSnowballFinalUnits(date).isEmpty());
                        morning+=" · "+(learningDone?"✓ Finale Sabqi":"Finale Sabqi");
                        evening=stabilizationDone?"✓ Finale Itqān":"Finale Itqān uniquement";
                        state=morningDone&&learningDone&&stabilizationDone?"Validé":
                            morningDone&&learningDone?"Soir à faire":"À faire";
                        // Sunday must NEVER forecast active/passive revision or consume
                        // its independent passive cursor, even during a look-ahead week.
                        break;
                    }
                    DashboardLedger.Record snowballActual=ledger.find(date,HifzSessionActivity.RECENT_SABQI_REVIEW);
                    boolean snowballDone=snowballActual!=null || (date.equals(today)
                        && !prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,date));
                    evening=snowballDone?"✓ Consolidation":prefs.itqanPostNasMaintenance()
                        ? "Consolidation · maintiens de la semaine · ×1 masqué"
                        : "Consolidation · révision cumulative";
                    state=morningDone&&snowballDone?"Validé":morningDone?"Soir à faire":"À faire";
                    break;
                }
                case REVISION:{
                    DashboardLedger.Record learningFinal=ledger.find(date,HifzSessionActivity.LEARNING_FINAL);
                    DashboardLedger.Record consolidationFinal=ledger.find(date,HifzSessionActivity.CONSOLIDATION_FINAL);
                    boolean learningDone=learningFinal!=null
                        ||date.toString().equals(prefs.lastLearningConsolidationDate());
                    // A future Sunday without weekday receipts is not a completed
                    // finale. Empty due material may mean simply "not yet learned".
                    boolean learningDue=!prefs.learningSnowballFinalUnits(date).isEmpty();
                    // In post-An-Nās maintenance no new Itqān finale exists.
                    // A pre-existing frozen first-regime OPEN finale may still resume.
                    boolean oldItqanFinal=prefs.unfinishedConsolidationSessionDate(
                        ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-final-")!=null;
                    boolean itqanFinalDue=!prefs.itqanPostNasMaintenance()||oldItqanFinal;
                    boolean consolidationDone=consolidationFinal!=null
                        ||date.toString().equals(prefs.lastRecentSabqiReviewDate());
                    boolean consolidationDue=itqanFinalDue
                        &&!prefs.stabilizationSnowballFinalUnits(date).isEmpty();
                    morning=learningDone?"✓ Finale Sabqi"
                        :learningDue?"Finale Sabqi · par page ×3"
                        :date.isAfter(today)?"Finale Sabqi · à prévoir selon les acquis"
                        :"Finale Sabqi · aucun passage admissible";
                    evening=!itqanFinalDue?"—":consolidationDone?"✓ Finale Itqān"
                        :consolidationDue?"Finale Itqān · par page ×3"
                        :date.isAfter(today)?"Finale Itqān · à prévoir selon les acquis"
                        :"Finale Itqān · aucun passage admissible";
                    state=learningDone&&(!itqanFinalDue||consolidationDone)?"Validé"
                        :learningDone?"Soir à faire"
                        :!learningDue&&!consolidationDue
                            ?(date.isAfter(today)?"Prévision":"Aucune finale à valider")
                            :"À faire";
                    // Sunday cannot consume a passive cursor or schedule any general revisions.
                    break;
                }
                default:throw new IllegalStateException("Action de cadence inconnue");
            }
            if(date.getDayOfWeek()!=java.time.DayOfWeek.SUNDAY) {
                // Contract: morning = active + passive review; evening = Hifz lesson + snowball.
                boolean activeDone=ledger.find(date,HifzSessionActivity.MURAJAAH_ACTIVE)!=null;
                boolean passiveDone=ledger.find(date,HifzSessionActivity.MURAJAAH)!=null;
                String durationLabel=revisionMinutes>0 ? revisionMinutes+" min" : "durée à vérifier";
                String active=(activeDone?"✓ ":"")+"Révision active · "+durationLabel;
                String passive=(passiveDone?"✓ ":"")+"Révision passive · "+durationLabel;
                if(!passiveDone && murajaahCursor!=null && murajaahCorpus!=null
                        && revisionMinutes>0) {
                    Projection projected=projectMurajaah(murajaahCursor,murajaahCorpus,revisionMinutes);
                    // projectMurajaah already includes duration; display it once.
                    String range=projected.label;
                    String trailing=" · "+revisionMinutes+" min";
                    if(range.endsWith(trailing))
                        range=range.substring(0,range.length()-trailing.length());
                    passive+=" · "+range;
                    murajaahCursor=projected.next; // Read-only forecast, not a saved cursor.
                } else if(!passiveDone) {
                    passive+=" · position à vérifier";
                }
                if(action==CadenceAction.LEARNING || action==CadenceAction.STABILIZATION) {
                    String lesson=morning, snowball=evening;
                    morning=active+" + "+passive;
                    evening=lesson+" + "+snowball;
                    boolean lessonDone=ledger.find(date,action==CadenceAction.LEARNING
                        ?HifzSessionActivity.SABQI:HifzSessionActivity.ITQAN)!=null
                        || (action==CadenceAction.STABILIZATION
                            && date.toString().equals(prefs.lastItqanDate()));
                    state=activeDone&&passiveDone&&lessonDone&&snowball.startsWith("✓ ")?"Validé"
                        :activeDone&&passiveDone?"Soir à faire":"À faire";
                } else {
                    morning=active+" + "+passive;
                    evening="—";
                    state=activeDone&&passiveDone?"Validé":"À faire";
                }
            }
            out.add(new Row(date,day(date,today),morning,evening,state));
            } catch(RuntimeException invalidForecast){
                // One problematic historical OPEN state or future unit never kills
                // all seven dates. No advancement, credit, or synthetic completion.
                out.add(new Row(date,day(date,today),
                    "Matin · détails à vérifier","Soir · détails à vérifier",
                    "Projection à vérifier"));
            }
        }
        ArrayList<Row> visible=new ArrayList<>();
        for(Row row:out)if(!row.date.isBefore(first)&&!row.date.isAfter(first.plusDays(6)))visible.add(row);
        return visible;
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
        return QuranSurahNames.range(a,b);
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

package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.Test;
import static org.junit.Assert.*;

/** J5 behavioural replay: identical validated cumulative sets are never due again. */
public final class SnowballValidationIdempotenceTest {
    private static String unit(String ... lines) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList(lines));
    }
    private static HifzPrefs prepare(InMemoryPrefs disk, LocalDate day,
                                      ConsolidationCycleEngine.Family family) {
        String prefix=family==ConsolidationCycleEngine.Family.LEARNING?"learning":"stabilization";
        String monday=day.minusDays(day.getDayOfWeek().getValue()-1).toString();
        disk.disk.put(prefix+"SnowballWeekAnchor",monday);
        disk.disk.put(prefix+"SnowballUnitIds","[\""+unit("p520-l01","p520-l02")+"\"]");
        return new HifzPrefs(disk);
    }
    @Test public void mondayRenforcementThenTuesdayNeverRecreditsSameCumulativeLines() {
        LocalDate monday=LocalDate.of(2026,10,5);
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs prefs=prepare(disk,monday,ConsolidationCycleEngine.Family.LEARNING);
        assertTrue(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,monday));
        assertTrue(prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING,monday.toString()));
        assertFalse(prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING,monday.toString()));
        HifzPrefs afterReboot=new HifzPrefs(disk);
        assertFalse(afterReboot.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(1)));
        assertFalse(afterReboot.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(1).toString()));
        assertEquals(monday.toString(),afterReboot.lastLearningSnowballEveningDate());
        // A genuinely new block on Wednesday creates a new cumul, rather than a replay.
        String later=unit("p521-l01");
        disk.disk.put("learningSnowballUnitIds","[\""+unit("p520-l01","p520-l02")+"\",\""+later+"\"]");
        assertTrue(afterReboot.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(2)));
        assertTrue(afterReboot.completeSnowballEvening(ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(2).toString()));
        assertFalse(afterReboot.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(3)));
    }
    @Test public void tueThuSatConsolidationSameVersusNewUnits() {
        LocalDate tuesday=LocalDate.of(2026,10,6);
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs p=prepare(disk,tuesday,ConsolidationCycleEngine.Family.STABILIZATION);
        assertTrue(p.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.toString()));
        assertFalse(new HifzPrefs(disk).snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.plusDays(1)));
        disk.disk.put("stabilizationSnowballUnitIds","[\""+unit("p520-l01","p520-l02")+"\",\""+unit("p521-l04")+"\"]");
        assertTrue(p.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.plusDays(2)));
        assertTrue(p.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.plusDays(2).toString()));
        assertFalse(p.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.plusDays(3)));
        disk.disk.put("stabilizationSnowballUnitIds","[\""+unit("p520-l01","p520-l02")+"\",\""+unit("p521-l04")+"\",\""+unit("p521-l05")+"\"]");
        assertTrue(p.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,tuesday.plusDays(4)));
    }
    @Test public void nextWeekDoesNotTreatLastWeekSnapshotAsCompletedAndProgramSwitchPreservesData() {
        LocalDate saturday=LocalDate.of(2026,10,10);
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs prefs=prepare(disk,saturday,ConsolidationCycleEngine.Family.STABILIZATION);
        assertTrue(prefs.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION,saturday.toString()));
        Map<String,Object> before=new LinkedHashMap<>(disk.disk);
        assertTrue(prefs.setWeeklyProgramMode(HifzPrefs.WeeklyProgram.RECOMMENDED));
        assertFalse(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,saturday.plusDays(1)));
        LocalDate nextTuesday=LocalDate.of(2026,10,13);
        disk.disk.put("stabilizationSnowballWeekAnchor","2026-10-12");
        assertTrue(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,nextTuesday));
        for(Map.Entry<String,Object> e:before.entrySet())assertEquals(e.getValue(),disk.disk.get(e.getKey()));
    }
    @Test public void interruptedSessionRemainsPersistedUntilSuccessfulValidation() {
        LocalDate date=LocalDate.of(2026,10,6);
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs p=prepare(disk,date,ConsolidationCycleEngine.Family.STABILIZATION);
        disk.disk.put("v6ConsolidationStabilizationState","interrupted-placeholder");
        assertTrue(p.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,date));
        assertTrue(disk.disk.containsKey("v6ConsolidationStabilizationState"));
        assertTrue(p.completeSnowballEvening(ConsolidationCycleEngine.Family.STABILIZATION,date.toString()));
        assertFalse(disk.disk.containsKey("v6ConsolidationStabilizationState"));
        assertFalse(new HifzPrefs(disk).snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,date));
    }
}

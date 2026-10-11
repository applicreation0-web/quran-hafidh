package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Independent audit #2: real persisted V6 history / legacy OPEN-session migrations.
 * These are behaviour tests, not source-string assertions.
 */
public final class ContradictoryAuditTwoPersistenceTest {
    private static String block(String... lines) {
        return ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList(lines));
    }
    private static String history(String week,String... blocks) throws Exception {
        JSONArray units=new JSONArray();
        for(String unit:blocks)units.put(unit);
        return new JSONArray().put(new JSONObject().put("week",week).put("units",units)).toString();
    }
    private static String units(String... blocks) {
        JSONArray array=new JSONArray();
        for(String unit:blocks)array.put(unit);
        return array.toString();
    }

    @Test public void partlyAcquiredSundayDoesNotRepeatEntirelyCreditedBlocks() throws Exception {
        LocalDate sunday=LocalDate.of(2026,10,11);
        String acquired=block("p520-l01"), pending=block("p520-l02");
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("v6AcquiredCreditLineIds",units("p520-l01"));
        disk.disk.put("learningSnowball8WeekHistory",
            history("2026-10-05",acquired,pending));
        disk.disk.put("stabilizationSnowball8WeekHistory",
            history("2026-10-05",acquired,pending));
        HifzPrefs prefs=new HifzPrefs(disk);

        assertTrue("No Sunday final without weekday x7/x10 receipt",
            prefs.learningSnowballFinalUnits(sunday).isEmpty()
                && prefs.stabilizationSnowballFinalUnits(sunday).isEmpty());
        // The pending block has now completed the actual evening protocol.
        disk.disk.put("v6LearningEarnedEveningUnitsV1",units(pending));
        disk.disk.put("v6StabilizationEarnedEveningUnitsV1",units(pending));
        for(List<ConsolidationCycleEngine.Unit> finale:Arrays.asList(
            prefs.learningSnowballFinalUnits(sunday),
            prefs.stabilizationSnowballFinalUnits(sunday))) {
            assertEquals("The already-Acquis block must not be repeated",1,finale.size());
            assertEquals(pending,finale.get(0).id());
        }

        disk.disk.put("v6AcquiredCreditLineIds",units("p520-l01","p520-l02"));
        assertTrue(prefs.learningSnowballFinalUnits(sunday.plusWeeks(1)).isEmpty());
        assertTrue(prefs.stabilizationSnowballFinalUnits(sunday.plusWeeks(1)).isEmpty());
    }

    @Test public void missingOldSnapshotRecoverOnlyOldLearningPrefix() throws Exception {
        LocalDate monday=LocalDate.of(2026,10,5);
        String first=block("p520-l01"),later=block("p521-l01");
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("learningSnowballWeekAnchor","2026-10-05");
        disk.disk.put("learningSnowballUnitIds",units(first));
        HifzPrefs prefs=new HifzPrefs(disk);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("learning-"+monday,
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit(first,ConsolidationCycleEngine.Protocol.SNOWBALL));
        ConsolidationCycleEngine.Session open=engine.openSession(cycle,"frozen-monday");
        assertTrue(prefs.persistConsolidationSession(open));

        String key="v6ConsolidationLearningState";
        JSONObject older=new JSONObject(disk.getString(key,"{}"));
        older.remove("sourceSnapshot"); // Simulate upgrade from the prior deployed schema.
        disk.disk.put(key,older.toString());
        disk.disk.put("learningSnowballUnitIds",units(first,later));

        HifzPrefs reboot=new HifzPrefs(disk);
        ConsolidationCycleEngine.Session restored=reboot.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.LEARNING);
        assertEquals(open.unitIds(),restored.unitIds());
        assertTrue(reboot.persistConsolidationSession(restored));
        assertEquals("2026-10-05|"+units(first),
            reboot.frozenConsolidationSnapshot(
                ConsolidationCycleEngine.Family.LEARNING,"learning-"+monday));
        assertTrue(reboot.completeSnowballEvening(
            ConsolidationCycleEngine.Family.LEARNING,monday.toString()));
        assertTrue("The newly appended unit must still be due after closing the old session",
            reboot.snowballEveningPending(
                ConsolidationCycleEngine.Family.LEARNING,monday.plusDays(2)));
    }

    @Test public void missingOldSnapshotRecoverOnlyOldStabilizationPrefix() throws Exception {
        LocalDate tuesday=LocalDate.of(2026,10,6);
        String first=block("p520-l01"),second=block("p520-l02"),later=block("p521-l01");
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("stabilizationSnowballWeekAnchor","2026-10-05");
        disk.disk.put("stabilizationSnowballUnitIds",units(first,second));
        HifzPrefs prefs=new HifzPrefs(disk);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("stabilization-"+tuesday,
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit(first,
                ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS));
        cycle=engine.addUnit(cycle,new ConsolidationCycleEngine.Unit(second,
                ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        ConsolidationCycleEngine.Session open=engine.openSession(cycle,"frozen-tuesday");
        assertTrue(prefs.persistConsolidationSession(open));
        String key="v6ConsolidationStabilizationState";
        JSONObject older=new JSONObject(disk.getString(key,"{}"));
        older.remove("sourceSnapshot");
        disk.disk.put(key,older.toString());
        disk.disk.put("stabilizationSnowballUnitIds",units(first,second,later));

        HifzPrefs reboot=new HifzPrefs(disk);
        ConsolidationCycleEngine.Session restored=reboot.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.STABILIZATION);
        assertEquals(open.unitIds(),restored.unitIds());
        assertTrue(reboot.persistConsolidationSession(restored));
        assertEquals("2026-10-05|"+units(first,second),
            reboot.frozenConsolidationSnapshot(
                ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-"+tuesday));
        assertTrue(reboot.completeSnowballEvening(
            ConsolidationCycleEngine.Family.STABILIZATION,tuesday.toString()));
        assertTrue("New post-restart Itqan block must still be due",
            reboot.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,
                tuesday.plusDays(2)));
    }

    @Test public void oldUnrecognizableSnapshotFailsOpenForPendingRatherThanConsumeNewCorpus() throws Exception {
        LocalDate tuesday=LocalDate.of(2026,10,6);
        String obsolete=block("p521-l01"),current=block("p522-l01");
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("learningSnowballWeekAnchor","2026-10-05");
        disk.disk.put("learningSnowballUnitIds",units(obsolete));
        HifzPrefs prefs=new HifzPrefs(disk);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle("learning-"+tuesday,
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit(obsolete,ConsolidationCycleEngine.Protocol.SNOWBALL));
        assertTrue(prefs.persistConsolidationSession(engine.openSession(cycle,"frozen")));
        String key="v6ConsolidationLearningState";
        JSONObject old=new JSONObject(disk.getString(key,"{}"));
        old.remove("sourceSnapshot");
        disk.disk.put(key,old.toString());
        disk.disk.put("learningSnowballUnitIds",units(current));
        assertTrue(prefs.persistConsolidationSession(prefs.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.LEARNING)));
        String frozen=prefs.frozenConsolidationSnapshot(
            ConsolidationCycleEngine.Family.LEARNING,"learning-"+tuesday);
        assertTrue("A mismatched upgrade must not silently consume currently stored lines",
            frozen.startsWith("LEGACY_OPEN|"));
        assertTrue(prefs.completeSnowballEvening(
            ConsolidationCycleEngine.Family.LEARNING,tuesday.toString()));
        assertTrue(prefs.snowballEveningPending(
            ConsolidationCycleEngine.Family.LEARNING,tuesday.plusDays(1)));
    }
}

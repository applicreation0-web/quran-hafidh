package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * 1.17.11 regression: synthetic physical QCF line scenario at At-Tur / An-Najm.
 * No personal backup, no inferred ×7 evening receipt and no automatic Acquis.
 */
public final class ItqanOverdueEveningReceiptRecoveryTest {
    private static final LocalDate CURRENT = LocalDate.of(2026, 11, 4);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 8);
    private static final List<String> OVERDUE = Arrays.asList(
        "525:8", "525:9", "525:10", "525:11", "525:12", "525:13", "525:14");
    private static final List<String> NEW_WEEK = Arrays.asList(
        "526:0", "526:1", "526:2", "526:3", "526:4", "526:5", "526:6", "526:7");
    private static final String OLD = ConsolidationPhysicalUnitPolicy.encodeLineUnit(OVERDUE);
    private static final String NEW = ConsolidationPhysicalUnitPolicy.encodeLineUnit(NEW_WEEK);

    private static InMemoryPrefs fixture(boolean currentWeek, boolean alreadyReceipted) throws Exception {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("schema", 6);
        ArrayList<String> stabilized = new ArrayList<>(OVERDUE);
        stabilized.addAll(NEW_WEEK);
        disk.disk.put("v6StabilizedLineIds", new JSONArray(stabilized).toString());
        disk.disk.put("v6AcquiredCreditLineIds", "[]");
        disk.disk.put("v6LearnedLineIds", "[]");
        disk.disk.put("v6QuarantineLineIds", "[]");
        disk.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
        disk.disk.put("itqanRanges", "[]");
        disk.disk.put("legacyMurajaahPromotedRanges", "[]");
        disk.disk.put("promotedRanges", "[]");
        disk.disk.put("unconsolidatedPromotedRanges", "[]");
        disk.disk.put("forcedPromotedRanges", "[]");
        disk.disk.put("anchoringQueue", "[]");
        disk.disk.put("anchoringQueueInitialized", true);
        JSONArray history = new JSONArray();
        history.put(new JSONObject().put("week", "2026-10-26")
            .put("units", new JSONArray().put(OLD)));
        if (currentWeek) {
            history.put(new JSONObject().put("week", "2026-11-02")
                .put("units", new JSONArray().put(NEW)));
        }
        disk.disk.put("stabilizationSnowball8WeekHistory", history.toString());
        disk.disk.put("stabilizationSnowballWeekAnchor", "2026-11-02");
        disk.disk.put("stabilizationSnowballUnitIds",
            currentWeek ? new JSONArray().put(NEW).toString() : "[]");
        disk.disk.put("v6StabilizationEarnedEveningUnitsV1",
            alreadyReceipted ? new JSONArray().put(OLD).put(NEW).toString()
                : currentWeek ? new JSONArray().put(NEW).toString() : "[]");
        disk.disk.put("lastStabilizationSnowballEveningDate", "2026-11-03");
        disk.disk.put("itqanCursor", "53:1");
        return disk;
    }

    @Test public void overdueAtTurBlockIsOfferedAfterWeekChangeWithoutMutatingProgress() throws Exception {
        InMemoryPrefs disk = fixture(true, false);
        Map<String,Object> before = new HashMap<>(disk.disk);
        HifzPrefs prefs = new HifzPrefs(disk);
        assertTrue(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION, CURRENT));
        List<ConsolidationCycleEngine.Unit> due = prefs.stabilizedConsolidationUnits(CURRENT);
        assertEquals(2, due.size());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_CONTINUOUS, due.get(0).protocol());
        assertEquals(NEW, due.get(0).id()); // already receipted this week: only ×1
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN, due.get(1).protocol());
        assertEquals(OLD, due.get(1).id()); // older block really requires ×7
        assertEquals("Read-only catch-up planning must never grant any credit", before, disk.disk);
        assertEquals("53:1", disk.getString("itqanCursor", ""));
    }

    @Test public void aLateSevenRepCycleEarnsOnlyItsReceiptAndEnablesFutureSunday() throws Exception {
        InMemoryPrefs disk = fixture(false, false);
        HifzPrefs prefs = new HifzPrefs(disk);
        List<ConsolidationCycleEngine.Unit> due = prefs.stabilizedConsolidationUnits(CURRENT);
        assertEquals(1, due.size());
        assertEquals(OLD, due.get(0).id());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN, due.get(0).protocol());

        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "stabilization-" + CURRENT, ConsolidationCycleEngine.Family.STABILIZATION, due.get(0));
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "synthetic-11711");
        assertTrue(prefs.persistConsolidationSession(session));
        while (!session.readyToClose()) {
            session = engine.recordRepetition(session);
            assertTrue(prefs.persistConsolidationSession(session));
        }
        assertTrue(prefs.completeSnowballEvening(
            ConsolidationCycleEngine.Family.STABILIZATION, CURRENT.toString(), session));
        assertTrue(new JSONArray(disk.getString("v6StabilizationEarnedEveningUnitsV1", "[]"))
            .toString().contains(OLD));
        assertEquals("No automatic promotion to Acquis", "[]",
            disk.getString("v6AcquiredCreditLineIds", "[]"));
        assertEquals("53:1", disk.getString("itqanCursor", ""));
        assertFalse("Same date must not duplicate a receipt",
            prefs.completeSnowballEvening(
                ConsolidationCycleEngine.Family.STABILIZATION, CURRENT.toString(), session));
        assertTrue("Next Sunday can now include the verified block",
            prefs.stabilizationSnowballFinalUnits(SUNDAY).stream()
                .anyMatch(unit -> OLD.equals(unit.id())));
        assertFalse("Already receipted overdue work must not be offered again",
            prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION, CURRENT.plusDays(1)));
    }

    @Test public void aPriorWeekWithNoCurrentEnrollmentStillShowsTheOverdueCycle() throws Exception {
        InMemoryPrefs disk = fixture(false, false);
        HifzPrefs prefs = new HifzPrefs(disk);
        assertTrue(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION, CURRENT));
        assertEquals(OLD, prefs.stabilizedConsolidationUnits(CURRENT).get(0).id());
        assertEquals(1, prefs.stabilizedConsolidationUnits(CURRENT).size());
    }

    @Test public void historicalUnitMustRemainStabilizedToBeRecoverable() throws Exception {
        InMemoryPrefs disk = fixture(false, false);
        disk.disk.put("v6StabilizedLineIds", "[]");
        HifzPrefs prefs = new HifzPrefs(disk);
        assertFalse("An absent receipt is not evidence of completed or even Stabilized material",
            prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION, CURRENT));
        assertTrue(prefs.stabilizedConsolidationUnits(CURRENT).isEmpty());
    }

    @Test public void anAlreadyEarnedLateReceiptCannotBeReissued() throws Exception {
        InMemoryPrefs disk = fixture(false, true);
        HifzPrefs prefs = new HifzPrefs(disk);
        assertFalse(prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION, CURRENT));
        assertTrue(prefs.stabilizedConsolidationUnits(CURRENT).isEmpty());
    }
}

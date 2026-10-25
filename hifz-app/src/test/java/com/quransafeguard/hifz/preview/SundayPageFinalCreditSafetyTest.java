package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

/** Actual V6 closures with real QCF page IDs; no private backup is embedded. */
public final class SundayPageFinalCreditSafetyTest {
    private static GeometryRepository geometry;
    private final ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
    @BeforeClass public static void geometry() throws Exception {
        Path path = Paths.get("../app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(path)) path = Paths.get("app/src/main/assets/reader109/geometry.json");
        geometry = GeometryRepository.fromJson(Files.readString(path));
    }
    private static String stateKey(ConsolidationCycleEngine.Family family) {
        return family == ConsolidationCycleEngine.Family.LEARNING ? "v6LearnedLineIds" : "v6StabilizedLineIds";
    }
    private static String receiptKey(ConsolidationCycleEngine.Family family) {
        return family == ConsolidationCycleEngine.Family.LEARNING
            ? "v6LearningEarnedEveningUnitsV1" : "v6StabilizationEarnedEveningUnitsV1";
    }
    private boolean close(HifzPrefs prefs, ConsolidationCycleEngine.Family family,
                          ConsolidationCycleEngine.Session session) {
        return family == ConsolidationCycleEngine.Family.LEARNING
            ? prefs.completeLearningConsolidationSessionV6(session, geometry, "2026-10-25", "verified")
            : prefs.completeConsolidationSessionV6(session, geometry, "2026-10-25", "verified");
    }
    private ConsolidationCycleEngine.Session open(ConsolidationCycleEngine.Family family, List<String> units) {
        ConsolidationCycleEngine.Cycle cycle = null;
        String id = (family == ConsolidationCycleEngine.Family.LEARNING ? "learning" : "stabilization") + "-final-2026-10-25";
        for (String unit : units) {
            ConsolidationCycleEngine.Unit u = new ConsolidationCycleEngine.Unit(unit, ConsolidationCycleEngine.Protocol.SUNDAY_PAGE);
            cycle = cycle == null ? engine.startCycle(id, family, u) : engine.addUnit(cycle, u);
        }
        return engine.openSession(cycle, "exact-pages");
    }
    @Test public void oneTwoAndFourPageFinalsCreditExactlyOnceOnlyAfterAllThreePasses() throws Exception {
        for (ConsolidationCycleEngine.Family family : ConsolidationCycleEngine.Family.values()) {
            for (int pageCount : new int[]{1, 2, 4}) {
                InMemoryPrefs disk = EarnedEveningProtocolRegressionTest.disk();
                List<String> expected = new ArrayList<>(), units = new ArrayList<>();
                for (int page = 600; page < 600 + pageCount; page++) {
                    List<String> ids = geometry.lineIdsOnPage(page).subList(0, 2);
                    expected.addAll(ids);
                    units.add(ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids));
                }
                String untouched = geometry.lineIdsOnPage(599).get(0);
                List<String> initial = new ArrayList<>(expected); initial.add(untouched);
                disk.disk.put(stateKey(family), new JSONArray(initial).toString());
                disk.disk.put(receiptKey(family), new JSONArray(units).toString());
                HifzPrefs prefs = new HifzPrefs(disk);
                ConsolidationCycleEngine.Session session = open(family, units);
                assertTrue(prefs.persistConsolidationSession(session));
                // Even finishing one page cannot prematurely validate a multi-page finale.
                Map<String, ?> before = disk.getAll();
                try { close(prefs, family, session); fail("Incomplete final must be refused"); }
                catch (IllegalStateException expectedFailure) { }
                assertEquals(before, disk.getAll());
                int repetitions = 0;
                while (!session.readyToClose()) {
                    session = engine.recordRepetition(session); repetitions++;
                    assertTrue(prefs.persistConsolidationSession(session));
                    prefs = new HifzPrefs(disk);
                    session = prefs.restoreConsolidationSession(engine, family, session.cycle().cycleId());
                    assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
                }
                assertEquals(pageCount * 3, repetitions);
                assertTrue(close(prefs, family, session));
                assertEquals(new LinkedHashSet<>(expected), prefs.progressionSnapshotV6().acquired);
                assertTrue(new JSONArray(disk.getString(stateKey(family), "[]")).toString().contains(untouched));
                Map<String, ?> once = disk.getAll();
                assertFalse(close(new HifzPrefs(disk), family, session));
                assertEquals(once, disk.getAll());
            }
        }
    }
    @Test public void missingReceiptOnOnePageRefusesTheWholeFinalWithoutAnyCredit() {
        for (ConsolidationCycleEngine.Family family : ConsolidationCycleEngine.Family.values()) {
            InMemoryPrefs disk = EarnedEveningProtocolRegressionTest.disk();
            List<String> a = geometry.lineIdsOnPage(602).subList(0, 2);
            List<String> b = geometry.lineIdsOnPage(603).subList(0, 2);
            List<String> all = new ArrayList<>(a); all.addAll(b);
            String ua = ConsolidationPhysicalUnitPolicy.encodeLineUnit(a);
            String ub = ConsolidationPhysicalUnitPolicy.encodeLineUnit(b);
            disk.disk.put(stateKey(family), new JSONArray(all).toString());
            disk.disk.put(receiptKey(family), new JSONArray().put(ua).toString());
            HifzPrefs prefs = new HifzPrefs(disk);
            ConsolidationCycleEngine.Session session = open(family, Arrays.asList(ua, ub));
            while (!session.readyToClose()) session = engine.recordRepetition(session);
            assertTrue(prefs.persistConsolidationSession(session));
            Map<String, ?> before = disk.getAll();
            try { close(prefs, family, session); fail("Missing page receipt must block all credit"); }
            catch (IllegalStateException expected) { }
            assertEquals(before, disk.getAll());
        }
    }
}

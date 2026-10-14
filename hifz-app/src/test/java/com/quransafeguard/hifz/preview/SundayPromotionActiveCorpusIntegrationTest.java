package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * CP-02: end-to-end Sunday promotion over the real QCF lines, progression stores,
 * snowball engines and active/passive corpora.
 */
public final class SundayPromotionActiveCorpusIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 1, 5);
    private static final LocalDate TUESDAY = MONDAY.plusDays(1);
    private static final LocalDate SUNDAY = MONDAY.plusDays(6);
    private static GeometryRepository geometry;

    @BeforeClass public static void loadRealQcfGeometry() throws Exception {
        Path path = Paths.get("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(path)) path = Paths.get("../app/src/main/assets/reader109/geometry.json");
        geometry = GeometryRepository.fromJson(
            new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
    }

    private static InMemoryPrefs emptySchema6Disk() {
        InMemoryPrefs disk = new InMemoryPrefs();
        disk.disk.put("schema", 6);
        disk.disk.put("itqanRanges", "[{\"start\":\"1:1\",\"end\":\"1:7\"}]");
        disk.disk.put("legacyMurajaahPromotedRanges", "[]");
        disk.disk.put("promotedRanges", "[]");
        disk.disk.put("unconsolidatedPromotedRanges", "[]");
        disk.disk.put("forcedPromotedRanges", "[]");
        disk.disk.put("anchoringQueue", "[]");
        disk.disk.put("anchoringQueueInitialized", true);
        for (String key : new String[]{
                "v6LearnedLineIds", "v6StabilizedLineIds", "v6AcquiredCreditLineIds",
                "v6QuarantineLineIds", "v6LegacyPartialAcquiredLineIds"}) {
            disk.disk.put(key, "[]");
        }
        return disk;
    }

    /** Same real five-line QCF block used by the earned-evening regression suite. */
    private static List<String> physicalFiveLineUnit() {
        int start = geometry.firstLineIndex(new VerseRef(2, 75));
        ArrayList<String> ids = new ArrayList<>();
        for (int i = 0; i < PreviewConfig.SABQI_LINES; i++) ids.add(geometry.line(start + i).id);
        assertEquals(PreviewConfig.SABQI_LINES, ids.size());
        return Collections.unmodifiableList(ids);
    }

    private static VerseRef firstVerse(List<String> ids) {
        return geometry.linesForExactIds(ids).get(0).verses.get(0);
    }

    private static VerseRef lastVerse(List<String> ids) {
        List<GeometryRepository.LineMeta> lines = geometry.linesForExactIds(ids);
        List<VerseRef> verses = lines.get(lines.size() - 1).verses;
        return verses.get(verses.size() - 1);
    }

    private static VerseRef fullyCoveredVerse(List<String> ids) {
        LinkedHashSet<String> exact = new LinkedHashSet<>(ids);
        for (VerseRef verse : geometry.allVersesFullyCredited(exact)) {
            if (GeometryRepository.ordinal(verse) >= GeometryRepository.ordinal(firstVerse(ids))
                    && GeometryRepository.ordinal(verse) <= GeometryRepository.ordinal(lastVerse(ids))) {
                return verse;
            }
        }
        fail("The real QCF block must contain at least one fully covered verse");
        return null;
    }

    private static ConsolidationCycleEngine.Session readySession(
            ConsolidationCycleEngine.Family family, String cycleId,
            List<ConsolidationCycleEngine.Unit> units) {
        assertFalse("A real persisted cycle needs physical units", units.isEmpty());
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = null;
        for (ConsolidationCycleEngine.Unit unit : units) {
            cycle = cycle == null ? engine.startCycle(cycleId, family, unit) : engine.addUnit(cycle, unit);
        }
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "cp02-proof");
        while (!session.readyToClose()) session = engine.recordRepetition(session);
        return session;
    }

    private static void earnWeekdayReceipt(HifzPrefs prefs,
                                            ConsolidationCycleEngine.Family family,
                                            LocalDate day,
                                            List<ConsolidationCycleEngine.Unit> units) {
        String prefix = family == ConsolidationCycleEngine.Family.LEARNING
            ? "learning-" : "stabilization-";
        ConsolidationCycleEngine.Session evening = readySession(family, prefix + day, units);
        assertTrue(prefs.persistConsolidationSession(evening));
        assertTrue(prefs.completeSnowballEvening(family, day.toString(), evening));
    }

    private static void assertExactState(HifzPrefs prefs, List<String> ids,
                                         HifzPrefs.ProgressState expected) {
        HifzPrefs.ProgressionSnapshot snapshot = prefs.progressionSnapshotV6();
        assertEquals("No line may be skipped or credited twice", ids.size(),
            snapshot.learned.size() + snapshot.stabilized.size() + snapshot.acquired.size());
        for (String id : ids) {
            assertEquals(expected == HifzPrefs.ProgressState.LEARNED, snapshot.learned.contains(id));
            assertEquals(expected == HifzPrefs.ProgressState.STABILIZED, snapshot.stabilized.contains(id));
            assertEquals(expected == HifzPrefs.ProgressState.ACQUIRED, snapshot.acquired.contains(id));
        }
    }

    @Test public void sabqiSundayFinalBecomesAcquiredAndImmediatelyJoinsActiveAfterRestart() {
        InMemoryPrefs disk = emptySchema6Disk();
        HifzPrefs prefs = new HifzPrefs(disk);
        List<String> ids = physicalFiveLineUnit();
        VerseRef target = fullyCoveredVerse(ids);
        int startLine = geometry.linesForExactIds(ids).get(0).globalIndex;

        assertFalse(prefs.activeMurajaahCorpus().contains(target));
        assertTrue(prefs.completeSabqiBlockV6(startLine, startLine + ids.size() - 1,
            startLine + ids.size(), ids, MONDAY.toString(), "Sabqi CP-02"));
        assertExactState(prefs, ids, HifzPrefs.ProgressState.LEARNED);
        assertTrue("Appris must enter passive immediately", prefs.murajaahCorpus(geometry).contains(target));
        assertFalse("Appris must not enter active before Sunday", prefs.activeMurajaahCorpus().contains(target));

        earnWeekdayReceipt(prefs, ConsolidationCycleEngine.Family.LEARNING, MONDAY,
            prefs.learningConsolidationUnits(MONDAY));
        HifzPrefs afterEveningRestart = new HifzPrefs(disk);
        List<ConsolidationCycleEngine.Unit> finalUnits =
            afterEveningRestart.learningSnowballFinalUnits(SUNDAY);
        ConsolidationCycleEngine.Session sunday = readySession(
            ConsolidationCycleEngine.Family.LEARNING, "learning-final-" + SUNDAY, finalUnits);
        assertTrue(afterEveningRestart.completeLearningConsolidationSessionV6(
            sunday, geometry, SUNDAY.toString(), "Finale Renforcement CP-02"));

        HifzPrefs afterSundayRestart = new HifzPrefs(disk);
        assertExactState(afterSundayRestart, ids, HifzPrefs.ProgressState.ACQUIRED);
        assertTrue("Sunday-acquired Sabqi must be traversable by active review immediately",
            afterSundayRestart.activeMurajaahCorpus().contains(target));
        assertTrue("The completed Sunday session must not reappear",
            afterSundayRestart.learningSnowballFinalUnits(SUNDAY).isEmpty());

        Map<String, Object> once = new LinkedHashMap<>(disk.disk);
        assertFalse("Replaying the same Sunday final must not double-credit",
            afterSundayRestart.completeLearningConsolidationSessionV6(
                sunday, geometry, SUNDAY.toString(), "duplicate"));
        assertEquals("Idempotent replay must not mutate persistence", once, disk.disk);
    }

    @Test public void itqanSundayFinalLeavesPendingCorpusThenBecomesActiveAfterRestart() {
        InMemoryPrefs disk = emptySchema6Disk();
        List<String> ids = physicalFiveLineUnit();
        VerseRef start = firstVerse(ids);
        VerseRef end = lastVerse(ids);
        VerseRef target = fullyCoveredVerse(ids);
        String pending = "[{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}]";
        disk.disk.put("promotedRanges", pending);
        disk.disk.put("unconsolidatedPromotedRanges", pending);
        disk.disk.put("anchoringQueueInitialized", false);

        HifzPrefs prefs = new HifzPrefs(disk);
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        assertEquals("The prepared passage must be one real queue unit", 1, prefs.anchoringQueue().size());
        AnchoringQueue.Entry queued = prefs.anchoringQueue().get(0);
        assertEquals(ids, CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            GeometryRepository.parseVerse(queued.start), GeometryRepository.parseVerse(queued.end), geometry));
        assertFalse("Pending Itqan material must be excluded from active review",
            prefs.activeMurajaahCorpus().contains(target));

        assertTrue(prefs.completeStabilizationBlockV6(ids, 1, true, start, end, end,
            TUESDAY.toString(), "Itqan CP-02", Collections.emptyList()));
        assertExactState(prefs, ids, HifzPrefs.ProgressState.STABILIZED);
        assertFalse("Stabilized-but-not-finalized material must remain outside active review",
            prefs.activeMurajaahCorpus().contains(target));

        earnWeekdayReceipt(prefs, ConsolidationCycleEngine.Family.STABILIZATION, TUESDAY,
            prefs.stabilizedConsolidationUnits(TUESDAY));
        HifzPrefs afterEveningRestart = new HifzPrefs(disk);
        List<ConsolidationCycleEngine.Unit> finalUnits =
            afterEveningRestart.stabilizationSnowballFinalUnits(SUNDAY);
        ConsolidationCycleEngine.Session sunday = readySession(
            ConsolidationCycleEngine.Family.STABILIZATION,
            "stabilization-final-" + SUNDAY, finalUnits);
        assertTrue(afterEveningRestart.completeConsolidationSessionV6(
            sunday, geometry, SUNDAY.toString(), "Finale Consolidation CP-02"));

        HifzPrefs afterSundayRestart = new HifzPrefs(disk);
        assertExactState(afterSundayRestart, ids, HifzPrefs.ProgressState.ACQUIRED);
        assertTrue("Sunday-acquired Itqan must be traversable by active review immediately",
            afterSundayRestart.activeMurajaahCorpus().contains(target));
        assertTrue("No pending coverage may hide the acquired passage",
            afterSundayRestart.unconsolidatedPromotedRanges().isEmpty());
        assertTrue("The completed Sunday session must not reappear",
            afterSundayRestart.stabilizationSnowballFinalUnits(SUNDAY).isEmpty());

        Map<String, Object> once = new LinkedHashMap<>(disk.disk);
        assertFalse("Replaying the same Sunday final must not double-credit",
            afterSundayRestart.completeConsolidationSessionV6(
                sunday, geometry, SUNDAY.toString(), "duplicate"));
        assertEquals("Idempotent replay must not mutate persistence", once, disk.disk);
    }

    @Test public void activeAndPassiveCursorsRemainIndependentAcrossPersistence() {
        InMemoryPrefs disk = emptySchema6Disk();
        HifzPrefs prefs = new HifzPrefs(disk);
        VerseRef passiveStart = new VerseRef(1, 1);
        VerseRef activeStart = new VerseRef(1, 2);
        prefs.setMurajaahCursor(passiveStart);
        prefs.setActiveMurajaahCursor(activeStart);

        assertTrue(prefs.completeActiveMurajaah(new VerseRef(1, 3), MONDAY.toString(), "active"));
        HifzPrefs firstRestart = new HifzPrefs(disk);
        assertEquals(passiveStart, firstRestart.murajaahCursor());
        assertEquals(new VerseRef(1, 3), firstRestart.activeMurajaahCursor());

        assertTrue(firstRestart.completeMurajaah(new VerseRef(1, 4), TUESDAY.toString(), "passive"));
        HifzPrefs secondRestart = new HifzPrefs(disk);
        assertEquals(new VerseRef(1, 4), secondRestart.murajaahCursor());
        assertEquals(new VerseRef(1, 3), secondRestart.activeMurajaahCursor());
    }

    @Test public void sundayCadenceAndUiRoutingContainNoGeneralActiveOrPassiveSession() throws Exception {
        assertEquals(DayOfWeek.SUNDAY, SUNDAY.getDayOfWeek());
        assertEquals(com.quransafeguard.hifz.core.CadenceAction.REVISION,
            WeeklyProgramPolicy.actionFor(DayOfWeek.SUNDAY, HifzPrefs.WeeklyProgram.CLASSIC, 3));
        assertEquals(com.quransafeguard.hifz.core.CadenceAction.STABILIZATION,
            WeeklyProgramPolicy.actionFor(DayOfWeek.SUNDAY, HifzPrefs.WeeklyProgram.RECOMMENDED, 3));
        assertEquals(com.quransafeguard.hifz.core.CadenceAction.STABILIZATION,
            WeeklyProgramPolicy.actionFor(DayOfWeek.SUNDAY, HifzPrefs.WeeklyProgram.MANUAL_SEVEN, 3));

        Path plannerPath = Paths.get(
            "hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        if (!Files.exists(plannerPath)) plannerPath = Paths.get("..", plannerPath.toString());
        String planner = new String(Files.readAllBytes(plannerPath), StandardCharsets.UTF_8);
        assertTrue(planner.contains("Sunday must NEVER forecast active/passive revision"));
        assertTrue(planner.contains("Sunday cannot consume a passive cursor or schedule any general revisions."));
    }
}

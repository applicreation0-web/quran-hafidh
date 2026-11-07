package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public final class ManualRangeCreditPreservationTest {
    private static final String[] PROGRESS = {
        "v6LearnedLineIds", "v6StabilizedLineIds", "v6AcquiredCreditLineIds"
    };

    private static VerseRange range(int first, int last) {
        return new VerseRange(new VerseRef(1, first), new VerseRef(1, last));
    }

    private static Map<String, ?> protectedState(InMemoryPrefs disk) {
        Map<String, ?> state = new LinkedHashMap<>(disk.getAll());
        for (String setting : List.of("itqanRanges", "promotedRanges",
                "unconsolidatedPromotedRanges", "anchoringQueueInitialized")) state.remove(setting);
        return state;
    }

    @Test public void editsKeepAllProgressAndOpenStateByteForByte() throws Exception {
        Path source = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(source)) source = Path.of("..").resolve(source);
        GeometryRepository geometry = GeometryRepository.fromJson(Files.readString(source));
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        for (int i = 0; i < PROGRESS.length; i++) {
            disk.disk.put(PROGRESS[i], "[\"" + geometry.line(i).id + "\"]");
        }
        disk.disk.put("v6ActiveJ10LastReviewed", "{\"" + geometry.line(2).id + "\":20000}");
        disk.disk.put("v6UnknownDueLineIds", "[]");
        HifzPrefs prefs = new HifzPrefs(disk, geometry);
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Family family = ConsolidationCycleEngine.Family.STABILIZATION;
        String unitId = ConsolidationPhysicalUnitPolicy.encodeLineUnit(List.of(geometry.line(1).id));
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle("stabilization-2026-10-13", family,
            new ConsolidationCycleEngine.Unit(unitId, ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        ConsolidationCycleEngine.Session open = engine.openSession(cycle, "manual-range-fixture");
        assertTrue(prefs.persistConsolidationSession(open));
        Map<String, ?> before = protectedState(disk);

        for (List<VerseRange> ranges : List.of(List.of(range(1, 3)), List.of(range(1, 7)),
                List.of(range(4, 7)), List.of(range(2, 5)), List.of(range(2, 5)),
                List.<VerseRange>of())) {
            assertTrue(prefs.setV6StabilizationRanges(ranges, geometry));
            assertEquals(before, protectedState(disk));
        }
        assertTrue(prefs.setV6AcquiredRanges(List.of(range(1, 7)), geometry));
        assertEquals(before, protectedState(disk));
        assertTrue(prefs.setV6AcquiredRanges(List.of(), geometry));
        assertEquals(before, protectedState(disk));
        InMemoryPrefs restored = PartialFinalEightWeekStressTest.backupRoundTrip(disk);
        ConsolidationCycleEngine.Session resumed =
            new HifzPrefs(restored, geometry).restoreConsolidationSession(engine, family);
        assertNotNull(resumed);
        assertEquals(open.sessionId(), resumed.sessionId());
        assertEquals(open.unitIds(), resumed.unitIds());
    }

    @Test public void quarantineRefusalDoesNotWriteAnyPreference() throws Exception {
        Path source = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(source)) source = Path.of("..").resolve(source);
        GeometryRepository geometry = GeometryRepository.fromJson(Files.readString(source));
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        disk.disk.put("v6QuarantineLineIds", "[\"" + geometry.line(0).id + "\"]");
        HifzPrefs prefs = new HifzPrefs(disk);
        Map<String, ?> before = new LinkedHashMap<>(disk.getAll());
        assertThrows(IllegalStateException.class,
            () -> prefs.setV6StabilizationRanges(List.of(range(1, 7)), geometry));
        assertEquals(before, disk.getAll());
    }

    @Test public void aMultilineVerseEntersBothReviewPoolsOnlyWhenFullyAcquired() throws Exception {
        Path source = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(source)) source = Path.of("..").resolve(source);
        GeometryRepository geometry = GeometryRepository.fromJson(Files.readString(source));
        VerseRef target = geometry.allVersesInOrder().stream().filter(verse ->
            verse.getSurah() > 2 && geometry.firstLineIndex(verse) < geometry.lastLineIndex(verse))
            .findFirst().orElseThrow();
        InMemoryPrefs disk = PartialFinalEightWeekStressTest.empty();
        HifzPrefs prefs = new HifzPrefs(disk, geometry);
        assertTrue(prefs.setV6AcquiredRanges(List.of(range(1, 7)), geometry));
        List<String> ids = geometry.lineIdsForVerseRange(target, target);
        disk.disk.put(PROGRESS[2], new org.json.JSONArray(ids.subList(0, ids.size()-1)).toString());
        assertFalse(prefs.activeMurajaahCorpus().contains(target));
        assertFalse(prefs.murajaahCorpus(geometry).contains(target));
        disk.disk.put(PROGRESS[2], new org.json.JSONArray(ids).toString());
        Map<String, ?> before = new LinkedHashMap<>(disk.getAll());
        assertTrue(prefs.activeMurajaahCorpus().contains(target));
        assertTrue(prefs.murajaahCorpus(geometry).contains(target));
        assertEquals(before, disk.getAll());
    }
}

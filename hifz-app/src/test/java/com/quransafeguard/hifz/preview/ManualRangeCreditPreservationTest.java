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
        disk.disk.put("v6ConsolidationStabilizationState", "{\"fixture\":\"preserve OPEN bytes\"}");
        HifzPrefs prefs = new HifzPrefs(disk);
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
}

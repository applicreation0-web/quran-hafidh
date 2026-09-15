package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class StabilizationHalfPagePolicyTest {
    @Test public void eightThroughElevenLinesStayWhole() {
        for (int count = 8; count <= 11; count++) {
            assertSizes(new int[]{count}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, count)));
        }
    }

    @Test public void twelveLinesSplitSixAndSix() {
        assertSizes(new int[]{6, 6}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 12)));
    }

    @Test public void thirteenLinesUseDeterministicShorterSideFirstTieBreak() {
        assertSizes(new int[]{6, 7}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 13)));
    }

    @Test public void fourteenLinesSplitSevenAndSeven() {
        assertSizes(new int[]{7, 7}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 14)));
    }

    @Test public void fifteenLinesSplitSevenAndEight() {
        assertSizes(new int[]{7, 8}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 15)));
    }

    @Test public void fifteenLinesWithOnlyFourElevenVerseBoundaryStayWhole() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef first = new VerseRef(2, 1);
        VerseRef second = new VerseRef(2, 2);
        for (int i = 0; i < 4; i++) lines.add(line(i, 100, "a-" + i, first));
        for (int i = 4; i < 15; i++) lines.add(line(i, 100, "b-" + i, second));

        assertSizes(new int[]{15}, StabilizationHalfPagePolicy.planPage(lines));
    }

    @Test public void surahBoundaryCreatesSeparateContinuousUnits() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        lines.addAll(uniqueLines(100, 2, 6));
        int base = lines.size();
        for (int i = 0; i < 7; i++) {
            lines.add(line(base + i, 100, "s3-" + i, new VerseRef(3, i + 1)));
        }

        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(lines);
        assertSizes(new int[]{6, 7}, units);
        assertEquals(2, units.get(0).surah);
        assertEquals(3, units.get(1).surah);
    }

    @Test public void pageBoundaryInputFailsClosed() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>(uniqueLines(100, 2, 6));
        for (int i = 0; i < 6; i++) {
            lines.add(line(6 + i, 101, "p101-" + i, new VerseRef(2, 20 + i)));
        }
        expectIllegalState(() -> StabilizationHalfPagePolicy.planPage(lines));
    }

    @Test public void physicalLineCrossingSurahBoundaryFailsClosed() {
        GeometryRepository.LineMeta mixed = new GeometryRepository.LineMeta(
            0, "mixed", 100, Arrays.asList(new VerseRef(2, 1), new VerseRef(3, 1))
        );
        expectIllegalState(() -> StabilizationHalfPagePolicy.planPage(Collections.singletonList(mixed)));
    }

    @Test public void pageOverFifteenPhysicalLinesFailsClosed() {
        expectIllegalState(() -> StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 16)));
    }

    @Test public void plannedUnitsPreserveEveryPhysicalLineExactlyOnceAndInOrder() {
        List<GeometryRepository.LineMeta> lines = uniqueLines(100, 2, 15);
        List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(lines);
        ArrayList<String> expected = new ArrayList<>();
        for (GeometryRepository.LineMeta line : lines) expected.add(line.id);
        ArrayList<String> actual = new ArrayList<>();
        for (StabilizationHalfPagePolicy.Unit unit : units) actual.addAll(unit.lineIds);
        assertEquals(expected, actual);
    }

    private static void assertSizes(int[] expected, List<StabilizationHalfPagePolicy.Unit> units) {
        int[] actual = new int[units.size()];
        for (int i = 0; i < units.size(); i++) actual[i] = units.get(i).lineIds.size();
        assertArrayEquals(expected, actual);
    }

    private static List<GeometryRepository.LineMeta> uniqueLines(int page, int surah, int count) {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(line(i, page, "p" + page + "-s" + surah + "-l" + i, new VerseRef(surah, i + 1)));
        }
        return lines;
    }

    private static GeometryRepository.LineMeta line(int index, int page, String id, VerseRef verse) {
        return new GeometryRepository.LineMeta(index, id, page, Collections.singletonList(verse));
    }

    private static void expectIllegalState(Runnable action) {
        try {
            action.run();
        } catch (IllegalStateException expected) {
            return;
        }
        throw new AssertionError("Expected fail-closed IllegalStateException");
    }
}

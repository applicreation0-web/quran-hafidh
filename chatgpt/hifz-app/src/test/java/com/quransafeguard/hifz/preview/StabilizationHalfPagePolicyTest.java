package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Test public void longVerseDoesNotPreventPhysicalHalfPageSplit() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef sameVerse = new VerseRef(2, 282);
        for (int i = 0; i < 15; i++) lines.add(line(i, 100, "same-verse-" + i, sameVerse));
        assertSizes(new int[]{7, 8}, StabilizationHalfPagePolicy.planPage(lines));
    }

    @Test public void verseBoundaryAtFourElevenDoesNotForceBadSplit() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef first = new VerseRef(2, 1);
        VerseRef second = new VerseRef(2, 2);
        for (int i = 0; i < 4; i++) lines.add(line(i, 100, "a-" + i, first));
        for (int i = 4; i < 15; i++) lines.add(line(i, 100, "b-" + i, second));
        assertSizes(new int[]{7, 8}, StabilizationHalfPagePolicy.planPage(lines));
    }

    /**
     * Mirrors a real case (Al-Hujurat, page 516): the raw 7/8 target lands mid-verse, but a clean
     * verse boundary exists one line short of it. That boundary must win, even though it makes the
     * halves uneven (6+9 instead of 7+8) — the whole point is to stop cutting a verse in half.
     */
    @Test public void fifteenLinesPrefersNearbyCleanVerseBoundaryOverMidVerseSplit() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef a = new VerseRef(2, 1), b = new VerseRef(2, 2), c = new VerseRef(2, 3);
        for (int i = 0; i < 6; i++) lines.add(line(i, 100, "a-" + i, a));
        for (int i = 6; i < 10; i++) lines.add(line(i, 100, "b-" + i, b));
        for (int i = 10; i < 15; i++) lines.add(line(i, 100, "c-" + i, c));
        assertSizes(new int[]{6, 9}, StabilizationHalfPagePolicy.planPage(lines));
    }

    /** The tolerance is exactly two lines either side of the 7.5 target — this is the edge of it. */
    @Test public void fifteenLinesAcceptsACleanBoundaryExactlyTwoLinesFromTheTarget() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef a = new VerseRef(2, 1), b = new VerseRef(2, 2);
        for (int i = 0; i < 9; i++) lines.add(line(i, 100, "a-" + i, a));
        for (int i = 9; i < 15; i++) lines.add(line(i, 100, "b-" + i, b));
        assertSizes(new int[]{9, 6}, StabilizationHalfPagePolicy.planPage(lines));
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

    /**
     * A Stabilisation weekly unit (see GeometryRepository.eligibleWeeklyStabilizationUnit) can run
     * to ~1.5 pages, so crossing a physical page boundary is now expected, not fail-closed —
     * ownership of each physical line is resolved separately (CorpusLinePolicy), not by this
     * policy. A 22-line unit spanning two pages must still split three ways at 8/7/7.
     */
    @Test public void weeklyUnitMaySpanMorePhysicalPagesThanOne() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>(uniqueLines(100, 2, 15));
        int base = lines.size();
        for (int i = 0; i < 7; i++) {
            lines.add(line(base + i, 101, "p101-" + i, new VerseRef(2, 20 + i)));
        }
        assertSizes(new int[]{8, 7, 7}, StabilizationHalfPagePolicy.planPage(lines));
    }

    @Test public void physicalLineCrossingSurahBoundaryFailsClosed() {
        GeometryRepository.LineMeta mixed = new GeometryRepository.LineMeta(
            0, "mixed", 100, Arrays.asList(new VerseRef(2, 1), new VerseRef(3, 1))
        );
        expectIllegalState(() -> StabilizationHalfPagePolicy.planPage(Collections.singletonList(mixed)));
    }

    /** The weekly line budget doubled as a sane fail-closed ceiling, not a per-page limit anymore. */
    @Test public void unitOverTheSaneLineBudgetFailsClosed() {
        expectIllegalState(() -> StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 45)));
    }

    @Test public void twentyTwoLineWeeklyUnitSplitsThreeWaysAtEightSevenSeven() {
        assertSizes(new int[]{8, 7, 7}, StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 22)));
    }

    /**
     * Each three-way cut is independently pulled up to ±2 lines toward its own nearest clean verse
     * boundary. A clean boundary at position 10 pulls split1 forward from its 8-line target, and
     * another at position 13 pulls split2 back from its 15-line target — squeezing the middle block
     * down to just 3 lines instead of ~7, even though both individual shifts stayed within the
     * normal ±2 tolerance. The fix must detect the middle block starved below the 5-line floor every
     * block is otherwise guaranteed and fall back to the exact 8/7/7 target split.
     */
    @Test public void threeWayCutsThatWouldSqueezeTheMiddleBlockFallBackToTheExactTarget() {
        ArrayList<GeometryRepository.LineMeta> lines = new ArrayList<>();
        VerseRef a = new VerseRef(2, 1), b = new VerseRef(2, 2), c = new VerseRef(2, 3);
        for (int i = 0; i < 10; i++) lines.add(line(i, 100, "a-" + i, a));
        for (int i = 10; i < 13; i++) lines.add(line(i, 100, "b-" + i, b));
        for (int i = 13; i < 22; i++) lines.add(line(i, 100, "c-" + i, c));
        assertSizes(new int[]{8, 7, 7}, StabilizationHalfPagePolicy.planPage(lines));
    }

    /**
     * A long final verse can push a real weekly unit past 22 lines (up to 30, confirmed across the
     * whole corpus this session). Every resulting block must still stay ≤11 lines: Consolidation
     * later re-verifies each frozen block in isolation by re-running planPage on just its own
     * lines and requiring exactly one whole result back, which only holds within that ≤11 tier.
     */
    @Test public void anOversizedWeeklyUnitStillKeepsEveryBlockIndependentlyVerifiable() {
        List<StabilizationHalfPagePolicy.Unit> units =
            StabilizationHalfPagePolicy.planPage(uniqueLines(100, 2, 30));
        assertSizes(new int[]{10, 10, 10}, units);
        for (StabilizationHalfPagePolicy.Unit unit : units) {
            assertTrue("block of " + unit.lineIds.size() + " lines must independently re-verify as whole",
                unit.lineIds.size() <= 11);
            List<StabilizationHalfPagePolicy.Unit> reverified = StabilizationHalfPagePolicy.planPage(
                uniqueLines(100, 2, unit.lineIds.size()));
            assertEquals(1, reverified.size());
        }
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

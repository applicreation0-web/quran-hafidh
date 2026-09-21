package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PreviewConfigTest {
    @Test public void sabqiPlanIsExactly37() {
        assertEquals(37, PreviewConfig.SABQI_TOTAL_REPS);
        for (int completed = 0; completed < 37; completed++) {
            int next = completed + 1;
            int expected = next <= 15 ? 0 : next <= 20 ? 25 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Sabqi repetition " + next, expected, PreviewConfig.sabqiMaskForNextRep(completed));
        }
        assertEquals(0, PreviewConfig.sabqiMaskForNextRep(37));
        assertEquals(0, PreviewConfig.sabqiMaskForNextRep(-1));
    }

    @Test public void fullAnchoringPlanIsExactly40WithTenFinalUnaidedRepetitions() {
        assertEquals(15, PreviewConfig.ITQAN_VISIBLE_REPS_WORKING);
        assertEquals(5, PreviewConfig.ITQAN_25_REPS_WORKING);
        assertEquals(5, PreviewConfig.ITQAN_50_REPS_WORKING);
        assertEquals(5, PreviewConfig.ITQAN_75_REPS_WORKING);
        assertEquals(10, PreviewConfig.ITQAN_100_REPS_WORKING);
        assertEquals(40, PreviewConfig.ITQAN_TOTAL_REPS);
        for (int completed = 0; completed < 40; completed++) {
            int next = completed + 1;
            int expected = next <= 15 ? 0 : next <= 20 ? 25 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Itqan repetition " + next, expected, PreviewConfig.itqanMaskForNextRep(completed));
        }
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(40));
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(-1));
    }

    @Test public void lightReconstructionPlanIsExactly35WithoutQuarterMask() {
        assertEquals(35, PreviewConfig.itqanTotalReps(AnchoringQueue.ItqanProtocol.LIGHT));
        int total = PreviewConfig.ITQAN_LIGHT_VISIBLE_REPS
            + PreviewConfig.ITQAN_LIGHT_25_REPS
            + PreviewConfig.ITQAN_LIGHT_50_REPS
            + PreviewConfig.ITQAN_LIGHT_75_REPS
            + PreviewConfig.ITQAN_LIGHT_100_REPS;
        assertEquals(PreviewConfig.itqanTotalReps(AnchoringQueue.ItqanProtocol.LIGHT), total);
        assertEquals(20, PreviewConfig.ITQAN_LIGHT_VISIBLE_REPS);
        assertEquals(0, PreviewConfig.ITQAN_LIGHT_25_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_50_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_75_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_100_REPS);
        for (int completed = 0; completed < total; completed++) {
            int next = completed + 1;
            int expected = next <= 20 ? 0 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Reconstruction repetition " + next, expected,
                PreviewConfig.itqanMaskForNextRep(completed, AnchoringQueue.ItqanProtocol.LIGHT));
        }
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(total, AnchoringQueue.ItqanProtocol.LIGHT));
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(-1, AnchoringQueue.ItqanProtocol.LIGHT));
    }

    @Test public void fullPlanAlsoSatisfiesTheSharedStageInvariant() {
        int total = PreviewConfig.ITQAN_VISIBLE_REPS_WORKING
            + PreviewConfig.ITQAN_25_REPS_WORKING
            + PreviewConfig.ITQAN_50_REPS_WORKING
            + PreviewConfig.ITQAN_75_REPS_WORKING
            + PreviewConfig.ITQAN_100_REPS_WORKING;
        assertEquals(PreviewConfig.itqanTotalReps(AnchoringQueue.ItqanProtocol.FULL), total);
        for (int completed = 0; completed < total; completed++) {
            int next = completed + 1;
            int expected = next <= 15 ? 0 : next <= 20 ? 25 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Full anchoring repetition " + next, expected,
                PreviewConfig.itqanMaskForNextRep(completed, AnchoringQueue.ItqanProtocol.FULL));
        }
    }

    @Test public void onlyLastTwoHundredPercentPassagesCountForValidation() {
        assertFalse(PreviewConfig.isItqanValidationRep(32, AnchoringQueue.ItqanProtocol.LIGHT));
        assertTrue(PreviewConfig.isItqanValidationRep(33, AnchoringQueue.ItqanProtocol.LIGHT));
        assertTrue(PreviewConfig.isItqanValidationRep(34, AnchoringQueue.ItqanProtocol.LIGHT));
        assertTrue(PreviewConfig.itqanValidationPassed(1));
        assertFalse(PreviewConfig.itqanValidationPassed(2));
    }

    @Test public void recentSabqiIndexLoopsEvenWithOneBlockUntilTimerEnds() {
        assertEquals(0, PreviewConfig.nextRecentReviewIndex(0, 1));
        assertEquals(1, PreviewConfig.nextRecentReviewIndex(0, 3));
        assertEquals(2, PreviewConfig.nextRecentReviewIndex(1, 3));
        assertEquals(0, PreviewConfig.nextRecentReviewIndex(2, 3));
        assertFalse(PreviewConfig.timedSessionComplete(29L * 60_000L + 59_000L, 30));
        assertTrue(PreviewConfig.timedSessionComplete(30L * 60_000L, 30));
    }

    @Test public void fractionatedAnchoringUsesBalancedFiveLineBlocksForOneToTwentyLines() {
        for (int n = 1; n <= 20; n++) {
            int[] sizes = PreviewConfig.fractionatedBlockSizes(n);
            assertEquals((n + PreviewConfig.SABQI_LINES - 1) / PreviewConfig.SABQI_LINES, sizes.length);
            int sum = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
            for (int size : sizes) {
                assertTrue(size > 0);
                sum += size;
                min = Math.min(min, size);
                max = Math.max(max, size);
            }
            assertEquals(n, sum);
            assertTrue(max - min <= 1);
        }
        assertEquals(1, PreviewConfig.fractionatedBlockSizes(0).length);
        assertEquals(0, PreviewConfig.fractionatedBlockSizes(0)[0]);
    }

    @Test public void fractionatedAnchoringNamedSplitsAndOffsetsAreExactAndClamped() {
        assertArrayEquals(new int[]{4}, PreviewConfig.fractionatedBlockSizes(4));
        assertArrayEquals(new int[]{3,3}, PreviewConfig.fractionatedBlockSizes(6));
        assertArrayEquals(new int[]{4,3}, PreviewConfig.fractionatedBlockSizes(7));
        assertArrayEquals(new int[]{5,4}, PreviewConfig.fractionatedBlockSizes(9));
        assertArrayEquals(new int[]{5,4,4}, PreviewConfig.fractionatedBlockSizes(13));
        assertArrayEquals(new int[]{5,5,5}, PreviewConfig.fractionatedBlockSizes(15));

        assertEquals(3, PreviewConfig.fractionatedBlockCount(13));
        assertEquals(0, PreviewConfig.fractionatedBlockStart(13, -4));
        assertEquals(5, PreviewConfig.fractionatedBlockLength(13, -4));
        assertEquals(5, PreviewConfig.fractionatedBlockStart(13, 1));
        assertEquals(4, PreviewConfig.fractionatedBlockLength(13, 1));
        assertEquals(9, PreviewConfig.fractionatedBlockStart(13, 99));
        assertEquals(4, PreviewConfig.fractionatedBlockLength(13, 99));
    }

    @Test public void fractionatedAnchoringBalancesEachSurahSegmentIndependently() {
        assertArrayEquals(new int[]{5,4,4}, PreviewConfig.fractionatedBlockSizes(new int[]{13}));
        assertArrayEquals(new int[]{5,4,4}, PreviewConfig.fractionatedBlockSizes(new int[]{13}));
        assertArrayEquals(new int[]{4,5,4}, PreviewConfig.fractionatedBlockSizes(new int[]{4,9}));
        assertArrayEquals(new int[]{3,3,4,3}, PreviewConfig.fractionatedBlockSizes(new int[]{6,7}));
        assertArrayEquals(new int[]{1,4,4,4}, PreviewConfig.fractionatedBlockSizes(new int[]{1,12}));
    }

    @Test public void fractionatedAnchoringSegmentAwareOffsetsMatchConcatenatedLayout() {
        int[] segments = new int[]{6,7};
        assertEquals(4, PreviewConfig.fractionatedBlockCount(segments));
        assertEquals(0, PreviewConfig.fractionatedBlockStart(segments, 0));
        assertEquals(3, PreviewConfig.fractionatedBlockLength(segments, 0));
        assertEquals(3, PreviewConfig.fractionatedBlockStart(segments, 1));
        assertEquals(3, PreviewConfig.fractionatedBlockLength(segments, 1));
        assertEquals(6, PreviewConfig.fractionatedBlockStart(segments, 2));
        assertEquals(4, PreviewConfig.fractionatedBlockLength(segments, 2));
        assertEquals(10, PreviewConfig.fractionatedBlockStart(segments, 3));
        assertEquals(3, PreviewConfig.fractionatedBlockLength(segments, 3));
    }

    @Test public void fractionatedAnchoringAlwaysUsesTheLightThirtyFiveRepProfile() {
        assertEquals(35, PreviewConfig.ITQAN_LIGHT_TOTAL_REPS);
        for (int completed = 0; completed < 35; completed++) {
            int next = completed + 1;
            int expected = next <= 20 ? 0 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals(expected, PreviewConfig.itqanMaskForNextRep(completed, AnchoringQueue.ItqanProtocol.LIGHT));
        }
    }
}

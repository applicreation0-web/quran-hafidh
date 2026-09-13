package com.quransafeguard.hifz.preview;

import org.junit.Test;

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
        assertEquals(35, PreviewConfig.itqanTotalReps(AnchoringQueue.Protocol.LIGHT));
        int total = PreviewConfig.ITQAN_LIGHT_VISIBLE_REPS
            + PreviewConfig.ITQAN_LIGHT_25_REPS
            + PreviewConfig.ITQAN_LIGHT_50_REPS
            + PreviewConfig.ITQAN_LIGHT_75_REPS
            + PreviewConfig.ITQAN_LIGHT_100_REPS;
        assertEquals(PreviewConfig.itqanTotalReps(AnchoringQueue.Protocol.LIGHT), total);
        assertEquals(20, PreviewConfig.ITQAN_LIGHT_VISIBLE_REPS);
        assertEquals(0, PreviewConfig.ITQAN_LIGHT_25_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_50_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_75_REPS);
        assertEquals(5, PreviewConfig.ITQAN_LIGHT_100_REPS);
        for (int completed = 0; completed < total; completed++) {
            int next = completed + 1;
            int expected = next <= 20 ? 0 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Reconstruction repetition " + next, expected,
                PreviewConfig.itqanMaskForNextRep(completed, AnchoringQueue.Protocol.LIGHT));
        }
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(total, AnchoringQueue.Protocol.LIGHT));
        assertEquals(0, PreviewConfig.itqanMaskForNextRep(-1, AnchoringQueue.Protocol.LIGHT));
    }

    @Test public void fullPlanAlsoSatisfiesTheSharedStageInvariant() {
        int total = PreviewConfig.ITQAN_VISIBLE_REPS_WORKING
            + PreviewConfig.ITQAN_25_REPS_WORKING
            + PreviewConfig.ITQAN_50_REPS_WORKING
            + PreviewConfig.ITQAN_75_REPS_WORKING
            + PreviewConfig.ITQAN_100_REPS_WORKING;
        assertEquals(PreviewConfig.itqanTotalReps(AnchoringQueue.Protocol.FULL), total);
        for (int completed = 0; completed < total; completed++) {
            int next = completed + 1;
            int expected = next <= 15 ? 0 : next <= 20 ? 25 : next <= 25 ? 50 : next <= 30 ? 75 : 100;
            assertEquals("Full anchoring repetition " + next, expected,
                PreviewConfig.itqanMaskForNextRep(completed, AnchoringQueue.Protocol.FULL));
        }
    }

    @Test public void onlyLastTwoHundredPercentPassagesCountForValidation() {
        assertFalse(PreviewConfig.isItqanValidationRep(32, AnchoringQueue.Protocol.LIGHT));
        assertTrue(PreviewConfig.isItqanValidationRep(33, AnchoringQueue.Protocol.LIGHT));
        assertTrue(PreviewConfig.isItqanValidationRep(34, AnchoringQueue.Protocol.LIGHT));
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
}

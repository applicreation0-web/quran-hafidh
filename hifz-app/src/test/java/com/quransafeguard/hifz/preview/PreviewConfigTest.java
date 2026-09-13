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

    @Test public void itqanPlanIsExactly40WithTenFinalUnaidedRepetitions() {
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

    @Test public void recentSabqiIndexLoopsEvenWithOneBlockUntilTimerEnds() {
        assertEquals(0, PreviewConfig.nextRecentReviewIndex(0, 1));
        assertEquals(1, PreviewConfig.nextRecentReviewIndex(0, 3));
        assertEquals(2, PreviewConfig.nextRecentReviewIndex(1, 3));
        assertEquals(0, PreviewConfig.nextRecentReviewIndex(2, 3));
        assertFalse(PreviewConfig.timedSessionComplete(29L * 60_000L + 59_000L, 30));
        assertTrue(PreviewConfig.timedSessionComplete(30L * 60_000L, 30));
    }
}

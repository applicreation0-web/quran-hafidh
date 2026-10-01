package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class MaintenanceCoveragePolicyTest {
    private static final int TOTAL_LINES = 8820; // the real Quran physical line count

    @Test public void staysAtTheFlatThirtyMinutesBelowFifteenEquivalentJuz() {
        // 100 acquired lines out of 8820 -> equivalentJuz ~= 0.34, far below the threshold.
        assertEquals(30, MaintenanceCoveragePolicy.minutes(100, TOTAL_LINES, 7.2));
    }

    @Test public void justBelowTheFifteenJuzThresholdStillStaysAtThirty() {
        int justBelow = (int) Math.floor(15.0 / 30 * TOTAL_LINES) - 1;
        assertEquals(30, MaintenanceCoveragePolicy.minutes(justBelow, TOTAL_LINES, 7.2));
    }

    @Test public void atTheThresholdGrowsButNeverBelowTheFortyFiveMinuteFloor() {
        int atThreshold = (int) (15.0 / 30 * TOTAL_LINES); // 4410
        assertEquals(45, MaintenanceCoveragePolicy.minutes(atThreshold, TOTAL_LINES, 7.2));
    }

    @Test public void growsAndRoundsUpToACleanFifteenMinuteStepForALargeCorpus() {
        // Fully memorized at the user's own real Révision speed (7.2 s/line): required ~= 70.56
        // minutes -> rounds up to 75, well above the 45-minute floor.
        assertEquals(75, MaintenanceCoveragePolicy.minutes(TOTAL_LINES, TOTAL_LINES, 7.2));
    }

    @Test public void fasterRecallNeedsFewerMinutesForTheSameCorpus() {
        int slow = MaintenanceCoveragePolicy.minutes(TOTAL_LINES, TOTAL_LINES, 10.0);
        int fast = MaintenanceCoveragePolicy.minutes(TOTAL_LINES, TOTAL_LINES, 3.0);
        assertEquals(105, slow);
        assertEquals(45, fast);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeAcquiredLines() {
        MaintenanceCoveragePolicy.minutes(-1, TOTAL_LINES, 7.2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveTotalLines() {
        MaintenanceCoveragePolicy.minutes(100, 0, 7.2);
    }
}

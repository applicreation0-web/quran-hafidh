package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SpeedCalibrationTest {
    @Test public void insufficientSampleIsSkippedWithoutChangingCalibration() {
        SpeedCalibration.Result result = SpeedCalibration.evaluate(9.0, false, 0, 19, 600_000L);
        assertEquals(SpeedCalibration.Status.REJECTED, result.status);
        assertEquals(9.0, result.secondsPerLine, 0.0001);
        assertEquals(0, result.samples);
        assertFalse(result.calibrated);
    }

    @Test public void firstThreeQualifyingSamplesUseObservedSpeedDirectly() {
        SpeedCalibration.Result first = SpeedCalibration.evaluate(9.0, false, 0, 100, 1_000_000L);
        assertEquals(SpeedCalibration.Status.ACCEPTED, first.status);
        assertEquals(10.0, first.secondsPerLine, 0.0001);
        assertEquals(1, first.samples);
        assertTrue(first.calibrated);

        SpeedCalibration.Result third = SpeedCalibration.evaluate(11.0, true, 2, 100, 1_200_000L);
        assertEquals(SpeedCalibration.Status.ACCEPTED, third.status);
        assertEquals(12.0, third.secondsPerLine, 0.0001);
        assertEquals(3, third.samples);
    }

    @Test public void acceptanceWindowDistinguishesNormalAtypicalAndRejected() {
        assertEquals(SpeedCalibration.Status.REJECTED,
            SpeedCalibration.evaluate(9.0, false, 0, 100, 340_000L).status);
        assertEquals(SpeedCalibration.Status.ACCEPTED,
            SpeedCalibration.evaluate(9.0, false, 0, 100, 360_000L).status);
        assertEquals(SpeedCalibration.Status.ATYPICAL,
            SpeedCalibration.evaluate(9.0, false, 0, 100, 2_500_000L).status);
        assertEquals(SpeedCalibration.Status.REJECTED,
            SpeedCalibration.evaluate(9.0, false, 0, 100, 4_100_000L).status);
    }

    @Test public void afterBootstrapLargeDeviationIsRejected() {
        SpeedCalibration.Result result = SpeedCalibration.evaluate(10.0, true, 3, 100, 2_500_000L);
        assertEquals(SpeedCalibration.Status.REJECTED, result.status);
        assertEquals(10.0, result.secondsPerLine, 0.0001);
        assertEquals(3, result.samples);
    }

    @Test public void afterBootstrapUsesSeventyThirtyThenTenPercentClamp() {
        SpeedCalibration.Result result = SpeedCalibration.evaluate(10.0, true, 3, 100, 1_400_000L);
        assertEquals(SpeedCalibration.Status.ACCEPTED, result.status);
        assertEquals(11.0, result.secondsPerLine, 0.0001);
        assertEquals(4, result.samples);
    }
}

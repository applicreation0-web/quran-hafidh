package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public final class HifzCadenceTest {
    @Test public void invalidCadenceFallsBackToInitialReference() {
        assertEquals(8.0, HifzCadence.sanitizedSecondsPerLine(Double.NaN), 0.0001);
        assertEquals(8.0, HifzCadence.sanitizedSecondsPerLine(Double.POSITIVE_INFINITY), 0.0001);
        assertEquals(8.0, HifzCadence.sanitizedSecondsPerLine(0.0), 0.0001);
        assertEquals(12.5, HifzCadence.sanitizedSecondsPerLine(12.5), 0.0001);
    }

    @Test public void fixedTimeProducesDeterministicVariableQuantity() {
        assertEquals(200, HifzCadence.targetLines(30, 9.0));
        assertEquals(400, HifzCadence.targetLines(60, 9.0));
        assertEquals(200, HifzCadence.targetFiveLineCapacity(30, 9.0));
        assertEquals(65, HifzCadence.targetFiveLineCapacity(10, 9.0));
        assertEquals(30, HifzCadence.targetFiveLineCapacity(5, 9.0));
    }

    @Test public void sessionTooShortForFiveLinesReturnsZero() {
        assertEquals(0, HifzCadence.targetFiveLineCapacity(5, 500.0));
        assertEquals(0, HifzCadence.targetFiveLineCapacity(1, 20.0));
        assertEquals(0, HifzCadence.targetFiveLineCapacity(0, 9.0));
    }

    @Test public void insufficientMurajaahSamplesNeverRecalibrate() {
        assertEquals(9.0, HifzCadence.recalibrate(9.0, 19, 600_000L), 0.0001);
        assertEquals(9.0, HifzCadence.recalibrate(9.0, 100, 59_000L), 0.0001);
        assertEquals(9.0, HifzCadence.recalibrate(9.0, 0, 600_000L), 0.0001);
    }

    @Test public void recalibrationUsesSeventyThirtyBlendAndTenPercentClamp() {
        assertEquals(10.3, HifzCadence.recalibrate(10.0, 100, 1_100_000L), 0.0001);
        assertEquals(11.0, HifzCadence.recalibrate(10.0, 50, 750_000L), 0.0001);
        assertEquals(9.0, HifzCadence.recalibrate(10.0, 100, 500_000L), 0.0001);
    }

    @Test public void advisoryUsesSameCadenceAndFiveLineBlocks() {
        assertArrayEquals(new int[]{30, 65}, HifzCadence.advisoryFiveLineRange(9.0));
    }
}

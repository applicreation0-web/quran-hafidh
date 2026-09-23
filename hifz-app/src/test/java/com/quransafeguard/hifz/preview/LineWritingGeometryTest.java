package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class LineWritingGeometryTest {
    @Test public void referenceSubpathsAreConcatenatedInRealReadingOrderRightToLeft() {
        // geometry.json lists a line's cells left-to-right on screen (ascending x); cell 0 here
        // (lowest x) is the LAST word read in Arabic, cell 1 (highest x) is the FIRST word read.
        float[][][] cellsForLine = new float[][][] {
            { {1f, 1f, 2f, 2f} },   // cell 0 (leftmost on screen): read second
            { {10f, 10f, 20f, 20f} }, // cell 1 (rightmost on screen): read first
        };
        List<double[]> result = LineWritingGeometry.referenceSubpathsForLine(cellsForLine);
        assertEquals(2, result.size());
        assertArrayEquals(new double[] {10, 10, 20, 20}, result.get(0), 1e-9);
        assertArrayEquals(new double[] {1, 1, 2, 2}, result.get(1), 1e-9);
    }

    @Test public void multipleSubpathsWithinACellKeepTheirOwnOrder() {
        float[][][] cellsForLine = new float[][][] {
            { {0f, 0f}, {1f, 1f}, {2f, 2f} },
        };
        List<double[]> result = LineWritingGeometry.referenceSubpathsForLine(cellsForLine);
        assertEquals(3, result.size());
        assertArrayEquals(new double[] {0, 0}, result.get(0), 1e-9);
        assertArrayEquals(new double[] {1, 1}, result.get(1), 1e-9);
        assertArrayEquals(new double[] {2, 2}, result.get(2), 1e-9);
    }

    @Test public void emptyOrNullLineYieldsNoSubpaths() {
        assertTrue(LineWritingGeometry.referenceSubpathsForLine(new float[0][][]).isEmpty());
        assertTrue(LineWritingGeometry.referenceSubpathsForLine(null).isEmpty());
    }

    @Test public void markersWithinBandKeepsOnlyThoseInsideTheInclusiveRange() {
        float[][] pageMarkers = new float[][] {
            {10f, 5f},   // above the band
            {10f, 20f},  // inside
            {10f, 30f},  // inside, on the upper boundary
            {10f, 30.01f}, // just above the band
            {10f, 19.99f}, // just below the band
        };
        float[][] result = LineWritingGeometry.markersWithinBand(pageMarkers, 20.0, 30.0);
        assertEquals(2, result.length);
        assertArrayEquals(new float[] {10f, 20f}, result[0], 1e-4f);
        assertArrayEquals(new float[] {10f, 30f}, result[1], 1e-4f);
    }

    @Test public void nullPageMarkersYieldsAnEmptyArray() {
        assertEquals(0, LineWritingGeometry.markersWithinBand(null, 0.0, 100.0).length);
    }
}

package com.quransafeguard.writingtest;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure per-line geometry helpers for the writing exercise: reordering a line's cells into real
 * reading order, and filtering a page's ayah-end markers down to one line's vertical band. Kept
 * free of any android.* import so it can be exercised by a plain JVM unit test if this module
 * ever gets one — WritingTestActivity's own Activity/Context-dependent code can't be, directly.
 */
final class LineWritingGeometry {
    private LineWritingGeometry() {}

    /**
     * Concatenates every cell's real reference subpaths for one physical line, in reading order.
     * geometry.json lists a line's cells left-to-right on screen (ascending x), but Arabic is read
     * right-to-left, so the highest-x cell is written first — this reverses cell order to match
     * the order a real stylus would trace them in.
     */
    static List<double[]> referenceSubpathsForLine(float[][][] cellsForLine) {
        List<double[]> result = new ArrayList<>();
        for (List<double[]> word : wordsForLine(cellsForLine)) result.addAll(word);
        return result;
    }

    /**
     * Like {@link #referenceSubpathsForLine}, but keeps each cell (word) as its own group instead
     * of concatenating them — so a test can compare against just the word(s) actually written
     * rather than the whole line, which otherwise unfairly penalizes a short or partial trace.
     * Returned in the same real reading order (right-to-left).
     */
    static List<List<double[]>> wordsForLine(float[][][] cellsForLine) {
        List<List<double[]>> words = new ArrayList<>();
        if (cellsForLine == null) return words;
        for (int ci = cellsForLine.length - 1; ci >= 0; ci--) {
            List<double[]> word = new ArrayList<>();
            for (float[] subpath : cellsForLine[ci]) {
                double[] converted = new double[subpath.length];
                for (int i = 0; i < subpath.length; i++) converted[i] = subpath[i];
                word.add(converted);
            }
            words.add(word);
        }
        return words;
    }

    /** The subset of a page's ayah-end markers ({x, y} pairs) whose y falls within [top, bottom]. */
    static float[][] markersWithinBand(float[][] pageMarkers, double top, double bottom) {
        List<float[]> filtered = new ArrayList<>();
        if (pageMarkers != null) {
            for (float[] marker : pageMarkers) {
                if (marker[1] >= top && marker[1] <= bottom) filtered.add(marker);
            }
        }
        return filtered.toArray(new float[0][]);
    }
}

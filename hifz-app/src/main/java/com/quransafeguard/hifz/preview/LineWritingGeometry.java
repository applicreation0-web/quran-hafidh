package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure per-line geometry helpers for the writing exercise: reordering a line's cells into real
 * reading order, and filtering a page's ayah-end markers down to one line's vertical band. Kept
 * free of any android.* import so it can be exercised by a plain JVM unit test (this project has
 * no Robolectric, so WritingExerciseActivity's own Activity/Context-dependent code can't be).
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
        if (cellsForLine == null) return result;
        for (int ci = cellsForLine.length - 1; ci >= 0; ci--) {
            for (float[] subpath : cellsForLine[ci]) {
                double[] converted = new double[subpath.length];
                for (int i = 0; i < subpath.length; i++) converted[i] = subpath[i];
                result.add(converted);
            }
        }
        return result;
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

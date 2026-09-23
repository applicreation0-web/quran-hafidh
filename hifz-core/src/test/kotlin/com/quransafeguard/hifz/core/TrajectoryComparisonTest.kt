package com.quransafeguard.hifz.core

import com.quransafeguard.hifz.core.TrajectoryComparison.Pt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TrajectoryComparisonTest {
    // Real production output for page 1, line "1:3", cell 4 (the word "Iyyaka" plus the
    // known-tolerated decorative bleed near the bottom, per the audited default tolerance).
    private val IYYAKA_CELL: List<DoubleArray> = listOf(
        doubleArrayOf(218.27, 135.49, 217.2, 136.96, 215.92, 137.43, 215.94, 137.96, 217.4, 137.88, 219.04, 137.18, 219.68, 135.6, 218.27, 135.49),
        doubleArrayOf(219.14, 136.04, 218.35, 136.95, 217.92, 136.52, 219.14, 136.04),
        doubleArrayOf(203.29, 110.34, 201.49, 111.08, 199.4, 111.94, 198.41, 113.06, 200.06, 112.36, 202.23, 111.47, 203.29, 110.34),
        doubleArrayOf(221.86, 111.19, 221.69, 113.22, 222.07, 115.22, 222.45, 117.27, 222.83, 119.25, 223.17, 121.27, 223.39, 123.3, 223.78, 122.78, 223.76, 120.91, 223.42, 118.71, 223.05, 116.66, 222.68, 114.7, 222.3, 112.67, 221.86, 111.19),
        doubleArrayOf(205.08, 111.37, 205.04, 114.45, 205.71, 118.14, 206.36, 121.34, 205.18, 122.42, 201.18, 122.85, 198.17, 122.66, 199.38, 124.27, 203.16, 124.04, 206.38, 123.53, 206.92, 120.43, 206.3, 117.13, 205.72, 113.94, 205.08, 111.37),
        doubleArrayOf(212.98, 113.58, 213.23, 114.23, 214.58, 113.77, 216.62, 113.07, 218.45, 112.42, 219.36, 111.45, 218.16, 111.75, 216.21, 112.43, 214.24, 113.11, 212.98, 113.58),
        doubleArrayOf(208.61, 113.29, 209.11, 117.01, 209.67, 121.43, 211.68, 124.08, 215.8, 124.29, 219.63, 123.63, 220.64, 120.45, 219.43, 120.42, 218.6, 122.61, 214.32, 122.94, 210.77, 121.8, 209.89, 118.08, 209.51, 113.93, 208.61, 113.29),
        doubleArrayOf(217.84, 114.63, 217.19, 116.0, 216.33, 115.33, 215.32, 116.44, 214.71, 115.9, 215.17, 117.2, 216.48, 116.55, 217.92, 116.19, 217.84, 114.63),
        doubleArrayOf(202.33, 115.7, 201.42, 117.31, 201.89, 118.74, 202.55, 119.42, 200.87, 119.82, 200.53, 120.32, 202.33, 120.4, 203.56, 119.15, 203.05, 117.76, 202.04, 116.87, 203.18, 116.2, 202.33, 115.7),
        doubleArrayOf(222.54, 126.13, 223.06, 127.27, 223.58, 127.61, 225.43, 127.09, 224.7, 126.63, 223.08, 125.92, 224.56, 125.97, 223.94, 124.9, 222.54, 126.13),
        doubleArrayOf(218.21, 125.26, 217.09, 125.87, 215.93, 126.37, 217.04, 127.35, 218.23, 126.76, 219.4, 126.16, 218.21, 125.26),
        doubleArrayOf(226.11, 127.75, 224.09, 128.47, 222.85, 129.64, 224.76, 128.94, 226.11, 127.75),
    )

    @Test fun referencePointsSpansTheRealCellBoundsWithoutCountingSubpathJumps() {
        val pts = TrajectoryComparison.referencePoints(IYYAKA_CELL, 64)
        assertEquals(64, pts.size)
        val xs = pts.map { it.x }
        val ys = pts.map { it.y }
        // Matches the real extracted bbox for this cell (197.95-226.21, 110.34-137.96) within a
        // couple of units — sampling shouldn't invent points far outside the real geometry.
        assertTrue(xs.min() > 195.0 && xs.max() < 229.0, "x range unexpectedly wide: ${xs.min()}..${xs.max()}")
        assertTrue(ys.min() > 108.0 && ys.max() < 140.0, "y range unexpectedly wide: ${ys.min()}..${ys.max()}")
    }

    @Test fun emptyGeometryYieldsNoReferencePoints() {
        assertTrue(TrajectoryComparison.referencePoints(emptyList(), 64).isEmpty())
    }

    @Test fun resamplePolylineNeedsAtLeastTwoDistinctPoints() {
        assertNull(TrajectoryComparison.resamplePolyline(listOf(Pt(0.0, 0.0)), 10))
        assertNull(TrajectoryComparison.resamplePolyline(listOf(Pt(1.0, 1.0), Pt(1.0, 1.0)), 10))
        assertNotNull(TrajectoryComparison.resamplePolyline(listOf(Pt(0.0, 0.0), Pt(10.0, 0.0)), 10))
    }

    @Test fun dtwDistanceIsZeroForIdenticalSequences() {
        val pts = listOf(Pt(0.0, 0.0), Pt(1.0, 1.0), Pt(2.0, 0.0))
        assertEquals(0.0, TrajectoryComparison.dtwDistance(pts, pts), 1e-9)
    }

    @Test fun tracingTheRealReferenceOutlineScoresNearlyPerfect() {
        // Use a single real subpath (already a closed, continuous loop in the production data,
        // regardless of which mark it draws) so resamplePolyline's "one continuous stroke"
        // assumption actually holds — concatenating all 12 subpaths here would introduce large
        // artificial jumps between disconnected letter parts that no real single stylus stroke
        // would ever draw. The "user" traces exactly this subpath's own sample points, in order:
        // the best possible trace, must score near the top.
        val singleSubpath = listOf(IYYAKA_CELL[0])
        val ref = TrajectoryComparison.referencePoints(singleSubpath, 64)
        val score = TrajectoryComparison.score(singleSubpath, ref)
        assertNotNull(score)
        assertTrue(score!! >= 95, "expected a near-perfect score for tracing the exact outline, got $score")
    }

    @Test fun tracingTheOutlineBackwardsScoresTheSameAsForwards() {
        // A real stylus stroke can start from either end of a letter; direction alone must not be
        // penalized, since score() tries both directions of the reference and keeps the better one.
        val ref = TrajectoryComparison.referencePoints(IYYAKA_CELL, 64)
        val forwardScore = TrajectoryComparison.score(IYYAKA_CELL, ref)
        val backwardScore = TrajectoryComparison.score(IYYAKA_CELL, ref.reversed())
        assertEquals(forwardScore, backwardScore)
    }

    @Test fun aTinyScribbleFarFromTheLetterScoresLow() {
        // A short, cramped scribble nowhere near the real letter's shape/extent.
        val scribble = listOf(Pt(0.0, 0.0), Pt(1.0, 1.0), Pt(0.5, 2.0), Pt(1.5, 0.5))
        val score = TrajectoryComparison.score(IYYAKA_CELL, scribble)
        assertNotNull(score)
        assertTrue(score!! < 70, "expected a clearly low score for an unrelated scribble, got $score")
    }

    @Test fun scoreStrokesTracingEachRealSubpathAsItsOwnPenLiftScoresNearlyPerfect() {
        // A real line has multiple words, so a real user traces it as several separate strokes
        // (one pen lift between each). Retracing every one of this cell's own subpaths, each as
        // its own stroke, is the best possible multi-stroke reproduction of this exact reference.
        val userStrokes = IYYAKA_CELL.map { subpath ->
            val pts = ArrayList<Pt>(subpath.size / 2)
            var i = 0
            while (i + 1 < subpath.size) { pts.add(Pt(subpath[i], subpath[i + 1])); i += 2 }
            pts
        }
        val score = TrajectoryComparison.scoreStrokes(IYYAKA_CELL, userStrokes)
        assertNotNull(score)
        assertTrue(score!! >= 95, "expected a near-perfect score for retracing every subpath as its own stroke, got $score")
    }

    @Test fun scoreStrokesReturnsNullWithNoUsableStrokes() {
        assertNull(TrajectoryComparison.scoreStrokes(IYYAKA_CELL, emptyList()))
        assertNull(TrajectoryComparison.scoreStrokes(IYYAKA_CELL, listOf(listOf(Pt(0.0, 0.0)))))
    }
}

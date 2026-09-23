package com.quransafeguard.hifz.core

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Palier 3: compares a user's traced stylus trajectory to the real letterform's outline by
 * dynamic time warping (DTW). This is a shape/path-similarity indicator, not a certified
 * calligraphic grade, and it is blind to tashkil (short vowels) — that gap is exactly why
 * InkContentVerifier exists as a separate, complementary content check.
 *
 * Ported from the browser prototype validated across several real on-device tests this session
 * (recalibrated tolerance, forward/reverse matching, 64-point resampling); see
 * TrajectoryComparisonTest for the exact real production word-shape data used to confirm the port
 * behaves the same way.
 */
object TrajectoryComparison {
    data class Pt(val x: Double, val y: Double)

    private const val DEFAULT_SAMPLE_COUNT = 64
    private const val DECAY = 0.55

    /**
     * Samples [n] points evenly by arc length across every subpath in order. Each subpath's own
     * segment lengths accumulate into one continuous arc-length domain, but the jump from one
     * subpath's last point to the next subpath's first point does NOT count as length — this
     * mirrors how an SVG path's moveto commands are skipped by getTotalLength()/getPointAtLength(),
     * which is what the original browser prototype sampled against.
     */
    @JvmStatic
    fun referencePoints(subpaths: List<DoubleArray>, n: Int = DEFAULT_SAMPLE_COUNT): List<Pt> {
        val segStarts = ArrayList<Pt>()
        val segEnds = ArrayList<Pt>()
        val segCumStart = ArrayList<Double>()
        val segLen = ArrayList<Double>()
        var cum = 0.0
        for (sp in subpaths) {
            var prevX = Double.NaN
            var prevY = Double.NaN
            var i = 0
            while (i + 1 < sp.size) {
                val x = sp[i]
                val y = sp[i + 1]
                if (!prevX.isNaN()) {
                    val len = hypot(x - prevX, y - prevY)
                    segStarts.add(Pt(prevX, prevY))
                    segEnds.add(Pt(x, y))
                    segCumStart.add(cum)
                    segLen.add(len)
                    cum += len
                }
                prevX = x
                prevY = y
                i += 2
            }
        }
        val total = cum
        if (segStarts.isEmpty() || total <= 0.0) return emptyList()

        val out = ArrayList<Pt>(n)
        var seg = 0
        for (i in 0 until n) {
            val target = total * i / (n - 1).coerceAtLeast(1)
            while (seg < segStarts.size - 1 && segCumStart[seg] + segLen[seg] < target) seg++
            val len = segLen[seg]
            val t = if (len > 0.0) ((target - segCumStart[seg]) / len).coerceIn(0.0, 1.0) else 0.0
            val a = segStarts[seg]
            val b = segEnds[seg]
            out.add(Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
        }
        return out
    }

    /** Resamples a single continuous polyline (e.g. a user's concatenated stylus strokes) to [n] points by arc length. */
    @JvmStatic
    fun resamplePolyline(points: List<Pt>, n: Int = DEFAULT_SAMPLE_COUNT): List<Pt>? {
        if (points.size < 2) return null
        val cum = DoubleArray(points.size)
        for (i in 1 until points.size) {
            cum[i] = cum[i - 1] + hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
        }
        val total = cum[cum.size - 1]
        if (total <= 0.0) return null

        val out = ArrayList<Pt>(n)
        var j = 0
        for (i in 0 until n) {
            val target = total * i / (n - 1).coerceAtLeast(1)
            while (j < points.size - 2 && cum[j + 1] < target) j++
            val segLen = (cum[j + 1] - cum[j]).let { if (it == 0.0) 1.0 else it }
            val t = ((target - cum[j]) / segLen).coerceIn(0.0, 1.0)
            out.add(Pt(points[j].x + (points[j + 1].x - points[j].x) * t, points[j].y + (points[j + 1].y - points[j].y) * t))
        }
        return out
    }

    /** Centers on the centroid and scales by RMS distance from it, so position and size don't affect the comparison. */
    @JvmStatic
    fun normalize(points: List<Pt>): List<Pt> {
        if (points.isEmpty()) return points
        val cx = points.sumOf { it.x } / points.size
        val cy = points.sumOf { it.y } / points.size
        var rms = 0.0
        for (p in points) rms += (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy)
        rms = sqrt(rms / points.size).let { if (it == 0.0) 1.0 else it }
        return points.map { Pt((it.x - cx) / rms, (it.y - cy) / rms) }
    }

    /** Standard DTW with Euclidean point cost, normalized by path length so longer sequences aren't unfairly penalized. */
    @JvmStatic
    fun dtwDistance(a: List<Pt>, b: List<Pt>): Double {
        val n = a.size
        val m = b.size
        if (n == 0 || m == 0) return Double.POSITIVE_INFINITY
        var prev = DoubleArray(m + 1) { Double.POSITIVE_INFINITY }
        var cur = DoubleArray(m + 1)
        prev[0] = 0.0
        for (i in 1..n) {
            cur[0] = Double.POSITIVE_INFINITY
            for (j in 1..m) {
                val cost = hypot(a[i - 1].x - b[j - 1].x, a[i - 1].y - b[j - 1].y)
                cur[j] = cost + minOf(prev[j], cur[j - 1], prev[j - 1])
            }
            val tmp = prev
            prev = cur
            cur = tmp
        }
        return prev[m] / (n + m)
    }

    /**
     * 0-100 indicative score, or null when there isn't enough real geometry or enough of a user
     * trace to compare. Tries the reference outline in both directions — a legitimate stroke can
     * reasonably start from either end of a letter, and that's a direction choice, not an error.
     */
    @JvmStatic
    fun score(referenceSubpaths: List<DoubleArray>, userStrokePoints: List<Pt>, sampleCount: Int = DEFAULT_SAMPLE_COUNT): Int? {
        val refRaw = referencePoints(referenceSubpaths, sampleCount)
        if (refRaw.isEmpty()) return null
        val userRaw = resamplePolyline(userStrokePoints, sampleCount) ?: return null

        val ref = normalize(refRaw)
        val user = normalize(userRaw)
        val dist = minOf(dtwDistance(ref, user), dtwDistance(ref.reversed(), user))
        return (100 * exp(-dist / DECAY)).roundToInt().coerceIn(0, 100)
    }
}

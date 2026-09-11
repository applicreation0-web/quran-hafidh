package com.quransafeguard.hifz.domain

import com.quransafeguard.hifz.core.MurajaahSpeedPolicy
import com.quransafeguard.hifz.core.SpeedSample

class MurajaahEngine(
    private val speedPolicy: MurajaahSpeedPolicy = MurajaahSpeedPolicy()
) {
    fun plannedLines(durationMillis: Long, secondsPerLine: Double): Int =
        speedPolicy.plannedLines(durationMillis, secondsPerLine)

    fun calibratedSecondsPerLine(current: Double, samples: List<SpeedSample>): Double =
        speedPolicy.calibratedSecondsPerLine(current, samples)
}

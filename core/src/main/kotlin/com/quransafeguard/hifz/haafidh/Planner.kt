package com.quransafeguard.hifz.haafidh

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class CycleWeek(val hizbQuota: Int) {
    A(3), B(2), A_PRIME(3), B_PRIME(2)
}

object HaafidhRules {
    const val SNOWBALL_REPETITIONS = 5
    const val SUNDAY_REPETITIONS = 10
    const val REVIEW_DELAY_DAYS = 14L

    fun cycleWeek(start: LocalDate, date: LocalDate): CycleWeek {
        val monday = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val current = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weeks = ChronoUnit.WEEKS.between(monday, current)
        return CycleWeek.entries[(weeks % 4).toInt()]
    }
}

data class Snowball(
    val unitIds: List<String>,
    val repetitions: Int,
)

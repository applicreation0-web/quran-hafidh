package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * First Quran Haafidh planning layer.
 * Keeps the existing Hifz domain and adds the two-week learning loop.
 */

enum class HaafidhWeek { A, B }

enum class HaafidhUnitType { APPRENTISSAGE, CONSOLIDATION }

data class HaafidhUnit(
    val id: String,
    val type: HaafidhUnitType,
    val lines: Int
)

data class SnowballReview(
    val sourceUnits: List<String>,
    val repetitions: Int
)

object QuranHaafidhEngine {
    const val EVENING_REPETITIONS = 5
    const val SUNDAY_REPETITIONS = 10
    const val DAYS_BEFORE_SUNDAY_REVIEW = 14

    fun hizbTarget(week: HaafidhWeek): Int = when (week) {
        HaafidhWeek.A -> 3
        HaafidhWeek.B -> 2
    }

    fun eveningSnowball(day: DayOfWeek, week: HaafidhWeek): SnowballReview? {
        val index = when (day) {
            DayOfWeek.MONDAY -> 1
            DayOfWeek.WEDNESDAY -> 2
            DayOfWeek.FRIDAY -> 3
            else -> return null
        }
        val max = if (week == HaafidhWeek.A) 3 else 6
        val start = if (week == HaafidhWeek.A) 1 else 1
        val end = if (week == HaafidhWeek.A) index else 3 + index
        return SnowballReview((start..end).map { "A$it" }, EVENING_REPETITIONS)
    }

    fun sundayReviewDate(date: LocalDate): LocalDate = date.minusDays(DAYS_BEFORE_SUNDAY_REVIEW.toLong())
}

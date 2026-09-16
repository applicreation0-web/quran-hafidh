package com.quransafeguard.hifz.core

import java.time.LocalDate

/**
 * Quran Haafidh planning domain.
 * Keeps planning rules in hifz-core and avoids a parallel engine.
 */
enum class HaafidhWeek { A, B }

enum class HaafidhUnitType { SABQI, ITQAN }

data class HaafidhUnit(
    val id: String,
    val type: HaafidhUnitType,
    val start: VerseRef,
    val end: VerseRef,
    val lines: Int
)

data class SnowballItem(
    val unit: HaafidhUnit,
    val sentDate: LocalDate,
    val repetitionTarget: Int = 5
)

data class SundayReview(
    val items: List<SnowballItem>,
    val repetitionTarget: Int = 10
)

object HaafidhRules {
    const val SABQI_LINES = 5
    const val ITQAN_MIN_LINES = 7
    const val ITQAN_MAX_LINES = 8
    const val WEEK_A_HIZB = 3
    const val WEEK_B_HIZB = 2
    const val SNOWBALL_REPETITIONS = 5
    const val SUNDAY_REPETITIONS = 10
    const val SUNDAY_OFFSET_DAYS = 14

    fun weekFor(index: Int): HaafidhWeek =
        if (index % 2 == 0) HaafidhWeek.A else HaafidhWeek.B

    fun eveningHizb(week: HaafidhWeek): Int = when (week) {
        HaafidhWeek.A -> WEEK_A_HIZB
        HaafidhWeek.B -> WEEK_B_HIZB
    }
}
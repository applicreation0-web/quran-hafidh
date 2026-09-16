package com.quransafeguard.hifz.haafidh

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class SnowballRecord(
    val date: LocalDate,
    val unitIds: List<String>,
    val repetitions: Int = HaafidhRules.SNOWBALL_REPETITIONS
)

object SnowballEngine {
    fun dueOnSunday(records: List<SnowballRecord>, date: LocalDate): List<SnowballRecord> {
        return records.filter {
            ChronoUnit.DAYS.between(it.date, date) == HaafidhRules.REVIEW_DELAY_DAYS
        }.map {
            it.copy(repetitions = HaafidhRules.SUNDAY_REPETITIONS)
        }
    }
}

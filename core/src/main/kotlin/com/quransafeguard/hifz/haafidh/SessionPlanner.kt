package com.quransafeguard.hifz.haafidh

import java.time.DayOfWeek

enum class SessionType { MORNING_NEW, EVENING_SNOWBALL }

data class HaafidhSession(
    val type: SessionType,
    val unitIds: List<String>
)

object SessionPlanner {
    fun morning(day: DayOfWeek, unitId: String): HaafidhSession? {
        return when (day) {
            DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY,
            DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY ->
                HaafidhSession(SessionType.MORNING_NEW, listOf(unitId))
            else -> null
        }
    }

    fun eveningSnowball(unitIds: List<String>): HaafidhSession {
        return HaafidhSession(SessionType.EVENING_SNOWBALL, unitIds)
    }
}

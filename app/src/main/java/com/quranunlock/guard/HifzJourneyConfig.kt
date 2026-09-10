package com.applicreation0.quransafeguard

import java.time.DayOfWeek

/** User-declared active time available for each structured Hifz track. */
data class HifzAvailableMinutes(
    val sabqi: Int? = null,
    val itqan: Int? = null,
    val murajaah: Int? = null
) {
    init {
        listOfNotNull(sabqi, itqan, murajaah).forEach {
            require(it > 0) { "Configured Hifz time must be positive." }
        }
    }

    fun forTrack(track: HifzTrack): Int? = when (track) {
        HifzTrack.SABQI -> sabqi
        HifzTrack.ITQAN -> itqan
        HifzTrack.MURAJAAH -> murajaah
    }
}

/**
 * Seven-day Hifz rhythm. The approved 0.10.10 rhythm remains the default, while every
 * weekday can be reassigned explicitly by the user. A saved setup must still contain
 * Sabqi, Itqan and Murajaah at least once so no part of the structured journey is
 * silently disabled.
 */
data class HifzWeeklySchedule(
    val monday: HifzTrack = HifzTrack.SABQI,
    val tuesday: HifzTrack = HifzTrack.ITQAN,
    val wednesday: HifzTrack = HifzTrack.SABQI,
    val thursday: HifzTrack = HifzTrack.ITQAN,
    val friday: HifzTrack = HifzTrack.SABQI,
    val saturday: HifzTrack = HifzTrack.MURAJAAH,
    val sunday: HifzTrack = HifzTrack.MURAJAAH
) {
    fun trackFor(day: DayOfWeek): HifzTrack = when (day) {
        DayOfWeek.MONDAY -> monday
        DayOfWeek.TUESDAY -> tuesday
        DayOfWeek.WEDNESDAY -> wednesday
        DayOfWeek.THURSDAY -> thursday
        DayOfWeek.FRIDAY -> friday
        DayOfWeek.SATURDAY -> saturday
        DayOfWeek.SUNDAY -> sunday
    }

    fun withTrack(day: DayOfWeek, track: HifzTrack): HifzWeeklySchedule = when (day) {
        DayOfWeek.MONDAY -> copy(monday = track)
        DayOfWeek.TUESDAY -> copy(tuesday = track)
        DayOfWeek.WEDNESDAY -> copy(wednesday = track)
        DayOfWeek.THURSDAY -> copy(thursday = track)
        DayOfWeek.FRIDAY -> copy(friday = track)
        DayOfWeek.SATURDAY -> copy(saturday = track)
        DayOfWeek.SUNDAY -> copy(sunday = track)
    }

    fun containsAllTracks(): Boolean {
        val configured = DayOfWeek.values().map(::trackFor).toSet()
        return configured.containsAll(
            setOf(HifzTrack.SABQI, HifzTrack.ITQAN, HifzTrack.MURAJAAH)
        )
    }

    companion object {
        val DEFAULT = HifzWeeklySchedule()
    }
}

/** Persistent rare setup, available time, weekly rhythm and measured pace. */
data class HifzJourneyConfig(
    val bounds: HifzJourneyBounds? = null,
    val pace: HifzPaceProfile = HifzPaceProfile(),
    val availableMinutes: HifzAvailableMinutes = HifzAvailableMinutes(),
    val schedule: HifzWeeklySchedule = HifzWeeklySchedule.DEFAULT
)
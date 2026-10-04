package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzWeeklyScheduleTest {
    @Test
    fun plannerUsesConfiguredTrackInsteadOfHardcodedWeekday() {
        val custom = HifzWeeklySchedule(
            monday = HifzTrack.SABQI,
            tuesday = HifzTrack.ITQAN,
            wednesday = HifzTrack.ITQAN,
            thursday = HifzTrack.MURAJAAH,
            friday = HifzTrack.SABQI,
            saturday = HifzTrack.MURAJAAH,
            sunday = HifzTrack.SABQI
        )
        assertTrue(custom.containsAllTracks())

        val state = HifzState(
            journeyConfig = HifzJourneyConfig(
                bounds = HifzJourneyBounds(
                    sabqi = HifzVerseRange(QuranVerseRef(1, 1), QuranVerseRef(1, 7)),
                    itqan = listOf(HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 2)))
                ),
                pace = HifzPaceProfile(
                    sabqiMinutesPerPage = 10.0,
                    itqanMinutesPerPage = 5.0
                ),
                availableMinutes = HifzAvailableMinutes(
                    sabqi = 30,
                    itqan = 20,
                    murajaah = 45
                ),
                schedule = custom
            )
        )
        val geometry = HifzGeometryIndex(
            mapOf(
                1 to (1..7).map { ordinal ->
                    HifzGeometryLine(
                        ref = HifzLineRef("1:$ordinal", 1, ordinal),
                        verses = setOf(QuranVerseRef(1, ordinal))
                    )
                },
                2 to listOf(
                    HifzGeometryLine(HifzLineRef("2:1", 2, 1), setOf(QuranVerseRef(2, 1))),
                    HifzGeometryLine(HifzLineRef("2:2", 2, 2), setOf(QuranVerseRef(2, 2)))
                )
            )
        )

        val wednesday = LocalDate.of(2026, 9, 9)
        val planned = HifzDailyPlanner.planDate(state, wednesday, geometry)

        assertEquals(HifzTrack.ITQAN, planned.tasks.single().track)
        assertEquals(wednesday, planned.tasks.single().originalScheduledDate)
        assertEquals(20, planned.tasks.single().quota)
    }

    @Test
    fun murajaahDefaultPaceIsOneJuzInFortyFiveMinutes() {
        val pace = HifzPaceProfile()
        assertEquals(
            MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE,
            pace.minutesPerPage(HifzTrack.MURAJAAH) ?: error("missing default"),
            0.0
        )
        assertEquals(20, MurajaahPolicy.INITIAL_REFERENCE_PAGES)
        assertEquals(45, MurajaahPolicy.INITIAL_REFERENCE_MINUTES)
    }
}
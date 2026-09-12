package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HifzCoreTest {
    @Test fun quranCanonHas6236VersesAndCrossesSurahBoundary() {
        assertEquals(6236, QuranCanon.TOTAL_VERSES)
        assertEquals(VerseRef(3,1), QuranCanon.next(VerseRef(2,286)))
        assertNull(QuranCanon.next(VerseRef(114,6)))
    }

    @Test fun eligibleCorpusMergesAdjacencyPreservesGapsAndWraps() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(50,1), VerseRef(50,10)),
            VerseRange(VerseRef(2,1), VerseRef(2,10)),
            VerseRange(VerseRef(2,8), VerseRef(2,20)),
            VerseRange(VerseRef(50,11), VerseRef(50,12)),
            VerseRange(VerseRef(114,5), VerseRef(114,6))
        ))
        assertEquals(3, corpus.ranges.size)
        assertEquals(VerseRange(VerseRef(2,1), VerseRef(2,20)), corpus.ranges[0])
        assertEquals(VerseRange(VerseRef(50,1), VerseRef(50,12)), corpus.ranges[1])
        assertFalse(corpus.contains(VerseRef(2,21)))
        assertEquals(VerseRef(2,1), corpus.next(VerseRef(114,6)))
        assertEquals(VerseRef(50,1), corpus.next(VerseRef(2,20)))
    }

    @Test fun corpusExtensionNeverRequiresCursorTeleportation() {
        val before = EligibleCorpus.of(listOf(VerseRange(VerseRef(49,1), VerseRef(114,6))))
        val cursor = VerseRef(52,10)
        assertTrue(before.contains(cursor))
        val after = EligibleCorpus.of(before.ranges + VerseRange(VerseRef(2,75), VerseRef(2,90)))
        assertTrue(after.contains(cursor))
        assertEquals(VerseRef(52,10), cursor)
    }

    @Test fun noRetroactiveOverdueBeforeProgramStart() {
        val start = LocalDate.of(2026,9,11)
        val today = start
        assertNull(HifzSchedule.scheduled(LocalDate.of(2026,9,10), start, today))
        assertEquals(SessionType.SABQI, HifzSchedule.scheduled(start, start, today)?.type)
    }

    @Test fun weeklyScheduleMatchesFrozenWeekdays() {
        val monday = LocalDate.of(2026,9,14)
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(1).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(2).dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(3).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(4).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(5).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(6).dayOfWeek))
    }

    @Test fun tuesdayAndThursdayAddEveningMurajaahWithoutReplacingItqan() {
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(DayOfWeek.TUESDAY))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(DayOfWeek.THURSDAY))
        assertTrue(HifzSchedule.hasEveningMurajaah(DayOfWeek.TUESDAY))
        assertTrue(HifzSchedule.hasEveningMurajaah(DayOfWeek.THURSDAY))
        assertFalse(HifzSchedule.hasEveningMurajaah(DayOfWeek.MONDAY))
        assertFalse(HifzSchedule.hasEveningMurajaah(DayOfWeek.SATURDAY))
    }
}

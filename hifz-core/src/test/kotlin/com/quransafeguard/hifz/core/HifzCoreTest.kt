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

    @Test fun anchoredCycleTraversesAfterAnchorThenEarlierCorpusBeforeWrapping() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(2,1), VerseRef(2,2)),
            VerseRange(VerseRef(49,1), VerseRef(49,2)),
            VerseRange(VerseRef(114,6), VerseRef(114,6))
        ))
        val anchor = VerseRef(49,1)
        assertEquals(VerseRef(49,2), corpus.nextAnchored(VerseRef(49,1), anchor))
        assertEquals(VerseRef(114,6), corpus.nextAnchored(VerseRef(49,2), anchor))
        assertEquals(VerseRef(2,1), corpus.nextAnchored(VerseRef(114,6), anchor))
        assertEquals(VerseRef(2,2), corpus.nextAnchored(VerseRef(2,1), anchor))
        assertEquals(anchor, corpus.nextAnchored(VerseRef(2,2), anchor))
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

    @Test fun weeklyMorningAndEveningPlansMatchFrozenEngine() {
        fun assertPlan(day: DayOfWeek, recentBlocks: Int, morning: SessionKind, evening: SessionKind, morningMinutes: Int, eveningMinutes: Int) {
            val plan = HifzSchedule.planFor(day, recentBlocks)
            assertEquals(morning, plan.morning.kind)
            assertEquals(evening, plan.evening.kind)
            assertEquals(morningMinutes, plan.morning.targetMinutes)
            assertEquals(eveningMinutes, plan.evening.targetMinutes)
        }
        assertPlan(DayOfWeek.MONDAY, 0, SessionKind.SABQI_NEW, SessionKind.SABQI_TODAY_REVIEW, 0, 30)
        assertPlan(DayOfWeek.TUESDAY, 0, SessionKind.ITQAN, SessionKind.OLD_ITQAN_MURAJAAH, 60, 45)
        assertPlan(DayOfWeek.WEDNESDAY, 0, SessionKind.SABQI_NEW, SessionKind.SABQI_TODAY_REVIEW, 0, 30)
        assertPlan(DayOfWeek.THURSDAY, 0, SessionKind.ITQAN, SessionKind.OLD_ITQAN_MURAJAAH, 60, 45)
        assertPlan(DayOfWeek.FRIDAY, 0, SessionKind.SABQI_NEW, SessionKind.SABQI_TODAY_REVIEW, 0, 30)
        assertPlan(DayOfWeek.SATURDAY, 0, SessionKind.ITQAN, SessionKind.OLD_ITQAN_MURAJAAH, 60, 45)
        assertPlan(DayOfWeek.SUNDAY, 0, SessionKind.ITQAN, SessionKind.OLD_ITQAN_MURAJAAH, 60, 45)
    }

    @Test fun sundaySwitchesToConsolidationAtTheCentralizedThreshold() {
        assertEquals(36, HifzSchedule.RECENT_BLOCKS_FOR_SUNDAY_CONSOLIDATION)
        assertEquals(SessionKind.ITQAN,
            HifzSchedule.planFor(DayOfWeek.SUNDAY, 35).morning.kind)
        val switched = HifzSchedule.planFor(DayOfWeek.SUNDAY, 36)
        assertEquals(SessionKind.RECENT_SABQI_REVIEW, switched.morning.kind)
        assertEquals(30, switched.morning.targetMinutes)
        assertEquals(45, switched.evening.targetMinutes)
    }

    @Test fun sundayConsolidationStaysActiveOnceActivationWasPersisted() {
        assertEquals(SessionKind.ITQAN,
            HifzSchedule.planFor(DayOfWeek.SUNDAY, 20, false).morning.kind)
        val afterActivation = HifzSchedule.planFor(DayOfWeek.SUNDAY, 20, true)
        assertEquals(SessionKind.RECENT_SABQI_REVIEW, afterActivation.morning.kind)
        assertEquals(30, afterActivation.morning.targetMinutes)
        assertEquals(45, afterActivation.evening.targetMinutes)
    }

    @Test fun legacyMorningTypeStillMatchesScheduleForCompatibility() {
        val monday = LocalDate.of(2026,9,14)
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(1).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(2).dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(3).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(4).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(5).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(6).dayOfWeek))
    }
}

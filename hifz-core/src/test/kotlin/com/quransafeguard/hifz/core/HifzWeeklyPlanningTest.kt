package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HifzWeeklyPlanningTest {
    @Test fun canonicalCadenceUsesLearningStabilizationAndRevisionOnly() {
        assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(DayOfWeek.MONDAY))
        assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(DayOfWeek.TUESDAY))
        assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(DayOfWeek.WEDNESDAY))
        assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(DayOfWeek.THURSDAY))
        assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(DayOfWeek.FRIDAY))
        assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(DayOfWeek.SATURDAY))
        assertEquals(CadenceAction.REVISION, HifzSchedule.actionFor(DayOfWeek.SUNDAY))
    }

    @Test fun noTaskExistsBeforeProgramStart() {
        val start = LocalDate.of(2026, 9, 16)
        assertNull(HifzSchedule.nextDue(start, LocalDate.of(2026, 9, 15), emptySet()))
    }

    @Test fun oldestMissedDayIsTheOnlyAutomaticCarryover() {
        val monday = LocalDate.of(2026, 9, 14)
        val wednesday = monday.plusDays(2)

        val due = HifzSchedule.nextDue(monday, wednesday, emptySet())

        requireNotNull(due)
        assertEquals(monday, due.scheduledDate)
        assertEquals(CadenceAction.LEARNING, due.action)
        assertTrue(due.overdue)
    }

    @Test fun completingOldestDebtRevealsNextWithoutDoublingTodaysQuota() {
        val monday = LocalDate.of(2026, 9, 14)
        val wednesday = monday.plusDays(2)

        val due = HifzSchedule.nextDue(monday, wednesday, setOf(monday))

        requireNotNull(due)
        assertEquals(monday.plusDays(1), due.scheduledDate)
        assertEquals(CadenceAction.STABILIZATION, due.action)
        assertTrue(due.overdue)
    }

    @Test fun whenEarlierDaysAreDoneTodayRemainsOneNormalQuota() {
        val monday = LocalDate.of(2026, 9, 14)
        val wednesday = monday.plusDays(2)

        val due = HifzSchedule.nextDue(monday, wednesday, setOf(monday, monday.plusDays(1)))

        requireNotNull(due)
        assertEquals(wednesday, due.scheduledDate)
        assertEquals(CadenceAction.LEARNING, due.action)
        assertFalse(due.overdue)
    }

    @Test fun noWorkRemainsWhenEveryScheduledDayThroughTodayIsComplete() {
        val monday = LocalDate.of(2026, 9, 14)
        val wednesday = monday.plusDays(2)
        assertNull(HifzSchedule.nextDue(
            monday,
            wednesday,
            setOf(monday, monday.plusDays(1), wednesday)))
    }

    @Test fun completedDatesBeforeProgramStartNeverMaskRealDebt() {
        val start = LocalDate.of(2026, 9, 16)
        val today = start.plusDays(1)
        val irrelevant = setOf(start.minusDays(2), start.minusDays(1))

        val due = HifzSchedule.nextDue(start, today, irrelevant)

        requireNotNull(due)
        assertEquals(start, due.scheduledDate)
        assertEquals(CadenceAction.LEARNING, due.action)
        assertTrue(due.overdue)
    }
}

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

    /**
     * Settings can raise Apprentissage's weekly share as high as MAX_LEARNING_DAYS_PER_WEEK (6),
     * at Stabilisation's expense, all the way down to MIN_LEARNING_DAYS_PER_WEEK (0) — Sunday
     * always stays REVISION regardless.
     */
    @Test fun learningDaysPerWeekCanBeRaisedAtStabilizationsExpense() {
        assertEquals(HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK, 0)
        assertEquals(HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK, 6)
        assertEquals(HifzSchedule.DEFAULT_LEARNING_DAYS_PER_WEEK, 3)

        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

        // Zero Apprentissage days: the whole week (Sunday aside) is Stabilisation.
        for (day in weekdays) assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(day, 0))
        assertEquals(CadenceAction.REVISION, HifzSchedule.actionFor(DayOfWeek.SUNDAY, 0))

        // One Apprentissage day (Monday only): the rest of the week, Sunday aside, is Stabilisation.
        assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(DayOfWeek.MONDAY, 1))
        for (day in listOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(day, 1))
        }
        assertEquals(CadenceAction.REVISION, HifzSchedule.actionFor(DayOfWeek.SUNDAY, 1))

        // Five Apprentissage days: only one weekday (Tuesday) is left for Stabilisation.
        assertEquals(CadenceAction.STABILIZATION, HifzSchedule.actionFor(DayOfWeek.TUESDAY, 5))
        for (day in listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)) {
            assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(day, 5))
        }
        assertEquals(CadenceAction.REVISION, HifzSchedule.actionFor(DayOfWeek.SUNDAY, 5))

        // Six Apprentissage days: the whole week (Sunday aside) is Apprentissage — Stabilisation
        // simply never comes due that week, by design.
        for (day in weekdays) assertEquals(CadenceAction.LEARNING, HifzSchedule.actionFor(day, 6))
        assertEquals(CadenceAction.REVISION, HifzSchedule.actionFor(DayOfWeek.SUNDAY, 6))
    }

    /** Every configurable split still gives exactly six non-Sunday days, split as N Apprentissage / (6-N) Stabilisation. */
    @Test fun everyConfigurableSplitAccountsForAllSixNonSundayDays() {
        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
        for (n in HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK..HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK) {
            val learning = weekdays.count { HifzSchedule.actionFor(it, n) == CadenceAction.LEARNING }
            val stabilization = weekdays.count { HifzSchedule.actionFor(it, n) == CadenceAction.STABILIZATION }
            assertEquals(n, learning)
            assertEquals(6 - n, stabilization)
        }
    }

    /** Out-of-range values fail closed to the nearest valid bound (now 0..6) rather than throwing or misbehaving. */
    @Test fun outOfRangeLearningDaysPerWeekClampsToValidBounds() {
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 0), HifzSchedule.actionFor(DayOfWeek.MONDAY, -1))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 0), HifzSchedule.actionFor(DayOfWeek.MONDAY, -5))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 6), HifzSchedule.actionFor(DayOfWeek.MONDAY, 7))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 6), HifzSchedule.actionFor(DayOfWeek.MONDAY, 100))
    }

    /** nextDue must respect a non-default weekly split when computing which action is due. */
    @Test fun nextDueRespectsANonDefaultLearningDaysPerWeek() {
        val wednesday = LocalDate.of(2026, 9, 16)

        // Wednesday is LEARNING under the default 3-day split, but STABILIZATION once
        // Apprentissage is lowered to 1 day/week — a visible flip that proves nextDue actually
        // uses the param (not just actionFor's own default).
        val dueDefault = HifzSchedule.nextDue(wednesday, wednesday, emptySet())
        val dueOneLearningDay = HifzSchedule.nextDue(wednesday, wednesday, emptySet(), 1)

        requireNotNull(dueDefault)
        requireNotNull(dueOneLearningDay)
        assertEquals(CadenceAction.LEARNING, dueDefault.action)
        assertEquals(CadenceAction.STABILIZATION, dueOneLearningDay.action)
    }
}

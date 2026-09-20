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
     * Settings can raise Apprentissage's weekly share as high as MAX_LEARNING_DAYS_PER_WEEK (5),
     * at Stabilisation's expense — Sunday always stays REVISION regardless.
     */
    @Test fun learningDaysPerWeekCanBeRaisedAtStabilizationsExpense() {
        assertEquals(HifzSchedule.MIN_LEARNING_DAYS_PER_WEEK, 1)
        assertEquals(HifzSchedule.MAX_LEARNING_DAYS_PER_WEEK, 5)
        assertEquals(HifzSchedule.DEFAULT_LEARNING_DAYS_PER_WEEK, 3)

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

    /** Out-of-range values fail closed to the nearest valid bound rather than throwing or misbehaving. */
    @Test fun outOfRangeLearningDaysPerWeekClampsToValidBounds() {
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 1), HifzSchedule.actionFor(DayOfWeek.MONDAY, 0))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 1), HifzSchedule.actionFor(DayOfWeek.MONDAY, -5))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 5), HifzSchedule.actionFor(DayOfWeek.MONDAY, 6))
        assertEquals(HifzSchedule.actionFor(DayOfWeek.MONDAY, 5), HifzSchedule.actionFor(DayOfWeek.MONDAY, 100))
    }

    /** nextDue must respect a non-default weekly split when computing which action is due. */
    @Test fun nextDueRespectsANonDefaultLearningDaysPerWeek() {
        val monday = LocalDate.of(2026, 9, 14) // Monday
        val tuesday = monday.plusDays(1)

        // Tuesday is STABILIZATION under the default 3-day split, but LEARNING once Apprentissage
        // is raised to 5 days/week — a visible flip that proves nextDue actually uses the param.
        val dueDefault = HifzSchedule.nextDue(monday, tuesday, setOf(monday))
        val dueFiveLearningDays = HifzSchedule.nextDue(monday, tuesday, setOf(monday), 5)

        requireNotNull(dueDefault)
        requireNotNull(dueFiveLearningDays)
        assertEquals(CadenceAction.STABILIZATION, dueDefault.action)
        assertEquals(CadenceAction.LEARNING, dueFiveLearningDays.action)
    }
}

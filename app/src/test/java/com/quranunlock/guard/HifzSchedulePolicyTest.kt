package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HifzSchedulePolicyTest {

    @Test
    fun defaultWeekUsesApprovedSabqiItqanMurajaahRhythm() {
        assertEquals(HifzTrack.SABQI, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.MONDAY))
        assertEquals(HifzTrack.ITQAN, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.TUESDAY))
        assertEquals(HifzTrack.SABQI, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.WEDNESDAY))
        assertEquals(HifzTrack.ITQAN, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.THURSDAY))
        assertEquals(HifzTrack.SABQI, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.FRIDAY))
        assertEquals(HifzTrack.MURAJAAH, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.SATURDAY))
        assertEquals(HifzTrack.MURAJAAH, HifzSchedulePolicy.defaultTrackFor(DayOfWeek.SUNDAY))
    }

    @Test
    fun customWeekControlsReplanWithoutChangingTaskIdentityOrQuota() {
        val schedule = HifzWeeklySchedule(
            monday = HifzTrack.SABQI,
            tuesday = HifzTrack.ITQAN,
            wednesday = HifzTrack.ITQAN,
            thursday = HifzTrack.MURAJAAH,
            friday = HifzTrack.SABQI,
            saturday = HifzTrack.MURAJAAH,
            sunday = HifzTrack.SABQI
        )
        val original = LocalDate.of(2026, 9, 8)
        val task = task(
            id = "custom-itqan",
            track = HifzTrack.ITQAN,
            date = original,
            quota = 27,
            status = HifzTaskStatus.OVERDUE
        )

        val suggested = HifzSchedulePolicy.suggestReplanDate(
            task = task,
            after = original,
            schedule = schedule
        )
        assertEquals(LocalDate.of(2026, 9, 9), suggested)

        val replanned = HifzSchedulePolicy.replan(task, requireNotNull(suggested), schedule)
        assertEquals(task.id, replanned.id)
        assertEquals(27, replanned.quota)
        assertEquals(task.cursor, replanned.cursor)
        assertEquals(original, replanned.originalScheduledDate)
        assertEquals(LocalDate.of(2026, 9, 9), replanned.scheduledDate)
    }

    @Test
    fun missedTaskBecomesOverdueWithoutMovingCursorQuotaOrDates() {
        val originalDate = LocalDate.of(2026, 9, 7)
        val originalCursor = HifzCursor.page(2, 1, 5, 2)
        val task = task(
            id = "sabqi-1",
            track = HifzTrack.SABQI,
            date = originalDate,
            cursor = originalCursor,
            quota = 5
        )

        val overdue = HifzSchedulePolicy.markOverdue(task, LocalDate.of(2026, 9, 8))

        assertEquals(HifzTaskStatus.OVERDUE, overdue.status)
        assertEquals(originalDate, overdue.originalScheduledDate)
        assertEquals(originalDate, overdue.scheduledDate)
        assertEquals(originalCursor, overdue.cursor)
        assertEquals(5, overdue.quota)
    }

    @Test
    fun sameDayTaskIsNotMarkedOverdue() {
        val today = LocalDate.of(2026, 9, 9)
        val task = task("sabqi-today", HifzTrack.SABQI, today)

        assertEquals(task, HifzSchedulePolicy.markOverdue(task, today))
    }

    @Test
    fun explicitReplanPreservesOriginCursorQuotaAndTrack() {
        val original = LocalDate.of(2026, 9, 8)
        val task = task(
            id = "itqan-1",
            track = HifzTrack.ITQAN,
            date = original,
            cursor = HifzCursor.page(67, 1, 4, 562),
            quota = 4,
            status = HifzTaskStatus.OVERDUE
        )

        val replanned = HifzSchedulePolicy.replan(task, LocalDate.of(2026, 9, 10))

        assertEquals(HifzTaskStatus.PLANNED, replanned.status)
        assertEquals(original, replanned.originalScheduledDate)
        assertEquals(LocalDate.of(2026, 9, 10), replanned.scheduledDate)
        assertEquals(task.cursor, replanned.cursor)
        assertEquals(task.quota, replanned.quota)
        assertEquals(task.track, replanned.track)
    }

    @Test
    fun suggestReplanUsesNextAvailableDayForSameTrackWithoutMutatingTask() {
        val task = task(
            id = "itqan-missed",
            track = HifzTrack.ITQAN,
            date = LocalDate.of(2026, 9, 8),
            status = HifzTaskStatus.OVERDUE
        )

        val suggested = HifzSchedulePolicy.suggestReplanDate(
            task = task,
            after = LocalDate.of(2026, 9, 9),
            available = { it != LocalDate.of(2026, 9, 10) }
        )

        assertEquals(LocalDate.of(2026, 9, 15), suggested)
        assertEquals(LocalDate.of(2026, 9, 8), task.scheduledDate)
        assertEquals(HifzTaskStatus.OVERDUE, task.status)
    }

    @Test
    fun backlogAwareSuggestionSkipsOccupiedHifzDates() {
        val missed = task(
            id = "itqan-missed",
            track = HifzTrack.ITQAN,
            date = LocalDate.of(2026, 9, 8),
            status = HifzTaskStatus.OVERDUE
        )
        val occupiedThursday = task(
            id = "itqan-thursday",
            track = HifzTrack.ITQAN,
            date = LocalDate.of(2026, 9, 10)
        )
        val occupiedTuesday = task(
            id = "itqan-tuesday",
            track = HifzTrack.ITQAN,
            date = LocalDate.of(2026, 9, 15)
        )

        val suggested = HifzSchedulePolicy.suggestReplanDate(
            task = missed,
            after = LocalDate.of(2026, 9, 9),
            existingTasks = listOf(missed, occupiedThursday, occupiedTuesday)
        )

        assertEquals(LocalDate.of(2026, 9, 17), suggested)
        assertEquals(LocalDate.of(2026, 9, 8), missed.scheduledDate)
        assertEquals(5, missed.quota)
    }

    @Test
    fun overdueTaskHasPriorityButDoesNotDoubleTodaysQuota() {
        val today = LocalDate.of(2026, 9, 9)
        val missedCursor = HifzCursor.page(1, 1, 4, 1)
        val missed = task(
            id = "missed",
            track = HifzTrack.ITQAN,
            date = LocalDate.of(2026, 9, 8),
            cursor = missedCursor,
            quota = 4
        )
        val scheduledToday = task(
            id = "today",
            track = HifzTrack.SABQI,
            date = today,
            cursor = HifzCursor.page(1, 5, 7, 1),
            quota = 3
        )

        val next = HifzSchedulePolicy.nextTask(today, listOf(scheduledToday, missed))

        assertEquals("missed", next?.id)
        assertEquals(4, next?.quota)
        assertEquals(missedCursor, next?.cursor)
        assertEquals(HifzTaskStatus.PLANNED, scheduledToday.status)
        assertEquals(3, scheduledToday.quota)
    }

    @Test
    fun oldestOverdueTaskWinsDeterministically() {
        val today = LocalDate.of(2026, 9, 9)
        val older = task("older", HifzTrack.SABQI, LocalDate.of(2026, 9, 5))
        val newer = task("newer", HifzTrack.ITQAN, LocalDate.of(2026, 9, 8))

        assertEquals("older", HifzSchedulePolicy.nextTask(today, listOf(newer, older))?.id)
    }

    @Test
    fun completedTasksAreNeverProposedAgain() {
        val today = LocalDate.of(2026, 9, 9)
        val done = task(
            id = "done",
            track = HifzTrack.SABQI,
            date = LocalDate.of(2026, 9, 8),
            status = HifzTaskStatus.COMPLETED
        )

        assertNull(HifzSchedulePolicy.nextTask(today, listOf(done)))
        assertNull(HifzSchedulePolicy.suggestReplanDate(done, today))
    }

    private fun task(
        id: String,
        track: HifzTrack,
        date: LocalDate,
        cursor: HifzCursor = HifzCursor.page(1, 1, 5, 1),
        quota: Int = 5,
        status: HifzTaskStatus = HifzTaskStatus.PLANNED
    ) = HifzTask(
        id = id,
        track = track,
        originalScheduledDate = date,
        scheduledDate = date,
        cursor = cursor,
        quota = quota,
        status = status
    )
}
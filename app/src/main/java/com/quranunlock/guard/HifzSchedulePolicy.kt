package com.applicreation0.quransafeguard

import java.time.DayOfWeek
import java.time.LocalDate

enum class HifzTrack {
    SABQI,
    ITQAN,
    MURAJAAH
}

enum class HifzTaskStatus {
    PLANNED,
    OVERDUE,
    COMPLETED
}

/**
 * Stable scheduling state for the structured Hifz journey.
 *
 * This state is deliberately independent from free Memorisation reader sessions.
 * A missed task keeps its identity, original date, typed Quran cursor and quota.
 * Replanning may change only the current scheduled date and status.
 */
data class HifzTask(
    val id: String,
    val track: HifzTrack,
    val originalScheduledDate: LocalDate,
    val scheduledDate: LocalDate = originalScheduledDate,
    val cursor: HifzCursor,
    val quota: Int,
    val status: HifzTaskStatus = HifzTaskStatus.PLANNED
) {
    init {
        require(id.isNotBlank()) { "A Hifz task id is required." }
        require(quota > 0) { "A Hifz quota must be positive." }
    }
}

object HifzSchedulePolicy {
    private const val MAX_REPLAN_SEARCH_DAYS = 56L

    /** Fixed default rhythm approved for 0.10.10. */
    fun defaultTrackFor(day: DayOfWeek): HifzTrack = when (day) {
        DayOfWeek.MONDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.FRIDAY -> HifzTrack.SABQI

        DayOfWeek.TUESDAY,
        DayOfWeek.THURSDAY -> HifzTrack.ITQAN

        DayOfWeek.SATURDAY,
        DayOfWeek.SUNDAY -> HifzTrack.MURAJAAH
    }

    /**
     * Missing a planned day is not a failure and never moves the task silently.
     * Only the status changes; cursor, quota and both dates remain untouched.
     */
    fun markOverdue(task: HifzTask, today: LocalDate): HifzTask {
        if (task.status != HifzTaskStatus.PLANNED) return task
        if (!task.scheduledDate.isBefore(today)) return task
        return task.copy(status = HifzTaskStatus.OVERDUE)
    }

    /**
     * Replanning is always explicit. It preserves identity, original date, cursor,
     * quota and track. A completed task cannot be moved back into the queue.
     */
    fun replan(task: HifzTask, newDate: LocalDate): HifzTask {
        require(task.status != HifzTaskStatus.COMPLETED) {
            "A completed Hifz task cannot be replanned."
        }
        return task.copy(
            scheduledDate = newDate,
            status = HifzTaskStatus.PLANNED
        )
    }

    /**
     * Suggests the next available day dedicated to the same Hifz track.
     * This function never mutates or replans the task by itself.
     */
    fun suggestReplanDate(
        task: HifzTask,
        after: LocalDate,
        available: (LocalDate) -> Boolean = { true }
    ): LocalDate? {
        if (task.status == HifzTaskStatus.COMPLETED) return null
        for (offset in 1L..MAX_REPLAN_SEARCH_DAYS) {
            val candidate = after.plusDays(offset)
            if (defaultTrackFor(candidate.dayOfWeek) == task.track && available(candidate)) {
                return candidate
            }
        }
        return null
    }

    /**
     * Default backlog-aware suggestion. Any non-completed Hifz task already occupying
     * a candidate date makes that date unavailable, so a missed task is never stacked
     * onto another scheduled task as an implicit catch-up burden.
     */
    fun suggestReplanDate(
        task: HifzTask,
        after: LocalDate,
        existingTasks: List<HifzTask>
    ): LocalDate? = suggestReplanDate(task, after) { candidate ->
        existingTasks.none { existing ->
            existing.id != task.id &&
                existing.status != HifzTaskStatus.COMPLETED &&
                existing.scheduledDate == candidate
        }
    }

    fun complete(task: HifzTask): HifzTask = task.copy(status = HifzTaskStatus.COMPLETED)

    /**
     * Return at most one task to work on. This prevents automatic quota doubling.
     * Oldest overdue work has priority; otherwise today's planned task is proposed.
     * Nothing is rescheduled by this selection.
     */
    fun nextTask(today: LocalDate, tasks: List<HifzTask>): HifzTask? {
        val normalized = tasks.map { markOverdue(it, today) }

        return normalized
            .asSequence()
            .filter { it.status == HifzTaskStatus.OVERDUE }
            .sortedWith(compareBy<HifzTask> { it.scheduledDate }.thenBy { it.id })
            .firstOrNull()
            ?: normalized
                .asSequence()
                .filter {
                    it.status == HifzTaskStatus.PLANNED &&
                        it.scheduledDate == today
                }
                .sortedBy { it.id }
                .firstOrNull()
    }
}

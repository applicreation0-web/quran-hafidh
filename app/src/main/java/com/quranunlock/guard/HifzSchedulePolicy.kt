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
 * Scheduling state for the structured Hifz journey.
 *
 * This state is deliberately independent from the free Memorisation reader sessions.
 * A missed task keeps its original date, cursor and quota until the user explicitly
 * replans or completes it.
 */
data class HifzTask(
    val id: String,
    val track: HifzTrack,
    val scheduledDate: LocalDate,
    val cursor: String,
    val quota: Int,
    val status: HifzTaskStatus = HifzTaskStatus.PLANNED
) {
    init {
        require(id.isNotBlank()) { "A Hifz task id is required." }
        require(cursor.isNotBlank()) { "A Hifz cursor is required." }
        require(quota > 0) { "A Hifz quota must be positive." }
    }
}

object HifzSchedulePolicy {
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
     * Only the status changes; cursor, quota and scheduled date remain untouched.
     */
    fun markOverdue(task: HifzTask, today: LocalDate): HifzTask {
        if (task.status != HifzTaskStatus.PLANNED) return task
        if (!task.scheduledDate.isBefore(today)) return task
        return task.copy(status = HifzTaskStatus.OVERDUE)
    }

    /**
     * Replanning is always explicit. It preserves the exact work cursor and quota.
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

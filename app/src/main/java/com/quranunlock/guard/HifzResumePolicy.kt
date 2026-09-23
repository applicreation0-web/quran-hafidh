package com.applicreation0.quransafeguard

import java.time.LocalDate

data class HifzResumeSnapshot(
    val state: HifzState,
    val nextTask: HifzTask?
)

/**
 * Pure restart/day-rollover policy. It may only normalize PLANNED tasks into OVERDUE.
 * It never changes dates, typed Quran cursors, quotas or training progress and never
 * performs an implicit replan.
 */
object HifzResumePolicy {
    fun resume(state: HifzState, today: LocalDate): HifzResumeSnapshot {
        val normalizedTasks = state.tasks.map { HifzSchedulePolicy.markOverdue(it, today) }
        val normalizedState = if (normalizedTasks == state.tasks) {
            state
        } else {
            state.copy(tasks = normalizedTasks)
        }
        return HifzResumeSnapshot(
            state = normalizedState,
            nextTask = HifzSchedulePolicy.nextTask(today, normalizedTasks)
        )
    }
}

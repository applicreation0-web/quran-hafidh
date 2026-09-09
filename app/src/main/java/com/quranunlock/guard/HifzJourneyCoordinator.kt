package com.applicreation0.quransafeguard

/**
 * Coordinates the boundary between training progress and the Hifz schedule.
 *
 * Training completion and schedule completion are intentionally two distinct states.
 * The task is marked COMPLETED only through this explicit transition after the training
 * protocol has finished. This prevents a reader callback or counter from silently moving
 * the Hifz cursor/calendar.
 */
object HifzJourneyCoordinator {

    fun canCompleteTask(task: HifzTask, progress: HifzTaskProgress?): Boolean =
        task.status != HifzTaskStatus.COMPLETED &&
            progress?.taskId == task.id &&
            progress.completed

    fun completeTask(task: HifzTask, progress: HifzTaskProgress): HifzTask {
        require(canCompleteTask(task, progress)) {
            "The Hifz training protocol must be completed before the task can be completed."
        }
        return HifzSchedulePolicy.complete(task)
    }

    /**
     * Returns a complete replacement state. Persistence can commit this state atomically.
     */
    fun completeTask(state: HifzState, taskId: String): HifzState {
        val taskIndex = state.tasks.indexOfFirst { it.id == taskId }
        require(taskIndex >= 0) { "Unknown Hifz task: $taskId" }
        val task = state.tasks[taskIndex]
        val progress = state.progressByTask[taskId]
            ?: error("Missing Hifz training progress for $taskId")
        val completedTask = completeTask(task, progress)
        val tasks = state.tasks.toMutableList()
        tasks[taskIndex] = completedTask
        return state.copy(tasks = tasks)
    }
}

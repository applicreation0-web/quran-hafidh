package com.applicreation0.quransafeguard

/**
 * Coordinates the boundary between training progress and the Hifz schedule.
 *
 * Training completion and schedule completion are intentionally two distinct states.
 * A task becomes COMPLETED only through this explicit transition. A completed Sabqi task
 * may atomically grow the eligible Itqan corpus, but existing Itqan/Murajaah task history
 * and cursors are never rewritten or teleported.
 */
object HifzJourneyCoordinator {

    fun canCompleteTask(
        task: HifzTask,
        progress: HifzTaskProgress?,
        segmentCount: Int = 1
    ): Boolean =
        task.status != HifzTaskStatus.COMPLETED &&
            progress != null &&
            progress.completed &&
            HifzTrainingEngine.isSemanticallyCoherent(task, progress, segmentCount)

    fun completeTask(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTask {
        require(canCompleteTask(task, progress, segmentCount)) {
            "The complete coherent Hifz training protocol is required before task completion."
        }
        return HifzSchedulePolicy.complete(task)
    }

    /**
     * Returns one complete replacement state so persistence can commit task completion
     * and eligible-corpus promotion together.
     */
    fun completeTask(
        state: HifzState,
        taskId: String,
        segmentCount: Int = 1
    ): HifzState {
        val taskIndex = state.tasks.indexOfFirst { it.id == taskId }
        require(taskIndex >= 0) { "Unknown Hifz task: $taskId" }
        val task = state.tasks[taskIndex]
        val progress = state.progressByTask[taskId]
            ?: error("Missing Hifz training progress for $taskId")
        val completedTask = completeTask(task, progress, segmentCount)
        val tasks = state.tasks.toMutableList()
        tasks[taskIndex] = completedTask

        val config = if (task.track == HifzTrack.SABQI) {
            val bounds = state.journeyConfig.bounds
            if (bounds == null) state.journeyConfig
            else state.journeyConfig.copy(
                bounds = HifzPromotionPolicy.promoteCompletedSabqi(bounds, task.cursor)
            )
        } else state.journeyConfig

        return state.copy(tasks = tasks, journeyConfig = config)
    }
}

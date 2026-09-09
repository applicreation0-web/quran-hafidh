package com.applicreation0.quransafeguard

/**
 * Pure transition engine for a structured Hifz task.
 *
 * The scheduler owns when/what must be studied. This engine owns only progress inside
 * that task. It never changes dates, cursor or quota and never reads reader109 state.
 */
object HifzTrainingEngine {

    fun initial(task: HifzTask): HifzTaskProgress {
        val steps = HifzTrainingPolicy.stepsFor(task.track)
        require(steps.isNotEmpty()) { "No structured training protocol is defined for ${task.track}." }
        return HifzTaskProgress(
            taskId = task.id,
            stepIndex = 0,
            stepProgress = HifzStepProgress(stepId = steps.first().id),
            completed = false
        )
    }

    fun currentStep(task: HifzTask, progress: HifzTaskProgress): HifzTrainingStep? {
        require(progress.taskId == task.id) { "Hifz progress belongs to another task." }
        val steps = HifzTrainingPolicy.stepsFor(task.track)
        if (progress.completed) return null
        return steps.getOrNull(progress.stepIndex)
    }

    fun reveal(task: HifzTask, progress: HifzTaskProgress): HifzTaskProgress {
        val step = requireCurrent(task, progress)
        val stepProgress = normalizeStepProgress(progress, step)
        return progress.copy(
            stepProgress = HifzTrainingProgressPolicy.reveal(stepProgress)
        )
    }

    fun attempt(
        task: HifzTask,
        progress: HifzTaskProgress,
        correct: Boolean
    ): HifzTaskProgress {
        val step = requireCurrent(task, progress)
        val stepProgress = normalizeStepProgress(progress, step)
        return progress.copy(
            stepProgress = HifzTrainingProgressPolicy.attempt(stepProgress, correct)
        )
    }

    /**
     * Advances only when the current step contract is satisfied. A reveal therefore
     * cannot advance the task, and a final test cannot be bypassed by raw counters.
     */
    fun advanceIfValid(task: HifzTask, progress: HifzTaskProgress): HifzTaskProgress {
        val step = requireCurrent(task, progress)
        val stepProgress = normalizeStepProgress(progress, step)
        if (!HifzTrainingProgressPolicy.canValidate(step, stepProgress)) return progress

        val steps = HifzTrainingPolicy.stepsFor(task.track)
        val nextIndex = progress.stepIndex + 1
        if (nextIndex >= steps.size) {
            return progress.copy(
                stepIndex = nextIndex,
                stepProgress = null,
                completed = true
            )
        }

        return progress.copy(
            stepIndex = nextIndex,
            stepProgress = HifzStepProgress(stepId = steps[nextIndex].id),
            completed = false
        )
    }

    private fun requireCurrent(
        task: HifzTask,
        progress: HifzTaskProgress
    ): HifzTrainingStep = currentStep(task, progress)
        ?: error("The Hifz task has no active training step.")

    private fun normalizeStepProgress(
        progress: HifzTaskProgress,
        step: HifzTrainingStep
    ): HifzStepProgress {
        val existing = progress.stepProgress
        return if (existing?.stepId == step.id) {
            existing
        } else {
            HifzStepProgress(stepId = step.id)
        }
    }
}

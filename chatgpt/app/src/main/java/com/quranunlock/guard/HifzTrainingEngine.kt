package com.applicreation0.quransafeguard

/**
 * Pure transition engine for one structured Hifz task.
 *
 * A scheduled Sabqi task is one exact five-real-line block. Rendering may span more
 * than one physical page, but that must never multiply the pedagogical protocol: the
 * whole block receives exactly one 37-repetition sequence. Itqan and Murajaah are also
 * task-wide. Audio is controlled outside this engine and can never increment progress.
 */
object HifzTrainingEngine {

    fun initial(task: HifzTask, @Suppress("UNUSED_PARAMETER") segmentCount: Int = 1): HifzTaskProgress {
        val steps = HifzTrainingPolicy.stepsFor(task.track)
        require(steps.isNotEmpty()) { "No structured training protocol is defined for ${task.track}." }
        return HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 0,
            stepIndex = 0,
            stepProgress = HifzStepProgress(stepId = steps.first().id),
            completed = false
        )
    }

    fun currentStep(
        task: HifzTask,
        progress: HifzTaskProgress,
        @Suppress("UNUSED_PARAMETER") segmentCount: Int = 1
    ): HifzTrainingStep? {
        requireProgressIdentity(task, progress)
        if (progress.completed) return null
        val steps = HifzTrainingPolicy.stepsFor(task.track)
        return steps.getOrNull(progress.stepIndex)
    }

    fun reveal(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val step = requireCurrent(task, progress, segmentCount)
        val stepProgress = normalizeStepProgress(progress, step)
        return progress.copy(
            stepProgress = HifzTrainingProgressPolicy.reveal(stepProgress),
            totalRevealCount = Math.addExact(progress.totalRevealCount, 1)
        )
    }

    fun attempt(
        task: HifzTask,
        progress: HifzTaskProgress,
        correct: Boolean,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val step = requireCurrent(task, progress, segmentCount)
        val stepProgress = normalizeStepProgress(progress, step)
        return progress.copy(
            stepProgress = HifzTrainingProgressPolicy.attempt(stepProgress, correct),
            totalIncorrectAttempts = if (correct) {
                progress.totalIncorrectAttempts
            } else {
                Math.addExact(progress.totalIncorrectAttempts, 1)
            }
        )
    }

    fun recordActiveSeconds(
        task: HifzTask,
        progress: HifzTaskProgress,
        seconds: Long,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        requireProgressIdentity(task, progress)
        require(!progress.completed) { "Completed Hifz training cannot accrue active time." }
        require(seconds >= 0L) { "Active Hifz time cannot be negative." }
        require(currentStep(task, progress, segmentCount) != null)
        return progress.copy(activeSeconds = Math.addExact(progress.activeSeconds, seconds))
    }

    fun advanceIfValid(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val step = requireCurrent(task, progress, segmentCount)
        val stepProgress = normalizeStepProgress(progress, step)
        if (!HifzTrainingProgressPolicy.canValidate(step, stepProgress)) return progress

        val steps = HifzTrainingPolicy.stepsFor(task.track)
        val nextIndex = progress.stepIndex + 1
        return if (nextIndex < steps.size) {
            progress.copy(
                segmentIndex = 0,
                stepIndex = nextIndex,
                stepProgress = HifzStepProgress(stepId = steps[nextIndex].id),
                completed = false
            )
        } else {
            progress.copy(
                segmentIndex = 0,
                stepIndex = steps.size,
                stepProgress = null,
                completed = true
            )
        }
    }

    fun isSemanticallyCoherent(
        task: HifzTask,
        progress: HifzTaskProgress,
        @Suppress("UNUSED_PARAMETER") segmentCount: Int = 1
    ): Boolean = runCatching {
        requireProgressIdentity(task, progress)
        require(progress.totalRevealCount >= (progress.stepProgress?.revealCount ?: 0))
        require(progress.segmentIndex == 0) {
            "Training progress is task-wide; rendering segments cannot multiply repetitions."
        }
        val steps = HifzTrainingPolicy.stepsFor(task.track)
        if (progress.completed) {
            require(progress.stepProgress == null)
            require(progress.stepIndex == steps.size)
            return@runCatching true
        }
        require(progress.stepIndex in steps.indices)
        val stepProgress = requireNotNull(progress.stepProgress)
        require(stepProgress.stepId == steps[progress.stepIndex].id)
        require(stepProgress.consecutiveSuccesses <= stepProgress.repetitions)
        true
    }.getOrDefault(false)

    private fun requireCurrent(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int
    ): HifzTrainingStep = currentStep(task, progress, segmentCount)
        ?: error("The Hifz task has no active training step.")

    private fun normalizeStepProgress(
        progress: HifzTaskProgress,
        step: HifzTrainingStep
    ): HifzStepProgress {
        val existing = progress.stepProgress
        return if (existing?.stepId == step.id) existing else HifzStepProgress(stepId = step.id)
    }

    private fun requireProgressIdentity(task: HifzTask, progress: HifzTaskProgress) {
        require(progress.taskId == task.id) { "Hifz progress belongs to another task." }
    }
}

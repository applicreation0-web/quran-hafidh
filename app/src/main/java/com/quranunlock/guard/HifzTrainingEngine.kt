package com.applicreation0.quransafeguard

/**
 * Pure transition engine for a structured Hifz task.
 *
 * The scheduler owns when/what must be studied. This engine owns only progress inside
 * that task. It never changes dates, cursor or quota and never reads reader109 state.
 */
object HifzTrainingEngine {

    fun initial(task: HifzTask, segmentCount: Int = 1): HifzTaskProgress {
        require(segmentCount > 0) { "A Hifz task must expose at least one pedagogical segment." }
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
        segmentCount: Int = 1
    ): HifzTrainingStep? {
        requireProgressIdentity(task, progress)
        require(segmentCount > 0)
        if (progress.completed) return null
        val steps = stepsForPhase(task, progress.segmentIndex, segmentCount)
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
            stepProgress = HifzTrainingProgressPolicy.reveal(stepProgress)
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
            stepProgress = HifzTrainingProgressPolicy.attempt(stepProgress, correct)
        )
    }

    /**
     * Advances only when the current step contract is satisfied. For multi-segment Sabqi,
     * finishing a segment advances only the pedagogical segment. After the last segment a
     * dedicated whole-passage assembly test is required; only that test may set completed.
     */
    fun advanceIfValid(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val step = requireCurrent(task, progress, segmentCount)
        val stepProgress = normalizeStepProgress(progress, step)
        if (!HifzTrainingProgressPolicy.canValidate(step, stepProgress)) return progress

        val phaseSteps = stepsForPhase(task, progress.segmentIndex, segmentCount)
        val nextIndex = progress.stepIndex + 1
        if (nextIndex < phaseSteps.size) {
            return progress.copy(
                stepIndex = nextIndex,
                stepProgress = HifzStepProgress(stepId = phaseSteps[nextIndex].id),
                completed = false
            )
        }

        val assemblyRequired = requiresAssembly(task.track, segmentCount)
        return when {
            progress.segmentIndex < segmentCount - 1 -> {
                val baseSteps = HifzTrainingPolicy.stepsFor(task.track)
                progress.copy(
                    segmentIndex = progress.segmentIndex + 1,
                    stepIndex = 0,
                    stepProgress = HifzStepProgress(stepId = baseSteps.first().id),
                    completed = false
                )
            }
            assemblyRequired && progress.segmentIndex == segmentCount - 1 -> {
                val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
                require(assembly.isNotEmpty())
                progress.copy(
                    segmentIndex = segmentCount,
                    stepIndex = 0,
                    stepProgress = HifzStepProgress(stepId = assembly.first().id),
                    completed = false
                )
            }
            else -> progress.copy(
                stepIndex = phaseSteps.size,
                stepProgress = null,
                completed = true
            )
        }
    }

    /**
     * Validates persisted progress against the protocol rather than trusting a serialized
     * completed flag or arbitrary step id. This is used by HifzStateStore to fail closed.
     */
    fun isSemanticallyCoherent(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): Boolean = runCatching {
        requireProgressIdentity(task, progress)
        require(segmentCount > 0)
        val assemblyRequired = requiresAssembly(task.track, segmentCount)
        val lastBaseSegment = segmentCount - 1

        if (progress.completed) {
            require(progress.stepProgress == null)
            if (assemblyRequired) {
                val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
                require(progress.segmentIndex == segmentCount)
                require(progress.stepIndex == assembly.size)
            } else {
                val base = HifzTrainingPolicy.stepsFor(task.track)
                require(progress.segmentIndex == lastBaseSegment)
                require(progress.stepIndex == base.size)
            }
            return@runCatching true
        }

        if (assemblyRequired && progress.segmentIndex == segmentCount) {
            val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
            require(progress.stepIndex in assembly.indices)
            val stepProgress = requireNotNull(progress.stepProgress)
            require(stepProgress.stepId == assembly[progress.stepIndex].id)
            require(stepProgress.consecutiveSuccesses <= stepProgress.repetitions)
            return@runCatching true
        }

        require(progress.segmentIndex in 0 until segmentCount)
        val base = HifzTrainingPolicy.stepsFor(task.track)
        require(progress.stepIndex in base.indices)
        val stepProgress = requireNotNull(progress.stepProgress)
        require(stepProgress.stepId == base[progress.stepIndex].id)
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
        return if (existing?.stepId == step.id) {
            existing
        } else {
            HifzStepProgress(stepId = step.id)
        }
    }

    private fun stepsForPhase(
        task: HifzTask,
        segmentIndex: Int,
        segmentCount: Int
    ): List<HifzTrainingStep> {
        require(segmentCount > 0)
        return if (requiresAssembly(task.track, segmentCount) && segmentIndex == segmentCount) {
            HifzTrainingPolicy.assemblyStepsFor(task.track)
        } else {
            require(segmentIndex in 0 until segmentCount) { "Invalid Hifz segment index." }
            HifzTrainingPolicy.stepsFor(task.track)
        }
    }

    private fun requiresAssembly(track: HifzTrack, segmentCount: Int): Boolean =
        track == HifzTrack.SABQI && segmentCount > 1

    private fun requireProgressIdentity(task: HifzTask, progress: HifzTaskProgress) {
        require(progress.taskId == task.id) { "Hifz progress belongs to another task." }
    }
}

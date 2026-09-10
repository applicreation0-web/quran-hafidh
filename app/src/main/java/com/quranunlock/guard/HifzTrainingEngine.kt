package com.applicreation0.quransafeguard

/**
 * Pure transition engine for a structured Hifz task.
 *
 * The scheduler owns when/what must be studied. This engine owns only progress inside
 * that task. It never changes dates, cursor or quota and never reads reader109 state.
 * Sabqi may use five-line pedagogical segments for an indivisible long verse; Itqan and
 * Murajaah are task-wide protocols and therefore always have one training phase.
 */
object HifzTrainingEngine {

    fun initial(task: HifzTask, segmentCount: Int = 1): HifzTaskProgress {
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        val steps = baseSteps(task)
        require(steps.isNotEmpty()) { "No structured training protocol is defined for ${task.track}." }
        return HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 0,
            stepIndex = 0,
            stepProgress = HifzStepProgress(stepId = steps.first().id),
            completed = false
        ).also { require(effectiveSegments > 0) }
    }

    fun currentStep(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTrainingStep? {
        requireProgressIdentity(task, progress)
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        if (progress.completed) return null
        val steps = stepsForPhase(task, progress.segmentIndex, effectiveSegments)
        return steps.getOrNull(progress.stepIndex)
    }

    fun reveal(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        val step = requireCurrent(task, progress, effectiveSegments)
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
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        val step = requireCurrent(task, progress, effectiveSegments)
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

    /**
     * One user/audio repetition is one confirmation. A correct repetition that makes the
     * current step valid advances atomically; an incorrect repetition is recorded but
     * never advances by itself. This removes the redundant second "Validate" gesture.
     */
    fun attemptAndAdvanceIfValid(
        task: HifzTask,
        progress: HifzTaskProgress,
        correct: Boolean,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val wasAssisted = progress.stepProgress?.assistedSinceLastAttempt == true
        val attempted = attempt(task, progress, correct, segmentCount)
        return if (correct && !wasAssisted) {
            advanceIfValid(task, attempted, segmentCount)
        } else {
            attempted
        }
    }

    fun recordActiveSeconds(
        task: HifzTask,
        progress: HifzTaskProgress,
        seconds: Long,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        requireProgressIdentity(task, progress)
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        require(!progress.completed) { "Completed Hifz training cannot accrue active time." }
        require(seconds >= 0L) { "Active Hifz time cannot be negative." }
        require(currentStep(task, progress, effectiveSegments) != null)
        return progress.copy(activeSeconds = Math.addExact(progress.activeSeconds, seconds))
    }

    fun advanceIfValid(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): HifzTaskProgress {
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        val step = requireCurrent(task, progress, effectiveSegments)
        val stepProgress = normalizeStepProgress(progress, step)
        if (!HifzTrainingProgressPolicy.canValidate(step, stepProgress)) return progress

        val phaseSteps = stepsForPhase(task, progress.segmentIndex, effectiveSegments)
        val nextIndex = progress.stepIndex + 1
        if (nextIndex < phaseSteps.size) {
            return progress.copy(
                stepIndex = nextIndex,
                stepProgress = HifzStepProgress(stepId = phaseSteps[nextIndex].id),
                completed = false
            )
        }

        val assemblyRequired = requiresAssembly(task.track, effectiveSegments)
        return when {
            progress.segmentIndex < effectiveSegments - 1 -> {
                val baseSteps = baseSteps(task)
                progress.copy(
                    segmentIndex = progress.segmentIndex + 1,
                    stepIndex = 0,
                    stepProgress = HifzStepProgress(stepId = baseSteps.first().id),
                    completed = false
                )
            }
            assemblyRequired && progress.segmentIndex == effectiveSegments - 1 -> {
                val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
                require(assembly.isNotEmpty())
                progress.copy(
                    segmentIndex = effectiveSegments,
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

    fun isSemanticallyCoherent(
        task: HifzTask,
        progress: HifzTaskProgress,
        segmentCount: Int = 1
    ): Boolean = runCatching {
        requireProgressIdentity(task, progress)
        val effectiveSegments = effectiveSegmentCount(task, segmentCount)
        require(progress.totalRevealCount >= (progress.stepProgress?.revealCount ?: 0))
        val assemblyRequired = requiresAssembly(task.track, effectiveSegments)
        val lastBaseSegment = effectiveSegments - 1

        if (progress.completed) {
            require(progress.stepProgress == null)
            if (assemblyRequired) {
                val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
                require(progress.segmentIndex == effectiveSegments)
                require(progress.stepIndex == assembly.size)
            } else {
                val base = baseSteps(task)
                require(progress.segmentIndex == lastBaseSegment)
                require(progress.stepIndex == base.size)
            }
            return@runCatching true
        }

        if (assemblyRequired && progress.segmentIndex == effectiveSegments) {
            val assembly = HifzTrainingPolicy.assemblyStepsFor(task.track)
            require(progress.stepIndex in assembly.indices)
            val stepProgress = requireNotNull(progress.stepProgress)
            require(stepProgress.stepId == assembly[progress.stepIndex].id)
            require(stepProgress.consecutiveSuccesses <= stepProgress.repetitions)
            return@runCatching true
        }

        require(progress.segmentIndex in 0 until effectiveSegments)
        val base = baseSteps(task)
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
        return if (existing?.stepId == step.id) existing else HifzStepProgress(stepId = step.id)
    }

    private fun stepsForPhase(
        task: HifzTask,
        segmentIndex: Int,
        segmentCount: Int
    ): List<HifzTrainingStep> = if (requiresAssembly(task.track, segmentCount) && segmentIndex == segmentCount) {
        HifzTrainingPolicy.assemblyStepsFor(task.track)
    } else {
        require(segmentIndex in 0 until segmentCount) { "Invalid Hifz segment index." }
        baseSteps(task)
    }

    private fun baseSteps(task: HifzTask): List<HifzTrainingStep> =
        HifzTrainingPolicy.stepsFor(
            task.track,
            audioAvailable = task.audioPhasesIncluded
        )

    private fun effectiveSegmentCount(task: HifzTask, requested: Int): Int {
        require(requested > 0) { "A Hifz task must expose at least one pedagogical segment." }
        return if (task.track == HifzTrack.SABQI) requested else 1
    }

    private fun requiresAssembly(track: HifzTrack, segmentCount: Int): Boolean =
        track == HifzTrack.SABQI && segmentCount > 1

    private fun requireProgressIdentity(task: HifzTask, progress: HifzTaskProgress) {
        require(progress.taskId == task.id) { "Hifz progress belongs to another task." }
    }
}
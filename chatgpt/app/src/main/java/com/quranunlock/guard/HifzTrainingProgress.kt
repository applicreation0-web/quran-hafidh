package com.applicreation0.quransafeguard

data class HifzStepProgress(
    val stepId: String,
    val repetitions: Int = 0,
    val consecutiveSuccesses: Int = 0,
    val revealCount: Int = 0,
    val assistedSinceLastAttempt: Boolean = false
) {
    init {
        require(stepId.isNotBlank())
        require(repetitions >= 0)
        require(consecutiveSuccesses >= 0)
        require(revealCount >= 0)
    }
}

/**
 * A reveal is observable help, never a successful repetition. After a reveal the next
 * attempt is recorded but cannot extend a consecutive-success streak. A later unassisted
 * correct attempt can start the streak again.
 */
object HifzTrainingProgressPolicy {
    fun reveal(progress: HifzStepProgress): HifzStepProgress = progress.copy(
        revealCount = progress.revealCount + 1,
        assistedSinceLastAttempt = true,
        consecutiveSuccesses = 0
    )

    fun attempt(progress: HifzStepProgress, correct: Boolean): HifzStepProgress {
        val assisted = progress.assistedSinceLastAttempt
        return progress.copy(
            repetitions = progress.repetitions + 1,
            consecutiveSuccesses = when {
                !correct -> 0
                assisted -> 0
                else -> progress.consecutiveSuccesses + 1
            },
            assistedSinceLastAttempt = false
        )
    }

    fun canValidate(step: HifzTrainingStep, progress: HifzStepProgress): Boolean {
        require(progress.stepId == step.id) { "Progress belongs to another Hifz step." }
        if (progress.assistedSinceLastAttempt) return false
        if (progress.repetitions < step.repetitions) return false
        return progress.consecutiveSuccesses >= step.requiresConsecutiveSuccesses
    }
}

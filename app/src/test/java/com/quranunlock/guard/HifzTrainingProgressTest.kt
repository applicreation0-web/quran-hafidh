package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzTrainingProgressTest {

    @Test
    fun revealIsCountedButNeverCountsAsSuccess() {
        val progress = HifzStepProgress(stepId = "itqan-final", repetitions = 1, consecutiveSuccesses = 1)

        val revealed = HifzTrainingProgressPolicy.reveal(progress)

        assertEquals(1, revealed.revealCount)
        assertEquals(0, revealed.consecutiveSuccesses)
        assertTrue(revealed.assistedSinceLastAttempt)
    }

    @Test
    fun firstAttemptAfterRevealCannotExtendSuccessStreak() {
        val revealed = HifzTrainingProgressPolicy.reveal(HifzStepProgress(stepId = "sabqi-final"))

        val attempt = HifzTrainingProgressPolicy.attempt(revealed, correct = true)

        assertEquals(1, attempt.repetitions)
        assertEquals(0, attempt.consecutiveSuccesses)
        assertFalse(attempt.assistedSinceLastAttempt)
    }

    @Test
    fun laterUnassistedCorrectAttemptCanStartSuccessAgain() {
        val revealed = HifzTrainingProgressPolicy.reveal(HifzStepProgress(stepId = "sabqi-final"))
        val assistedAttempt = HifzTrainingProgressPolicy.attempt(revealed, correct = true)

        val cleanAttempt = HifzTrainingProgressPolicy.attempt(assistedAttempt, correct = true)

        assertEquals(2, cleanAttempt.repetitions)
        assertEquals(1, cleanAttempt.consecutiveSuccesses)
    }

    @Test
    fun finalSabqiValidationRequiresThreeUnassistedConsecutiveSuccesses() {
        val step = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI).last()
        var progress = HifzStepProgress(stepId = step.id)
        repeat(3) { progress = HifzTrainingProgressPolicy.attempt(progress, correct = true) }

        assertTrue(HifzTrainingProgressPolicy.canValidate(step, progress))
    }

    @Test
    fun assistedStateBlocksValidationEvenWhenCountersAreHighEnough() {
        val step = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN).last()
        val progress = HifzStepProgress(
            stepId = step.id,
            repetitions = 5,
            consecutiveSuccesses = 5,
            assistedSinceLastAttempt = true
        )

        assertFalse(HifzTrainingProgressPolicy.canValidate(step, progress))
    }
}

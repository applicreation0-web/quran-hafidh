package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzTrainingEngineTest {

    @Test
    fun sabqiStartsAtPassiveAudio() {
        val task = task(HifzTrack.SABQI)
        val progress = HifzTrainingEngine.initial(task)

        assertEquals("sabqi-audio-passive", progress.stepProgress?.stepId)
        assertEquals(HifzTrainingKind.AUDIO_PASSIVE, HifzTrainingEngine.currentStep(task, progress)?.kind)
    }

    @Test
    fun itqanStartsAtThirtyVisibleRepetitions() {
        val task = task(HifzTrack.ITQAN)
        val progress = HifzTrainingEngine.initial(task)

        val step = HifzTrainingEngine.currentStep(task, progress)
        assertEquals("itqan-visible-30", step?.id)
        assertEquals(30, step?.repetitions)
    }

    @Test
    fun murajaahCannotStartUntilItsProtocolIsDefined() {
        val task = task(HifzTrack.MURAJAAH)
        assertThrows(IllegalArgumentException::class.java) {
            HifzTrainingEngine.initial(task)
        }
    }

    @Test
    fun revealCannotAdvanceCurrentStep() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        progress = HifzTrainingEngine.reveal(task, progress)

        val afterAdvance = HifzTrainingEngine.advanceIfValid(task, progress)

        assertEquals(0, afterAdvance.stepIndex)
        assertEquals("itqan-visible-30", afterAdvance.stepProgress?.stepId)
        assertEquals(1, afterAdvance.stepProgress?.revealCount)
    }

    @Test
    fun completedTrainingDoesNotMutateScheduleIdentity() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        val steps = HifzTrainingPolicy.stepsFor(task.track)

        steps.forEach { step ->
            var current = HifzTrainingEngine.currentStep(task, progress)
            assertEquals(step.id, current?.id)
            repeat(step.repetitions) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true)
            }
            while ((progress.stepProgress?.consecutiveSuccesses ?: 0) < step.requiresConsecutiveSuccesses) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true)
            }
            progress = HifzTrainingEngine.advanceIfValid(task, progress)
        }

        assertTrue(progress.completed)
        assertNull(HifzTrainingEngine.currentStep(task, progress))
        assertEquals(LocalDate.of(2026, 9, 8), task.originalScheduledDate)
        assertEquals(LocalDate.of(2026, 9, 8), task.scheduledDate)
        assertEquals("2:1-2:5", task.cursor)
        assertEquals(5, task.quota)
        assertFalse(task.status == HifzTaskStatus.COMPLETED)
    }

    private fun task(track: HifzTrack) = HifzTask(
        id = "task-${track.name.lowercase()}",
        track = track,
        originalScheduledDate = LocalDate.of(2026, 9, 8),
        cursor = "2:1-2:5",
        quota = 5
    )
}

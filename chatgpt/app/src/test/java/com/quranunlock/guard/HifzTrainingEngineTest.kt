package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzTrainingEngineTest {

    @Test
    fun sabqiStartsWithFifteenVisibleRepetitionsAndNoAudioProgressStep() {
        val task = task(HifzTrack.SABQI)
        val progress = HifzTrainingEngine.initial(task)
        val step = HifzTrainingEngine.currentStep(task, progress)

        assertEquals("sabqi-visible", progress.stepProgress?.stepId)
        assertEquals(HifzTrainingKind.VISIBLE, step?.kind)
        assertEquals(15, step?.repetitions)
        assertFalse(step?.requiresAudio ?: true)
    }

    @Test
    fun itqanStartsAtCentralizedPreviewVisibleRepetitions() {
        val task = task(HifzTrack.ITQAN)
        val progress = HifzTrainingEngine.initial(task)

        val step = HifzTrainingEngine.currentStep(task, progress)
        assertEquals("itqan-visible", step?.id)
        assertEquals(HifzTrainingPolicy.PREVIEW_ITQAN_VISIBLE_REPETITIONS, step?.repetitions)
    }

    @Test
    fun murajaahStartsVisibleAndCompletesOneCleanRecall() {
        val task = task(HifzTrack.MURAJAAH)
        var progress = HifzTrainingEngine.initial(task)

        val step = HifzTrainingEngine.currentStep(task, progress)
        assertEquals("murajaah-recall", step?.id)
        assertEquals(0, step?.maskPercent)
        progress = HifzTrainingEngine.attempt(task, progress, correct = true)
        progress = HifzTrainingEngine.advanceIfValid(task, progress)

        assertTrue(progress.completed)
        assertNull(HifzTrainingEngine.currentStep(task, progress))
    }

    @Test
    fun revealCannotAdvanceCurrentStep() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        progress = HifzTrainingEngine.reveal(task, progress)

        val afterAdvance = HifzTrainingEngine.advanceIfValid(task, progress)

        assertEquals(0, afterAdvance.stepIndex)
        assertEquals("itqan-visible", afterAdvance.stepProgress?.stepId)
        assertEquals(1, afterAdvance.stepProgress?.revealCount)
    }

    @Test
    fun incorrectMaskedItqanAttemptCannotAdvance() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        val first = HifzTrainingEngine.currentStep(task, progress)!!

        repeat(first.repetitions) {
            progress = HifzTrainingEngine.attempt(task, progress, correct = true)
        }
        progress = HifzTrainingEngine.advanceIfValid(task, progress)
        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)

        progress = HifzTrainingEngine.attempt(task, progress, correct = false)
        progress = HifzTrainingEngine.advanceIfValid(task, progress)

        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)
        assertEquals(0, progress.stepProgress?.consecutiveSuccesses)
    }

    @Test
    fun renderingSegmentsNeverMultiplySabqiRepetitionsOrAddAssembly() {
        val task = task(HifzTrack.SABQI)
        val renderingSegments = 3
        var progress = HifzTrainingEngine.initial(task, renderingSegments)

        HifzTrainingPolicy.stepsFor(HifzTrack.SABQI).forEach { step ->
            assertEquals(0, progress.segmentIndex)
            assertEquals(step.id, HifzTrainingEngine.currentStep(task, progress, renderingSegments)?.id)
            repeat(step.repetitions) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true, segmentCount = renderingSegments)
            }
            progress = HifzTrainingEngine.advanceIfValid(task, progress, renderingSegments)
        }

        assertTrue(progress.completed)
        assertEquals(0, progress.segmentIndex)
        assertNull(progress.stepProgress)
        assertTrue(HifzTrainingPolicy.assemblyStepsFor(HifzTrack.SABQI).isEmpty())
    }

    @Test
    fun forgedCompletedFlagOrWrongStepIdIsRejectedSemantically() {
        val task = task(HifzTrack.ITQAN)
        val forgedCompleted = HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 0,
            stepIndex = 0,
            stepProgress = null,
            completed = true
        )
        val forgedStep = HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 0,
            stepIndex = 1,
            stepProgress = HifzStepProgress(stepId = "itqan-mask-100")
        )

        assertFalse(HifzTrainingEngine.isSemanticallyCoherent(task, forgedCompleted))
        assertFalse(HifzTrainingEngine.isSemanticallyCoherent(task, forgedStep))
        assertTrue(HifzTrainingEngine.isSemanticallyCoherent(task, HifzTrainingEngine.initial(task)))
    }

    @Test
    fun completedTrainingDoesNotMutateScheduleIdentity() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        val steps = HifzTrainingPolicy.stepsFor(task.track)

        steps.forEach { step ->
            val current = HifzTrainingEngine.currentStep(task, progress)
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
        assertEquals(HifzCursor.page(2, 1, 5, 2), task.cursor)
        assertEquals(5, task.quota)
        assertFalse(task.status == HifzTaskStatus.COMPLETED)
    }

    private fun task(track: HifzTrack) = HifzTask(
        id = "task-${track.name.lowercase()}",
        track = track,
        originalScheduledDate = LocalDate.of(2026, 9, 8),
        cursor = HifzCursor.page(2, 1, 5, 2),
        quota = 5
    )
}

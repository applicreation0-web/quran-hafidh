package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzTrainingEngineTest {

    @Test
    fun sabqiEngineStartsWithAlHusaryListeningPhaseByDefault() {
        val task = task(HifzTrack.SABQI)
        val progress = HifzTrainingEngine.initial(task)

        assertEquals("sabqi-audio-passive", progress.stepProgress?.stepId)
        assertEquals(HifzTrainingKind.AUDIO_PASSIVE, HifzTrainingEngine.currentStep(task, progress)?.kind)
    }

    @Test
    fun itqanStartsAtTwentyVisibleRepetitions() {
        val task = task(HifzTrack.ITQAN)
        val progress = HifzTrainingEngine.initial(task)

        val step = HifzTrainingEngine.currentStep(task, progress)
        assertEquals("itqan-visible-20", step?.id)
        assertEquals(20, step?.repetitions)
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
    fun correctFinalRepetitionAdvancesWithoutSeparateValidationGesture() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)

        repeat(19) {
            progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
            assertEquals("itqan-visible-20", HifzTrainingEngine.currentStep(task, progress)?.id)
        }

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)

        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)
        assertEquals(0, progress.stepProgress?.repetitions)
    }

    @Test
    fun audioPassAtQuotaAlsoAdvancesWithoutSeparateValidationGesture() {
        val task = task(HifzTrack.SABQI)
        var progress = HifzTrainingEngine.initial(task)

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        assertEquals("sabqi-audio-passive", HifzTrainingEngine.currentStep(task, progress)?.id)

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        assertEquals("sabqi-audio-active", HifzTrainingEngine.currentStep(task, progress)?.id)
        assertEquals(0, progress.stepProgress?.repetitions)
    }

    @Test
    fun incorrectFinalRepetitionNeverAutoAdvances() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)

        repeat(19) {
            progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        }
        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = false)

        assertEquals("itqan-visible-20", HifzTrainingEngine.currentStep(task, progress)?.id)
        assertEquals(20, progress.stepProgress?.repetitions)
        assertEquals(0, progress.stepProgress?.consecutiveSuccesses)

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)
    }

    @Test
    fun revealCannotAdvanceCurrentStep() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)
        progress = HifzTrainingEngine.reveal(task, progress)

        val afterAdvance = HifzTrainingEngine.advanceIfValid(task, progress)

        assertEquals(0, afterAdvance.stepIndex)
        assertEquals("itqan-visible-20", afterAdvance.stepProgress?.stepId)
        assertEquals(1, afterAdvance.stepProgress?.revealCount)
    }


    @Test
    fun assistedIntermediateRepetitionCannotAutoAdvance() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)

        repeat(20) {
            progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        }
        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        progress = HifzTrainingEngine.reveal(task, progress)
        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)

        assertEquals("itqan-mask-25", HifzTrainingEngine.currentStep(task, progress)?.id)
        assertEquals(2, progress.stepProgress?.repetitions)

        progress = HifzTrainingEngine.attemptAndAdvanceIfValid(task, progress, correct = true)
        assertEquals("itqan-mask-50", HifzTrainingEngine.currentStep(task, progress)?.id)
    }

    @Test
    fun incorrectMaskedItqanAttemptCannotAdvance() {
        val task = task(HifzTrack.ITQAN)
        var progress = HifzTrainingEngine.initial(task)

        repeat(20) {
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
    fun threeSegmentSabqiCannotCreditAnyFractionAndRequiresWholePassageAssembly() {
        val task = task(HifzTrack.SABQI)
        val segmentCount = 3
        var progress = HifzTrainingEngine.initial(task, segmentCount)

        repeat(segmentCount) { expectedSegment ->
            assertEquals(expectedSegment, progress.segmentIndex)
            progress = finishCurrentBasePhase(task, progress, segmentCount)
            if (expectedSegment < segmentCount - 1) {
                assertFalse(progress.completed)
                assertEquals(expectedSegment + 1, progress.segmentIndex)
                assertEquals("sabqi-audio-passive", progress.stepProgress?.stepId)
            }
        }

        assertFalse(progress.completed)
        assertEquals(segmentCount, progress.segmentIndex)
        assertEquals("sabqi-assembly-final", HifzTrainingEngine.currentStep(task, progress, segmentCount)?.id)

        repeat(2) {
            progress = HifzTrainingEngine.attempt(task, progress, correct = true, segmentCount = segmentCount)
        }
        assertFalse(HifzTrainingEngine.advanceIfValid(task, progress, segmentCount).completed)

        progress = HifzTrainingEngine.attempt(task, progress, correct = true, segmentCount = segmentCount)
        progress = HifzTrainingEngine.advanceIfValid(task, progress, segmentCount)
        assertTrue(progress.completed)
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

    private fun finishCurrentBasePhase(
        task: HifzTask,
        initial: HifzTaskProgress,
        segmentCount: Int
    ): HifzTaskProgress {
        var progress = initial
        val startingSegment = progress.segmentIndex
        while (!progress.completed && progress.segmentIndex == startingSegment) {
            val step = HifzTrainingEngine.currentStep(task, progress, segmentCount) ?: break
            repeat(step.repetitions) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true, segmentCount = segmentCount)
            }
            while ((progress.stepProgress?.consecutiveSuccesses ?: 0) < step.requiresConsecutiveSuccesses) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true, segmentCount = segmentCount)
            }
            progress = HifzTrainingEngine.advanceIfValid(task, progress, segmentCount)
        }
        return progress
    }

    private fun task(track: HifzTrack) = HifzTask(
        id = "task-${track.name.lowercase()}",
        track = track,
        originalScheduledDate = LocalDate.of(2026, 9, 8),
        cursor = HifzCursor.page(2, 1, 5, 2),
        quota = 5
    )
}

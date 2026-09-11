package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzJourneyCoordinatorTest {

    @Test
    fun unfinishedTrainingCannotCompleteScheduleTask() {
        val task = task()
        val progress = HifzTrainingEngine.initial(task)
        assertFalse(HifzJourneyCoordinator.canCompleteTask(task, progress))
        assertThrows(IllegalArgumentException::class.java) {
            HifzJourneyCoordinator.completeTask(task, progress)
        }
    }

    @Test
    fun forgedCompletedFlagCannotCompleteScheduleTask() {
        val task = task()
        val forged = HifzTaskProgress(taskId = task.id, completed = true)
        assertFalse(HifzJourneyCoordinator.canCompleteTask(task, forged))
        assertThrows(IllegalArgumentException::class.java) {
            HifzJourneyCoordinator.completeTask(task, forged)
        }
    }

    @Test
    fun completedCoherentTrainingStillRequiresExplicitCompletionTransition() {
        val task = task()
        val progress = completeTraining(task)
        assertTrue(HifzJourneyCoordinator.canCompleteTask(task, progress))
        assertEquals(HifzTaskStatus.PLANNED, task.status)
        val completed = HifzJourneyCoordinator.completeTask(task, progress)
        assertEquals(HifzTaskStatus.COMPLETED, completed.status)
        assertEquals(task.cursor, completed.cursor)
        assertEquals(task.quota, completed.quota)
    }

    @Test
    fun partialSabqiPromotesOnlyFullyCompletedVerses() {
        val sabqi = task("sabqi-partial").copy(
            cursor = HifzCursor(
                start = QuranVerseRef(2, 80),
                end = QuranVerseRef(2, 82),
                startPage = 12,
                endPage = 12,
                startLineId = "12:6",
                endLineId = "12:10",
                endVersePartial = true
            )
        )
        val state = referenceState(sabqi, completeTraining(sabqi), lowerFrontier = QuranVerseRef(2, 79))

        val updated = HifzJourneyCoordinator.completeTask(state, sabqi.id)
        val ranges = requireNotNull(updated.journeyConfig.bounds).itqan

        assertEquals(QuranVerseRef(2, 81), ranges.first().end)
        assertEquals(QuranVerseRef(49, 1), ranges[1].start)
    }

    @Test
    fun continuationThatFinishesPartialVerseAdvancesFrontierWithoutTeleportingItqanHistory() {
        val sabqi = task("sabqi-continuation").copy(
            cursor = HifzCursor(
                start = QuranVerseRef(2, 82),
                end = QuranVerseRef(2, 83),
                startPage = 12,
                endPage = 12,
                startLineId = "12:11",
                endLineId = "12:15",
                endVersePartial = false
            )
        )
        val itqanHistory = HifzTask(
            id = "itqan-existing",
            track = HifzTrack.ITQAN,
            originalScheduledDate = LocalDate.of(2026, 9, 8),
            cursor = HifzCursor.page(52, 10, 10, 525),
            quota = 60,
            status = HifzTaskStatus.COMPLETED
        )
        val state = referenceState(sabqi, completeTraining(sabqi), lowerFrontier = QuranVerseRef(2, 81))
            .copy(tasks = listOf(sabqi, itqanHistory))

        val updated = HifzJourneyCoordinator.completeTask(state, sabqi.id)

        assertEquals(QuranVerseRef(2, 83), requireNotNull(updated.journeyConfig.bounds).itqan.first().end)
        assertEquals(itqanHistory, updated.tasks.first { it.id == itqanHistory.id })
    }

    @Test
    fun firstFiveLinesInsideOneLongVerseDoNotPromoteIncompleteVerse() {
        val verse = QuranVerseRef(2, 282)
        val sabqi = task("long-verse").copy(
            cursor = HifzCursor(
                start = verse,
                end = verse,
                startPage = 48,
                endPage = 48,
                startLineId = "48:1",
                endLineId = "48:5",
                endVersePartial = true
            )
        )
        val bounds = HifzJourneyBounds(
            sabqi = HifzVerseRange(verse, verse),
            itqan = listOf(HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 281)))
        )
        val state = HifzState(
            journeyConfig = HifzJourneyConfig(bounds = bounds),
            tasks = listOf(sabqi),
            progressByTask = mapOf(sabqi.id to completeTraining(sabqi))
        )

        val updated = HifzJourneyCoordinator.completeTask(state, sabqi.id)
        assertEquals(bounds.itqan, requireNotNull(updated.journeyConfig.bounds).itqan)
    }

    @Test
    fun promotionMergesLowerRangeWithUpperTailWhenGapCloses() {
        val sabqi = task("bridge").copy(
            cursor = HifzCursor(
                start = QuranVerseRef(48, 29),
                end = QuranVerseRef(48, 29),
                startPage = 515,
                endPage = 515,
                startLineId = "515:1",
                endLineId = "515:5",
                endVersePartial = false
            )
        )
        val bounds = HifzJourneyBounds(
            sabqi = HifzVerseRange(QuranVerseRef(48, 29), QuranVerseRef(48, 29)),
            itqan = listOf(
                HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(48, 28)),
                HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
            )
        )
        val state = HifzState(
            journeyConfig = HifzJourneyConfig(bounds = bounds),
            tasks = listOf(sabqi),
            progressByTask = mapOf(sabqi.id to completeTraining(sabqi))
        )

        val updated = HifzJourneyCoordinator.completeTask(state, sabqi.id)
        val ranges = requireNotNull(updated.journeyConfig.bounds).itqan
        assertEquals(1, ranges.size)
        assertEquals(HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(114, 6)), ranges.single())
    }

    @Test
    fun stateCompletionChangesOnlyRequestedTaskWhenNoJourneyBoundsAreConfigured() {
        val first = task("first")
        val second = task("second").copy(
            scheduledDate = LocalDate.of(2026, 9, 11),
            originalScheduledDate = LocalDate.of(2026, 9, 11)
        )
        val firstProgress = completeTraining(first)
        val secondProgress = HifzTrainingEngine.initial(second)
        val state = HifzState(
            tasks = listOf(first, second),
            progressByTask = mapOf(first.id to firstProgress, second.id to secondProgress)
        )

        val updated = HifzJourneyCoordinator.completeTask(state, first.id)
        assertEquals(HifzTaskStatus.COMPLETED, updated.tasks.first { it.id == first.id }.status)
        assertEquals(second, updated.tasks.first { it.id == second.id })
        assertEquals(state.progressByTask, updated.progressByTask)
    }

    @Test
    fun progressFromAnotherTaskCannotCompleteTask() {
        val task = task()
        val foreign = HifzTaskProgress(taskId = "foreign", completed = true)
        assertFalse(HifzJourneyCoordinator.canCompleteTask(task, foreign))
    }

    private fun referenceState(
        sabqi: HifzTask,
        progress: HifzTaskProgress,
        lowerFrontier: QuranVerseRef
    ) = HifzState(
        journeyConfig = HifzJourneyConfig(
            bounds = HifzJourneyBounds(
                sabqi = HifzVerseRange(QuranVerseRef(2, 75), QuranVerseRef(48, 29)),
                itqan = listOf(
                    HifzVerseRange(QuranVerseRef(2, 1), lowerFrontier),
                    HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
                )
            )
        ),
        tasks = listOf(sabqi),
        progressByTask = mapOf(sabqi.id to progress)
    )

    private fun completeTraining(task: HifzTask): HifzTaskProgress {
        var progress = HifzTrainingEngine.initial(task)
        while (!progress.completed) {
            val step = requireNotNull(HifzTrainingEngine.currentStep(task, progress))
            repeat(step.repetitions) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true)
            }
            while ((progress.stepProgress?.consecutiveSuccesses ?: 0) < step.requiresConsecutiveSuccesses) {
                progress = HifzTrainingEngine.attempt(task, progress, correct = true)
            }
            progress = HifzTrainingEngine.advanceIfValid(task, progress)
        }
        return progress
    }

    private fun task(id: String = "sabqi-1") = HifzTask(
        id = id,
        track = HifzTrack.SABQI,
        originalScheduledDate = LocalDate.of(2026, 9, 9),
        cursor = HifzCursor.page(2, 75, 76, 11),
        quota = 90
    )
}

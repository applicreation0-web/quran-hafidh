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
        val progress = HifzTaskProgress(taskId = task.id, completed = false)

        assertFalse(HifzJourneyCoordinator.canCompleteTask(task, progress))
        assertThrows(IllegalArgumentException::class.java) {
            HifzJourneyCoordinator.completeTask(task, progress)
        }
    }

    @Test
    fun completedTrainingStillRequiresExplicitCompletionTransition() {
        val task = task()
        val progress = HifzTaskProgress(taskId = task.id, completed = true)

        assertTrue(HifzJourneyCoordinator.canCompleteTask(task, progress))
        assertEquals(HifzTaskStatus.PLANNED, task.status)

        val completed = HifzJourneyCoordinator.completeTask(task, progress)

        assertEquals(HifzTaskStatus.COMPLETED, completed.status)
        assertEquals(task.id, completed.id)
        assertEquals(task.originalScheduledDate, completed.originalScheduledDate)
        assertEquals(task.scheduledDate, completed.scheduledDate)
        assertEquals(task.cursor, completed.cursor)
        assertEquals(task.quota, completed.quota)
    }

    @Test
    fun stateCompletionChangesOnlyRequestedTask() {
        val first = task("first")
        val second = task("second").copy(
            scheduledDate = LocalDate.of(2026, 9, 10),
            originalScheduledDate = LocalDate.of(2026, 9, 10)
        )
        val state = HifzState(
            tasks = listOf(first, second),
            progressByTask = mapOf(
                first.id to HifzTaskProgress(first.id, completed = true),
                second.id to HifzTaskProgress(second.id, completed = false)
            )
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

    private fun task(id: String = "sabqi-1") = HifzTask(
        id = id,
        track = HifzTrack.SABQI,
        originalScheduledDate = LocalDate.of(2026, 9, 9),
        cursor = HifzCursor.page(2, 1, 5, 2),
        quota = 5
    )
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzStateCodecTest {

    @Test
    fun roundTripPreservesTaskAndTrainingProgress() {
        val task = HifzTask(
            id = "itqan|task-1",
            track = HifzTrack.ITQAN,
            originalScheduledDate = LocalDate.of(2026, 9, 8),
            scheduledDate = LocalDate.of(2026, 9, 15),
            cursor = "67:1-67:7 | page 560",
            quota = 30,
            status = HifzTaskStatus.OVERDUE
        )
        val progress = HifzTaskProgress(
            taskId = task.id,
            stepIndex = 3,
            stepProgress = HifzStepProgress(
                stepId = "itqan-mask-50",
                repetitions = 4,
                consecutiveSuccesses = 2,
                revealCount = 1,
                assistedSinceLastAttempt = true
            )
        )
        val state = HifzState(
            tasks = listOf(task),
            progressByTask = mapOf(task.id to progress)
        )

        val decoded = HifzStateCodec.decode(HifzStateCodec.encode(state))

        assertEquals(state, decoded)
    }

    @Test
    fun unsupportedSchemaFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzStateCodec.decode("999\n")
        }
    }

    @Test
    fun duplicateTaskIdsAreRejected() {
        val date = LocalDate.of(2026, 9, 9)
        val a = HifzTask("same", HifzTrack.SABQI, date, cursor = "1:1", quota = 1)
        val b = HifzTask("same", HifzTrack.ITQAN, date, cursor = "1:2", quota = 1)

        assertThrows(IllegalArgumentException::class.java) {
            HifzState(tasks = listOf(a, b))
        }
    }

    @Test
    fun progressCannotReferenceUnknownTask() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzState(
                tasks = emptyList(),
                progressByTask = mapOf(
                    "missing" to HifzTaskProgress(taskId = "missing")
                )
            )
        }
    }
}

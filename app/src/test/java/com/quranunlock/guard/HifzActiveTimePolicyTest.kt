package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzActiveTimePolicyTest {
    private fun task() = HifzTask(
        id = "active-time",
        track = HifzTrack.ITQAN,
        originalScheduledDate = LocalDate.of(2026, 9, 10),
        cursor = HifzCursor.page(2, 1, 5, 2),
        quota = 20
    )

    @Test
    fun activeSecondsAccumulateExactlyWithoutChangingProtocolPosition() {
        val task = task()
        val initial = HifzTrainingEngine.initial(task)
        val after = HifzTrainingEngine.recordActiveSeconds(task, initial, 37)

        assertEquals(37L, after.activeSeconds)
        assertEquals(initial.segmentIndex, after.segmentIndex)
        assertEquals(initial.stepIndex, after.stepIndex)
        assertEquals(initial.stepProgress, after.stepProgress)
    }

    @Test
    fun backgroundOrNegativeTimeCannotBeInventedByEngine() {
        val task = task()
        val initial = HifzTrainingEngine.initial(task)
        assertEquals(0L, HifzTrainingEngine.recordActiveSeconds(task, initial, 0).activeSeconds)
        assertThrows(IllegalArgumentException::class.java) {
            HifzTrainingEngine.recordActiveSeconds(task, initial, -1)
        }
    }
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HifzResumePolicyTest {
    @Test
    fun sameDayRestartIsIdempotent() {
        val today = LocalDate.of(2026, 9, 9)
        val state = state(today)
        val restored = HifzStateCodec.decode(HifzStateCodec.encode(state))

        val resumed = HifzResumePolicy.resume(restored, today)

        assertEquals(restored, resumed.state)
        assertEquals("sabqi-resume", resumed.nextTask?.id)
    }

    @Test
    fun nextDayRestartPreservesAllItqanIntervalsAndProgress() {
        val plannedDate = LocalDate.of(2026, 9, 9)
        val state = state(plannedDate)
        val restored = HifzStateCodec.decode(HifzStateCodec.encode(state))
        val beforeTask = restored.tasks.single()
        val beforeProgress = restored.progressByTask.getValue(beforeTask.id)
        val beforeConfig = restored.journeyConfig

        val resumed = HifzResumePolicy.resume(restored, LocalDate.of(2026, 9, 10))
        val afterTask = resumed.state.tasks.single()

        assertEquals(HifzTaskStatus.OVERDUE, afterTask.status)
        assertEquals(beforeConfig, resumed.state.journeyConfig)
        assertEquals(2, resumed.state.journeyConfig.bounds?.itqan?.size)
        assertEquals(beforeTask.originalScheduledDate, afterTask.originalScheduledDate)
        assertEquals(beforeTask.scheduledDate, afterTask.scheduledDate)
        assertEquals(beforeTask.cursor, afterTask.cursor)
        assertEquals(beforeTask.quota, afterTask.quota)
        assertEquals(beforeProgress, resumed.state.progressByTask.getValue(afterTask.id))
        assertEquals(afterTask.id, resumed.nextTask?.id)
    }

    @Test
    fun resumeNeverReopensCompletedTask() {
        val date = LocalDate.of(2026, 9, 8)
        val done = HifzTask(
            id = "done",
            track = HifzTrack.ITQAN,
            originalScheduledDate = date,
            cursor = HifzCursor.page(2, 1, 5, 2),
            quota = 5,
            status = HifzTaskStatus.COMPLETED
        )
        val state = HifzState(tasks = listOf(done))

        val resumed = HifzResumePolicy.resume(state, LocalDate.of(2026, 9, 10))

        assertEquals(done, resumed.state.tasks.single())
        assertEquals(null, resumed.nextTask)
    }

    private fun state(date: LocalDate): HifzState {
        val task = HifzTask(
            id = "sabqi-resume",
            track = HifzTrack.SABQI,
            originalScheduledDate = date,
            cursor = HifzCursor.page(2, 1, 5, 2),
            quota = 5
        )
        val progress = HifzTaskProgress(
            taskId = task.id,
            stepIndex = 2,
            stepProgress = HifzStepProgress(
                stepId = "sabqi-visible",
                repetitions = 6,
                consecutiveSuccesses = 0,
                revealCount = 1,
                assistedSinceLastAttempt = false
            )
        )
        val config = HifzJourneyConfig(
            bounds = HifzJourneyBounds(
                sabqi = HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 20)),
                itqan = listOf(
                    HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 286)),
                    HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
                )
            ),
            pace = HifzPaceProfile(sabqiMinutesPerPage = 16.0, itqanMinutesPerPage = 6.0)
        )
        return HifzState(
            journeyConfig = config,
            tasks = listOf(task),
            progressByTask = mapOf(task.id to progress)
        )
    }
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzStateCodecTest {

    @Test
    fun roundTripPreservesConfigPlanningLedgerTaskAndSegmentMetrics() {
        val task = HifzTask(
            id = "itqan|task-1",
            track = HifzTrack.ITQAN,
            originalScheduledDate = LocalDate.of(2026, 9, 8),
            scheduledDate = LocalDate.of(2026, 9, 15),
            cursor = HifzCursor.page(67, 1, 7, 562),
            quota = 30,
            status = HifzTaskStatus.OVERDUE
        )
        val progress = HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 2,
            stepIndex = 3,
            stepProgress = HifzStepProgress(
                stepId = "itqan-mask-50",
                repetitions = 4,
                consecutiveSuccesses = 2,
                revealCount = 1,
                assistedSinceLastAttempt = true
            ),
            totalRevealCount = 4,
            totalIncorrectAttempts = 3,
            activeSeconds = 901
        )
        val config = HifzJourneyConfig(
            bounds = HifzJourneyBounds(
                sabqi = HifzVerseRange(QuranVerseRef(67, 1), QuranVerseRef(67, 30)),
                itqan = listOf(
                    HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 286)),
                    HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
                )
            ),
            pace = HifzPaceProfile(
                sabqiMinutesPerPage = 14.5,
                itqanMinutesPerPage = 5.25,
                murajaahMinutesPerPage = 2.4
            ),
            availableMinutes = HifzAvailableMinutes(
                sabqi = 30,
                itqan = 25,
                murajaah = 45
            )
        )
        val state = HifzState(
            journeyConfig = config,
            planningDates = setOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)),
            tasks = listOf(task),
            progressByTask = mapOf(task.id to progress)
        )

        val decoded = HifzStateCodec.decode(HifzStateCodec.encode(state))

        assertEquals(state, decoded)
        assertEquals(2, decoded.journeyConfig.bounds?.itqan?.size)
        assertEquals(2, decoded.progressByTask[task.id]?.segmentIndex)
        assertEquals(901, decoded.progressByTask[task.id]?.activeSeconds)
    }

    @Test
    fun emptyPreSetupConfigAlsoRoundTrips() {
        val state = HifzState()
        assertEquals(state, HifzStateCodec.decode(HifzStateCodec.encode(state)))
    }

    @Test
    fun legacySchemasFailClosed() {
        (1..5).forEach { schema ->
            assertThrows(IllegalArgumentException::class.java) {
                HifzStateCodec.decode("$schema\n")
            }
        }
    }

    @Test
    fun currentSchemaWithoutConfigFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzStateCodec.decode("6\n")
        }
    }

    @Test
    fun missingItqanIntervalIndexFailsClosed() {
        val raw = "6\nC|67|1|67|30|-|-|-|-|-|-\nI|1|49|1|114|6\n"
        assertThrows(IllegalArgumentException::class.java) {
            HifzStateCodec.decode(raw)
        }
    }

    @Test
    fun malformedOldProgressRecordFailsClosed() {
        val raw = "6\nC|-|-|-|-|-|-|-|-|-|-\nP|dGFzaw|0|0|-|0|0|0|false|false\n"
        assertThrows(IllegalArgumentException::class.java) {
            HifzStateCodec.decode(raw)
        }
    }

    @Test
    fun duplicateTaskIdsAreRejected() {
        val date = LocalDate.of(2026, 9, 9)
        val a = HifzTask("same", HifzTrack.SABQI, date, cursor = HifzCursor.page(1, 1, 3, 1), quota = 1)
        val b = HifzTask("same", HifzTrack.ITQAN, date, cursor = HifzCursor.page(1, 4, 7, 1), quota = 1)

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

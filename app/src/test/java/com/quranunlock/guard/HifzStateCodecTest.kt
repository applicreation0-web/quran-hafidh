package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzStateCodecTest {

    @Test
    fun schemaSevenRoundTripPreservesExactLineBoundedCursorAndMetrics() {
        val task = HifzTask(
            id = "sabqi|task-1",
            track = HifzTrack.SABQI,
            originalScheduledDate = LocalDate.of(2026, 9, 9),
            scheduledDate = LocalDate.of(2026, 9, 16),
            cursor = HifzCursor(
                start = QuranVerseRef(2, 80),
                end = QuranVerseRef(2, 82),
                startPage = 12,
                endPage = 12,
                startLineId = "12:5",
                endLineId = "12:9",
                endVersePartial = true
            ),
            quota = 90,
            status = HifzTaskStatus.OVERDUE
        )
        val progress = HifzTaskProgress(
            taskId = task.id,
            segmentIndex = 0,
            stepIndex = 2,
            stepProgress = HifzStepProgress(
                stepId = "sabqi-mask-50",
                repetitions = 4,
                consecutiveSuccesses = 2,
                revealCount = 1,
                assistedSinceLastAttempt = true
            ),
            totalRevealCount = 4,
            totalIncorrectAttempts = 3,
            activeSeconds = 901
        )
        val customSchedule = HifzWeeklySchedule(
            monday = HifzTrack.SABQI,
            tuesday = HifzTrack.ITQAN,
            wednesday = HifzTrack.ITQAN,
            thursday = HifzTrack.MURAJAAH,
            friday = HifzTrack.SABQI,
            saturday = HifzTrack.MURAJAAH,
            sunday = HifzTrack.SABQI
        )
        val config = HifzJourneyConfig(
            bounds = HifzJourneyBounds(
                sabqi = HifzVerseRange(QuranVerseRef(2, 75), QuranVerseRef(48, 29)),
                itqan = listOf(
                    HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 74)),
                    HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
                )
            ),
            pace = HifzPaceProfile(
                sabqiMinutesPerPage = 14.5,
                itqanMinutesPerPage = 5.25,
                murajaahMinutesPerPage = 2.4
            ),
            availableMinutes = HifzAvailableMinutes(sabqi = 90, itqan = 60, murajaah = 45),
            schedule = customSchedule
        )
        val state = HifzState(
            journeyConfig = config,
            planningDates = setOf(LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9)),
            tasks = listOf(task),
            progressByTask = mapOf(task.id to progress)
        )

        val encoded = HifzStateCodec.encode(state)
        val decoded = HifzStateCodec.decode(encoded)

        assertEquals("7", encoded.lineSequence().first())
        assertEquals(state, decoded)
        assertEquals("12:5", decoded.tasks.single().cursor.startLineId)
        assertEquals("12:9", decoded.tasks.single().cursor.endLineId)
        assertTrue(decoded.tasks.single().cursor.endVersePartial)
        assertEquals(901L, decoded.progressByTask[task.id]?.activeSeconds)
    }

    @Test
    fun schemaSixTaskMigratesWithoutInventingLinePrecision() {
        val taskId = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("legacy-task".toByteArray())
        val raw = buildString {
            append("6\n")
            append("C|2|75|48|29|-|-|-|90|60|45\n")
            append("I|0|2|1|2|74\n")
            append("I|1|49|1|114|6\n")
            append("W|SABQI|ITQAN|SABQI|ITQAN|SABQI|MURAJAAH|MURAJAAH\n")
            append("T|$taskId|SABQI|20707|20707|2|75|2|76|11|11|90|PLANNED\n")
        }

        val decoded = HifzStateCodec.decode(raw)
        val cursor = decoded.tasks.single().cursor

        assertEquals(HifzStateCodec.SCHEMA, decoded.schema)
        assertNull(cursor.startLineId)
        assertNull(cursor.endLineId)
        assertFalse(cursor.endVersePartial)
        assertEquals(QuranVerseRef(2, 75), cursor.start)
        assertEquals(QuranVerseRef(2, 76), cursor.end)
    }

    @Test
    fun emptyPreSetupConfigAlsoRoundTrips() {
        val state = HifzState()
        assertEquals(state, HifzStateCodec.decode(HifzStateCodec.encode(state)))
    }

    @Test
    fun schemaSixStateWithoutWeeklyRecordUsesApprovedDefaultForCompatibility() {
        val raw = "6\nC|-|-|-|-|-|-|-|-|-|-\n"
        val decoded = HifzStateCodec.decode(raw)
        assertEquals(HifzWeeklySchedule.DEFAULT, decoded.journeyConfig.schedule)
        assertEquals(HifzStateCodec.SCHEMA, decoded.schema)
    }

    @Test
    fun weeklyRecordMustKeepAllThreeTracks() {
        val raw = "7\nC|-|-|-|-|-|-|-|-|-|-\nW|SABQI|SABQI|SABQI|SABQI|SABQI|SABQI|SABQI\n"
        assertThrows(IllegalArgumentException::class.java) { HifzStateCodec.decode(raw) }
    }

    @Test
    fun schemasOneThroughFiveFailClosed() {
        (1..5).forEach { schema ->
            assertThrows(IllegalArgumentException::class.java) { HifzStateCodec.decode("$schema\n") }
        }
    }

    @Test
    fun currentSchemaWithoutConfigFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) { HifzStateCodec.decode("7\n") }
    }

    @Test
    fun missingItqanIntervalIndexFailsClosed() {
        val raw = "7\nC|67|1|67|30|-|-|-|-|-|-\nI|1|49|1|114|6\n"
        assertThrows(IllegalArgumentException::class.java) { HifzStateCodec.decode(raw) }
    }

    @Test
    fun malformedProgressRecordFailsClosed() {
        val raw = "7\nC|-|-|-|-|-|-|-|-|-|-\nP|dGFzaw|0|0|-|0|0|0|false|false\n"
        assertThrows(IllegalArgumentException::class.java) { HifzStateCodec.decode(raw) }
    }

    @Test
    fun duplicateTaskIdsAreRejected() {
        val date = LocalDate.of(2026, 9, 9)
        val a = HifzTask("same", HifzTrack.SABQI, date, cursor = HifzCursor.page(1, 1, 3, 1), quota = 1)
        val b = HifzTask("same", HifzTrack.ITQAN, date, cursor = HifzCursor.page(1, 4, 7, 1), quota = 1)
        assertThrows(IllegalArgumentException::class.java) { HifzState(tasks = listOf(a, b)) }
    }

    @Test
    fun progressCannotReferenceUnknownTask() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzState(
                tasks = emptyList(),
                progressByTask = mapOf("missing" to HifzTaskProgress(taskId = "missing"))
            )
        }
    }
}

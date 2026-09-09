package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzDailyPlannerTest {

    @Test
    fun missingMeasuredSabqiPaceDoesNotConsumeDateAndCanRetryAfterConfiguration() {
        val today = LocalDate.of(2026, 9, 9) // Wednesday = Sabqi
        val bounds = HifzJourneyBounds(
            sabqi = range(1, 1, 1, 7),
            itqan = listOf(range(2, 1, 2, 2))
        )
        val initial = HifzState(
            journeyConfig = HifzJourneyConfig(
                bounds = bounds,
                availableMinutes = HifzAvailableMinutes(sabqi = 30)
            )
        )

        val blocked = HifzDailyPlanner.planDate(initial, today, basicGeometry())

        assertTrue(blocked.tasks.isEmpty())
        assertFalse(today in blocked.planningDates)

        val configured = blocked.copy(
            journeyConfig = blocked.journeyConfig.copy(
                pace = HifzPaceProfile(sabqiMinutesPerPage = 10.0)
            )
        )
        val planned = HifzDailyPlanner.planDate(configured, today, basicGeometry())

        assertEquals(1, planned.tasks.size)
        assertEquals(HifzTrack.SABQI, planned.tasks.single().track)
        assertEquals(30, planned.tasks.single().quota)
        assertTrue(today in planned.planningDates)
    }

    @Test
    fun backlogConsumesDailySlotAndCannotCreateSecondQuotaAfterCompletionSameDay() {
        val monday = LocalDate.of(2026, 9, 7)
        val wednesday = LocalDate.of(2026, 9, 9)
        val missed = HifzTask(
            id = "sabqi-missed",
            track = HifzTrack.SABQI,
            originalScheduledDate = monday,
            cursor = HifzCursor.page(1, 1, 1, 1),
            quota = 20
        )
        val state = configuredState(tasks = listOf(missed))

        val withBacklog = HifzDailyPlanner.planDate(state, wednesday, basicGeometry())

        assertEquals(1, withBacklog.tasks.size)
        assertEquals(HifzTaskStatus.OVERDUE, withBacklog.tasks.single().status)
        assertTrue(wednesday in withBacklog.planningDates)

        val afterCompletion = withBacklog.copy(
            tasks = withBacklog.tasks.map { it.copy(status = HifzTaskStatus.COMPLETED) }
        )
        val reopened = HifzDailyPlanner.planDate(afterCompletion, wednesday, basicGeometry())

        assertEquals(1, reopened.tasks.size)
        assertTrue(reopened.tasks.single().status == HifzTaskStatus.COMPLETED)
    }

    @Test
    fun oneVerseLargerThanCapacityRemainsOneCanonicalVerseNeverAFraction() {
        val verse = QuranVerseRef(2, 282)
        val geometry = HifzGeometryIndex(
            mapOf(
                48 to (1..15).map { ordinal ->
                    line("48:$ordinal", 48, ordinal, verse)
                }
            )
        )
        val planned = HifzPassagePlanningPolicy.takeContiguousWithinCapacity(
            index = geometry,
            allowed = HifzVerseRange(verse, verse),
            start = verse,
            pageEquivalentCapacity = 0.10
        )

        requireNotNull(planned)
        assertEquals(verse, planned.cursor.start)
        assertEquals(verse, planned.cursor.end)
        assertEquals(48, planned.cursor.startPage)
        assertEquals(48, planned.cursor.endPage)
        assertEquals(1.0, planned.pageEquivalent, 0.0001)

        val segments = HifzGeometryPolicy.segment(geometry, HifzVerseRange(verse, verse))
        assertEquals(3, segments.size)
        assertEquals(listOf(1..5, 6..10, 11..15), segments.map {
            it.lines.first().ref.ordinal..it.lines.last().ref.ordinal
        })
    }

    @Test
    fun itqanNeverCrossesUnselectedGapAndNextTaskStartsAtNextInterval() {
        val tuesday = LocalDate.of(2026, 9, 8)
        val thursday = LocalDate.of(2026, 9, 10)
        val bounds = HifzJourneyBounds(
            sabqi = range(67, 1, 67, 2),
            itqan = listOf(
                range(2, 1, 2, 2),
                range(49, 1, 49, 2)
            )
        )
        val config = HifzJourneyConfig(
            bounds = bounds,
            pace = HifzPaceProfile(itqanMinutesPerPage = 1.0),
            availableMinutes = HifzAvailableMinutes(itqan = 30)
        )
        val geometry = gapGeometry()

        val first = HifzDailyPlanner.planDate(HifzState(journeyConfig = config), tuesday, geometry)
        val firstTask = first.tasks.single()
        assertEquals(QuranVerseRef(2, 1), firstTask.cursor.start)
        assertEquals(QuranVerseRef(2, 2), firstTask.cursor.end)

        val afterFirst = first.copy(
            tasks = listOf(firstTask.copy(status = HifzTaskStatus.COMPLETED))
        )
        val second = HifzDailyPlanner.planDate(afterFirst, thursday, geometry)
        val secondTask = second.tasks.first { it.originalScheduledDate == thursday }

        assertEquals(QuranVerseRef(49, 1), secondTask.cursor.start)
        assertEquals(QuranVerseRef(49, 2), secondTask.cursor.end)
        assertFalse(secondTask.cursor.start.surah in 3..48)
    }

    @Test
    fun midPageItqanCursorTargetsOnlyConfiguredSurahWhilePageRemainsWhole() {
        val index = HifzGeometryIndex(
            mapOf(
                515 to listOf(
                    line("515:1", 515, 1, QuranVerseRef(48, 29)),
                    line("515:2", 515, 2, QuranVerseRef(48, 29), QuranVerseRef(49, 1)),
                    line("515:3", 515, 3, QuranVerseRef(49, 1), QuranVerseRef(49, 2))
                )
            )
        )
        val target = HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(49, 2))

        val cursor = HifzPassagePlanningPolicy.cursor(index, target)
        val active = HifzGeometryPolicy.targetLines(index, target)

        assertEquals(QuranVerseRef(49, 1), cursor.start)
        assertEquals(QuranVerseRef(49, 2), cursor.end)
        assertEquals(515, cursor.startPage)
        assertTrue(active.flatMap { it.targetVerses }.all { it.surah == 49 })
        assertTrue(index.linesByPage.getValue(515).flatMap { it.verses }.any { it.surah == 48 })
    }

    @Test
    fun murajaahUsesFragilityEvidenceWithoutFixedSabqiItqanRatio() {
        val saturday = LocalDate.of(2026, 9, 12)
        val sabqi = HifzTask(
            id = "sabqi-source",
            track = HifzTrack.SABQI,
            originalScheduledDate = LocalDate.of(2026, 9, 7),
            cursor = HifzCursor.page(67, 1, 2, 562),
            quota = 20,
            status = HifzTaskStatus.COMPLETED
        )
        val itqan = HifzTask(
            id = "itqan-source",
            track = HifzTrack.ITQAN,
            originalScheduledDate = LocalDate.of(2026, 9, 8),
            cursor = HifzCursor.page(2, 1, 2, 2),
            quota = 20,
            status = HifzTaskStatus.COMPLETED
        )
        val geometry = HifzGeometryIndex(
            mapOf(
                2 to listOf(
                    line("2:1", 2, 1, QuranVerseRef(2, 1)),
                    line("2:2", 2, 2, QuranVerseRef(2, 2))
                ),
                562 to listOf(
                    line("562:1", 562, 1, QuranVerseRef(67, 1)),
                    line("562:2", 562, 2, QuranVerseRef(67, 2))
                )
            )
        )
        val state = HifzState(
            journeyConfig = HifzJourneyConfig(
                bounds = HifzJourneyBounds(
                    sabqi = range(67, 1, 67, 2),
                    itqan = listOf(range(2, 1, 2, 2))
                ),
                availableMinutes = HifzAvailableMinutes(murajaah = 45)
            ),
            tasks = listOf(sabqi, itqan),
            progressByTask = mapOf(
                sabqi.id to HifzTaskProgress(
                    taskId = sabqi.id,
                    totalIncorrectAttempts = 0,
                    totalRevealCount = 0,
                    completed = true,
                    stepIndex = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI).size
                ),
                itqan.id to HifzTaskProgress(
                    taskId = itqan.id,
                    totalIncorrectAttempts = 4,
                    totalRevealCount = 2,
                    completed = true,
                    stepIndex = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN).size
                )
            )
        )

        val planned = HifzDailyPlanner.planDate(state, saturday, geometry)
        val review = planned.tasks.firstOrNull { it.track == HifzTrack.MURAJAAH }

        requireNotNull(review)
        assertEquals(2, review.cursor.start.surah)
        assertEquals(45, review.quota)
    }

    @Test
    fun planningSameDateIsIdempotent() {
        val today = LocalDate.of(2026, 9, 9)
        val once = HifzDailyPlanner.planDate(configuredState(), today, basicGeometry())
        val twice = HifzDailyPlanner.planDate(once, today, basicGeometry())

        assertEquals(once, twice)
        assertEquals(1, once.tasks.size)
    }

    private fun configuredState(tasks: List<HifzTask> = emptyList()) = HifzState(
        journeyConfig = HifzJourneyConfig(
            bounds = HifzJourneyBounds(
                sabqi = range(1, 1, 1, 7),
                itqan = listOf(range(2, 1, 2, 2))
            ),
            pace = HifzPaceProfile(
                sabqiMinutesPerPage = 10.0,
                itqanMinutesPerPage = 5.0
            ),
            availableMinutes = HifzAvailableMinutes(
                sabqi = 30,
                itqan = 20,
                murajaah = 45
            )
        ),
        tasks = tasks
    )

    private fun basicGeometry() = HifzGeometryIndex(
        mapOf(
            1 to listOf(
                line("1:1", 1, 1, QuranVerseRef(1, 1)),
                line("1:2", 1, 2, QuranVerseRef(1, 2)),
                line("1:3", 1, 3, QuranVerseRef(1, 3)),
                line("1:4", 1, 4, QuranVerseRef(1, 4)),
                line("1:5", 1, 5, QuranVerseRef(1, 5)),
                line("1:6", 1, 6, QuranVerseRef(1, 6)),
                line("1:7", 1, 7, QuranVerseRef(1, 7))
            ),
            2 to listOf(
                line("2:1", 2, 1, QuranVerseRef(2, 1)),
                line("2:2", 2, 2, QuranVerseRef(2, 2))
            )
        )
    )

    private fun gapGeometry() = HifzGeometryIndex(
        mapOf(
            2 to listOf(
                line("2:1", 2, 1, QuranVerseRef(2, 1)),
                line("2:2", 2, 2, QuranVerseRef(2, 2))
            ),
            515 to listOf(
                line("515:1", 515, 1, QuranVerseRef(48, 29)),
                line("515:2", 515, 2, QuranVerseRef(48, 29), QuranVerseRef(49, 1)),
                line("515:3", 515, 3, QuranVerseRef(49, 1), QuranVerseRef(49, 2))
            )
        )
    )

    private fun range(ss: Int, sa: Int, es: Int, ea: Int) =
        HifzVerseRange(QuranVerseRef(ss, sa), QuranVerseRef(es, ea))

    private fun line(
        id: String,
        page: Int,
        ordinal: Int,
        vararg verses: QuranVerseRef
    ) = HifzGeometryLine(
        ref = HifzLineRef(id, page, ordinal),
        verses = verses.toSet()
    )
}

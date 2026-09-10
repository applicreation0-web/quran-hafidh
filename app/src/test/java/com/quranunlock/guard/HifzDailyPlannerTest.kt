package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HifzDailyPlannerTest {

    @Test
    fun sabqiPlansFiveRealMushafLinesWithoutRequiringMeasuredPace() {
        val today = LocalDate.of(2026, 9, 9) // Wednesday = Sabqi
        val state = configuredState().copy(
            journeyConfig = configuredState().journeyConfig.copy(
                pace = HifzPaceProfile()
            )
        )

        val planned = HifzDailyPlanner.planDate(state, today, basicGeometry())
        val task = planned.tasks.single()

        assertEquals(HifzTrack.SABQI, task.track)
        assertEquals(QuranVerseRef(1, 1), task.cursor.start)
        assertEquals(QuranVerseRef(1, 5), task.cursor.end)
        assertEquals(5, HifzGeometryPolicy.targetLines(
            basicGeometry(), HifzVerseRange(task.cursor.start, task.cursor.end)
        ).size)
        assertEquals(30, task.quota)
        assertTrue(today in planned.planningDates)
    }

    @Test
    fun sabqiNeverExceedsFiveLinesExceptForOneIndivisibleLongVerse() {
        val longVerse = QuranVerseRef(2, 282)
        val geometry = HifzGeometryIndex(
            mapOf(48 to (1..15).map { ordinal ->
                line("48:$ordinal", 48, ordinal, longVerse)
            })
        )
        val planned = HifzPassagePlanningPolicy.takeSabqiFiveLines(
            geometry,
            HifzVerseRange(longVerse, longVerse),
            longVerse
        )

        requireNotNull(planned)
        assertEquals(longVerse, planned.cursor.start)
        assertEquals(longVerse, planned.cursor.end)
        assertEquals(15, HifzGeometryPolicy.targetLines(
            geometry, HifzVerseRange(longVerse, longVerse)
        ).size)
    }

    @Test
    fun itqanPlansOnePhysicalPageAndProtocolRequiresThirtyTotalRepetitions() {
        val tuesday = LocalDate.of(2026, 9, 8)
        val planned = HifzDailyPlanner.planDate(configuredState(), tuesday, basicGeometry())
        val task = planned.tasks.single()

        assertEquals(HifzTrack.ITQAN, task.track)
        assertEquals(task.cursor.startPage, task.cursor.endPage)
        assertEquals(2, task.cursor.startPage)
        val steps = HifzTrainingPolicy.stepsFor(HifzTrack.ITQAN)
        assertEquals(20, steps.first().repetitions)
        assertEquals(0, steps.first().maskPercent)
        assertEquals(30, steps.sumOf { it.repetitions })
        assertEquals(10, steps.filter { it.maskPercent > 0 }.sumOf { it.repetitions })
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
    }

    @Test
    fun itqanNeverCrossesUnselectedGapAndNextTaskStartsAtNextInterval() {
        val tuesday = LocalDate.of(2026, 9, 8)
        val thursday = LocalDate.of(2026, 9, 10)
        val bounds = HifzJourneyBounds(
            sabqi = range(67, 1, 67, 2),
            itqan = listOf(range(2, 1, 2, 2), range(49, 1, 49, 2))
        )
        val config = HifzJourneyConfig(
            bounds = bounds,
            availableMinutes = HifzAvailableMinutes(itqan = 30)
        )
        val geometry = gapGeometry()

        val first = HifzDailyPlanner.planDate(HifzState(journeyConfig = config), tuesday, geometry)
        val firstTask = first.tasks.single()
        assertEquals(QuranVerseRef(2, 1), firstTask.cursor.start)
        assertEquals(QuranVerseRef(2, 2), firstTask.cursor.end)
        assertEquals(2, firstTask.cursor.startPage)
        assertEquals(2, firstTask.cursor.endPage)

        val afterFirst = first.copy(tasks = listOf(firstTask.copy(status = HifzTaskStatus.COMPLETED)))
        val second = HifzDailyPlanner.planDate(afterFirst, thursday, geometry)
        val secondTask = second.tasks.first { it.originalScheduledDate == thursday }
        assertEquals(QuranVerseRef(49, 1), secondTask.cursor.start)
        assertEquals(QuranVerseRef(49, 2), secondTask.cursor.end)
        assertEquals(515, secondTask.cursor.startPage)
        assertFalse(secondTask.cursor.start.surah in 3..48)
    }

    @Test
    fun midPageItqanUsesOnlyConfiguredVersesOnThatPhysicalPage() {
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
        val planned = HifzPassagePlanningPolicy.takeItqanPage(
            index, target, QuranVerseRef(49, 1)
        )

        requireNotNull(planned)
        assertEquals(515, planned.cursor.startPage)
        assertEquals(515, planned.cursor.endPage)
        val active = HifzGeometryPolicy.targetLines(index, planned.cursor)
        assertTrue(active.flatMap { it.targetVerses }.all { it.surah == 49 })
        assertTrue(index.linesByPage.getValue(515).flatMap { it.verses }.any { it.surah == 48 })
    }

    @Test
    fun murajaahUsesFragilityEvidenceWithoutFixedSabqiItqanRatio() {
        val saturday = LocalDate.of(2026, 9, 12)
        val sabqi = HifzTask(
            id = "sabqi-source", track = HifzTrack.SABQI,
            originalScheduledDate = LocalDate.of(2026, 9, 7),
            cursor = HifzCursor.page(67, 1, 2, 562), quota = 20,
            status = HifzTaskStatus.COMPLETED
        )
        val itqan = HifzTask(
            id = "itqan-source", track = HifzTrack.ITQAN,
            originalScheduledDate = LocalDate.of(2026, 9, 8),
            cursor = HifzCursor.page(2, 1, 2, 2), quota = 20,
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
                    taskId = sabqi.id, totalIncorrectAttempts = 0,
                    totalRevealCount = 0, completed = true,
                    stepIndex = HifzTrainingPolicy.stepsFor(HifzTrack.SABQI).size
                ),
                itqan.id to HifzTaskProgress(
                    taskId = itqan.id, totalIncorrectAttempts = 4,
                    totalRevealCount = 2, completed = true,
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
    fun sabqiWithoutLocalAudioStartsDirectlyWithTheVisualProtocol() {
        val today = LocalDate.of(2026, 9, 9) // Wednesday = Sabqi
        val planned = HifzDailyPlanner.planDate(
            configuredState(),
            today,
            basicGeometry(),
            audioAvailableFor = { false }
        )
        val task = planned.tasks.single()

        assertEquals(HifzTrack.SABQI, task.track)
        assertFalse(task.audioPhasesIncluded)
        val progress = HifzTrainingEngine.initial(task, segmentCount = 1)
        val first = HifzTrainingEngine.currentStep(task, progress, segmentCount = 1)
        requireNotNull(first)
        assertEquals("sabqi-visible", first.id)
        assertEquals(HifzTrainingKind.VISIBLE, first.kind)
        assertEquals(10, first.repetitions)
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
            availableMinutes = HifzAvailableMinutes(sabqi = 30, itqan = 20, murajaah = 45)
        ),
        tasks = tasks
    )

    private fun basicGeometry() = HifzGeometryIndex(
        mapOf(
            1 to (1..7).map { ordinal ->
                line("1:$ordinal", 1, ordinal, QuranVerseRef(1, ordinal))
            },
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

    private fun line(id: String, page: Int, ordinal: Int, vararg verses: QuranVerseRef) =
        HifzGeometryLine(HifzLineRef(id, page, ordinal), verses.toSet())
}
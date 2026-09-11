package com.quransafeguard.hifz.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzCoreTest {

    @Test
    fun quranCanonHas6236VersesAndCorrectSurahTransitions() {
        assertEquals(6236, QuranCanon.totalVerses)
        assertEquals(VerseRef(3, 1), QuranCanon.next(VerseRef(2, 286)))
        assertEquals(VerseRef(2, 286), QuranCanon.previous(VerseRef(3, 1)))
        assertNull(QuranCanon.next(VerseRef(114, 6)))
        assertEquals(6236, QuranCanon.ordinal(VerseRef(114, 6)))
    }

    @Test
    fun eligibleCorpusNormalizesAdjacentOrOverlappingRanges() {
        val corpus = EligibleCorpus.of(
            VerseRange(VerseRef(2, 1), VerseRef(2, 50)),
            VerseRange(VerseRef(2, 51), VerseRef(2, 74)),
            VerseRange(VerseRef(49, 1), VerseRef(50, 10))
        )

        assertEquals(2, corpus.ranges.size)
        assertEquals(VerseRange(VerseRef(2, 1), VerseRef(2, 74)), corpus.ranges[0])
    }

    @Test
    fun initialReferenceCorpusSkipsGapAfterBaqarahFrontier() {
        val corpus = referenceCorpus()

        assertEquals(VerseRef(49, 1), corpus.next(VerseRef(2, 74)))
        assertFalse(corpus.contains(VerseRef(2, 75)))
        assertFalse(corpus.contains(VerseRef(48, 29)))
        assertTrue(corpus.contains(VerseRef(49, 1)))
    }

    @Test
    fun itqanWrapsFromNasToFirstEligibleLowerRange() {
        val corpus = referenceCorpus()

        assertEquals(VerseRef(2, 1), corpus.next(VerseRef(114, 6)))
    }

    @Test
    fun grownFrontierExtendsFirstRangeWithoutMovingExistingCursor() {
        val state = referenceState(
            itqanCursor = VerseRef(52, 10),
            murajaahCursor = VerseRef(55, 1)
        )

        val grown = state.promoteTo(VerseRef(2, 95))

        assertEquals(VerseRef(2, 95), grown.promotedFrontier)
        assertEquals(VerseRef(52, 10), grown.itqanCursor)
        assertEquals(VerseRef(55, 1), grown.murajaahItqanCursor)
        assertEquals(VerseRef(49, 1), grown.corpus().next(VerseRef(2, 95)))
        assertTrue(grown.corpus().contains(VerseRef(2, 90)))
    }

    @Test
    fun rangesMergeWhenPromotedFrontierReachesUpperTail() {
        val corpus = EligibleCorpus.dynamic(
            lowerBound = VerseRef(2, 1),
            promotedFrontier = VerseRef(49, 1),
            upperTailStart = VerseRef(49, 1)
        )

        assertEquals(1, corpus.ranges.size)
        assertEquals(VerseRef(2, 1), corpus.ranges.single().start)
        assertEquals(VerseRef(114, 6), corpus.ranges.single().endInclusive)
    }

    @Test
    fun tenThousandCyclicStepsNeverEnterGap() {
        val corpus = referenceCorpus()
        var cursor = VerseRef(49, 1)

        repeat(10_000) {
            assertTrue("Cursor $cursor must stay eligible", corpus.contains(cursor))
            cursor = corpus.next(cursor)
        }
    }

    @Test
    fun sabqiPlannerAlwaysRequestsExactlyFiveQuranLines() {
        val start = MushafPosition(VerseRef(2, 75), page = 11, line = 11)
        val expected = MushafPosition(VerseRef(2, 76), page = 11, line = 15)
        val geometry = RecordingGeometry(expected)

        val block = SabqiPlanner(geometry).plan(start)

        assertEquals(5, geometry.lastRequestedLineCount)
        assertEquals(start, block.start)
        assertEquals(expected, block.endInclusive)
        assertEquals(5, block.lineCount)
        assertFalse(block.endsInsideVerse)
    }

    @Test
    fun sabqiPlannerPreservesPartialVerseBoundary() {
        val start = MushafPosition(VerseRef(2, 80), page = 12, line = 6)
        val expected = MushafPosition(
            verse = VerseRef(2, 82),
            page = 12,
            line = 10,
            fragmentIndex = 1
        )

        val block = SabqiPlanner(RecordingGeometry(expected)).plan(start)

        assertEquals(VerseRef(2, 82), block.endInclusive.verse)
        assertTrue(block.endsInsideVerse)
    }

    @Test
    fun sabqiRepetitionPlanTotals37WithFrozenStageBoundaries() {
        assertEquals(37, SabqiRepetitionPlan.TOTAL_REPS)
        assertEquals(MaskStage.VISIBLE, SabqiRepetitionPlan.stageForNextRepetition(0))
        assertEquals(MaskStage.VISIBLE, SabqiRepetitionPlan.stageForNextRepetition(14))
        assertEquals(MaskStage.MASK_25, SabqiRepetitionPlan.stageForNextRepetition(15))
        assertEquals(MaskStage.MASK_50, SabqiRepetitionPlan.stageForNextRepetition(20))
        assertEquals(MaskStage.MASK_75, SabqiRepetitionPlan.stageForNextRepetition(25))
        assertEquals(MaskStage.MASK_100, SabqiRepetitionPlan.stageForNextRepetition(30))
        assertNull(SabqiRepetitionPlan.stageForNextRepetition(37))
    }

    @Test
    fun interruptedSabqiAt23ResumesAt24WithoutReset() {
        val progress = SabqiProgress(
            block = sampleSabqiBlock(),
            completedRepetitions = 23,
            assistedRepetitions = 2
        )

        assertEquals(24, progress.nextRepetitionNumber)
        val resumed = progress.completeRepetition(assisted = true)
        assertEquals(24, resumed.completedRepetitions)
        assertEquals(3, resumed.assistedRepetitions)
    }

    @Test
    fun sabqiCannotExceed37RequiredRepetitions() {
        val complete = SabqiProgress(
            block = sampleSabqiBlock(),
            completedRepetitions = 37
        )

        assertTrue(complete.isComplete)
        assertNull(complete.nextRepetitionNumber)
    }

    @Test
    fun workingItqanMaskPlanIsCentralizedAndTotals30() {
        val plan = ItqanMaskPlan.WORKING_DEFAULT

        assertEquals(30, plan.total)
        assertEquals(MaskStage.VISIBLE, plan.stageForNextRepetition(0))
        assertEquals(MaskStage.MASK_25, plan.stageForNextRepetition(10))
        assertEquals(MaskStage.MASK_100, plan.stageForNextRepetition(25))
        assertNull(plan.stageForNextRepetition(30))
    }

    @Test
    fun interruptedItqanAt18ResumesAt19() {
        val unit = ItqanUnit(
            start = MushafPosition(VerseRef(49, 1), 515, 9),
            endInclusive = MushafPosition(VerseRef(49, 4), 515, 15)
        )
        val progress = ItqanProgress(
            unit = unit,
            completedRepetitions = 18,
            assistedRepetitions = 1
        )

        assertEquals(19, progress.nextRepetitionNumber)
        val next = progress.completeRepetition()
        assertEquals(19, next.completedRepetitions)
        assertEquals(1, next.assistedRepetitions)
    }

    @Test
    fun murajaahCursorAndItqanCursorAreIndependent() {
        val state = referenceState(
            itqanCursor = VerseRef(49, 10),
            murajaahCursor = VerseRef(52, 1)
        )

        val afterMurajaah = state.advanceMurajaahVerse()

        assertEquals(VerseRef(49, 10), afterMurajaah.itqanCursor)
        assertEquals(state.corpus().next(VerseRef(52, 1)), afterMurajaah.murajaahItqanCursor)
    }

    @Test
    fun itqanAdvanceDoesNotMoveMurajaahCursor() {
        val state = referenceState(
            itqanCursor = VerseRef(114, 6),
            murajaahCursor = VerseRef(60, 1)
        )

        val next = state.advanceItqanVerse()

        assertEquals(VerseRef(2, 1), next.itqanCursor)
        assertEquals(VerseRef(60, 1), next.murajaahItqanCursor)
    }

    @Test
    fun fridayProgramStartDoesNotInventEarlierMissedSessions() {
        val start = LocalDate.of(2026, 9, 11)

        assertNull(HifzSchedule.scheduledKind(LocalDate.of(2026, 9, 7), start))
        assertNull(HifzSchedule.scheduledKind(LocalDate.of(2026, 9, 10), start))
        assertEquals(SessionKind.SABQI, HifzSchedule.scheduledKind(start, start))
    }

    @Test
    fun weeklyScheduleMatchesFrozenDefault() {
        val start = LocalDate.of(2026, 9, 7)
        val expected = listOf(
            SessionKind.SABQI,
            SessionKind.ITQAN,
            SessionKind.SABQI,
            SessionKind.ITQAN,
            SessionKind.SABQI,
            SessionKind.MURAJAAH,
            SessionKind.MURAJAAH
        )

        val actual = (0L..6L).map { offset ->
            HifzSchedule.scheduledKind(start.plusDays(offset), start)
        }

        assertEquals(expected, actual)
    }

    @Test
    fun missedSessionBecomesOverdueWithoutChangingLaterKind() {
        val start = LocalDate.of(2026, 9, 11)
        val today = LocalDate.of(2026, 9, 15)
        val entries = HifzCalendar.entries(start, today, completedDates = setOf(start))

        val friday = entries.single { it.date == LocalDate.of(2026, 9, 11) }
        val saturday = entries.single { it.date == LocalDate.of(2026, 9, 12) }
        val tuesday = entries.single { it.date == LocalDate.of(2026, 9, 15) }

        assertEquals(SessionStatus.COMPLETED, friday.status)
        assertEquals(SessionStatus.OVERDUE, saturday.status)
        assertEquals(SessionKind.ITQAN, tuesday.kind)
        assertEquals(SessionStatus.DUE, tuesday.status)
    }

    @Test
    fun workingSessionDurationsRemainCentralized() {
        val durations = SessionDurations.WORKING_DEFAULT

        assertEquals(90, durations.minutesFor(SessionKind.SABQI))
        assertEquals(60, durations.minutesFor(SessionKind.ITQAN))
        assertEquals(45, durations.minutesFor(SessionKind.MURAJAAH))
        assertEquals(45, durations.minutesFor(SessionKind.FREE_MEMORIZATION))
    }

    @Test
    fun thirtyMinutesAtNineSecondsPerLinePlans200Lines() {
        val policy = MurajaahSpeedPolicy()

        assertEquals(200, policy.plannedLines(durationMillis = 30 * 60_000L, secondsPerLine = 9.0))
    }

    @Test
    fun speedDoesNotRecalibrateBeforeThreeSessionsAnd60Lines() {
        val policy = MurajaahSpeedPolicy()
        val samples = listOf(
            SpeedSample(activeMillis = 180_000, completedLines = 25),
            SpeedSample(activeMillis = 180_000, completedLines = 30)
        )

        assertEquals(9.0, policy.calibratedSecondsPerLine(9.0, samples), 0.0001)
    }

    @Test
    fun fastObservedSpeedIsClampedToTenPercentChange() {
        val policy = MurajaahSpeedPolicy()
        val samples = listOf(
            SpeedSample(activeMillis = 140_000, completedLines = 20),
            SpeedSample(activeMillis = 140_000, completedLines = 20),
            SpeedSample(activeMillis = 140_000, completedLines = 20)
        )

        assertEquals(8.1, policy.calibratedSecondsPerLine(9.0, samples), 0.0001)
    }

    @Test
    fun slowObservedSpeedIsClampedToTenPercentChange() {
        val policy = MurajaahSpeedPolicy()
        val samples = listOf(
            SpeedSample(activeMillis = 240_000, completedLines = 20),
            SpeedSample(activeMillis = 240_000, completedLines = 20),
            SpeedSample(activeMillis = 240_000, completedLines = 20)
        )

        assertEquals(9.9, policy.calibratedSecondsPerLine(9.0, samples), 0.0001)
    }

    @Test
    fun activeDurationTrackerCountsOnlyRunningIntervals() {
        val clock = FakeClock(1_000L)
        val tracker = ActiveDurationTracker(clock)

        tracker.start()
        clock.now = 6_000L
        assertEquals(5_000L, tracker.snapshotMillis())
        assertEquals(5_000L, tracker.pause())

        clock.now = 60_000L
        assertEquals(5_000L, tracker.snapshotMillis())

        tracker.start()
        clock.now = 63_000L
        assertEquals(8_000L, tracker.pause())
    }

    @Test
    fun repetitionCounterNeverRequestsFullScreenEinkRefresh() {
        val policy = EinkRefreshPolicy()

        repeat(37) {
            assertEquals(EinkRefreshAction.LOCAL_COUNTER, policy.repetitionChanged())
        }
        assertEquals(EinkRefreshAction.MASK_LAYER, policy.maskStageChanged())
        assertEquals(EinkRefreshAction.LOCAL_VERSE_HIGHLIGHT, policy.audioVerseChanged())
    }

    @Test
    fun workingEinkFullCleanIntervalIsCentralized() {
        val policy = EinkRefreshPolicy(fullCleanPageInterval = 8)

        assertEquals(EinkRefreshAction.PAGE_BODY_PARTIAL, policy.pageChanged(0))
        assertEquals(EinkRefreshAction.PAGE_BODY_PARTIAL, policy.pageChanged(6))
        assertEquals(EinkRefreshAction.FULL_CLEAN, policy.pageChanged(7))
    }

    private fun referenceCorpus(): EligibleCorpus = EligibleCorpus.dynamic(
        lowerBound = VerseRef(2, 1),
        promotedFrontier = VerseRef(2, 74),
        upperTailStart = VerseRef(49, 1)
    )

    private fun referenceState(
        itqanCursor: VerseRef = VerseRef(49, 1),
        murajaahCursor: VerseRef = VerseRef(49, 1)
    ): HifzState = HifzState(
        lowerEligibleBound = VerseRef(2, 1),
        promotedFrontier = VerseRef(2, 74),
        upperTailStart = VerseRef(49, 1),
        itqanCursor = itqanCursor,
        murajaahItqanCursor = murajaahCursor
    )

    private fun sampleSabqiBlock(): SabqiBlock = SabqiBlock(
        start = MushafPosition(VerseRef(2, 75), 11, 11),
        endInclusive = MushafPosition(VerseRef(2, 76), 11, 15)
    )

    private class RecordingGeometry(private val result: MushafPosition) : LineGeometry {
        var lastRequestedLineCount: Int? = null

        override fun advanceQuranLines(start: MushafPosition, lineCount: Int): MushafPosition {
            lastRequestedLineCount = lineCount
            return result
        }
    }

    private class FakeClock(var now: Long) : MonotonicClock {
        override fun nowMillis(): Long = now
    }
}

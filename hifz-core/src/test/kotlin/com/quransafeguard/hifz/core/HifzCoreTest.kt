package com.quransafeguard.hifz.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HifzCoreTest {
    @Test fun quranCanonHas6236VersesAndCrossesSurahBoundary() {
        assertEquals(6236, QuranCanon.TOTAL_VERSES)
        assertEquals(VerseRef(3,1), QuranCanon.next(VerseRef(2,286)))
        assertNull(QuranCanon.next(VerseRef(114,6)))
    }

    @Test fun referenceCorpusStartsAt49AndWrapsThroughBaqarah() {
        val state = HifzState.referenceScenario()
        val corpus = state.corpus()
        assertTrue(corpus.contains(VerseRef(2,1)))
        assertTrue(corpus.contains(VerseRef(2,74)))
        assertFalse(corpus.contains(VerseRef(2,75)))
        assertTrue(corpus.contains(VerseRef(49,1)))
        assertEquals(VerseRef(2,1), corpus.next(VerseRef(114,6)))
        assertEquals(VerseRef(49,1), corpus.next(VerseRef(2,74)))
    }

    @Test fun promotionGrowsCorpusWithoutTeleportingEitherCursor() {
        val before = HifzState.referenceScenario().copy(
            itqanCursor = VerseRef(52,10),
            murajaahCursor = VerseRef(53,1)
        )
        val after = before.promoteTo(VerseRef(2,95))
        assertEquals(VerseRef(52,10), after.itqanCursor)
        assertEquals(VerseRef(53,1), after.murajaahCursor)
        assertTrue(after.corpus().contains(VerseRef(2,95)))
    }

    @Test fun murajaahNeverAdvancesItqanCursor() {
        val before = HifzState.referenceScenario().copy(
            itqanCursor = VerseRef(49,10),
            murajaahCursor = VerseRef(52,1)
        )
        val after = before.advanceMurajaah()
        assertEquals(VerseRef(49,10), after.itqanCursor)
        assertEquals(VerseRef(52,2), after.murajaahCursor)
    }

    @Test fun sabqiIsExactlyFiveLinesAnd37Repetitions() {
        assertEquals(37, SabqiPlan.TOTAL)
        assertEquals(MaskStage.VISIBLE, SabqiPlan.stageForNext(0))
        assertEquals(MaskStage.MASK_25, SabqiPlan.stageForNext(15))
        assertEquals(MaskStage.MASK_50, SabqiPlan.stageForNext(20))
        assertEquals(MaskStage.MASK_75, SabqiPlan.stageForNext(25))
        assertEquals(MaskStage.MASK_100, SabqiPlan.stageForNext(30))
        assertNull(SabqiPlan.stageForNext(37))

        val start = MushafPosition(VerseRef(2,75), 11, 11)
        val expectedEnd = MushafPosition(VerseRef(2,76), 11, 15)
        val geometry = object : LineGeometry {
            override fun advanceQuranLines(start: MushafPosition, lineCount: Int): MushafPosition {
                assertEquals(5, lineCount)
                return expectedEnd
            }
        }
        val block = SabqiPlanner(geometry).plan(start)
        assertEquals(5, block.lineCount)
        assertEquals(expectedEnd, block.endInclusive)
    }

    @Test fun sabqiInterruptionResumesAtNextRepetition() {
        val block = SabqiBlock(
            MushafPosition(VerseRef(2,80),12,6),
            MushafPosition(VerseRef(2,82),12,10, fragmentIndex = 1)
        )
        val progress = SabqiProgress(block, completedRepetitions = 23)
        assertEquals(24, progress.nextRepetition)
        assertTrue(progress.block.endsInsideVerse)
    }

    @Test fun itqanInterruptedAt18Resumes19AndAssistanceIsRecordedSeparately() {
        val progress = ItqanProgress(VerseRef(49,7), 18, 2)
        assertEquals(19, progress.nextRepetition)
        val assisted = progress.complete(assisted = true)
        assertEquals(19, assisted.completedRepetitions)
        assertEquals(3, assisted.assistedRepetitions)
    }

    @Test fun noRetroactiveOverdueBeforeProgramStart() {
        val start = LocalDate.of(2026,9,11)
        val today = start
        assertNull(HifzSchedule.scheduled(LocalDate.of(2026,9,10), start, today))
        assertEquals(SessionType.SABQI, HifzSchedule.scheduled(start, start, today)?.type)
    }

    @Test fun weeklyScheduleMatchesFrozenWeekdays() {
        val monday = LocalDate.of(2026,9,14)
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(1).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(2).dayOfWeek))
        assertEquals(SessionType.ITQAN, HifzSchedule.typeFor(monday.plusDays(3).dayOfWeek))
        assertEquals(SessionType.SABQI, HifzSchedule.typeFor(monday.plusDays(4).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(5).dayOfWeek))
        assertEquals(SessionType.MURAJAAH, HifzSchedule.typeFor(monday.plusDays(6).dayOfWeek))
    }

    @Test fun murajaahDefaultDurationMatchesThirtyPlusThirty() {
        assertEquals(60, SessionDurations().murajaahMinutes)
    }

    @Test fun thirtyMinutesAtNineSecondsPerLinePlans200Lines() {
        assertEquals(200, MurajaahPlanner.plannedLines(1800, 9.0))
    }

    @Test fun calibrationWaitsForEvidenceAndClampsToTenPercent() {
        val insufficient = SpeedCalibration(9.0, validSessions = 2, validLines = 55)
        assertEquals(9.0, MurajaahPlanner.recalibrate(insufficient, 7.0).secondsPerLine)
        val sufficient = SpeedCalibration(9.0, validSessions = 3, validLines = 60)
        assertEquals(8.1, MurajaahPlanner.recalibrate(sufficient, 7.0).secondsPerLine, 0.0001)
        assertEquals(9.9, MurajaahPlanner.recalibrate(sufficient, 12.0).secondsPerLine, 0.0001)
    }

    @Test fun activeCounterNeverUsesWallClockAndNeverExceedsTarget() {
        val counter = ActiveDurationCounter(60_000)
        counter.addActive(20_000)
        assertEquals(40_000, counter.remainingMs)
        counter.addActive(100_000)
        assertTrue(counter.expired)
        assertEquals(60_000, counter.activeMs)
    }

    @Test fun repetitionRefreshOnlyTouchesCounterUnlessMaskChanges() {
        assertEquals(setOf(RefreshRegion.COUNTER), EinkPolicy.onRepetition(false))
        assertEquals(setOf(RefreshRegion.COUNTER, RefreshRegion.MASK), EinkPolicy.onRepetition(true))
        assertEquals(setOf(RefreshRegion.VERSE_HIGHLIGHT), EinkPolicy.onAudioVerseChanged())
    }
}

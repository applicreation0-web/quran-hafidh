package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzPlanningPolicyTest {
    @Test
    fun itqanSupportsSeveralIndependentOrderedIntervals() {
        val baqarah = HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 74))
        val hujuratToNas = HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
        val bounds = HifzJourneyBounds(
            sabqi = HifzVerseRange(QuranVerseRef(2, 75), QuranVerseRef(48, 29)),
            itqan = listOf(baqarah, hujuratToNas)
        )

        assertEquals(listOf(baqarah, hujuratToNas), bounds.itqan)
        assertEquals(QuranVerseRef(2, 74), bounds.itqan[0].end)
        assertEquals(QuranVerseRef(49, 1), bounds.itqan[1].start)
    }

    @Test
    fun itqanIntervalsCannotOverlapOrBeOutOfOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzJourneyBounds(
                sabqi = HifzVerseRange(QuranVerseRef(67, 1), QuranVerseRef(67, 30)),
                itqan = listOf(
                    HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6)),
                    HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 286))
                )
            )
        }
    }

    @Test
    fun itqanSkipsGapAndWrapsAfterNasInsteadOfStopping() {
        val intervals = listOf(
            HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 74)),
            HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
        )

        assertEquals(QuranVerseRef(49, 1), HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(2, 74)))
        assertEquals(QuranVerseRef(2, 1), HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(114, 6)))
        assertEquals(QuranVerseRef(49, 1), HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(3, 1)))
    }

    @Test
    fun growingFirstItqanRangeDoesNotChangeTraversalOrder() {
        val intervals = listOf(
            HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 95)),
            HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
        )
        assertEquals(QuranVerseRef(49, 1), HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(2, 95)))
        assertEquals(QuranVerseRef(52, 11), HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(52, 10)))
    }

    @Test
    fun reversedSetupBoundsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzVerseRange(QuranVerseRef(67, 10), QuranVerseRef(67, 1))
        }
    }

    @Test
    fun legacyQuotaHelperUsesSeparateMeasuredPacesAndRealAvailableTime() {
        val pace = HifzPaceProfile(
            sabqiMinutesPerPage = 15.0,
            itqanMinutesPerPage = 5.0,
            murajaahMinutesPerPage = 3.0
        )

        assertEquals(3.0, HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.SABQI, 45.0, pace)!!, 0.0001)
        assertEquals(9.0, HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.ITQAN, 45.0, pace)!!, 0.0001)
        assertEquals(15.0, HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.MURAJAAH, 45.0, pace)!!, 0.0001)
    }

    @Test
    fun unmeasuredSabqiOrItqanDoesNotInventFallbackSpeed() {
        val pace = HifzPaceProfile()

        assertNull(HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.SABQI, 30.0, pace))
        assertNull(HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.ITQAN, 30.0, pace))
        assertEquals(20.0, HifzTimeQuotaPolicy.pageEquivalentCapacity(HifzTrack.MURAJAAH, 45.0, pace)!!, 0.0001)
    }

    @Test
    fun murajaahKeepsAcceptedOneRecitationAndNineSecondLineReference() {
        assertEquals(1, MurajaahPolicy.RECITATIONS_PER_PORTION)
        assertEquals(true, MurajaahPolicy.LOCAL_CORRECTION_ONLY)
        assertEquals(20, MurajaahPolicy.INITIAL_REFERENCE_PAGES)
        assertEquals(45, MurajaahPolicy.INITIAL_REFERENCE_MINUTES)
        assertEquals(9.0, MurajaahPolicy.INITIAL_SECONDS_PER_LINE, 0.0001)
    }

    @Test
    fun legacyAdaptiveOrderDoesNotInjectAnyFixedSabqiItqanRatio() {
        val sabqiA = candidate("sabqi-a", MurajaahOrigin.RECENT_SABQI)
        val sabqiB = candidate("sabqi-b", MurajaahOrigin.RECENT_SABQI)
        val sabqiC = candidate("sabqi-c", MurajaahOrigin.RECENT_SABQI)
        val itqan = candidate("itqan", MurajaahOrigin.CONSOLIDATED_ITQAN)

        val selected = MurajaahPolicy.takeAdaptiveOrder(
            listOf(sabqiA, sabqiB, sabqiC, itqan),
            maxItems = 3
        )

        assertEquals(listOf("sabqi-a", "sabqi-b", "sabqi-c"), selected.map { it.id })
    }

    private fun candidate(id: String, origin: MurajaahOrigin) = MurajaahCandidate(
        id = id,
        cursor = HifzCursor.page(67, 1, 7, 562),
        origin = origin,
        volumePageEquivalent = 1.0,
        lastReviewedDate = LocalDate.of(2026, 9, 1),
        fragilityRank = 1,
        errorCount = 0,
        observedMinutesPerPage = 2.5
    )
}

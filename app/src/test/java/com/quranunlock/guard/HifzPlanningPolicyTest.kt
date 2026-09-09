package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class HifzPlanningPolicyTest {
    @Test
    fun sabqiAndItqanKeepIndependentExactVerseBounds() {
        val bounds = HifzJourneyBounds(
            sabqi = HifzVerseRange(QuranVerseRef(67, 1), QuranVerseRef(67, 30)),
            itqan = HifzVerseRange(QuranVerseRef(78, 1), QuranVerseRef(79, 46))
        )

        assertEquals(QuranVerseRef(67, 1), bounds.sabqi.start)
        assertEquals(QuranVerseRef(67, 30), bounds.sabqi.end)
        assertEquals(QuranVerseRef(78, 1), bounds.itqan.start)
        assertEquals(QuranVerseRef(79, 46), bounds.itqan.end)
    }

    @Test
    fun reversedSetupBoundsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzVerseRange(QuranVerseRef(67, 10), QuranVerseRef(67, 1))
        }
    }

    @Test
    fun quotaUsesSeparateMeasuredPacesAndRealAvailableTime() {
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
    fun murajaahUsesAcceptedOneRecitationAndLocalCorrectionReference() {
        assertEquals(1, MurajaahPolicy.RECITATIONS_PER_PORTION)
        assertEquals(true, MurajaahPolicy.LOCAL_CORRECTION_ONLY)
        assertEquals(20, MurajaahPolicy.INITIAL_REFERENCE_PAGES)
        assertEquals(45, MurajaahPolicy.INITIAL_REFERENCE_MINUTES)
    }

    @Test
    fun murajaahDoesNotInjectAnyFixedSabqiItqanRatio() {
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

    @Test
    fun adaptiveInputsRetainVolumeAgeFragilityErrorsAndSpeedWithoutInventedWeights() {
        val candidate = MurajaahCandidate(
            id = "review-1",
            cursor = HifzCursor.page(67, 1, 7, 562),
            origin = MurajaahOrigin.CONSOLIDATED_ITQAN,
            volumePageEquivalent = 1.0,
            lastReviewedDate = LocalDate.of(2026, 9, 1),
            fragilityRank = 3,
            errorCount = 2,
            observedMinutesPerPage = 2.8
        )

        assertEquals(1.0, candidate.volumePageEquivalent, 0.0001)
        assertEquals(LocalDate.of(2026, 9, 1), candidate.lastReviewedDate)
        assertEquals(3, candidate.fragilityRank)
        assertEquals(2, candidate.errorCount)
        assertEquals(2.8, candidate.observedMinutesPerPage!!, 0.0001)
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

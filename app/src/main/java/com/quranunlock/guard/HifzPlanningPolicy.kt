package com.applicreation0.quransafeguard

import java.time.LocalDate

/** Rare setup bounds: Sabqi and Itqan each keep their own exact Quran start/end. */
data class HifzVerseRange(
    val start: QuranVerseRef,
    val end: QuranVerseRef
) {
    init {
        QuranCanonicalBounds.requireValid(start)
        QuranCanonicalBounds.requireValid(end)
        require(compareRefs(start, end) <= 0) { "Hifz range end cannot precede its start." }
    }

    companion object {
        private fun compareRefs(left: QuranVerseRef, right: QuranVerseRef): Int =
            compareValuesBy(left, right, QuranVerseRef::surah, QuranVerseRef::ayah)
    }
}

data class HifzJourneyBounds(
    val sabqi: HifzVerseRange,
    val itqan: HifzVerseRange
)

/**
 * Observed pace is deliberately separate for each track. Null means Safeguard has not
 * measured enough information and must not invent a speed for that track.
 */
data class HifzPaceProfile(
    val sabqiMinutesPerPage: Double? = null,
    val itqanMinutesPerPage: Double? = null,
    val murajaahMinutesPerPage: Double? = null
) {
    init {
        listOfNotNull(sabqiMinutesPerPage, itqanMinutesPerPage, murajaahMinutesPerPage)
            .forEach { require(it > 0.0 && it.isFinite()) { "Hifz pace must be positive and finite." } }
    }

    fun minutesPerPage(track: HifzTrack): Double? = when (track) {
        HifzTrack.SABQI -> sabqiMinutesPerPage
        HifzTrack.ITQAN -> itqanMinutesPerPage
        HifzTrack.MURAJAAH -> murajaahMinutesPerPage
            ?: MurajaahPolicy.INITIAL_MINUTES_PER_PAGE_REFERENCE
    }
}

/**
 * Converts real available time into a page-equivalent capacity without rounding up.
 * The caller may turn a fractional page into an exact verse portion; this policy never
 * silently forces a whole extra page that exceeds the user's available time.
 */
object HifzTimeQuotaPolicy {
    fun pageEquivalentCapacity(
        track: HifzTrack,
        availableMinutes: Double,
        pace: HifzPaceProfile
    ): Double? {
        require(availableMinutes >= 0.0 && availableMinutes.isFinite()) {
            "Available Hifz time must be finite and non-negative."
        }
        val minutesPerPage = pace.minutesPerPage(track) ?: return null
        return availableMinutes / minutesPerPage
    }
}

enum class MurajaahOrigin {
    RECENT_SABQI,
    CONSOLIDATED_ITQAN
}

/**
 * Raw adaptive inputs retained for Murajaah ranking. The foundation deliberately does
 * not assign arbitrary weights to them: the adaptive ranking layer must make that choice
 * explicitly and can evolve it from observed performance.
 */
data class MurajaahCandidate(
    val id: String,
    val cursor: HifzCursor,
    val origin: MurajaahOrigin,
    val volumePageEquivalent: Double,
    val lastReviewedDate: LocalDate?,
    val fragilityRank: Int,
    val errorCount: Int,
    val observedMinutesPerPage: Double?
) {
    init {
        require(id.isNotBlank())
        require(volumePageEquivalent > 0.0 && volumePageEquivalent.isFinite())
        require(fragilityRank >= 0)
        require(errorCount >= 0)
        observedMinutesPerPage?.let {
            require(it > 0.0 && it.isFinite())
        }
    }
}

/**
 * Murajaah covers recent Sabqi and consolidated Itqan without any fixed source ratio.
 * The adaptive scorer/order is intentionally external because no fixed weighting between
 * volume, age, fragility, errors, speed and available time has been approved yet.
 */
object MurajaahPolicy {
    const val RECITATIONS_PER_PORTION = 1
    const val LOCAL_CORRECTION_ONLY = true
    const val INITIAL_REFERENCE_PAGES = 20
    const val INITIAL_REFERENCE_MINUTES = 45
    const val INITIAL_MINUTES_PER_PAGE_REFERENCE = 2.25

    /**
     * Applies an adaptive order produced from the required factors. This method never
     * inserts, balances or reserves a Sabqi/Itqan ratio on its own.
     */
    fun takeAdaptiveOrder(
        candidatesInAdaptivePriorityOrder: List<MurajaahCandidate>,
        maxItems: Int
    ): List<MurajaahCandidate> {
        require(maxItems >= 0)
        if (maxItems == 0) return emptyList()
        val seen = mutableSetOf<String>()
        return candidatesInAdaptivePriorityOrder
            .filter { seen.add(it.id) }
            .take(maxItems)
    }
}

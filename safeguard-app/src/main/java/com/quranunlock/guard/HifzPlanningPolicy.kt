package com.applicreation0.quransafeguard

import java.time.LocalDate

internal fun compareQuranVerseRefs(left: QuranVerseRef, right: QuranVerseRef): Int =
    compareValuesBy(left, right, QuranVerseRef::surah, QuranVerseRef::ayah)

/** Canonical Quran interval. Verse identity is always the primary Hifz unit. */
data class HifzVerseRange(
    val start: QuranVerseRef,
    val end: QuranVerseRef
) {
    init {
        QuranCanonicalBounds.requireValid(start)
        QuranCanonicalBounds.requireValid(end)
        require(compareQuranVerseRefs(start, end) <= 0) {
            "Hifz range end cannot precede its start."
        }
    }

    fun contains(ref: QuranVerseRef): Boolean =
        compareQuranVerseRefs(start, ref) <= 0 && compareQuranVerseRefs(ref, end) <= 0

    fun isStrictlyBefore(other: HifzVerseRange): Boolean =
        compareQuranVerseRefs(end, other.start) < 0
}

/**
 * Rare setup bounds. Sabqi has its own canonical range. Itqan may contain several
 * independent Quran intervals. Gaps are intentional and are never silently merged.
 */
data class HifzJourneyBounds(
    val sabqi: HifzVerseRange,
    val itqan: List<HifzVerseRange>
) {
    init {
        require(itqan.isNotEmpty()) { "At least one Itqan interval is required." }
        itqan.zipWithNext().forEach { (previous, next) ->
            require(previous.isStrictlyBefore(next)) {
                "Itqan intervals must be ordered and non-overlapping."
            }
        }
    }
}

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

    /** Stores only an actually observed pace; no fallback is persisted as a measurement. */
    fun withObserved(track: HifzTrack, minutesPerPage: Double): HifzPaceProfile {
        require(minutesPerPage > 0.0 && minutesPerPage.isFinite()) {
            "Observed Hifz pace must be positive and finite."
        }
        return when (track) {
            HifzTrack.SABQI -> copy(sabqiMinutesPerPage = minutesPerPage)
            HifzTrack.ITQAN -> copy(itqanMinutesPerPage = minutesPerPage)
            HifzTrack.MURAJAAH -> copy(murajaahMinutesPerPage = minutesPerPage)
        }
    }
}

/**
 * Legacy page-equivalent helper retained only for compatibility with existing Claude
 * state/statistics. New Hifz planning must not use page-equivalent pace as the primary
 * business unit when an exact verse/line calculation is available.
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

/** Verse-first endless cyclic traversal across a possibly discontinuous Itqan corpus. */
object HifzItqanTraversalPolicy {
    /**
     * Returns the next eligible verse. When the last eligible interval ends, traversal
     * wraps to the first interval instead of returning null. Gaps are skipped by jumping
     * to the next interval's start. This is the frozen endless-Itqan rule.
     */
    fun nextAfter(intervals: List<HifzVerseRange>, current: QuranVerseRef): QuranVerseRef {
        require(intervals.isNotEmpty())
        intervals.zipWithNext().forEach { (previous, next) ->
            require(previous.isStrictlyBefore(next))
        }

        val containingIndex = intervals.indexOfFirst { it.contains(current) }
        if (containingIndex >= 0) {
            val interval = intervals[containingIndex]
            val nextVerse = nextCanonicalVerse(current)
            if (nextVerse != null && interval.contains(nextVerse)) return nextVerse
            return intervals.getOrNull(containingIndex + 1)?.start ?: intervals.first().start
        }

        // A cursor recovered inside a gap resumes at the next eligible interval. If it
        // lies after the final range, wrap to the first range rather than ending Itqan.
        return intervals.firstOrNull { compareQuranVerseRefs(current, it.start) < 0 }?.start
            ?: intervals.first().start
    }

    internal fun nextCanonicalVerse(ref: QuranVerseRef): QuranVerseRef? {
        QuranCanonicalBounds.requireValid(ref)
        val ayahCount = requireNotNull(QuranCanonicalBounds.ayahCount(ref.surah))
        return when {
            ref.ayah < ayahCount -> QuranVerseRef(ref.surah, ref.ayah + 1)
            ref.surah < QuranCanonicalBounds.SURAH_COUNT -> QuranVerseRef(ref.surah + 1, 1)
            else -> null
        }
    }
}

enum class MurajaahOrigin {
    RECENT_SABQI,
    CONSOLIDATED_ITQAN
}

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

object MurajaahPolicy {
    const val RECITATIONS_PER_PORTION = 1
    const val LOCAL_CORRECTION_ONLY = true
    const val INITIAL_REFERENCE_PAGES = 20
    const val INITIAL_REFERENCE_MINUTES = 45
    const val INITIAL_MINUTES_PER_PAGE_REFERENCE = 2.25
    const val INITIAL_SECONDS_PER_LINE = 9.0

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

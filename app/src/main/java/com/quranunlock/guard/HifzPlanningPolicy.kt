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

/** Verse-first traversal across a possibly discontinuous Itqan corpus. */
object HifzItqanTraversalPolicy {
    fun nextAfter(intervals: List<HifzVerseRange>, current: QuranVerseRef): QuranVerseRef? {
        require(intervals.isNotEmpty())
        intervals.zipWithNext().forEach { (previous, next) ->
            require(previous.isStrictlyBefore(next))
        }

        val containingIndex = intervals.indexOfFirst { it.contains(current) }
        if (containingIndex >= 0) {
            val interval = intervals[containingIndex]
            val nextVerse = nextCanonicalVerse(current)
            if (nextVerse != null && interval.contains(nextVerse)) return nextVerse
            return intervals.getOrNull(containingIndex + 1)?.start
        }

        return intervals.firstOrNull { compareQuranVerseRefs(current, it.start) < 0 }?.start
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

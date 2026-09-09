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

    fun isStrictlyBefore(other: HifzVerseRange): Boolean =
        compareQuranVerseRefs(end, other.start) < 0
}

/**
 * Rare setup bounds. Sabqi has its own canonical range. Itqan may contain several
 * independent Quran intervals (for example Al-Baqarah plus Al-Hujurat through An-Nas).
 * Gaps are intentional and are never silently merged into the Itqan corpus.
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
 * Optional pedagogical subdivision inside a canonical verse target. Line numbers are
 * 1-based within a fixed Medina Mushaf page and are never an official progress cursor.
 */
data class HifzLineWindow(
    val page: Int,
    val firstLine: Int,
    val lastLine: Int
) {
    init {
        require(page in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT)
        require(firstLine >= 1)
        require(lastLine >= firstLine)
    }
}

data class HifzPedagogicalSegment(
    val canonicalTarget: HifzVerseRange,
    val lineWindow: HifzLineWindow
)

/**
 * Splits a long passage into line-sized training chunks while keeping the exact same
 * canonical verse target on every chunk. Completing one chunk cannot move the Hifz
 * verse cursor; only validation of the canonical target may do that.
 */
object HifzLineSegmentationPolicy {
    fun split(
        canonicalTarget: HifzVerseRange,
        page: Int,
        firstLine: Int,
        lastLine: Int,
        maxLinesPerSegment: Int
    ): List<HifzPedagogicalSegment> {
        require(maxLinesPerSegment > 0)
        val whole = HifzLineWindow(page, firstLine, lastLine)
        val result = mutableListOf<HifzPedagogicalSegment>()
        var cursor = whole.firstLine
        while (cursor <= whole.lastLine) {
            val end = minOf(whole.lastLine, cursor + maxLinesPerSegment - 1)
            result += HifzPedagogicalSegment(
                canonicalTarget = canonicalTarget,
                lineWindow = HifzLineWindow(page, cursor, end)
            )
            cursor = end + 1
        }
        return result
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

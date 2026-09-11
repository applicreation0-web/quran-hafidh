package com.applicreation0.quransafeguard

/** Canonical Quran bounds used by the structured Hifz cursor. */
object QuranCanonicalBounds {
    const val SURAH_COUNT = 114
    const val MUSHAF_PAGE_COUNT = 604
    const val TOTAL_VERSES = 6236

    private val ayahsPerSurah = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99,
        128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34,
        30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29,
        18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12,
        12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19,
        36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    init {
        check(ayahsPerSurah.size == SURAH_COUNT)
        check(ayahsPerSurah.sum() == TOTAL_VERSES)
    }

    fun ayahCount(surah: Int): Int? = ayahsPerSurah.getOrNull(surah - 1)

    fun isValid(ref: QuranVerseRef): Boolean =
        ref.surah in 1..SURAH_COUNT &&
            ref.ayah in 1..(ayahCount(ref.surah) ?: 0)

    fun requireValid(ref: QuranVerseRef) {
        require(isValid(ref)) { "Invalid Quran verse reference: ${ref.surah}:${ref.ayah}" }
    }
}

/**
 * Typed, immutable Hifz position. Verse identity remains the primary user-facing unit.
 * Optional real-line ids preserve an exact five-line Sabqi boundary when the block ends
 * inside a verse. Legacy cursors remain valid with null line ids.
 */
data class HifzCursor(
    val start: QuranVerseRef,
    val end: QuranVerseRef = start,
    val startPage: Int,
    val endPage: Int = startPage,
    val startLineId: String? = null,
    val endLineId: String? = null,
    val endVersePartial: Boolean = false
) {
    init {
        QuranCanonicalBounds.requireValid(start)
        QuranCanonicalBounds.requireValid(end)
        require(compareVerseRefs(start, end) <= 0) { "Hifz cursor end cannot precede its start." }
        require(startPage in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT) {
            "Invalid Hifz start page: $startPage"
        }
        require(endPage in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT) {
            "Invalid Hifz end page: $endPage"
        }
        require(startPage <= endPage) { "Hifz end page cannot precede its start page." }
        require((startLineId == null) == (endLineId == null)) {
            "Exact Hifz line bounds must provide both start and end line ids."
        }
        startLineId?.let { require(it.isNotBlank()) }
        endLineId?.let { require(it.isNotBlank()) }
        require(!endVersePartial || endLineId != null) {
            "A partial verse boundary requires an exact end line id."
        }
    }

    val hasExactLineBounds: Boolean get() = startLineId != null && endLineId != null

    val label: String
        get() {
            val verses = if (start == end) start.label else "${start.label}–${end.label}"
            val partial = if (endVersePartial) " (partiel)" else ""
            return "$verses$partial"
        }

    companion object {
        fun page(surah: Int, startAyah: Int, endAyah: Int = startAyah, page: Int): HifzCursor =
            HifzCursor(
                start = QuranVerseRef(surah, startAyah),
                end = QuranVerseRef(surah, endAyah),
                startPage = page,
                endPage = page
            )

        fun range(
            startSurah: Int,
            startAyah: Int,
            endSurah: Int,
            endAyah: Int,
            startPage: Int,
            endPage: Int
        ): HifzCursor = HifzCursor(
            start = QuranVerseRef(startSurah, startAyah),
            end = QuranVerseRef(endSurah, endAyah),
            startPage = startPage,
            endPage = endPage
        )

        private fun compareVerseRefs(left: QuranVerseRef, right: QuranVerseRef): Int =
            compareValuesBy(left, right, QuranVerseRef::surah, QuranVerseRef::ayah)
    }
}

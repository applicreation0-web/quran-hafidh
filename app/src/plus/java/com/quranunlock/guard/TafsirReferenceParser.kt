package com.applicreation0.quransafeguard

/**
 * Finds only explicit chapter:verse notation already present in a Tafsir source.
 * Nothing is inferred from prose. Invalid chapter/verse numbers are deliberately
 * left as plain source text so a source typo cannot become a false Quran link.
 */
internal object TafsirReferenceParser {
    private val explicitReference = Regex(
        """(?<!\d)(\d{1,3}):(\d{1,3})(?:\s*[–—-]\s*(\d{1,3}))?(?!\d)"""
    )

    private val verseCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52,
        99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69,
        60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35,
        38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13, 14, 11, 11,
        18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8,
        8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    data class Match(
        val start: Int,
        val endExclusive: Int,
        val reference: QuranReferenceRef
    )

    fun find(text: String): List<Match> = explicitReference.findAll(text)
        .mapNotNull { match ->
            val surah = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
            val startAyah = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
            val endAyah = match.groupValues[3].takeIf(String::isNotBlank)
                ?.toIntOrNull() ?: startAyah
            if (!isCanonical(surah, startAyah) ||
                !isCanonical(surah, endAyah) ||
                endAyah < startAyah
            ) {
                return@mapNotNull null
            }
            Match(
                start = match.range.first,
                endExclusive = match.range.last + 1,
                reference = QuranReferenceRef(surah, startAyah, endAyah)
            )
        }
        .toList()

    fun isCanonical(surah: Int, ayah: Int): Boolean =
        surah in 1..verseCounts.size && ayah in 1..verseCounts[surah - 1]
}

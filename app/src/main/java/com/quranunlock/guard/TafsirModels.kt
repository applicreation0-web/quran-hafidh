package com.applicreation0.quransafeguard

data class VerseRef(
    val surah: Int,
    val ayah: Int
)

enum class TafsirEditionId(
    val stableId: String,
    val displayName: String,
    val coverageLabel: String
) {
    JALALAYN(
        stableId = "jalalayn_en",
        displayName = "Jalalayn",
        coverageLabel = "Complete Qur'an coverage"
    ),
    QURTUBI(
        stableId = "qurtubi_en_bewley",
        displayName = "Qurtubi",
        coverageLabel = "English volumes available through Qur'an 4:23"
    ),
    QUSHAYRI(
        stableId = "qushayri_en_sands",
        displayName = "Qushayri",
        coverageLabel = "English edition available for Sūras 1–4"
    );

    fun covers(verse: VerseRef): Boolean = when (this) {
        JALALAYN -> verse.surah in 1..114 && verse.ayah > 0
        QURTUBI ->
            verse.surah in 1..3 ||
                (verse.surah == 4 && verse.ayah in 1..23)
        QUSHAYRI -> verse.surah in 1..4 && verse.ayah > 0
    }

    companion object {
        fun fromStableId(value: String?): TafsirEditionId =
            entries.firstOrNull { it.stableId == value } ?: JALALAYN

        /**
         * Only editions whose reviewed source covers the tapped verse are offered.
         * Jalalayn is always the baseline/default edition.
         */
        fun availableFor(verse: VerseRef): List<TafsirEditionId> =
            entries.filter { it.covers(verse) }

        /**
         * If a previously selected partial edition does not cover the new verse,
         * fall back explicitly to Jalalayn instead of showing an unavailable edition.
         */
        fun effectiveFor(
            verse: VerseRef,
            preferred: TafsirEditionId
        ): TafsirEditionId = preferred.takeIf { it.covers(verse) } ?: JALALAYN
    }
}

data class TafsirRequestKey(
    val verse: VerseRef,
    val editionId: TafsirEditionId
)

data class TafsirNote(
    val number: Int,
    val runs: List<TafsirRun>,
    val sourceLabel: String? = null
) {
    val displayLabel: String
        get() = sourceLabel?.trim()?.takeIf(String::isNotBlank) ?: number.toString()
}

data class TafsirEntry(
    val verse: VerseRef,
    val commentaryRuns: List<TafsirRun>,
    val notes: List<TafsirNote>,
    val editionId: TafsirEditionId = TafsirEditionId.JALALAYN,
    val verseStart: Int = verse.ayah,
    val verseEnd: Int = verse.ayah,
    val segmentCount: Int = 1,
    val sourceRanges: List<IntRange> = listOf(verseStart..verseEnd)
) {
    val rangeLabel: String?
        get() {
            val distinct = sourceRanges.distinct()
            if (distinct.size != 1) return null
            val range = distinct.single()
            return if (range.last > range.first) {
                "Commentary on ${verse.surah}:${range.first}–${range.last}"
            } else {
                null
            }
        }
}

enum class TafsirRunStyle {
    REGULAR,
    ITALIC,
    BOLD,
    BOLD_ITALIC,
    NOTE_REF
}

data class TafsirRun(
    val style: TafsirRunStyle,
    val text: String
)

sealed interface TafsirLoadState {
    data object Closed : TafsirLoadState
    data object Loading : TafsirLoadState
    data object Unavailable : TafsirLoadState
    data class Available(val entry: TafsirEntry) : TafsirLoadState
}

object MushafVerseIndex {
    private val polygonTag = Regex(
        """<path\b[^>]*\bclass=[\"'][^\"']*\bayahPolygon\b[^\"']*[\"'][^>]*>""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val surahAttribute = Regex("""\bsurah=[\"'](\d+)[\"']""")
    private val ayahAttribute = Regex("""\bayah=[\"'](\d+)[\"']""")

    fun fromSvg(svg: String): Set<VerseRef> = polygonTag.findAll(svg)
        .mapNotNull { match ->
            val tag = match.value
            val surah = surahAttribute.find(tag)?.groupValues?.get(1)?.toIntOrNull()
            val ayah = ayahAttribute.find(tag)?.groupValues?.get(1)?.toIntOrNull()
            if (surah == null || ayah == null) null else VerseRef(surah, ayah)
        }
        .toSet()
}

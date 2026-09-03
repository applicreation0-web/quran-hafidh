package com.applicreation0.quransafeguard

data class VerseRef(
    val surah: Int,
    val ayah: Int
)

data class TafsirNote(
    val number: Int,
    val runs: List<TafsirRun>
)

data class TafsirEntry(
    val verse: VerseRef,
    val commentaryRuns: List<TafsirRun>,
    val notes: List<TafsirNote>
)

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

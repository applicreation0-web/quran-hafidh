package com.applicreation0.quransafeguard

data class VerseRef(
    val surah: Int,
    val ayah: Int
)

data class QuranReferenceRef(
    val surah: Int,
    val startAyah: Int,
    val endAyah: Int = startAyah
) {
    val startVerse: VerseRef
        get() = VerseRef(surah, startAyah)

    val label: String
        get() = if (startAyah == endAyah) {
            "$surah:$startAyah"
        } else {
            "$surah:$startAyah–$endAyah"
        }
}

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
    TECHNICAL_TERM,
    TRANSLITERATION,
    POETRY,
    NOTE_REF
}

data class TafsirRun(
    val style: TafsirRunStyle,
    val text: String
)

enum class TafsirBlockKind {
    PROSE,
    POETRY
}

data class TafsirRenderBlock(
    val kind: TafsirBlockKind,
    val runs: List<TafsirRun>
)

/**
 * Preserve explicit source semantics at paragraph level. Poetry is recognized
 * only when the source/extractor supplied the POETRY role; italics alone never
 * imply poetry. This makes the non-justified poetry rule executable without
 * guessing from typography.
 */
internal fun splitTafsirRenderBlocks(runs: List<TafsirRun>): List<TafsirRenderBlock> {
    if (runs.isEmpty()) return emptyList()
    val blocks = mutableListOf<TafsirRenderBlock>()
    var currentKind: TafsirBlockKind? = null
    var currentRuns = mutableListOf<TafsirRun>()

    fun flush() {
        val kind = currentKind ?: return
        if (currentRuns.isNotEmpty()) {
            blocks += TafsirRenderBlock(kind, currentRuns.toList())
        }
        currentRuns = mutableListOf()
    }

    runs.forEach { run ->
        val kind = if (run.style == TafsirRunStyle.POETRY) {
            TafsirBlockKind.POETRY
        } else {
            TafsirBlockKind.PROSE
        }
        if (currentKind != null && currentKind != kind) flush()
        currentKind = kind
        currentRuns += run
    }
    flush()
    return blocks
}

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

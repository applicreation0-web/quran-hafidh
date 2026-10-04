package com.applicreation0.quransafeguard

import android.content.Context
import org.brotli.dec.BrotliInputStream

/**
 * Verifies that the typed Hifz cursor actually points to the verse polygons contained
 * in the fixed Medina Mushaf pages bundled with the app. This closes the gap between
 * individually valid verse/page numbers and a truly coherent verse↔page pair.
 */
object HifzMushafCursorVerifier {
    private val elementRegex = Regex("""<[^>]+>""")
    private val classRegex = Regex("""\bclass\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
    private val surahRegex = Regex("""\bsurah\s*=\s*["']([0-9]{1,3})["']""", RegexOption.IGNORE_CASE)
    private val ayahRegex = Regex("""\bayah\s*=\s*["']([0-9]{1,3})["']""", RegexOption.IGNORE_CASE)
    private val whitespaceRegex = Regex("""\s+""")

    internal fun verseRefsInSvg(svg: String): Set<QuranVerseRef> =
        elementRegex.findAll(svg).mapNotNull { match ->
            val tag = match.value
            val classes = classRegex.find(tag)?.groupValues?.get(1)
                ?.split(whitespaceRegex)
                .orEmpty()
            if ("ayahPolygon" !in classes) return@mapNotNull null

            val surah = surahRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null
            val ayah = ayahRegex.find(tag)?.groupValues?.get(1)?.toIntOrNull()
                ?: return@mapNotNull null
            QuranVerseRef(surah, ayah).takeIf(QuranCanonicalBounds::isValid)
        }.toSet()

    /** Pure form used by tests and by the Android asset-backed verifier. */
    fun allCoherent(
        cursors: Collection<HifzCursor>,
        pageSvg: (Int) -> String?
    ): Boolean {
        if (cursors.isEmpty()) return true
        val cache = mutableMapOf<Int, Set<QuranVerseRef>?>()

        fun refs(page: Int): Set<QuranVerseRef>? {
            if (!cache.containsKey(page)) {
                cache[page] = pageSvg(page)?.let(::verseRefsInSvg)
            }
            return cache[page]
        }

        for (cursor in cursors) {
            val startRefs = refs(cursor.startPage) ?: return false
            if (cursor.start !in startRefs) return false
            val endRefs = if (cursor.endPage == cursor.startPage) {
                startRefs
            } else {
                refs(cursor.endPage) ?: return false
            }
            if (cursor.end !in endRefs) return false
        }
        return true
    }

    fun allCoherent(context: Context, cursors: Collection<HifzCursor>): Boolean =
        allCoherent(cursors) { page -> readPageSvg(context, page) }

    private fun readPageSvg(context: Context, page: Int): String? = runCatching {
        require(page in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT)
        BrotliInputStream(
            context.assets.open("mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page))
        ).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrNull()
}

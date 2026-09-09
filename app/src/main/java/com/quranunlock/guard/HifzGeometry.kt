package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONObject

/** Stable reference to one real line produced by reader109 geometry.json. */
data class HifzLineRef(
    val geometryId: String,
    val page: Int,
    val ordinal: Int
) {
    init {
        require(geometryId.isNotBlank())
        require(page in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT)
        require(ordinal >= 1)
    }
}

data class HifzGeometryLine(
    val ref: HifzLineRef,
    val verses: Set<QuranVerseRef>
) {
    init {
        require(verses.isNotEmpty())
        verses.forEach(QuranCanonicalBounds::requireValid)
    }
}

data class HifzGeometryIndex(
    val linesByPage: Map<Int, List<HifzGeometryLine>>
) {
    init {
        val ids = mutableSetOf<String>()
        linesByPage.forEach { (page, lines) ->
            require(page in 1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT)
            lines.forEachIndexed { index, line ->
                require(line.ref.page == page)
                require(line.ref.ordinal == index + 1) {
                    "Hifz geometry line ordinals must follow display order."
                }
                require(ids.add(line.ref.geometryId)) { "Duplicate Hifz geometry line id." }
            }
        }
    }
}

/**
 * One real Mushaf line filtered to the canonical Hifz target. Off-target verses sharing
 * the same physical line remain visible but never become part of the exercise.
 */
data class HifzTargetLine(
    val ref: HifzLineRef,
    val targetVerses: Set<QuranVerseRef>
) {
    init {
        require(targetVerses.isNotEmpty())
    }
}

data class HifzPedagogicalSegment(
    val canonicalTarget: HifzVerseRange,
    val lines: List<HifzTargetLine>
) {
    init {
        require(lines.isNotEmpty())
        lines.forEach { line ->
            require(line.targetVerses.all(canonicalTarget::contains))
        }
    }
}

object HifzGeometryPolicy {
    const val DEFAULT_MAX_LINES_PER_SEGMENT = 5

    fun targetLines(index: HifzGeometryIndex, target: HifzVerseRange): List<HifzTargetLine> =
        index.linesByPage.toSortedMap().values
            .flatten()
            .mapNotNull { line ->
                val active = line.verses.filter(target::contains).toSet()
                active.takeIf { it.isNotEmpty() }?.let { HifzTargetLine(line.ref, it) }
            }

    /**
     * Uses only real geometry lines. Segments never cross a Mushaf page boundary and all
     * retain the exact same canonical verse target. Completing a segment cannot advance
     * the canonical cursor.
     */
    fun segment(
        index: HifzGeometryIndex,
        target: HifzVerseRange,
        maxLinesPerSegment: Int = DEFAULT_MAX_LINES_PER_SEGMENT
    ): List<HifzPedagogicalSegment> {
        require(maxLinesPerSegment > 0)
        return targetLines(index, target)
            .groupBy { it.ref.page }
            .toSortedMap()
            .values
            .flatMap { pageLines ->
                pageLines.chunked(maxLinesPerSegment).map { chunk ->
                    HifzPedagogicalSegment(target, chunk)
                }
            }
    }
}

/** Loads the exact geometry generated from the shipped 604-page Mushaf corpus. */
object HifzGeometryAssetLoader {
    fun load(context: Context): HifzGeometryIndex {
        val raw = context.assets.open("reader109/geometry.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return decode(raw)
    }

    internal fun decode(raw: String): HifzGeometryIndex {
        val root = JSONObject(raw)
        require(root.getInt("schema") == 1) { "Unsupported reader geometry schema." }
        val pages = root.getJSONObject("pages")
        val linesByPage = linkedMapOf<Int, List<HifzGeometryLine>>()
        val pageKeys = pages.keys().asSequence().map { it.toInt() }.sorted().toList()
        pageKeys.forEach { page ->
            val sourceLines = pages.getJSONObject(page.toString()).getJSONArray("lines")
            val lines = (0 until sourceLines.length()).map { index ->
                val source = sourceLines.getJSONObject(index)
                val sourceVerses = source.getJSONArray("verses")
                val verses = (0 until sourceVerses.length()).map { verseIndex ->
                    parseVerse(sourceVerses.getString(verseIndex))
                }.toSet()
                HifzGeometryLine(
                    ref = HifzLineRef(
                        geometryId = source.getString("id"),
                        page = source.getInt("page"),
                        ordinal = index + 1
                    ),
                    verses = verses
                )
            }
            linesByPage[page] = lines
        }
        return HifzGeometryIndex(linesByPage)
    }

    private fun parseVerse(value: String): QuranVerseRef {
        val parts = value.split(':')
        require(parts.size == 2) { "Malformed geometry verse reference." }
        return QuranVerseRef(parts[0].toInt(), parts[1].toInt()).also {
            QuranCanonicalBounds.requireValid(it)
        }
    }
}

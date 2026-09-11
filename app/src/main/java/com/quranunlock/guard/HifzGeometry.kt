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
    init { require(targetVerses.isNotEmpty()) }
}

data class HifzPedagogicalSegment(
    val canonicalTarget: HifzVerseRange,
    val lines: List<HifzTargetLine>
) {
    init {
        require(lines.isNotEmpty())
        lines.forEach { line -> require(line.targetVerses.all(canonicalTarget::contains)) }
    }
}

object HifzGeometryPolicy {
    const val DEFAULT_MAX_LINES_PER_SEGMENT = 5

    fun orderedLines(index: HifzGeometryIndex): List<HifzGeometryLine> =
        index.linesByPage.toSortedMap().values.flatten()

    fun lineById(index: HifzGeometryIndex, id: String): HifzGeometryLine? =
        orderedLines(index).firstOrNull { it.ref.geometryId == id }

    fun nextLine(index: HifzGeometryIndex, id: String): HifzGeometryLine? {
        val lines = orderedLines(index)
        val position = lines.indexOfFirst { it.ref.geometryId == id }
        if (position < 0 || position + 1 >= lines.size) return null
        return lines[position + 1]
    }

    fun targetLines(index: HifzGeometryIndex, target: HifzVerseRange): List<HifzTargetLine> =
        orderedLines(index).mapNotNull { line -> targetLine(line, target) }

    /**
     * Exact task-aware form. New Sabqi cursors carry start/end real-line ids so a block
     * ending inside a verse resumes at the following physical line instead of rounding
     * to the next verse. Legacy cursors fall back to page bounds.
     */
    fun targetLines(index: HifzGeometryIndex, cursor: HifzCursor): List<HifzTargetLine> {
        val target = HifzVerseRange(cursor.start, cursor.end)
        val sourceLines = if (cursor.hasExactLineBounds) {
            val all = orderedLines(index)
            val startIndex = all.indexOfFirst { it.ref.geometryId == cursor.startLineId }
            val endIndex = all.indexOfFirst { it.ref.geometryId == cursor.endLineId }
            require(startIndex >= 0 && endIndex >= startIndex) { "Unknown or reversed Hifz line bounds." }
            all.subList(startIndex, endIndex + 1)
        } else {
            index.linesByPage.toSortedMap()
                .filterKeys { it in cursor.startPage..cursor.endPage }
                .values
                .flatten()
        }
        return sourceLines.mapNotNull { line -> targetLine(line, target) }
    }

    /** Rendering segmentation only; training repetitions remain task-wide. */
    fun segment(
        index: HifzGeometryIndex,
        target: HifzVerseRange,
        maxLinesPerSegment: Int = DEFAULT_MAX_LINES_PER_SEGMENT
    ): List<HifzPedagogicalSegment> = segmentLines(target, targetLines(index, target), maxLinesPerSegment)

    /** Task-aware rendering segmentation constrained to the exact cursor line bounds. */
    fun segment(
        index: HifzGeometryIndex,
        cursor: HifzCursor,
        maxLinesPerSegment: Int = DEFAULT_MAX_LINES_PER_SEGMENT
    ): List<HifzPedagogicalSegment> = segmentLines(
        HifzVerseRange(cursor.start, cursor.end),
        targetLines(index, cursor),
        maxLinesPerSegment
    )

    private fun targetLine(line: HifzGeometryLine, target: HifzVerseRange): HifzTargetLine? {
        val active = line.verses.filter(target::contains).toSet()
        return active.takeIf { it.isNotEmpty() }?.let { HifzTargetLine(line.ref, it) }
    }

    private fun segmentLines(
        target: HifzVerseRange,
        lines: List<HifzTargetLine>,
        maxLinesPerSegment: Int
    ): List<HifzPedagogicalSegment> {
        require(maxLinesPerSegment > 0)
        return lines
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

/** Loads and validates the immutable 604-page geometry once per app process. */
object HifzGeometryAssetLoader {
    @Volatile private var cached: HifzGeometryIndex? = null

    fun load(context: Context): HifzGeometryIndex {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: run {
                val raw = context.applicationContext.assets.open("reader109/geometry.json")
                    .bufferedReader(Charsets.UTF_8).use { it.readText() }
                decode(raw).also { cached = it }
            }
        }
    }

    internal fun decode(raw: String): HifzGeometryIndex {
        val root = JSONObject(raw)
        require(root.getInt("schema") == 1) { "Unsupported reader geometry schema." }
        val pages = root.getJSONObject("pages")
        val pageKeys = pages.keys().asSequence().map { it.toInt() }.sorted().toList()
        require(pageKeys == (1..QuranCanonicalBounds.MUSHAF_PAGE_COUNT).toList()) {
            "Reader geometry is not the complete 604-page Mushaf corpus."
        }
        val linesByPage = linkedMapOf<Int, List<HifzGeometryLine>>()
        pageKeys.forEach { page ->
            val sourceLines = pages.getJSONObject(page.toString()).getJSONArray("lines")
            require(sourceLines.length() > 0) { "Missing geometry lines for Mushaf page $page." }
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

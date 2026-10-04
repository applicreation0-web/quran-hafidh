package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzGeometryPolicyTest {
    @Test
    fun midPageTargetKeepsOnlyTargetVersesActive() {
        val index = HifzGeometryIndex(
            mapOf(
                515 to listOf(
                    line("515:0", 515, 1, QuranVerseRef(48, 29)),
                    line("515:1", 515, 2, QuranVerseRef(48, 29), QuranVerseRef(49, 1)),
                    line("515:2", 515, 3, QuranVerseRef(49, 1), QuranVerseRef(49, 2))
                )
            )
        )
        val target = HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(49, 2))

        val active = HifzGeometryPolicy.targetLines(index, target)

        assertEquals(listOf("515:1", "515:2"), active.map { it.ref.geometryId })
        assertTrue(active.flatMap { it.targetVerses }.none { it.surah == 48 })
        assertEquals(setOf(QuranVerseRef(49, 1)), active.first().targetVerses)
    }

    @Test
    fun fifteenRealLinesSplitFiveByFiveButKeepOneCanonicalVerse() {
        val verse = QuranVerseRef(2, 282)
        val target = HifzVerseRange(verse, verse)
        val index = HifzGeometryIndex(
            mapOf(
                48 to (1..15).map { ordinal ->
                    line("48:${ordinal - 1}", 48, ordinal, verse)
                }
            )
        )

        val segments = HifzGeometryPolicy.segment(index, target, maxLinesPerSegment = 5)

        assertEquals(3, segments.size)
        assertEquals(listOf(1..5, 6..10, 11..15), segments.map {
            it.lines.first().ref.ordinal..it.lines.last().ref.ordinal
        })
        assertTrue(segments.all { it.canonicalTarget == target })
        assertTrue(segments.flatMap { it.lines }.all { it.targetVerses == setOf(verse) })
    }

    @Test
    fun itqanTraversalJumpsDirectlyAcrossUnselectedGap() {
        val intervals = listOf(
            HifzVerseRange(QuranVerseRef(2, 1), QuranVerseRef(2, 286)),
            HifzVerseRange(QuranVerseRef(49, 1), QuranVerseRef(114, 6))
        )

        assertEquals(
            QuranVerseRef(49, 1),
            HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(2, 286))
        )
        assertEquals(
            QuranVerseRef(49, 2),
            HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(49, 1))
        )
        assertEquals(null, HifzItqanTraversalPolicy.nextAfter(intervals, QuranVerseRef(114, 6)))
    }

    private fun line(
        id: String,
        page: Int,
        ordinal: Int,
        vararg verses: QuranVerseRef
    ) = HifzGeometryLine(
        ref = HifzLineRef(id, page, ordinal),
        verses = verses.toSet()
    )
}

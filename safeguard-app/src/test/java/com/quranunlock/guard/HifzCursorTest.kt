package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzCursorTest {
    @Test
    fun canonicalBoundsCoverTheWholeQuranAndMushaf() {
        assertEquals(114, QuranCanonicalBounds.SURAH_COUNT)
        assertEquals(604, QuranCanonicalBounds.MUSHAF_PAGE_COUNT)
        assertEquals(6236, QuranCanonicalBounds.TOTAL_VERSES)
        assertEquals(286, QuranCanonicalBounds.ayahCount(2))
        assertEquals(6, QuranCanonicalBounds.ayahCount(114))
    }

    @Test
    fun typedCursorKeepsVerseFirstLabelAndCanonicalPageInternally() {
        val cursor = HifzCursor.page(67, 1, 7, 562)

        assertEquals(QuranVerseRef(67, 1), cursor.start)
        assertEquals(QuranVerseRef(67, 7), cursor.end)
        assertEquals(562, cursor.startPage)
        assertEquals("67:1–67:7", cursor.label)
    }

    @Test
    fun exactLineCursorCanExposePartialVerseWithoutInventingAnotherVerse() {
        val cursor = HifzCursor(
            start = QuranVerseRef(2, 80),
            end = QuranVerseRef(2, 82),
            startPage = 12,
            endPage = 12,
            startLineId = "12:6",
            endLineId = "12:10",
            endVersePartial = true
        )

        assertTrue(cursor.hasExactLineBounds)
        assertEquals("2:80–2:82 (partiel)", cursor.label)
    }

    @Test
    fun impossibleVerseIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzCursor.page(2, 287, 287, 42)
        }
    }

    @Test
    fun reversedVerseRangeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzCursor.page(2, 10, 5, 2)
        }
    }

    @Test
    fun pageOutsideFixedMushafIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            HifzCursor.page(1, 1, 7, 605)
        }
    }
}

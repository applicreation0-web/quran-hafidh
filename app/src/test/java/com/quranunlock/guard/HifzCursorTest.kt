package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    fun typedCursorAcceptsCanonicalVerseAndPageBounds() {
        val cursor = HifzCursor.page(67, 1, 7, 562)

        assertEquals(QuranVerseRef(67, 1), cursor.start)
        assertEquals(QuranVerseRef(67, 7), cursor.end)
        assertEquals(562, cursor.startPage)
        assertEquals("67:1–67:7 • page 562", cursor.label)
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

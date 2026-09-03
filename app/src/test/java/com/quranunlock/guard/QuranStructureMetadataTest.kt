package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranStructureMetadataTest {
    @Test
    fun juzSixUsesItsExactVerseBoundary() {
        val juz = QuranStructureMetadata.division(
            QuranSelectionMode.JUZ,
            6
        )

        assertEquals(QuranVerseRef(4, 148), juz.start)
        assertEquals(QuranVerseRef(5, 81), juz.end)
        assertEquals(102, juz.startPage)
        assertEquals(121, juz.endPage)
        assertFalse(juz.startsInsidePage)
        assertTrue(juz.endsInsidePage)
    }

    @Test
    fun pageElevenBelongsToBothAdjacentHizb() {
        val numbers = QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.HIZB,
            11
        ).map(QuranDivision::number)

        assertEquals(listOf(1, 2), numbers)
        assertTrue(
            QuranStructureMetadata.division(
                QuranSelectionMode.HIZB,
                1
            ).endsInsidePage
        )
        assertTrue(
            QuranStructureMetadata.division(
                QuranSelectionMode.HIZB,
                2
            ).startsInsidePage
        )
    }

    @Test
    fun selectionIncludesSharedBoundaryPages() {
        val hizbOne = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(1)
        )
        val hizbTwo = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(2)
        )

        assertTrue(11 in hizbOne)
        assertTrue(11 in hizbTwo)
    }

    @Test
    fun protectionQuotaRemainsTenPagesAndContinuationCanFollow() {
        val quota = QuranPageSelector.tenPageQuotaFromHizb(1)

        assertEquals(UsageCyclePolicy.HIZB_PAGE_COUNT, quota.size)
        assertEquals((1..10).toList(), quota)
        assertEquals(11, quota.last() + 1)
        assertTrue(
            quota.last() + 1 in QuranStructureMetadata.division(
                QuranSelectionMode.HIZB,
                1
            ).pageRange
        )
    }

    @Test
    fun everyJuzAndHizbHasOrderedValidBounds() {
        listOf(
            QuranSelectionMode.JUZ to 30,
            QuranSelectionMode.HIZB to 60
        ).forEach { (mode, count) ->
            (1..count).forEach { number ->
                val division = QuranStructureMetadata.division(mode, number)
                assertTrue(division.startPage in 1..604)
                assertTrue(division.endPage in division.startPage..604)
                assertTrue(division.pageRange.isNotEmpty())
            }
        }
    }
}

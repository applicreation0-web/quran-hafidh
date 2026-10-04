package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCanonicalPageBoundaryTest {
    @Test
    fun everyAdjacentJuzAndHizbUsesOnlyCanonicalPageTransition() {
        listOf(
            QuranSelectionMode.JUZ to 30,
            QuranSelectionMode.HIZB to 60
        ).forEach { (mode, count) ->
            val divisions = (1..count).map {
                QuranStructureMetadata.division(mode, it)
            }

            assertFalse(divisions.first().startsInsidePage)
            assertFalse(divisions.last().endsInsidePage)

            divisions.zipWithNext().forEach { (current, next) ->
                val sharedBoundaryPage = current.endPage == next.startPage
                val cleanPageBreak = current.endPage + 1 == next.startPage

                assertTrue(
                    "${mode.name} ${current.number}/${next.number}: " +
                        "boundary pages must be shared or consecutive, got " +
                        "${current.endPage}/${next.startPage}",
                    sharedBoundaryPage || cleanPageBreak
                )
                assertEquals(
                    "${mode.name} ${current.number} endsInsidePage",
                    sharedBoundaryPage,
                    current.endsInsidePage
                )
                assertEquals(
                    "${mode.name} ${next.number} startsInsidePage",
                    sharedBoundaryPage,
                    next.startsInsidePage
                )
            }
        }
    }

    @Test
    fun page552AndSaffRemainInHizb55Only() {
        val divisions = QuranStructureMetadata.divisionsForPage(
            QuranSelectionMode.HIZB,
            552
        )

        assertEquals(listOf(55), divisions.map(QuranDivision::number))
        val hizb55 = divisions.single()
        assertEquals(QuranVerseRef(58, 1), hizb55.start)
        assertEquals(QuranVerseRef(61, 14), hizb55.end)
        assertEquals(542, hizb55.startPage)
        assertEquals(552, hizb55.endPage)
        assertFalse(hizb55.endsInsidePage)

        val hizb56 = QuranStructureMetadata.division(QuranSelectionMode.HIZB, 56)
        assertEquals(QuranVerseRef(62, 1), hizb56.start)
        assertEquals(553, hizb56.startPage)
        assertFalse(hizb56.startsInsidePage)
    }
}

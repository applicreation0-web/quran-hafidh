package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranPageSelectorTest {
    @Test
    fun hizb60UsesExpectedMadaniPages() {
        val pages = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(60)
        )

        assertEquals(591, pages.first())
        assertEquals(604, pages.last())
        assertEquals(14, pages.size)
    }

    @Test
    fun recentPagesAreAvoidedWhenAlternativesExist() {
        val pages = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(60)
        )
        val excluded = pages.dropLast(1)

        repeat(20) {
            val selected = QuranPageSelector.randomPage(
                mode = QuranSelectionMode.HIZB,
                selectedUnits = setOf(60),
                recentPagesNewestFirst = excluded.reversed(),
                maxRecentExclusions = 30
            )
            assertEquals(pages.last(), selected)
        }
    }

    @Test
    fun smallSelectionNeverRunsOutOfCandidates() {
        val pages = QuranPageSelector.availablePages(
            QuranSelectionMode.HIZB,
            setOf(1)
        )

        repeat(50) {
            val selected = QuranPageSelector.randomPage(
                mode = QuranSelectionMode.HIZB,
                selectedUnits = setOf(1),
                recentPagesNewestFirst = pages.reversed(),
                maxRecentExclusions = 30
            )
            assertTrue(selected in pages)
        }
    }

    @Test
    fun selectedJuzDoesNotLeakOutsideItsPages() {
        val allowed = QuranPageSelector.availablePages(
            QuranSelectionMode.JUZ,
            setOf(30)
        ).toSet()

        repeat(50) {
            val selected = QuranPageSelector.randomPage(
                mode = QuranSelectionMode.JUZ,
                selectedUnits = setOf(30),
                recentPagesNewestFirst = emptyList()
            )
            assertTrue(selected in allowed)
            assertFalse(selected < 582)
        }
    }
}

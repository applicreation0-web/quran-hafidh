package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranCanonicalPlanOrderTest {
    @Test
    fun challengeStopsAtEndOfShortHizbInsteadOfWrapping() {
        val tail = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(15),
            cursor = 8,
            pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT
        )

        assertEquals(listOf(150), tail.pages)
        assertEquals(0, tail.nextCursor)

        val restart = QuranPageSelector.sequentialCanonicalQuotaPages(
            mode = QuranSelectionMode.HIZB,
            selectedUnits = setOf(15),
            cursor = tail.nextCursor,
            pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT
        )
        assertEquals((142..150).toList(), restart.pages)
    }

    @Test
    fun everyPlanRemainsForwardInMushafOrder() {
        listOf(
            QuranSelectionMode.JUZ to setOf(3, 4, 26),
            QuranSelectionMode.HIZB to setOf(1, 15, 55, 56)
        ).forEach { (mode, units) ->
            val pool = QuranPageSelector.availablePages(mode, units)
            pool.indices.forEach { cursor ->
                val plan = QuranPageSelector.sequentialCanonicalQuotaPages(
                    mode = mode,
                    selectedUnits = units,
                    cursor = cursor,
                    pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
                )
                assertTrue(plan.pages.isNotEmpty())
                assertTrue(plan.pages.all { it in pool })
                assertTrue(
                    plan.pages.zipWithNext().all { (current, next) -> next > current }
                )
            }
        }
    }
}

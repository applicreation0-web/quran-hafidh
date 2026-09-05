package com.applicreation0.quransafeguard

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaddaburPolicyTest {
    @Test
    fun poolIsFixedFromOneToSixtyAndWrapsWithoutSelection() {
        assertEquals(1, TaddaburPolicy.FIRST_HIZB)
        assertEquals(60, TaddaburPolicy.LAST_HIZB)
        assertEquals(2, TaddaburPolicy.nextHizb(1))
        assertEquals(60, TaddaburPolicy.nextHizb(59))
        assertEquals(1, TaddaburPolicy.nextHizb(60))
    }

    @Test
    fun pageRequiresExactlyNinetySecondsMinimum() {
        assertEquals(90_000L, TaddaburPolicy.MIN_PAGE_MS)
    }

    @Test
    fun activeReadingStartsAtSevenAndCanContinueAfterDeadline() {
        assertFalse(TaddaburPolicy.mayAccumulate(LocalTime.of(6, 59, 59)))
        assertTrue(TaddaburPolicy.mayAccumulate(LocalTime.of(7, 0)))
        assertTrue(TaddaburPolicy.mayAccumulate(LocalTime.of(20, 0)))
        assertTrue(TaddaburPolicy.mayAccumulate(LocalTime.of(23, 59)))
    }

    @Test
    fun deadlineStartsAtTwenty() {
        assertFalse(TaddaburPolicy.deadlinePassed(LocalTime.of(19, 59, 59)))
        assertTrue(TaddaburPolicy.deadlinePassed(LocalTime.of(20, 0)))
        assertTrue(TaddaburPolicy.deadlinePassed(LocalTime.of(23, 59)))
    }

    @Test
    fun midnightEndsDeadlineWindow() {
        assertFalse(TaddaburPolicy.deadlinePassed(LocalTime.MIDNIGHT))
        assertFalse(TaddaburPolicy.deadlinePassed(LocalTime.of(0, 0, 1)))
    }

    @Test
    fun navigationChangesPhysicalAssetAndRejectsStaleCallbacks() {
        val pageN = 11
        val pageNPlusOne = pageN + 1
        assertNotEquals(
            TaddaburPageSessionPolicy.mushafAssetPath(pageN),
            TaddaburPageSessionPolicy.mushafAssetPath(pageNPlusOne),
        )
        assertEquals("mushaf/hafs/kfqc/svg-br/011.svg.br", TaddaburPageSessionPolicy.mushafAssetPath(pageN))
        assertEquals("mushaf/hafs/kfqc/svg-br/012.svg.br", TaddaburPageSessionPolicy.mushafAssetPath(pageNPlusOne))
        assertTrue(TaddaburPageSessionPolicy.acceptsPageCallback(pageNPlusOne, pageNPlusOne))
        assertFalse(TaddaburPageSessionPolicy.acceptsPageCallback(pageNPlusOne, pageN))
    }

    @Test
    fun activeTimeUsesInMemoryCheckpointBeforePersisting() {
        assertEquals(5_000L, TaddaburPageSessionPolicy.CHECKPOINT_MS)
        assertFalse(TaddaburPageSessionPolicy.shouldCheckpoint(4_000L, 89_000L))
        assertTrue(TaddaburPageSessionPolicy.shouldCheckpoint(5_000L, 5_000L))
        assertTrue(TaddaburPageSessionPolicy.shouldCheckpoint(1_000L, 90_000L))
    }

    @Test
    fun openReaderDetectsCalendarDayChangeBeforeCreditingMoreTime() {
        val day = 20_000L
        assertFalse(TaddaburPageSessionPolicy.dayChanged(day, day))
        assertTrue(TaddaburPageSessionPolicy.dayChanged(day, day + 1L))
        assertTrue(TaddaburPageSessionPolicy.dayChanged(day, day - 1L))
    }

    @Test
    fun completedAdjacentHizbCarriesOnlyTheSharedCanonicalBoundaryPage() {
        var sharedBoundaryCount = 0
        for (hizb in TaddaburPolicy.FIRST_HIZB until TaddaburPolicy.LAST_HIZB) {
            val current = QuranStructureMetadata.division(QuranSelectionMode.HIZB, hizb)
            val next = QuranStructureMetadata.division(QuranSelectionMode.HIZB, hizb + 1)
            val carried = TaddaburBoundaryPolicy.carriedBoundaryPage(
                previousHizb = hizb,
                currentHizb = hizb + 1,
                previousComplete = true,
            )
            if (current.endPage == next.startPage) {
                sharedBoundaryCount += 1
                assertEquals(current.endPage, carried)
            } else {
                assertNull(carried)
            }
            assertNull(
                TaddaburBoundaryPolicy.carriedBoundaryPage(
                    previousHizb = hizb,
                    currentHizb = hizb + 1,
                    previousComplete = false,
                )
            )
        }
        assertTrue("Expected canonical mid-page Hizb boundaries", sharedBoundaryCount > 0)
        assertNull(
            TaddaburBoundaryPolicy.carriedBoundaryPage(
                previousHizb = 60,
                currentHizb = 1,
                previousComplete = true,
            )
        )
    }
}

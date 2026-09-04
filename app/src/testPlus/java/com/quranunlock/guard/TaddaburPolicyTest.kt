package com.applicreation0.quransafeguard

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}

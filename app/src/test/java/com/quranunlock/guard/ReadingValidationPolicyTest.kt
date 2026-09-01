package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingValidationPolicyTest {
    @Test
    fun fiftyNineSecondsCannotValidateEvenAtBottom() {
        assertFalse(ReadingValidationPolicy.canValidate(59_999L, bottomReached = true))
    }

    @Test
    fun sixtyActiveSecondsAndBottomCanValidate() {
        assertTrue(ReadingValidationPolicy.canValidate(60_000L, bottomReached = true))
    }

    @Test
    fun sixtySecondsWithoutPageProgressCannotValidate() {
        assertFalse(ReadingValidationPolicy.canValidate(60_000L, bottomReached = false))
    }

    @Test
    fun pausedTimeCannotBeInventedByValidationPolicy() {
        assertEquals(40_000L, ReadingValidationPolicy.remainingMs(20_000L))
        assertEquals(0L, ReadingValidationPolicy.remainingMs(65_000L))
    }
}

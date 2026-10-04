package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingValidationPolicyTest {
    @Test
    fun fiftyNineSecondsCannotValidateEvenAtBottom() {
        assertFalse(ReadingValidationPolicy.canValidate(59_999L))
    }

    @Test
    fun sixtyActiveSecondsCanValidate() {
        assertTrue(ReadingValidationPolicy.canValidate(60_000L))
    }

    @Test
    fun missingScrollSignalCannotKeepSixtySecondPageLocked() {
        assertTrue(ReadingValidationPolicy.canValidate(60_000L))
    }

    @Test
    fun pausedTimeCannotBeInventedByValidationPolicy() {
        assertEquals(40_000L, ReadingValidationPolicy.remainingMs(20_000L))
        assertEquals(0L, ReadingValidationPolicy.remainingMs(65_000L))
    }

    @Test
    fun fullyVisiblePageDoesNotRequireScroll() {
        assertFalse(ReadingValidationPolicy.requiresScroll(1_200, 1_200))
    }

    @Test
    fun minorWebViewRoundingDoesNotCreateFakeScrollRequirement() {
        assertFalse(ReadingValidationPolicy.requiresScroll(1_224, 1_200))
    }

    @Test
    fun overflowingPageRequiresNaturalScroll() {
        assertTrue(ReadingValidationPolicy.requiresScroll(1_225, 1_200))
    }
}

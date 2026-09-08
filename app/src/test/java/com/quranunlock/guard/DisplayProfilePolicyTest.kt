package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayProfilePolicyTest {
    @Test fun onyxAndBooxAreDetectedCentrally() {
        assertTrue(DisplayProfileManager.looksLikeEInkDevice(null, "ONYX", "", "BOOX Note Air"))
    }

    @Test fun ordinaryAndroidDeviceStaysStandard() {
        assertFalse(DisplayProfileManager.looksLikeEInkDevice(null, "Google", "google", "Pixel 8a"))
    }

    @Test fun standardMotionRemainsUnchangedAndEInkMotionIsImmediate() {
        assertEquals(200, DisplayProfileManager.motionDurationMillis(DisplayProfile.STANDARD))
        assertEquals(0, DisplayProfileManager.motionDurationMillis(DisplayProfile.EINK))
    }

    @Test fun manualStandardOverridesDetectedEInk() {
        assertEquals(DisplayProfile.STANDARD, DisplayProfileManager.resolvePreference(
            DisplayProfilePreference.STANDARD, automaticDetection = true
        ))
    }

    @Test fun manualEInkOverridesOrdinaryDevice() {
        assertEquals(DisplayProfile.EINK, DisplayProfileManager.resolvePreference(
            DisplayProfilePreference.EINK, automaticDetection = false
        ))
    }

    @Test fun fullRefreshIsThresholdedRatherThanPerInteraction() {
        assertTrue(EInkRefreshController.FULL_REFRESH_THRESHOLD > VisualChange.MASK_LEVEL.ghostingWeight)
        assertTrue(EInkRefreshController.FULL_REFRESH_THRESHOLD <=
            VisualChange.REVEAL.ghostingWeight + VisualChange.REVEAL_RETURN.ghostingWeight +
                VisualChange.MASK_LEVEL.ghostingWeight)
    }
}

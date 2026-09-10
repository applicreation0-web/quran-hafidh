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

    @Test fun manualOverridesWinInBothDirections() {
        assertEquals(DisplayProfile.STANDARD, DisplayProfileManager.resolvePreference(
            DisplayProfilePreference.STANDARD, automaticDetection = true
        ))
        assertEquals(DisplayProfile.EINK, DisplayProfileManager.resolvePreference(
            DisplayProfilePreference.EINK, automaticDetection = false
        ))
    }

    @Test fun deniedRevealCleanupIsDeferredAndEventuallyRuns() {
        val policy = EInkRefreshPolicy()
        assertEquals(RefreshAction.FULL_NOW, policy.onChange(VisualChange.PAGE, 0).action)
        assertEquals(RefreshAction.PARTIAL, policy.onChange(VisualChange.REVEAL, 100).action)
        val denied = policy.onChange(VisualChange.REVEAL_RETURN, 1_000)
        assertEquals(RefreshAction.FULL_LATER, denied.action)
        assertEquals(2_500L, denied.dueAtMs)
        assertEquals(RefreshAction.FULL_LATER, policy.onPendingDue(2_499).action)
        assertEquals(RefreshAction.FULL_NOW, policy.onPendingDue(2_500).action)
        assertEquals(RefreshAction.NONE, policy.onPendingDue(2_501).action)
    }

    @Test fun refreshDuringRevealCannotConsumeFinalMaskCleanup() {
        val policy = EInkRefreshPolicy()
        policy.onChange(VisualChange.PAGE, 0)
        policy.onChange(VisualChange.REVEAL, 2_600)
        policy.onChange(VisualChange.MASK_LEVEL, 2_650)
        assertEquals(RefreshAction.FULL_NOW,
            policy.onChange(VisualChange.LINE, 2_700).action)
        val returned = policy.onChange(VisualChange.REVEAL_RETURN, 3_000)
        assertEquals(RefreshAction.FULL_LATER, returned.action)
        assertEquals(5_200L, returned.dueAtMs)
        assertEquals(RefreshAction.FULL_NOW, policy.onPendingDue(5_200).action)
    }

    @Test fun rapidRevealsCoalesceWithoutLosingLatestCleanup() {
        val policy = EInkRefreshPolicy()
        policy.onChange(VisualChange.PAGE, 0)
        assertEquals(2_500L, policy.onChange(VisualChange.REVEAL_RETURN, 900).dueAtMs)
        assertEquals(2_500L, policy.onChange(VisualChange.REVEAL_RETURN, 1_400).dueAtMs)
        assertEquals(RefreshAction.FULL_NOW, policy.onPendingDue(2_500).action)
        assertEquals(RefreshAction.NONE, policy.onPendingDue(2_600).action)
    }

    @Test fun obsoleteTimerCannotRefreshAReplacementPage() {
        val policy = EInkRefreshPolicy()
        policy.onChange(VisualChange.PAGE, 0)
        assertEquals(RefreshAction.FULL_LATER,
            policy.onChange(VisualChange.REVEAL_RETURN, 900).action)
        assertEquals(RefreshAction.FULL_NOW,
            policy.onChange(VisualChange.PAGE, 1_200).action)
        assertEquals(RefreshAction.NONE, policy.onPendingDue(2_500).action)
    }
}

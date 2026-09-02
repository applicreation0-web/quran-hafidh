package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveHandoffPolicyTest {
    private val origin = "com.example.privatebank"
    private val chrome = "com.android.chrome"

    @Test
    fun bankingOriginAndAndroidSettingsAreAllowedDuringLease() {
        assertTrue(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 1_000L,
                eventPackage = origin,
                eventClassName = "com.example.BankActivity"
            )
        )
        assertTrue(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = ProtectedApps.ANDROID_SETTINGS,
                eventClassName = "com.android.settings.Settings\$BiometricEnrollActivity"
            )
        )
    }

    @Test
    fun accessibilitySettingsRemainProtectedDuringBankingLease() {
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = ProtectedApps.ANDROID_SETTINGS,
                eventClassName = "com.android.settings.Settings\$AccessibilityDetailsSettingsActivity"
            )
        )
        assertTrue(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = ProtectedApps.ANDROID_SETTINGS,
                eventClassName = "com.android.settings.SubSettings"
            )
        )
    }

    @Test
    fun chromeAuthenticationCustomTabIsAllowedDuringLease() {
        assertTrue(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = chrome,
                eventClassName = "org.chromium.chrome.browser.customtabs.CustomTabActivity"
            )
        )
    }

    @Test
    fun ordinaryBrowserWindowIsNeverExempted() {
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = chrome,
                eventClassName = "org.chromium.chrome.browser.ChromeTabbedActivity"
            )
        )
    }

    @Test
    fun expiredLeaseCannotExemptSettingsOrAuthentication() {
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 121_001L,
                eventPackage = ProtectedApps.ANDROID_SETTINGS,
                eventClassName = "com.android.settings.Settings\$BiometricEnrollActivity"
            )
        )
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 121_001L,
                eventPackage = chrome,
                eventClassName = "org.chromium.chrome.browser.customtabs.CustomTabActivity"
            )
        )
    }

    @Test
    fun unrelatedAppCannotCreateOrReuseTheException() {
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = null,
                validUntilElapsedMs = 0L,
                nowElapsedMs = 1L,
                eventPackage = ProtectedApps.ANDROID_SETTINGS,
                eventClassName = "com.android.settings.Settings\$BiometricEnrollActivity"
            )
        )
        assertFalse(
            SensitiveHandoffPolicy.shouldAllowHandoff(
                originPackage = origin,
                validUntilElapsedMs = 121_000L,
                nowElapsedMs = 2_000L,
                eventPackage = "com.example.unrelated",
                eventClassName = "com.example.MainActivity"
            )
        )
    }
}

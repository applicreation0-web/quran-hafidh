package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedAppsPolicyTest {
    @Test
    fun socialScopeIsExactlyTheSixRequestedApplications() {
        val expected = setOf(
            "com.whatsapp",
            "com.twitter.android",
            "com.instagram.android",
            "com.facebook.katana",
            "com.google.android.youtube",
            "com.zhiliaoapp.musically"
        )
        assertEquals(expected, ProtectedApps.socialTargets.map { it.packageName }.toSet())
    }

    @Test
    fun formerSocialTargetsAreNoLongerSelectable() {
        listOf(
            "org.telegram.messenger",
            "com.discord",
            "com.reddit.frontpage",
            "com.snapchat.android"
        ).forEach { packageName ->
            assertFalse(ProtectedApps.isSelectableTarget(packageName))
        }
    }

    @Test
    fun browserCoverageIsExactlyTheDeclaredEight() {
        val expected = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.opera.browser",
            "com.sec.android.app.sbrowser",
            "com.duckduckgo.mobile.android",
            "com.vivaldi.browser"
        )
        assertEquals(expected, BrowserDetector.supportedPackages)
        assertEquals(expected, ProtectedApps.browserTargets.map { it.packageName }.toSet())
    }

    @Test
    fun selectableScopeContainsOnlySixSocialTargetsAndEightBrowsers() {
        val expected = (
            ProtectedApps.socialTargets + ProtectedApps.browserTargets
        ).map { it.packageName }.toSet()

        assertEquals(14, expected.size)
        assertEquals(expected, ProtectedApps.selectableScopePackages)
    }

    @Test
    fun everyNonTargetCategoryIsOutsideSafeguardWithoutClassification() {
        val outside = setOf(
            "com.android.settings",
            "com.android.phone",
            "com.google.android.dialer",
            "com.google.android.deskclock",
            "com.google.android.apps.walletnfcrel",
            "com.google.android.apps.authenticator2",
            "com.x8bit.bitwarden",
            "com.barclays.android.barclaysmobilebanking",
            "com.revolut.revolut",
            "com.google.android.apps.maps",
            "com.example.transport",
            "com.example.health",
            "com.example.professional"
        )

        outside.forEach { packageName ->
            assertFalse(ProtectedApps.isSelectableTarget(packageName))
        }
    }

    @Test
    fun whatsappRemainsOneTargetWhileCallWindowsAreHandledSeparately() {
        assertTrue(ProtectedApps.isSelectableTarget("com.whatsapp"))
        assertTrue(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.whatsapp",
                "com.whatsapp.voipcalling.VoipActivityV3"
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.whatsapp",
                "com.whatsapp.HomeActivity"
            )
        )
    }
}

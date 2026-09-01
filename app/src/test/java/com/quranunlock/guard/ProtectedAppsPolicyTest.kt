package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectedAppsPolicyTest {
    @Test
    fun financeSecurityAndIdentityLabelsAreExcluded() {
        assertTrue(ProtectedApps.looksSensitive("com.example.finance", "My Bank"))
        assertTrue(ProtectedApps.looksSensitive("com.example.auth", "Secure Authenticator"))
        assertTrue(ProtectedApps.looksSensitive("com.example.identity", "Digital ID Check"))
        assertTrue(ProtectedApps.looksSensitive("com.example.vpn", "Private VPN"))
    }

    @Test
    fun knownSensitivePackageFamiliesAreExcluded() {
        assertTrue(
            ProtectedApps.looksSensitive(
                "com.barclays.android.barclaysmobilebanking",
                "Barclays"
            )
        )
        assertTrue(ProtectedApps.looksSensitive("com.revolut.revolut", "Revolut"))
        assertTrue(ProtectedApps.looksSensitive("com.x8bit.bitwarden", "Bitwarden"))
        assertTrue(
            ProtectedApps.looksSensitive(
                "com.google.android.apps.authenticator2",
                "Authenticator"
            )
        )
    }

    @Test
    fun expandedFinanceAndCredentialFamiliesAreExcluded() {
        assertTrue(ProtectedApps.looksSensitive("com.wise.android", "Wise"))
        assertTrue(ProtectedApps.looksSensitive("com.klarna.mobile", "Klarna"))
        assertTrue(ProtectedApps.looksSensitive("com.coinbase.android", "Coinbase"))
        assertTrue(ProtectedApps.looksSensitive("com.example.pass", "Password Manager"))
        assertTrue(ProtectedApps.looksSensitive("com.example.id", "Identity Verification"))
        assertTrue(ProtectedApps.looksSensitive("com.example.trade", "Investment Trading"))
    }

    @Test
    fun criticalStaticPackagesAreAlwaysAllowed() {
        assertTrue(ProtectedApps.isAlwaysAllowed(ProtectedApps.PLAY_STORE))
        assertTrue(ProtectedApps.isAlwaysAllowed(ProtectedApps.ANDROID_SETTINGS))
        assertTrue(ProtectedApps.isAlwaysAllowed("com.android.phone"))
        assertTrue(ProtectedApps.isAlwaysAllowed("com.google.android.dialer"))
        assertTrue(ProtectedApps.isAlwaysAllowed("com.google.android.gms"))
    }

    @Test
    fun browserCoverageIsExactlyTheDeclaredEightAndNotSensitive() {
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
        assertTrue(BrowserDetector.supportedPackages == expected)

        val labels = mapOf(
            "com.android.chrome" to "Chrome",
            "org.mozilla.firefox" to "Firefox",
            "com.microsoft.emmx" to "Microsoft Edge",
            "com.brave.browser" to "Brave",
            "com.opera.browser" to "Opera",
            "com.sec.android.app.sbrowser" to "Samsung Internet",
            "com.duckduckgo.mobile.android" to "DuckDuckGo",
            "com.vivaldi.browser" to "Vivaldi"
        )
        expected.forEach { packageName ->
            assertFalse(
                ProtectedApps.looksSensitive(
                    packageName,
                    labels.getValue(packageName)
                )
            )
        }
    }

    @Test
    fun ordinaryAwarenessAppsRemainSelectable() {
        assertFalse(ProtectedApps.looksSensitive("com.whatsapp", "WhatsApp"))
        assertFalse(ProtectedApps.looksSensitive("com.google.android.youtube", "YouTube"))
        assertFalse(ProtectedApps.looksSensitive("com.reddit.frontpage", "Reddit"))
        assertFalse(ProtectedApps.looksSensitive("com.instagram.android", "Instagram"))
    }
}

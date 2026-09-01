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
    fun ordinaryAwarenessAppsRemainSelectable() {
        assertFalse(ProtectedApps.looksSensitive("com.whatsapp", "WhatsApp"))
        assertFalse(ProtectedApps.looksSensitive("com.google.android.youtube", "YouTube"))
        assertFalse(ProtectedApps.looksSensitive("com.reddit.frontpage", "Reddit"))
        assertFalse(ProtectedApps.looksSensitive("com.instagram.android", "Instagram"))
    }
}

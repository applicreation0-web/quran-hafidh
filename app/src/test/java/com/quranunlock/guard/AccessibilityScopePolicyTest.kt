package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityScopePolicyTest {
    private val own = "com.applicreation0.quransafeguard"
    private val browsers = setOf("com.android.chrome")
    private val infrastructure = setOf("com.android.systemui", "com.example.launcher")
    private val alwaysAllowed = setOf("com.android.settings", "com.android.vending")

    @Test
    fun unselectedBankingAppNeverEntersAccessibilityScope() {
        val scope = AccessibilityScopePolicy.eventPackages(
            selectedPackages = setOf("com.whatsapp"),
            browserPackages = browsers,
            infrastructurePackages = infrastructure,
            alwaysAllowedPackages = alwaysAllowed,
            ownPackage = own
        )

        assertFalse("com.example.bank" in scope)
        assertFalse("com.example.payment" in scope)
    }

    @Test
    fun explicitlySelectedAppAndSupportedBrowserAreIncluded() {
        val scope = AccessibilityScopePolicy.eventPackages(
            selectedPackages = setOf("com.whatsapp"),
            browserPackages = browsers,
            infrastructurePackages = infrastructure,
            alwaysAllowedPackages = alwaysAllowed,
            ownPackage = own
        )

        assertTrue("com.whatsapp" in scope)
        assertTrue("com.android.chrome" in scope)
    }

    @Test
    fun alwaysAllowedPackagesStayOutEvenIfSelected() {
        val scope = AccessibilityScopePolicy.eventPackages(
            selectedPackages = setOf("com.android.settings", "com.android.vending"),
            browserPackages = browsers,
            infrastructurePackages = infrastructure,
            alwaysAllowedPackages = alwaysAllowed,
            ownPackage = own
        )

        assertFalse("com.android.settings" in scope)
        assertFalse("com.android.vending" in scope)
    }

    @Test
    fun onlySafeInfrastructureIsAddedForForegroundTransitions() {
        val scope = AccessibilityScopePolicy.eventPackages(
            selectedPackages = emptySet(),
            browserPackages = emptySet(),
            infrastructurePackages = infrastructure,
            alwaysAllowedPackages = alwaysAllowed,
            ownPackage = own
        )

        assertTrue(own in scope)
        assertTrue("com.android.systemui" in scope)
        assertTrue("com.example.launcher" in scope)
        assertFalse("com.example.bank" in scope)
    }
}

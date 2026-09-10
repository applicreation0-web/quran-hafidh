package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetPresenceScopePolicyTest {
    private val targets = setOf(
        "com.android.chrome",
        "com.google.android.youtube",
        "com.instagram.android"
    )
    private val admitted = targets + setOf(
        "com.applicreation0.quransafeguard",
        "com.android.systemui",
        "com.android.launcher3"
    )

    @Test
    fun runningSelectedTargetArmsAnonymousExitSentinel() {
        assertTrue(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.android.chrome",
                runningBudgetPackage = "com.android.chrome",
                selectedTargets = targets
            )
        )
    }

    @Test
    fun sentinelCannotArmWithoutMatchingRunningSelectedTarget() {
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.android.chrome",
                runningBudgetPackage = null,
                selectedTargets = targets
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.example.bank",
                runningBudgetPackage = "com.example.bank",
                selectedTargets = targets
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = false,
                foregroundPackage = "com.android.chrome",
                runningBudgetPackage = "com.android.chrome",
                selectedTargets = targets
            )
        )
    }

    @Test
    fun outsideWindowEventStopsButTargetSystemAndImeEventsDoNot() {
        val ime = "com.google.android.inputmethod.latin"
        assertTrue(
            TargetPresenceScopePolicy.shouldStopForAnonymousOutsideEvent(
                sentinelArmed = true,
                eventPackage = "com.example.bank",
                activeImePackage = ime,
                admittedPackages = admitted
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.shouldStopForAnonymousOutsideEvent(
                sentinelArmed = true,
                eventPackage = "com.android.chrome",
                activeImePackage = ime,
                admittedPackages = admitted
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.shouldStopForAnonymousOutsideEvent(
                sentinelArmed = true,
                eventPackage = "com.android.systemui",
                activeImePackage = ime,
                admittedPackages = admitted
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.shouldStopForAnonymousOutsideEvent(
                sentinelArmed = true,
                eventPackage = ime,
                activeImePackage = ime,
                admittedPackages = admitted
            )
        )
        assertFalse(
            TargetPresenceScopePolicy.shouldStopForAnonymousOutsideEvent(
                sentinelArmed = false,
                eventPackage = "com.example.bank",
                activeImePackage = ime,
                admittedPackages = admitted
            )
        )
    }
}

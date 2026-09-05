package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Test

class TargetPresenceScopePolicyTest {
    private val targets = setOf(
        "com.android.chrome",
        "com.google.android.youtube",
        "com.instagram.android"
    )

    @Test
    fun runningSelectedTargetNeverEnablesAnonymousExitSentinel() {
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.android.chrome",
                runningBudgetPackage = "com.android.chrome",
                selectedTargets = targets
            )
        )
    }

    @Test
    fun outsideApplicationCanNeverTriggerBroadScope() {
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.example.bank",
                runningBudgetPackage = "com.example.bank",
                selectedTargets = targets
            )
        )
    }

    @Test
    fun narrowScopeRemainsNarrowWithoutRunningBudget() {
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = false,
                foregroundPackage = null,
                runningBudgetPackage = null,
                selectedTargets = targets
            )
        )
    }
}

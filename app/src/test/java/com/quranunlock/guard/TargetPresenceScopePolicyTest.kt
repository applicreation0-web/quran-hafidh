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

    @Test
    fun runningSelectedTargetRequiresOneAnonymousExitSignal() {
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
    fun noSentinelExistsWithoutAnActivelyRunningTargetBudget() {
        assertFalse(
            TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                broadRequested = true,
                foregroundPackage = "com.android.chrome",
                runningBudgetPackage = null,
                selectedTargets = targets
            )
        )
    }

    @Test
    fun outsideApplicationCanNeverOwnTheSharedBudgetScope() {
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
    fun narrowScopeIsRestoredAfterTheExitSignal() {
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

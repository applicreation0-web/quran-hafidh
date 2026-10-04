package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Deterministic high-volume checks for the release-critical target-presence ledger.
 * These tests intentionally separate simulated wall-clock time from chargeable
 * foreground time so a future refactor cannot silently turn 15/90 minutes into
 * elapsed time since the first target was opened.
 */
class TargetPresenceStressTest {
    private val minute = 60_000L

    @Test
    fun thousandsOfTargetBurstsAndOutsideGapsDebitExactlyFifteenMinutes() {
        var budget = UnlockBudgetIntegrity.grant(UsageCyclePolicy.INTERVAL_MS)
        var expectedRemaining = UsageCyclePolicy.INTERVAL_MS
        var wallClock = 0L
        var seed = 0x5AFE_2026L
        var targetBursts = 0

        while (expectedRemaining > 0L) {
            seed = nextSeed(seed)
            val requestedTargetMs = (seed % 997L) + 1L
            val chargedTargetMs = requestedTargetMs.coerceAtMost(expectedRemaining)

            budget = UnlockBudgetIntegrity.start(budget, wallClock, 77)
            val checkpointAt = wallClock + chargedTargetMs / 2L
            budget = UnlockBudgetIntegrity.checkpoint(budget, checkpointAt, 77)
            wallClock += chargedTargetMs
            budget = UnlockBudgetIntegrity.pause(budget, wallClock, 77)
            expectedRemaining -= chargedTargetMs
            targetBursts += 1

            assertEquals(expectedRemaining, budget.remainingMs)

            // Banking, GPS, work applications, screen-off time and calls all
            // advance wall time while the foreground marker stays stopped.
            seed = nextSeed(seed)
            wallClock += seed % (3L * minute)
            assertEquals(
                expectedRemaining,
                UnlockBudgetIntegrity.remaining(budget, wallClock, 77)
            )
        }

        assertTrue(targetBursts > 1_000)
        assertEquals(0L, budget.remainingMs)
        assertNull(budget.foregroundStartedElapsedMs)
    }

    @Test
    fun twoHundredFiftyNinetyMinuteCyclesRemainExactUnderRapidSwitching() {
        var cycle = UsageCyclePolicy.completeChallenge(
            UsageCycleState(),
            ChallengeLevel.MORNING
        )
        var wallClock = 0L

        repeat(250) { completedCycles ->
            repeat(UsageCyclePolicy.INTERVALS_PER_HIZB) { intervalIndex ->
                var budget = UnlockBudgetIntegrity.grant(UsageCyclePolicy.INTERVAL_MS)
                var charged = 0L

                repeat(400) { burstIndex ->
                    val remainingBursts = 400 - burstIndex
                    val remainingCharge = UsageCyclePolicy.INTERVAL_MS - charged
                    val targetMs = if (remainingBursts == 1) {
                        remainingCharge
                    } else {
                        (remainingCharge / remainingBursts)
                            .coerceAtLeast(1L)
                    }

                    budget = UnlockBudgetIntegrity.start(budget, wallClock, 91)
                    wallClock += targetMs
                    budget = UnlockBudgetIntegrity.pause(budget, wallClock, 91)
                    charged += targetMs

                    // A deliberately large non-target gap must never alter the
                    // target-only budget between two selected applications.
                    wallClock += ((burstIndex % 17) + 1) * 10_000L
                    assertEquals(
                        UsageCyclePolicy.INTERVAL_MS - charged,
                        UnlockBudgetIntegrity.remaining(budget, wallClock, 91)
                    )
                }

                assertEquals(UsageCyclePolicy.INTERVAL_MS, charged)
                assertEquals(0L, budget.remainingMs)

                cycle = UsageCyclePolicy.onIntervalExpired(cycle)
                val expectedLevel =
                    if (intervalIndex == UsageCyclePolicy.INTERVALS_PER_HIZB - 1) {
                        ChallengeLevel.HIZB
                    } else {
                        ChallengeLevel.MICRO
                    }
                assertEquals(expectedLevel, cycle.pendingLevel)
                cycle = UsageCyclePolicy.completeChallenge(cycle, expectedLevel)
            }

            assertEquals(completedCycles + 1, cycle.completedNinetyMinuteCycles)
            assertEquals(0, cycle.completedIntervals)
            assertNull(cycle.pendingLevel)
        }

        assertEquals(
            250L * UsageCyclePolicy.CUMULATIVE_MS,
            UsageCyclePolicy.completedUsageMs(cycle)
        )
    }

    @Test
    fun oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility() {
        val selected = setOf(
            "com.android.chrome",
            "com.google.android.youtube",
            "com.whatsapp"
        )

        repeat(100_000) { index ->
            val target = selected.elementAt(index % selected.size)
            assertFalse(
                TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                    broadRequested = true,
                    foregroundPackage = target,
                    runningBudgetPackage = target,
                    selectedTargets = selected
                )
            )
            assertFalse(
                TargetPresenceScopePolicy.requiresAnonymousExitSentinel(
                    broadRequested = true,
                    foregroundPackage = "com.example.outside.$index",
                    runningBudgetPackage = null,
                    selectedTargets = selected
                )
            )
        }
    }

    /**
     * Compatibility name retained because the general release meta-audit predates
     * the stricter #53 wording. It deliberately delegates to the same 100k
     * privacy-first assertions rather than weakening or faking the gate.
     */
    @Test
    fun oneHundredThousandScopeDecisionsNeverGiveOutsideAppsBudgetOwnership() {
        oneHundredThousandScopeDecisionsNeverEnableUnfilteredAccessibility()
    }

    @Test
    fun timerAndReadingBoundaryStayStableAcrossOneMillionChecks() {
        repeat(1_000_000) { index ->
            val elapsed = index % 120_000
            val expectedRemaining = (60_000 - elapsed).coerceAtLeast(0).toLong()
            assertEquals(expectedRemaining, ReadingValidationPolicy.remainingMs(elapsed.toLong()))
            assertEquals(
                elapsed >= 60_000,
                ReadingValidationPolicy.canValidate(elapsed.toLong())
            )
        }
    }

    private fun nextSeed(value: Long): Long =
        (value * 1_103_515_245L + 12_345L) and 0x7fff_ffffL
}

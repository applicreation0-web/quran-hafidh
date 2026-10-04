package com.quransafeguard.safeguard.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SafeguardCoreTest {
    @Test fun allowlistContainsExactlyHistoricalPositiveScope() {
        assertEquals(14, SafeguardAllowlist.packages.size)
        assertTrue("com.whatsapp" in SafeguardAllowlist.packages)
        assertTrue("com.android.chrome" in SafeguardAllowlist.packages)
        assertTrue("com.vivaldi.browser" in SafeguardAllowlist.packages)
        assertFalse("com.example.bank" in SafeguardAllowlist.packages)
    }

    @Test fun bankCannotBePersistedAsBudgetTarget() {
        assertFailsWith<IllegalArgumentException> {
            PerAppBudgetLedger(mapOf("com.example.bank" to 20 * 60_000L))
        }
    }

    @Test fun budgetsAreIndependentPerApplication() {
        val ledger = PerAppBudgetLedger(
            mapOf(
                "com.whatsapp" to 20 * 60_000L,
                "com.android.chrome" to 20 * 60_000L
            )
        )
        ledger.debit("com.whatsapp", 5 * 60_000L)
        assertEquals(15 * 60_000L, ledger.budget("com.whatsapp").remainingMs)
        assertEquals(20 * 60_000L, ledger.budget("com.android.chrome").remainingMs)
    }

    @Test fun oneHourOutsideTargetConsumesNothing() {
        val ledger = PerAppBudgetLedger(mapOf("com.whatsapp" to 20 * 60_000L))
        val tracker = TargetPresenceTracker(ledger)
        tracker.enterTarget("com.whatsapp", 0)
        tracker.exitTarget(5 * 60_000L)
        // One hour passes with no protected target active.
        assertNull(tracker.exitTarget(65 * 60_000L))
        assertEquals(15 * 60_000L, ledger.budget("com.whatsapp").remainingMs)
        tracker.enterTarget("com.whatsapp", 65 * 60_000L)
        tracker.exitTarget(69 * 60_000L)
        assertEquals(11 * 60_000L, ledger.budget("com.whatsapp").remainingMs)
    }

    @Test fun transientOverlayDoesNotEndUnderlyingTargetSession() {
        val ledger = PerAppBudgetLedger(mapOf("com.whatsapp" to 20 * 60_000L))
        val tracker = TargetPresenceTracker(ledger)
        tracker.enterTarget("com.whatsapp", 1_000L)
        tracker.transientOverlay(31_000L)
        tracker.exitTarget(61_000L)
        assertEquals(19 * 60_000L, ledger.budget("com.whatsapp").remainingMs)
    }

    @Test fun switchingTargetsDebitsEachOwnBudget() {
        val ledger = PerAppBudgetLedger(
            mapOf(
                "com.whatsapp" to 20 * 60_000L,
                "com.android.chrome" to 20 * 60_000L
            )
        )
        val tracker = TargetPresenceTracker(ledger)
        tracker.enterTarget("com.whatsapp", 0)
        tracker.enterTarget("com.android.chrome", 5 * 60_000L)
        tracker.exitTarget(9 * 60_000L)
        assertEquals(15 * 60_000L, ledger.budget("com.whatsapp").remainingMs)
        assertEquals(16 * 60_000L, ledger.budget("com.android.chrome").remainingMs)
    }

    @Test fun checkpointDoesNotRecreditOrDoubleCharge() {
        val ledger = PerAppBudgetLedger(mapOf("com.whatsapp" to 20 * 60_000L))
        val tracker = TargetPresenceTracker(ledger)
        tracker.enterTarget("com.whatsapp", 0)
        tracker.checkpoint(5_000L)
        tracker.checkpoint(10_000L)
        tracker.exitTarget(15_000L)
        assertEquals(20 * 60_000L - 15_000L, ledger.budget("com.whatsapp").remainingMs)
    }

    @Test fun warningsAreEmittedOnceWhenCrossed() {
        val ledger = PerAppBudgetLedger(mapOf("com.whatsapp" to 11 * 60_000L))
        val first = ledger.debit("com.whatsapp", 2 * 60_000L)
        assertEquals(setOf(WarningThreshold.TEN_MINUTES), first.newlyCrossedWarnings)
        val second = ledger.debit("com.whatsapp", 30_000L)
        assertTrue(second.newlyCrossedWarnings.isEmpty())
        val third = ledger.debit("com.whatsapp", 4 * 60_000L)
        assertEquals(setOf(WarningThreshold.FIVE_MINUTES), third.newlyCrossedWarnings)
    }

    @Test fun newCreditTrancheResetsWarningEmissionOnlyForThatApp() {
        val ledger = PerAppBudgetLedger(
            mapOf(
                "com.whatsapp" to 11 * 60_000L,
                "com.android.chrome" to 11 * 60_000L
            )
        )
        ledger.debit("com.whatsapp", 2 * 60_000L)
        ledger.debit("com.android.chrome", 2 * 60_000L)
        ledger.grantCredit("com.whatsapp", 11 * 60_000L)
        assertTrue(ledger.budget("com.whatsapp").emittedWarnings.isEmpty())
        assertEquals(setOf(WarningThreshold.TEN_MINUTES), ledger.budget("com.android.chrome").emittedWarnings)
    }

    @Test fun readingGateCountsOnlyExplicitActiveTime() {
        val gate = ReadingUnlockGate()
        gate.addActive(30_000L)
        // Any wall/background time is absent from this API and therefore cannot be credited.
        assertFalse(gate.canValidate)
        assertEquals(30_000L, gate.remainingMs)
        gate.addActive(29_999L)
        assertFalse(gate.canValidate)
        gate.addActive(1L)
        assertTrue(gate.canValidate)
    }

    @Test fun onlyThreeJokersCanBeConsumedWithoutDefiningTheirEffect() {
        val jokers = DailyJokerCounter()
        assertTrue(jokers.consume())
        assertTrue(jokers.consume())
        assertTrue(jokers.consume())
        assertFalse(jokers.consume())
        assertEquals(0, jokers.remaining)
    }
}

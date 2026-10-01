package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterceptionStateMachineTest {
    @Test
    fun duplicateTargetIsSuppressedDuringShortCooldown() {
        val machine = InterceptionStateMachine()

        assertTrue(machine.begin("com.android.chrome", 1_000L))
        assertFalse(machine.begin("com.android.chrome", 1_200L))
        assertTrue(machine.begin("com.android.chrome", 2_600L))
    }

    @Test
    fun retryStopsAsSoonAsGateIsVisible() {
        val machine = InterceptionStateMachine()

        assertTrue(machine.begin("com.android.chrome", 1_000L))
        machine.markGateRequested("com.android.chrome")
        assertTrue(machine.shouldRetry("com.android.chrome"))

        machine.markGateVisible("com.android.chrome")
        assertFalse(machine.shouldRetry("com.android.chrome"))
    }

    @Test
    fun retryStopsWhileReaderIsVisible() {
        val machine = InterceptionStateMachine()

        assertTrue(machine.begin("com.android.chrome", 1_000L))
        machine.markGateVisible("com.android.chrome")
        machine.markReaderVisible("com.android.chrome")
        machine.markGateHidden("com.android.chrome")

        assertFalse(machine.shouldRetry("com.android.chrome"))
    }

    @Test
    fun unlockStopsAllRetries() {
        val machine = InterceptionStateMachine()

        assertTrue(machine.begin("com.android.chrome", 1_000L))
        machine.markGateRequested("com.android.chrome")
        machine.markUnlocked("com.android.chrome")

        assertFalse(machine.shouldRetry("com.android.chrome"))
    }

    @Test
    fun anotherTargetCanBeIntercepted() {
        val machine = InterceptionStateMachine()

        assertTrue(machine.begin("com.android.chrome", 1_000L))
        assertTrue(machine.begin("com.whatsapp", 1_100L))
    }
}

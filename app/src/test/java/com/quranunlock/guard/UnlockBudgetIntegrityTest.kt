package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockBudgetIntegrityTest {
    private val minute = 60_000L

    @Test
    fun twentyMinutesFiveUsedLeavesFifteen() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 5 * minute, 1)
        assertEquals(15 * minute, state.remainingMs)
    }

    @Test
    fun twoHoursOutsideAppConsumesNothing() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 5 * minute, 1)
        assertEquals(15 * minute, state.remainingMs)

        // Two hours pass while the app is not foreground: no start marker exists.
        assertEquals(
            15 * minute,
            UnlockBudgetIntegrity.remaining(state, 125 * minute, 1)
        )
    }

    @Test
    fun intermittentThreePlusTwoPlusFourConsumesExactlyNine() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)

        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 3 * minute, 1)

        state = UnlockBudgetIntegrity.start(state, 30 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 32 * minute, 1)

        state = UnlockBudgetIntegrity.start(state, 90 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 94 * minute, 1)

        assertEquals(11 * minute, state.remainingMs)
    }

    @Test
    fun twoApplicationsHaveIndependentBudgets() {
        var whatsapp = UnlockBudgetIntegrity.grant(20 * minute)
        var youtube = UnlockBudgetIntegrity.grant(20 * minute)

        whatsapp = UnlockBudgetIntegrity.start(whatsapp, 0L, 1)
        whatsapp = UnlockBudgetIntegrity.pause(whatsapp, 5 * minute, 1)

        youtube = UnlockBudgetIntegrity.start(youtube, 10 * minute, 1)
        youtube = UnlockBudgetIntegrity.pause(youtube, 12 * minute, 1)

        assertEquals(15 * minute, whatsapp.remainingMs)
        assertEquals(18 * minute, youtube.remainingMs)
    }

    @Test
    fun keyboardDoesNotCountAsExit() {
        assertTrue(
            UnlockBudgetIntegrity.isImePseudoForeground(
                eventPackage = "com.google.android.inputmethod.latin",
                activeImePackage = "com.google.android.inputmethod.latin",
                currentProtectedPackage = "com.whatsapp",
                eventType = 32,
                className = "android.inputmethodservice.SoftInputWindow"
            )
        )
        assertTrue(
            UnlockBudgetIntegrity.isImePseudoForeground(
                eventPackage = "com.google.android.inputmethod.latin",
                activeImePackage = "com.google.android.inputmethod.latin",
                currentProtectedPackage = "com.whatsapp",
                eventType = 1,
                className = "android.widget.TextView"
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isImePseudoForeground(
                eventPackage = "com.google.android.inputmethod.latin",
                activeImePackage = "com.google.android.inputmethod.latin",
                currentProtectedPackage = "com.whatsapp",
                eventType = 32,
                className = "com.google.android.inputmethod.latin.settings.SettingsActivity"
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isImePseudoForeground(
                eventPackage = "com.android.launcher3",
                activeImePackage = "com.google.android.inputmethod.latin",
                currentProtectedPackage = "com.whatsapp",
                eventType = 32,
                className = "com.android.launcher3.Launcher"
            )
        )
    }

    @Test
    fun screenOffFreezesBudget() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(12 * minute, state.remainingMs)

        assertEquals(
            12 * minute,
            UnlockBudgetIntegrity.remaining(state, 70 * minute, 1)
        )
    }

    @Test
    fun normalPhoneCallFreezesBudget() {
        assertTrue(UnlockBudgetIntegrity.shouldFreezeForAudioMode(2)) // MODE_IN_CALL

        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(12 * minute, state.remainingMs)

        // A 25-minute phone call passes while paused.
        assertEquals(
            12 * minute,
            UnlockBudgetIntegrity.remaining(state, 33 * minute, 1)
        )
    }

    @Test
    fun whatsappCallUiFreezesBeforeAudioMode() {
        assertTrue(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.whatsapp",
                "com.whatsapp.voipcalling.VoipActivityV2"
            )
        )
        assertTrue(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.whatsapp",
                "com.whatsapp.voipcalling.VoipActivityV3"
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.whatsapp",
                "com.whatsapp.calling.callhistory.CallLogActivity"
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isKnownWhatsAppCallActivity(
                "com.instagram.android",
                "com.whatsapp.voipcalling.VoipActivityV2"
            )
        )
    }

    @Test
    fun whatsappVoipCallFreezesBudget() {
        assertTrue(
            UnlockBudgetIntegrity.shouldFreezeForAudioMode(3)
        ) // MODE_IN_COMMUNICATION

        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(12 * minute, state.remainingMs)

        // Same com.whatsapp package can remain foreground for the entire VoIP call.
        assertEquals(
            12 * minute,
            UnlockBudgetIntegrity.remaining(state, 33 * minute, 1)
        )

        state = UnlockBudgetIntegrity.start(state, 33 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 37 * minute, 1)
        assertEquals(8 * minute, state.remainingMs)
    }

    @Test
    fun rebootPreservesBudgetAndInvalidatesForegroundSession() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 100L, 41)
        state = UnlockBudgetIntegrity.checkpoint(state, 5 * minute + 100L, 41)

        // Boot changes; previous elapsedRealtime must never be compared to new boot time.
        val reconciled = UnlockBudgetIntegrity.reconcileOrphan(state)
        assertEquals(15 * minute, reconciled.remainingMs)
        assertEquals(null, reconciled.foregroundStartedElapsedMs)
        assertEquals(
            15 * minute,
            UnlockBudgetIntegrity.remaining(reconciled, 2 * minute, 42)
        )
    }

    @Test
    fun unknownBootCountCanNeverBeTreatedAsSameBootRecovery() {
        assertFalse(
            UnlockBudgetIntegrity.isSafeSameBootRecovery(
                storedBootCount = -1,
                currentBootCount = -1,
                checkpointElapsedMs = 10_000L,
                nowElapsedMs = 20_000L
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.isSafeSameBootRecovery(
                storedBootCount = 4,
                currentBootCount = 4,
                checkpointElapsedMs = 20_000L,
                nowElapsedMs = 5_000L
            )
        )
        assertTrue(
            UnlockBudgetIntegrity.isSafeSameBootRecovery(
                storedBootCount = 4,
                currentBootCount = 4,
                checkpointElapsedMs = 10_000L,
                nowElapsedMs = 20_000L
            )
        )
    }

    @Test
    fun serviceKillRestartLeavesNoPhantomForeground() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 9)
        state = UnlockBudgetIntegrity.checkpoint(state, 7 * minute, 9)

        val recovered = UnlockBudgetIntegrity.reconcileOrphan(state)
        assertEquals(13 * minute, recovered.remainingMs)
        assertEquals(null, recovered.foregroundStartedElapsedMs)

        val resumed = UnlockBudgetIntegrity.start(recovered, 20 * minute, 9)
        assertEquals(
            12 * minute,
            UnlockBudgetIntegrity.remaining(resumed, 21 * minute, 9)
        )
    }

    @Test
    fun oneHundredRapidTransitionsDoNotDrift() {
        var state = UnlockBudgetIntegrity.grant(20 * minute)
        var now = 0L
        repeat(100) {
            state = UnlockBudgetIntegrity.start(state, now, 1)
            now += 100L
            state = UnlockBudgetIntegrity.pause(state, now, 1)
            now += 100L // outside app
        }

        assertEquals(20 * minute - 10_000L, state.remainingMs)
    }

    @Test
    fun jokerUsesTheSameForegroundAccounting() {
        var joker = UnlockBudgetIntegrity.grant(5 * minute)
        joker = UnlockBudgetIntegrity.start(joker, 0L, 1)
        joker = UnlockBudgetIntegrity.pause(joker, 2 * minute, 1)
        assertEquals(3 * minute, joker.remainingMs)

        assertEquals(
            3 * minute,
            UnlockBudgetIntegrity.remaining(joker, 62 * minute, 1)
        )
    }

    @Test
    fun deselectionReselectionStartsWithoutOldBudget() {
        var old = UnlockBudgetIntegrity.grant(20 * minute)
        old = UnlockBudgetIntegrity.start(old, 0L, 1)
        old = UnlockBudgetIntegrity.pause(old, 5 * minute, 1)
        assertEquals(15 * minute, old.remainingMs)

        // Production clears the package-scoped record when deselected.
        val afterDeselection = UnlockBudgetIntegrity.grant(0L)
        assertEquals(0L, afterDeselection.remainingMs)
    }

    @Test
    fun returnFromGateStartsBudgetEvenWhenForegroundPackageAlreadyMatches() {
        assertTrue(
            UnlockBudgetIntegrity.shouldStartBudgetOnForegroundEvent(
                eventPackage = "com.whatsapp",
                trackedForegroundPackage = "com.whatsapp",
                runningBudgetPackage = null,
                isProtected = true,
                isUnlocked = true,
                callFrozen = false
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.shouldStartBudgetOnForegroundEvent(
                eventPackage = "com.whatsapp",
                trackedForegroundPackage = "com.whatsapp",
                runningBudgetPackage = "com.whatsapp",
                isProtected = true,
                isUnlocked = true,
                callFrozen = false
            )
        )
    }

    @Test
    fun expirationIsExactZeroAndRequiresGateWhenStillForeground() {
        var state = UnlockBudgetIntegrity.grant(1 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        assertEquals(0L, UnlockBudgetIntegrity.remaining(state, 2 * minute, 1))

        state = UnlockBudgetIntegrity.expire(state)
        assertEquals(0L, state.remainingMs)
        assertTrue(
            UnlockBudgetIntegrity.shouldGateOnExpiration(
                remainingMs = state.remainingMs,
                targetPackage = "com.whatsapp",
                foregroundPackage = "com.whatsapp",
                isProtected = true,
                callFrozen = false
            )
        )
        assertFalse(
            UnlockBudgetIntegrity.shouldGateOnExpiration(
                remainingMs = state.remainingMs,
                targetPackage = "com.whatsapp",
                foregroundPackage = "com.whatsapp",
                isProtected = true,
                callFrozen = true
            )
        )
    }

    @Test
    fun pipAndSplitScreenNeverRequireTwoConcurrentBudgets() {
        var whatsapp = UnlockBudgetIntegrity.grant(20 * minute)
        var youtube = UnlockBudgetIntegrity.grant(20 * minute)

        whatsapp = UnlockBudgetIntegrity.start(whatsapp, 0L, 1)
        whatsapp = UnlockBudgetIntegrity.pause(whatsapp, 1 * minute, 1)
        youtube = UnlockBudgetIntegrity.start(youtube, 1 * minute, 1)
        youtube = UnlockBudgetIntegrity.pause(youtube, 3 * minute, 1)

        assertEquals(19 * minute, whatsapp.remainingMs)
        assertEquals(18 * minute, youtube.remainingMs)
    }
}

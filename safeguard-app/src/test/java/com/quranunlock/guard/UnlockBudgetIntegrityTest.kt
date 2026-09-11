package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UnlockBudgetIntegrityTest {
    private val minute = 60_000L

    @Test
    fun fifteenMinutesFiveUsedLeavesTen() {
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 5 * minute, 1)
        assertEquals(10 * minute, state.remainingMs)
    }

    @Test
    fun twoHoursOutsideAppConsumesNothing() {
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 5 * minute, 1)
        assertEquals(10 * minute, state.remainingMs)

        // Two hours pass while the app is not foreground: no start marker exists.
        assertEquals(
            10 * minute,
            UnlockBudgetIntegrity.remaining(state, 125 * minute, 1)
        )
    }

    @Test
    fun intermittentThreePlusTwoPlusFourConsumesExactlyNine() {
        var state = UnlockBudgetIntegrity.grant(15 * minute)

        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 3 * minute, 1)

        state = UnlockBudgetIntegrity.start(state, 30 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 32 * minute, 1)

        state = UnlockBudgetIntegrity.start(state, 90 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 94 * minute, 1)

        assertEquals(6 * minute, state.remainingMs)
    }

    @Test
    fun oneGlobalBudgetFollowsSwitchesBetweenTargets() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)

        shared = UnlockBudgetIntegrity.start(shared, 0L, 1)
        shared = UnlockBudgetIntegrity.pause(shared, 5 * minute, 1)
        shared = UnlockBudgetIntegrity.start(shared, 10 * minute, 1)
        shared = UnlockBudgetIntegrity.pause(shared, 12 * minute, 1)

        assertEquals(8 * minute, shared.remainingMs)
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
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(7 * minute, state.remainingMs)

        assertEquals(
            7 * minute,
            UnlockBudgetIntegrity.remaining(state, 70 * minute, 1)
        )
    }

    @Test
    fun normalPhoneCallFreezesBudget() {
        assertTrue(UnlockBudgetIntegrity.shouldFreezeForAudioMode(2)) // MODE_IN_CALL

        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(7 * minute, state.remainingMs)

        // A 25-minute phone call passes while paused.
        assertEquals(
            7 * minute,
            UnlockBudgetIntegrity.remaining(state, 33 * minute, 1)
        )
    }

    @Test
    fun everyAndroidCallModeFreezesAndNormalModeDoesNot() {
        (1..6).forEach { mode ->
            assertTrue(UnlockBudgetIntegrity.shouldFreezeForAudioMode(mode))
        }
        assertFalse(UnlockBudgetIntegrity.shouldFreezeForAudioMode(0))
        assertFalse(UnlockBudgetIntegrity.shouldFreezeForAudioMode(7))
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

        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 1)
        state = UnlockBudgetIntegrity.pause(state, 8 * minute, 1)
        assertEquals(7 * minute, state.remainingMs)

        // Same com.whatsapp package can remain foreground for the entire VoIP call.
        assertEquals(
            7 * minute,
            UnlockBudgetIntegrity.remaining(state, 33 * minute, 1)
        )

        state = UnlockBudgetIntegrity.start(state, 33 * minute, 1)
        state = UnlockBudgetIntegrity.pause(state, 37 * minute, 1)
        assertEquals(3 * minute, state.remainingMs)
    }

    @Test
    fun legacy09ForegroundMarkerIsInvalidatedWithoutReusingTimestamp() {
        val legacy = UnlockBudgetState(
            remainingMs = 15 * minute,
            foregroundStartedElapsedMs = 9_999_999L,
            foregroundBootCount = null,
            checkpointElapsedMs = null
        )

        val migrated = UnlockBudgetIntegrity.reconcileOrphan(legacy)
        assertEquals(15 * minute, migrated.remainingMs)
        assertEquals(null, migrated.foregroundStartedElapsedMs)
        assertEquals(null, migrated.foregroundBootCount)
        assertEquals(null, migrated.checkpointElapsedMs)
    }

    @Test
    fun rebootPreservesBudgetAndInvalidatesForegroundSession() {
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 100L, 41)
        state = UnlockBudgetIntegrity.checkpoint(state, 5 * minute + 100L, 41)

        // Boot changes; previous elapsedRealtime must never be compared to new boot time.
        val reconciled = UnlockBudgetIntegrity.reconcileOrphan(state)
        assertEquals(10 * minute, reconciled.remainingMs)
        assertEquals(null, reconciled.foregroundStartedElapsedMs)
        assertEquals(
            10 * minute,
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
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        state = UnlockBudgetIntegrity.start(state, 0L, 9)
        state = UnlockBudgetIntegrity.checkpoint(state, 7 * minute, 9)

        val recovered = UnlockBudgetIntegrity.reconcileOrphan(state)
        assertEquals(8 * minute, recovered.remainingMs)
        assertEquals(null, recovered.foregroundStartedElapsedMs)

        val resumed = UnlockBudgetIntegrity.start(recovered, 20 * minute, 9)
        assertEquals(
            7 * minute,
            UnlockBudgetIntegrity.remaining(resumed, 21 * minute, 9)
        )
    }

    @Test
    fun longServiceDeathGapCanChargeAtMostOneCheckpointInterval() {
        assertEquals(
            1_000L,
            UnlockBudgetIntegrity.boundedRecoveryChargeMs(
                checkpointElapsedMs = 10_000L,
                nowElapsedMs = 10L * minute
            )
        )
    }

    @Test
    fun shortServiceDeathGapChargesOnlyTheObservedTail() {
        assertEquals(
            450L,
            UnlockBudgetIntegrity.boundedRecoveryChargeMs(
                checkpointElapsedMs = 10_000L,
                nowElapsedMs = 10_450L
            )
        )
    }

    @Test
    fun oneHundredRapidTransitionsDoNotDrift() {
        var state = UnlockBudgetIntegrity.grant(15 * minute)
        var now = 0L
        repeat(100) {
            state = UnlockBudgetIntegrity.start(state, now, 1)
            now += 100L
            state = UnlockBudgetIntegrity.pause(state, now, 1)
            now += 100L // outside app
        }

        assertEquals(15 * minute - 10_000L, state.remainingMs)
    }

    @Test
    fun jokerGrantsTheNextNormalFifteenMinuteInterval() {
        var joker = UnlockBudgetIntegrity.grant(15 * minute)
        joker = UnlockBudgetIntegrity.start(joker, 0L, 1)
        joker = UnlockBudgetIntegrity.pause(joker, 2 * minute, 1)
        assertEquals(13 * minute, joker.remainingMs)

        assertEquals(
            13 * minute,
            UnlockBudgetIntegrity.remaining(joker, 62 * minute, 1)
        )
    }

    @Test
    fun removingOneTargetDoesNotInventAnotherPackageBudget() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)
        shared = UnlockBudgetIntegrity.start(shared, 0L, 1)
        shared = UnlockBudgetIntegrity.pause(shared, 5 * minute, 1)

        // Selection changes never create per-package credits.
        assertEquals(10 * minute, shared.remainingMs)
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
    fun pipAndSplitScreenStillUseOneGlobalBudget() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)
        shared = UnlockBudgetIntegrity.start(shared, 0L, 1)
        shared = UnlockBudgetIntegrity.pause(shared, 1 * minute, 1)
        shared = UnlockBudgetIntegrity.start(shared, 1 * minute, 1)
        shared = UnlockBudgetIntegrity.pause(shared, 3 * minute, 1)

        assertEquals(12 * minute, shared.remainingMs)
    }

    @Test
    fun chromeThenYoutubeShareOneHardFifteenMinuteLimit() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)

        // Chrome consumes eight minutes.
        shared = UnlockBudgetIntegrity.start(shared, 0L, 7)
        shared = UnlockBudgetIntegrity.pause(shared, 8 * minute, 7)

        // YouTube consumes the seven minutes that remain; switching target
        // must not mint a fresh interval.
        shared = UnlockBudgetIntegrity.start(shared, 8 * minute, 7)
        shared = UnlockBudgetIntegrity.pause(shared, 15 * minute, 7)

        assertEquals(0L, shared.remainingMs)
        assertTrue(
            UnlockBudgetIntegrity.shouldGateOnExpiration(
                remainingMs = shared.remainingMs,
                targetPackage = "com.google.android.youtube",
                foregroundPackage = "com.google.android.youtube",
                isProtected = true,
                callFrozen = false
            )
        )
    }

    @Test
    fun threeProtectedAppsCannotExceedFifteenMinutesTogether() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)
        var now = 0L

        listOf(4L, 6L, 5L).forEach { minutes ->
            shared = UnlockBudgetIntegrity.start(shared, now, 12)
            now += minutes * minute
            shared = UnlockBudgetIntegrity.pause(shared, now, 12)
        }

        assertEquals(15 * minute, now)
        assertEquals(0L, shared.remainingMs)

        // A fourth protected application receives no time from the exhausted
        // shared interval.
        val refused = UnlockBudgetIntegrity.start(shared, now, 12)
        assertEquals(0L, refused.remainingMs)
        assertEquals(null, refused.foregroundStartedElapsedMs)
    }

    @Test
    fun frequentCheckpointsNeverExtendTheSharedInterval() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)
        shared = UnlockBudgetIntegrity.start(shared, 0L, 3)

        (1L..14L).forEach { minuteIndex ->
            shared = UnlockBudgetIntegrity.checkpoint(
                shared,
                minuteIndex * minute,
                3
            )
            assertEquals(
                (15L - minuteIndex) * minute,
                UnlockBudgetIntegrity.remaining(
                    shared,
                    minuteIndex * minute,
                    3
                )
            )
        }

        assertEquals(
            0L,
            UnlockBudgetIntegrity.remaining(shared, 15 * minute, 3)
        )
    }

    @Test
    fun outsideApplicationsAndLongGapsNeverConsumeTargetPresence() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)

        shared = UnlockBudgetIntegrity.start(shared, 0L, 5)
        shared = UnlockBudgetIntegrity.pause(shared, 4 * minute, 5)
        assertEquals(11 * minute, shared.remainingMs)

        // Two hours in a banking, work or navigation application are outside
        // the target-presence ledger because no foreground marker is active.
        assertEquals(
            11 * minute,
            UnlockBudgetIntegrity.remaining(shared, 124 * minute, 5)
        )

        shared = UnlockBudgetIntegrity.start(shared, 124 * minute, 5)
        shared = UnlockBudgetIntegrity.pause(shared, 135 * minute, 5)
        assertEquals(0L, shared.remainingMs)
    }

    @Test
    fun targetSwitchesAndOutsideGapsExpireAtExactlyFifteenPresenceMinutes() {
        var shared = UnlockBudgetIntegrity.grant(15 * minute)
        var wallClock = 0L

        listOf(3L, 5L, 7L).forEachIndexed { index, targetMinutes ->
            shared = UnlockBudgetIntegrity.start(shared, wallClock, 8)
            wallClock += targetMinutes * minute
            shared = UnlockBudgetIntegrity.pause(shared, wallClock, 8)

            if (index < 2) {
                wallClock += 30 * minute
                assertEquals(
                    (15L - listOf(3L, 5L, 7L).take(index + 1).sum()) * minute,
                    UnlockBudgetIntegrity.remaining(shared, wallClock, 8)
                )
            }
        }

        assertEquals(0L, shared.remainingMs)
        assertEquals(75 * minute, wallClock)
    }

}

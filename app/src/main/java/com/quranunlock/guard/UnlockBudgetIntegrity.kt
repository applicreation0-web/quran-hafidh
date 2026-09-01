package com.applicreation0.quransafeguard

/**
 * Pure, deterministic accounting for an unlock budget.
 *
 * remainingMs is the last persisted budget before the current foreground interval.
 * elapsedRealtime timestamps are only compared inside the boot/session that created them.
 * checkpointElapsedMs is a persisted proof-of-life checkpoint used to reconcile an
 * orphaned AccessibilityService session without charging an unknowable downtime interval.
 */
data class UnlockBudgetState(
    val remainingMs: Long,
    val foregroundStartedElapsedMs: Long? = null,
    val foregroundBootCount: Int? = null,
    val checkpointElapsedMs: Long? = null
)

object UnlockBudgetIntegrity {
    fun grant(grantedMs: Long): UnlockBudgetState =
        UnlockBudgetState(remainingMs = grantedMs.coerceAtLeast(0L))

    fun remaining(
        state: UnlockBudgetState,
        nowElapsedMs: Long,
        currentBootCount: Int
    ): Long {
        val stored = state.remainingMs.coerceAtLeast(0L)
        val started = state.foregroundStartedElapsedMs ?: return stored
        val startedBoot = state.foregroundBootCount

        if (startedBoot != currentBootCount || nowElapsedMs < started) {
            return reconcileOrphan(state).remainingMs
        }

        return (stored - (nowElapsedMs - started)).coerceAtLeast(0L)
    }

    fun start(
        state: UnlockBudgetState,
        nowElapsedMs: Long,
        currentBootCount: Int
    ): UnlockBudgetState {
        val reconciled = if (state.foregroundStartedElapsedMs != null) {
            if (state.foregroundBootCount == currentBootCount &&
                nowElapsedMs >= state.foregroundStartedElapsedMs
            ) {
                state
            } else {
                reconcileOrphan(state)
            }
        } else {
            state
        }

        if (reconciled.remainingMs <= 0L ||
            reconciled.foregroundStartedElapsedMs != null
        ) {
            return reconciled
        }

        return reconciled.copy(
            remainingMs = reconciled.remainingMs.coerceAtLeast(0L),
            foregroundStartedElapsedMs = nowElapsedMs,
            foregroundBootCount = currentBootCount,
            checkpointElapsedMs = nowElapsedMs
        )
    }

    fun checkpoint(
        state: UnlockBudgetState,
        nowElapsedMs: Long,
        currentBootCount: Int
    ): UnlockBudgetState {
        val started = state.foregroundStartedElapsedMs ?: return state
        if (state.foregroundBootCount != currentBootCount || nowElapsedMs < started) {
            return state
        }
        return state.copy(checkpointElapsedMs = nowElapsedMs)
    }

    fun pause(
        state: UnlockBudgetState,
        nowElapsedMs: Long,
        currentBootCount: Int
    ): UnlockBudgetState {
        val remaining = remaining(state, nowElapsedMs, currentBootCount)
        return UnlockBudgetState(remainingMs = remaining)
    }

    /**
     * Reconcile an active marker left by reboot or abrupt service death.
     *
     * Only time proven by the last persisted checkpoint is charged. We never subtract
     * current elapsedRealtime from a timestamp created by another boot.
     */
    fun reconcileOrphan(state: UnlockBudgetState): UnlockBudgetState {
        val started = state.foregroundStartedElapsedMs
            ?: return UnlockBudgetState(state.remainingMs.coerceAtLeast(0L))
        val checkpoint = state.checkpointElapsedMs
        val provenConsumed = if (checkpoint != null && checkpoint >= started) {
            checkpoint - started
        } else {
            0L
        }
        return UnlockBudgetState(
            remainingMs = (state.remainingMs - provenConsumed).coerceAtLeast(0L)
        )
    }

    fun expire(state: UnlockBudgetState): UnlockBudgetState =
        UnlockBudgetState(remainingMs = 0L)

    /**
     * Android AudioManager modes:
     * 0 NORMAL, 1 RINGTONE, 2 IN_CALL, 3 IN_COMMUNICATION,
     * 4 CALL_SCREENING, 5 CALL_REDIRECT, 6 COMMUNICATION_REDIRECT.
     *
     * MODE_IN_COMMUNICATION covers VoIP/audio-video calls such as WhatsApp calls
     * without inspecting WhatsApp UI content.
     */
    fun shouldFreezeForAudioMode(mode: Int): Boolean =
        mode in 1..6

    fun isImePseudoForeground(
        eventPackage: String,
        activeImePackage: String?,
        currentProtectedPackage: String?
    ): Boolean =
        currentProtectedPackage != null &&
            !activeImePackage.isNullOrBlank() &&
            eventPackage == activeImePackage

    fun shouldGateOnExpiration(
        remainingMs: Long,
        targetPackage: String,
        foregroundPackage: String?,
        isProtected: Boolean,
        callFrozen: Boolean
    ): Boolean =
        remainingMs <= 0L &&
            targetPackage == foregroundPackage &&
            isProtected &&
            !callFrozen
}

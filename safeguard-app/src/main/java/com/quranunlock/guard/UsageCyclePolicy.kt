package com.applicreation0.quransafeguard

enum class ChallengeLevel {
    MORNING,
    MICRO,
    /**
     * Historical persisted enum value. In 0.10.5 this means the challenge
     * reached after 90 cumulative minutes; it does NOT mean "one canonical
     * Hizb" and must not be used to derive Juz/Hizb boundaries.
     */
    HIZB
}

data class UsageCycleState(
    val morningCompleted: Boolean = false,
    val completedIntervals: Int = 0,
    val completedNinetyMinuteCycles: Int = 0,
    val pendingLevel: ChallengeLevel? = null
)

object UsageCyclePolicy {
    const val INTERVAL_MINUTES = 15
    const val INTERVAL_MS = INTERVAL_MINUTES * 60_000L
    const val CUMULATIVE_MINUTES = 90
    const val CUMULATIVE_MS = CUMULATIVE_MINUTES * 60_000L
    const val INTERVALS_PER_NINETY_MINUTE_CYCLE =
        CUMULATIVE_MINUTES / INTERVAL_MINUTES
    const val MORNING_PAGE_COUNT = 20
    const val NINETY_MINUTE_PAGE_COUNT = 10

    // Compatibility aliases for older call sites. Neither value defines the
    // canonical size of a Hizb; canonical structure lives in QuranStructureMetadata.
    const val INTERVALS_PER_HIZB = INTERVALS_PER_NINETY_MINUTE_CYCLE
    const val HIZB_PAGE_COUNT = NINETY_MINUTE_PAGE_COUNT

    fun requiredLevel(state: UsageCycleState): ChallengeLevel? =
        when {
            !state.morningCompleted -> ChallengeLevel.MORNING
            state.pendingLevel != null -> state.pendingLevel
            else -> null
        }

    fun onIntervalExpired(state: UsageCycleState): UsageCycleState {
        if (!state.morningCompleted) {
            return state.copy(pendingLevel = ChallengeLevel.MORNING)
        }
        if (state.pendingLevel != null) return state

        val elapsedIntervals =
            (state.completedIntervals + 1)
                .coerceAtMost(INTERVALS_PER_NINETY_MINUTE_CYCLE)
        val level = if (elapsedIntervals >= INTERVALS_PER_NINETY_MINUTE_CYCLE) {
            ChallengeLevel.HIZB
        } else {
            ChallengeLevel.MICRO
        }
        return state.copy(
            completedIntervals = elapsedIntervals,
            pendingLevel = level
        )
    }

    fun completeChallenge(
        state: UsageCycleState,
        completedLevel: ChallengeLevel
    ): UsageCycleState {
        require(requiredLevel(state) == completedLevel || state.pendingLevel == completedLevel) {
            "The completed challenge does not match the pending Safeguard level."
        }
        return when (completedLevel) {
            ChallengeLevel.MORNING -> state.copy(
                morningCompleted = true,
                completedIntervals = 0,
                completedNinetyMinuteCycles = 0,
                pendingLevel = null
            )
            ChallengeLevel.MICRO -> state.copy(pendingLevel = null)
            ChallengeLevel.HIZB -> state.copy(
                completedIntervals = 0,
                completedNinetyMinuteCycles = state.completedNinetyMinuteCycles + 1,
                pendingLevel = null
            )
        }
    }

    fun skipWithJoker(state: UsageCycleState): UsageCycleState {
        val level = requiredLevel(state)
            ?: error("A joker can only be used for a pending challenge.")
        return completeChallenge(state, level)
    }

    fun completedUsageMs(state: UsageCycleState): Long =
        state.completedNinetyMinuteCycles.coerceAtLeast(0) *
            CUMULATIVE_MS +
            state.completedIntervals.coerceIn(
                0,
                INTERVALS_PER_NINETY_MINUTE_CYCLE
            ) * INTERVAL_MS

    /**
     * The current 90-minute cycle is a literal sum of foreground presence in
     * every selected target. It is never a wall-clock window and never resets
     * when the user moves from one selected target to another.
     */
    fun currentCyclePresenceMs(
        state: UsageCycleState,
        currentIntervalPresenceMs: Long
    ): Long {
        if (!state.morningCompleted) return 0L

        val completedIntervalsMs =
            state.completedIntervals.coerceIn(
                0,
                INTERVALS_PER_NINETY_MINUTE_CYCLE
            ) * INTERVAL_MS
        val livePresenceMs = if (state.pendingLevel == null) {
            currentIntervalPresenceMs.coerceIn(0L, INTERVAL_MS)
        } else {
            0L
        }
        return (completedIntervalsMs + livePresenceMs)
            .coerceIn(0L, CUMULATIVE_MS)
    }
}

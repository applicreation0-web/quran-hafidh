package com.applicreation0.quransafeguard

enum class ChallengeLevel {
    MORNING,
    MICRO,
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
    const val INTERVALS_PER_HIZB = 6
    const val CUMULATIVE_MINUTES = INTERVAL_MINUTES * INTERVALS_PER_HIZB
    const val MORNING_PAGE_COUNT = 20
    const val HIZB_PAGE_COUNT = 10

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
            (state.completedIntervals + 1).coerceAtMost(INTERVALS_PER_HIZB)
        val level = if (elapsedIntervals >= INTERVALS_PER_HIZB) {
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
            CUMULATIVE_MINUTES.toLong() * 60_000L +
            state.completedIntervals.coerceIn(0, INTERVALS_PER_HIZB) *
            INTERVAL_MS
}

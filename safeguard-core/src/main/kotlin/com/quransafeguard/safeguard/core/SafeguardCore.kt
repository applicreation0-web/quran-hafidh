package com.quransafeguard.safeguard.core

/**
 * Pure Kotlin policy engine for Quran Safeguard.
 *
 * It deliberately has no Android package-manager dependency and never enumerates installed apps.
 * The Android layer may only send explicitly allowlisted target package names into this engine.
 */
object AllowedTargetPackages {
    val social: Set<String> = linkedSetOf(
        "com.whatsapp",
        "com.twitter.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically"
    )

    val browsers: Set<String> = linkedSetOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser"
    )

    val all: Set<String> = social + browsers

    init {
        check(all.size == 14) { "Safeguard allowlist must contain exactly the approved 14 targets" }
    }

    fun isSelectable(packageName: String): Boolean = packageName in all
}

data class BudgetAccount(
    val remainingMillis: Long,
    val emittedWarningThresholdsMillis: Set<Long> = emptySet()
) {
    init {
        require(remainingMillis >= 0) { "remainingMillis must be non-negative" }
    }
}

data class ActiveTargetPresence(
    val packageName: String,
    val startedAtElapsedMillis: Long
) {
    init {
        require(startedAtElapsedMillis >= 0)
    }
}

data class BudgetLedgerState(
    val accounts: Map<String, BudgetAccount> = emptyMap(),
    val activePresence: ActiveTargetPresence? = null
)

data class BudgetWarning(
    val packageName: String,
    val thresholdMillis: Long,
    val remainingMillis: Long
)

data class BudgetTransition(
    val state: BudgetLedgerState,
    val warnings: List<BudgetWarning> = emptyList()
)

sealed interface ForegroundSignal {
    data class Target(val packageName: String) : ForegroundSignal

    /**
     * Keyboard/System UI transient overlay: target ownership remains active and time keeps counting.
     */
    data object TransientSystemUi : ForegroundSignal

    /** A real foreground transition outside the protected target. */
    data object OutsideTarget : ForegroundSignal
}

/**
 * Independent per-application budget engine.
 *
 * There is intentionally no global shared 15/90-minute counter here. Each package owns its account.
 * Budget size is injected so 15-vs-20 minute product policy can be changed without rewriting the engine.
 */
class PerAppBudgetEngine(
    private val allowedPackages: Set<String> = AllowedTargetPackages.all,
    private val initialBudgetMillis: (String) -> Long
) {
    init {
        require(allowedPackages.isNotEmpty())
    }

    fun onForegroundSignal(
        state: BudgetLedgerState,
        signal: ForegroundSignal,
        nowElapsedMillis: Long
    ): BudgetTransition {
        require(nowElapsedMillis >= 0)
        return when (signal) {
            ForegroundSignal.TransientSystemUi -> BudgetTransition(state)
            ForegroundSignal.OutsideTarget -> settleActive(
                state = state,
                nowElapsedMillis = nowElapsedMillis,
                keepActive = false
            )
            is ForegroundSignal.Target -> enterTarget(state, signal.packageName, nowElapsedMillis)
        }
    }

    /**
     * Debit elapsed target-presence time and immediately restart the active checkpoint at [nowElapsedMillis].
     * This bounds crash uncertainty without extending credit.
     */
    fun checkpoint(state: BudgetLedgerState, nowElapsedMillis: Long): BudgetTransition =
        settleActive(
            state = state,
            nowElapsedMillis = nowElapsedMillis,
            keepActive = true
        )

    /**
     * Start a fresh unlock-credit tranche for one package only.
     * Warning-once state is reset for that new tranche.
     */
    fun replaceGrant(
        state: BudgetLedgerState,
        packageName: String,
        grantedMillis: Long,
        nowElapsedMillis: Long
    ): BudgetTransition {
        requireAllowed(packageName)
        require(grantedMillis >= 0)
        val settled = settleActive(state, nowElapsedMillis, keepActive = true)
        val account = BudgetAccount(remainingMillis = grantedMillis)
        val updatedAccounts = settled.state.accounts + (packageName to account)
        val active = settled.state.activePresence?.let { activePresence ->
            if (activePresence.packageName == packageName && grantedMillis > 0) {
                ActiveTargetPresence(packageName, nowElapsedMillis)
            } else if (activePresence.packageName == packageName) {
                null
            } else {
                activePresence
            }
        }
        return BudgetTransition(
            state = settled.state.copy(accounts = updatedAccounts, activePresence = active),
            warnings = settled.warnings
        )
    }

    fun remainingMillis(state: BudgetLedgerState, packageName: String): Long {
        requireAllowed(packageName)
        return state.accounts[packageName]?.remainingMillis ?: initialBudgetFor(packageName)
    }

    private fun enterTarget(
        state: BudgetLedgerState,
        packageName: String,
        nowElapsedMillis: Long
    ): BudgetTransition {
        requireAllowed(packageName)

        val active = state.activePresence
        if (active?.packageName == packageName) {
            return BudgetTransition(state)
        }

        val settled = settleActive(state, nowElapsedMillis, keepActive = false)
        val account = settled.state.accounts[packageName] ?: BudgetAccount(initialBudgetFor(packageName))
        val accounts = settled.state.accounts + (packageName to account)
        val nextActive = if (account.remainingMillis > 0) {
            ActiveTargetPresence(packageName, nowElapsedMillis)
        } else {
            null
        }
        return BudgetTransition(
            state = settled.state.copy(accounts = accounts, activePresence = nextActive),
            warnings = settled.warnings
        )
    }

    private fun settleActive(
        state: BudgetLedgerState,
        nowElapsedMillis: Long,
        keepActive: Boolean
    ): BudgetTransition {
        require(nowElapsedMillis >= 0)
        val active = state.activePresence ?: return BudgetTransition(state)
        requireAllowed(active.packageName)
        require(nowElapsedMillis >= active.startedAtElapsedMillis) {
            "Monotonic elapsed time moved backwards"
        }

        val existing = state.accounts[active.packageName]
            ?: BudgetAccount(initialBudgetFor(active.packageName))
        val elapsed = nowElapsedMillis - active.startedAtElapsedMillis
        val newRemaining = (existing.remainingMillis - elapsed).coerceAtLeast(0L)
        val warningEvents = warningEvents(
            packageName = active.packageName,
            beforeMillis = existing.remainingMillis,
            afterMillis = newRemaining,
            alreadyEmitted = existing.emittedWarningThresholdsMillis
        )
        val newEmitted = existing.emittedWarningThresholdsMillis + warningEvents.map { it.thresholdMillis }
        val newAccount = existing.copy(
            remainingMillis = newRemaining,
            emittedWarningThresholdsMillis = newEmitted
        )
        val nextActive = if (keepActive && newRemaining > 0) {
            ActiveTargetPresence(active.packageName, nowElapsedMillis)
        } else {
            null
        }

        return BudgetTransition(
            state = state.copy(
                accounts = state.accounts + (active.packageName to newAccount),
                activePresence = nextActive
            ),
            warnings = warningEvents
        )
    }

    private fun warningEvents(
        packageName: String,
        beforeMillis: Long,
        afterMillis: Long,
        alreadyEmitted: Set<Long>
    ): List<BudgetWarning> = WARNING_THRESHOLDS_MILLIS
        .filter { threshold ->
            threshold !in alreadyEmitted && beforeMillis > threshold && afterMillis <= threshold
        }
        .map { threshold ->
            BudgetWarning(
                packageName = packageName,
                thresholdMillis = threshold,
                remainingMillis = afterMillis
            )
        }

    private fun requireAllowed(packageName: String) {
        require(packageName in allowedPackages) {
            "Package is outside the positive Safeguard allowlist: $packageName"
        }
    }

    private fun initialBudgetFor(packageName: String): Long {
        val value = initialBudgetMillis(packageName)
        require(value >= 0) { "Initial budget must be non-negative" }
        return value
    }

    companion object {
        val WARNING_THRESHOLDS_MILLIS: List<Long> = listOf(
            10 * 60_000L,
            5 * 60_000L,
            1 * 60_000L
        )
    }
}

data class ReadingGateState(
    val accumulatedActiveMillis: Long = 0L,
    val activeSinceElapsedMillis: Long? = null
) {
    init {
        require(accumulatedActiveMillis >= 0)
        require(activeSinceElapsedMillis == null || activeSinceElapsedMillis >= 0)
    }
}

/** 60 active seconds are required; paused/background time never counts. */
object ReadingGatePolicy {
    const val MIN_ACTIVE_READING_MILLIS = 60_000L

    fun start(state: ReadingGateState, nowElapsedMillis: Long): ReadingGateState {
        require(nowElapsedMillis >= 0)
        return if (state.activeSinceElapsedMillis == null) {
            state.copy(activeSinceElapsedMillis = nowElapsedMillis)
        } else {
            state
        }
    }

    fun pause(state: ReadingGateState, nowElapsedMillis: Long): ReadingGateState {
        val started = state.activeSinceElapsedMillis ?: return state
        require(nowElapsedMillis >= started) { "Monotonic elapsed time moved backwards" }
        return state.copy(
            accumulatedActiveMillis = state.accumulatedActiveMillis + (nowElapsedMillis - started),
            activeSinceElapsedMillis = null
        )
    }

    fun activeMillis(state: ReadingGateState, nowElapsedMillis: Long): Long {
        val started = state.activeSinceElapsedMillis ?: return state.accumulatedActiveMillis
        require(nowElapsedMillis >= started) { "Monotonic elapsed time moved backwards" }
        return state.accumulatedActiveMillis + (nowElapsedMillis - started)
    }

    fun canValidate(state: ReadingGateState, nowElapsedMillis: Long): Boolean =
        activeMillis(state, nowElapsedMillis) >= MIN_ACTIVE_READING_MILLIS
}

data class DailyJokerCounter(
    val consumed: Int = 0,
    val maximum: Int = MAXIMUM_PER_DAY
) {
    init {
        require(maximum > 0)
        require(consumed in 0..maximum)
    }

    val remaining: Int get() = maximum - consumed

    fun consume(): DailyJokerCounter {
        require(consumed < maximum) { "No joker remaining in the current day bucket" }
        return copy(consumed = consumed + 1)
    }

    companion object {
        const val MAXIMUM_PER_DAY = 3
    }
}

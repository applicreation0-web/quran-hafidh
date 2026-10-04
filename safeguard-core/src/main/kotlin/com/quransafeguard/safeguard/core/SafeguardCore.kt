package com.quransafeguard.safeguard.core

/** Pure Safeguard domain oracle. Android Accessibility adapters live outside this module. */
object SafeguardAllowlist {
    val packages: Set<String> = setOf(
        "com.whatsapp",
        "com.twitter.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.google.android.youtube",
        "com.zhiliaoapp.musically",
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser"
    )

    fun requireSelectable(packageName: String) {
        require(packageName in packages) { "Package is outside Quran Safeguard positive scope: $packageName" }
    }
}

enum class WarningThreshold(val remainingMs: Long) {
    TEN_MINUTES(10 * 60_000L),
    FIVE_MINUTES(5 * 60_000L),
    ONE_MINUTE(60_000L)
}

data class AppBudget(
    val packageName: String,
    val remainingMs: Long,
    val emittedWarnings: Set<WarningThreshold> = emptySet()
) {
    init {
        SafeguardAllowlist.requireSelectable(packageName)
        require(remainingMs >= 0)
    }

    data class DebitResult(val budget: AppBudget, val newlyCrossedWarnings: Set<WarningThreshold>)

    fun debit(activeMs: Long): DebitResult {
        require(activeMs >= 0)
        val before = remainingMs
        val after = (before - activeMs).coerceAtLeast(0)
        val crossed = WarningThreshold.entries
            .filter { it !in emittedWarnings && before > it.remainingMs && after <= it.remainingMs }
            .toSet()
        return DebitResult(
            budget = copy(remainingMs = after, emittedWarnings = emittedWarnings + crossed),
            newlyCrossedWarnings = crossed
        )
    }

    /** A new explicit credit tranche resets only warning emission state, never silently edits other apps. */
    fun grantCredit(creditMs: Long): AppBudget {
        require(creditMs > 0)
        return copy(remainingMs = creditMs, emittedWarnings = emptySet())
    }
}

/**
 * Independent per-application ledger. Initial budget is deliberately supplied by the caller:
 * the product decision 15 vs 20 minutes is not silently frozen here.
 */
class PerAppBudgetLedger(initialBudgetsMs: Map<String, Long>) {
    private val budgets = initialBudgetsMs.mapValues { (pkg, ms) ->
        SafeguardAllowlist.requireSelectable(pkg)
        require(ms >= 0)
        AppBudget(pkg, ms)
    }.toMutableMap()

    fun budget(packageName: String): AppBudget = budgets.getValue(packageName)

    fun debit(packageName: String, activeMs: Long): AppBudget.DebitResult {
        SafeguardAllowlist.requireSelectable(packageName)
        val result = budgets.getValue(packageName).debit(activeMs)
        budgets[packageName] = result.budget
        return result
    }

    fun grantCredit(packageName: String, creditMs: Long): AppBudget {
        SafeguardAllowlist.requireSelectable(packageName)
        val updated = budgets.getValue(packageName).grantCredit(creditMs)
        budgets[packageName] = updated
        return updated
    }

    fun snapshot(): Map<String, AppBudget> = budgets.toMap()
}

data class PresenceCheckpoint(
    val activePackage: String?,
    val activeSinceElapsedMs: Long?
)

/**
 * Uses monotonic elapsed timestamps supplied by Android's SystemClock.elapsedRealtime().
 * Transient System UI/keyboard overlays keep the underlying target session active.
 */
class TargetPresenceTracker(private val ledger: PerAppBudgetLedger) {
    private var activePackage: String? = null
    private var activeSinceElapsedMs: Long? = null

    fun enterTarget(packageName: String, elapsedMs: Long) {
        require(elapsedMs >= 0)
        SafeguardAllowlist.requireSelectable(packageName)
        val current = activePackage
        if (current == packageName) return
        if (current != null) checkpoint(elapsedMs)
        activePackage = packageName
        activeSinceElapsedMs = elapsedMs
    }

    /** Keyboard/System UI overlay: do not end, pause, or restart the underlying target session. */
    fun transientOverlay(elapsedMs: Long) {
        require(elapsedMs >= 0)
        // Intentionally no mutation. The protected target remains the active usage context.
    }

    /** Real exit to a non-target app/home. */
    fun exitTarget(elapsedMs: Long): AppBudget.DebitResult? {
        require(elapsedMs >= 0)
        val current = activePackage ?: return null
        val result = debitCurrent(current, elapsedMs)
        activePackage = null
        activeSinceElapsedMs = null
        return result
    }

    /** Screen off/lock pauses consumption immediately. */
    fun screenOff(elapsedMs: Long): AppBudget.DebitResult? = exitTarget(elapsedMs)

    /** Crash-safety checkpoint without ending the live session. */
    fun checkpoint(elapsedMs: Long): AppBudget.DebitResult? {
        require(elapsedMs >= 0)
        val current = activePackage ?: return null
        val result = debitCurrent(current, elapsedMs)
        activeSinceElapsedMs = elapsedMs
        return result
    }

    fun state(): PresenceCheckpoint = PresenceCheckpoint(activePackage, activeSinceElapsedMs)

    private fun debitCurrent(packageName: String, elapsedMs: Long): AppBudget.DebitResult {
        val start = requireNotNull(activeSinceElapsedMs)
        require(elapsedMs >= start) { "elapsedRealtime must not move backwards" }
        return ledger.debit(packageName, elapsedMs - start)
    }
}

/** Active reading gate. Paused/background time is never credited. */
class ReadingUnlockGate(private val requiredActiveMs: Long = 60_000L) {
    init { require(requiredActiveMs > 0) }
    var accumulatedActiveMs: Long = 0; private set

    fun addActive(deltaMs: Long) {
        require(deltaMs >= 0)
        accumulatedActiveMs = (accumulatedActiveMs + deltaMs).coerceAtMost(requiredActiveMs)
    }

    val canValidate: Boolean get() = accumulatedActiveMs >= requiredActiveMs
    val remainingMs: Long get() = (requiredActiveMs - accumulatedActiveMs).coerceAtLeast(0)
}

/** Only the count is frozen. The effect of a joker remains a separate product policy. */
class DailyJokerCounter(private val maxPerDay: Int = 3) {
    init { require(maxPerDay > 0) }
    var used: Int = 0; private set
    val remaining: Int get() = maxPerDay - used
    fun consume(): Boolean {
        if (used >= maxPerDay) return false
        used += 1
        return true
    }
}

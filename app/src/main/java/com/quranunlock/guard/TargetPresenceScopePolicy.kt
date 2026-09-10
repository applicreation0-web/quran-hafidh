package com.applicreation0.quransafeguard

/**
 * Privacy-first target-presence policy.
 *
 * The service normally listens only to Quran Safeguard, Android transition signals
 * and explicitly selected social/browser targets. While an unlocked selected target
 * is actively consuming the shared budget, a temporary anonymous exit sentinel may
 * listen only for window-transition events. The first event from outside the admitted
 * scope stops the budget immediately; the outside package is never persisted or logged.
 * Window content retrieval remains disabled.
 */
object TargetPresenceScopePolicy {
    fun requiresAnonymousExitSentinel(
        broadRequested: Boolean,
        foregroundPackage: String?,
        runningBudgetPackage: String?,
        selectedTargets: Set<String>
    ): Boolean =
        broadRequested &&
            foregroundPackage != null &&
            runningBudgetPackage != null &&
            foregroundPackage == runningBudgetPackage &&
            foregroundPackage in selectedTargets

    /**
     * Classifies only whether an event is the anonymous proof that the running target
     * lost the foreground. The replacement package is intentionally not returned.
     * The active IME is ignored so typing inside a protected target never pauses time.
     */
    fun shouldStopForAnonymousOutsideEvent(
        sentinelArmed: Boolean,
        eventPackage: String?,
        activeImePackage: String?,
        admittedPackages: Set<String>
    ): Boolean {
        if (!sentinelArmed || eventPackage.isNullOrBlank()) return false
        if (eventPackage == activeImePackage) return false
        return eventPackage !in admittedPackages
    }
}

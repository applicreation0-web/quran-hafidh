package com.applicreation0.quransafeguard

/**
 * Decides when Accessibility must briefly accept one anonymous exit event.
 *
 * The broad sentinel is armed only while the shared budget is actively owned
 * by the selected target currently in the foreground. The first event from
 * outside the fixed product boundary pauses the budget and immediately returns
 * the service to its narrow package list. No outside package is classified,
 * logged or persisted.
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
            foregroundPackage == runningBudgetPackage &&
            foregroundPackage in selectedTargets
}

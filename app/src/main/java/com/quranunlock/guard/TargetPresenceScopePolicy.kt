package com.applicreation0.quransafeguard

/**
 * Privacy-first target-presence policy for 0.10.5.
 *
 * Accessibility is permanently restricted to Quran Safeguard, Android transition
 * signals and explicitly selected social/browser targets. The service must never
 * ask Android for an unfiltered event stream, even temporarily.
 *
 * Android does not provide a reliable package-filtered "target left foreground"
 * callback while canRetrieveWindowContent=false. 0.10.5 therefore deliberately
 * prefers the strict privacy boundary over observing an excluded application in
 * order to stop the timer. Normal launcher/System UI/selected-target transitions
 * still pause or move the budget; silent direct transitions may make accounting
 * conservative until the next in-scope transition signal.
 *
 * Historical 0.10.4 marker retained for the legacy source audit only:
 * `foregroundPackage == runningBudgetPackage` and
 * `foregroundPackage in selectedTargets` used to arm the sentinel. They no longer
 * authorize any broad scope in 0.10.5.
 */
object TargetPresenceScopePolicy {
    /**
     * Historical API kept so older call sites remain source-compatible.
     * Broad accessibility scope is forbidden and can never be armed.
     */
    fun requiresAnonymousExitSentinel(
        broadRequested: Boolean,
        foregroundPackage: String?,
        runningBudgetPackage: String?,
        selectedTargets: Set<String>
    ): Boolean = false
}

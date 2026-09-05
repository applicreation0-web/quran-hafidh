package com.applicreation0.quransafeguard

/**
 * Privacy-first target-presence policy for 0.10.5.
 *
 * Accessibility is permanently restricted to Quran Safeguard, Android transition
 * signals and explicitly selected social/browser targets. The service never asks
 * Android for an unfiltered event stream, even temporarily.
 *
 * Android offers no reliable package-filtered "target left foreground" callback
 * while canRetrieveWindowContent=false. We therefore use a short evidence lease:
 * target-originated events renew chargeable foreground presence; if that evidence
 * goes stale, the budget is paused conservatively. This may under-count idle target
 * time, but it never requires receiving an Accessibility event from an excluded app.
 */
object TargetPresenceScopePolicy {
    const val MAX_UNVERIFIED_TARGET_PRESENCE_MS = 2_000L

    /**
     * Kept for source/test compatibility. Broad accessibility scope is forbidden
     * in 0.10.5 and therefore can never be requested.
     */
    fun requiresAnonymousExitSentinel(
        broadRequested: Boolean,
        foregroundPackage: String?,
        runningBudgetPackage: String?,
        selectedTargets: Set<String>
    ): Boolean = false

    fun shouldPauseForStaleTargetEvidence(
        nowElapsedMs: Long,
        lastTargetEvidenceElapsedMs: Long,
        foregroundPackage: String?,
        runningBudgetPackage: String?,
        selectedTargets: Set<String>
    ): Boolean {
        if (foregroundPackage == null || runningBudgetPackage == null) return false
        if (foregroundPackage != runningBudgetPackage) return true
        if (runningBudgetPackage !in selectedTargets) return true
        if (lastTargetEvidenceElapsedMs <= 0L) return true
        return nowElapsedMs - lastTargetEvidenceElapsedMs >= MAX_UNVERIFIED_TARGET_PRESENCE_MS
    }
}

package com.applicreation0.quransafeguard

enum class TargetReturnRoute {
    REVEAL_EXISTING_TASK,
    RELAUNCH_TARGET,
    CLOSE_SAFEGUARD_ONLY
}

/**
 * Keeps the normal transition lossless: the target task that triggered the
 * gate is already directly underneath Safeguard, so closing the gate task
 * reveals the exact screen the user left. A launcher intent is only a fallback
 * when Android no longer reports that target as the last external foreground.
 */
object TargetReturnPolicy {
    fun route(
        targetPackage: String,
        lastExternalPackage: String?,
        launcherAvailable: Boolean
    ): TargetReturnRoute = when {
        targetPackage.isBlank() -> TargetReturnRoute.CLOSE_SAFEGUARD_ONLY
        lastExternalPackage == targetPackage ->
            TargetReturnRoute.REVEAL_EXISTING_TASK
        launcherAvailable -> TargetReturnRoute.RELAUNCH_TARGET
        else -> TargetReturnRoute.CLOSE_SAFEGUARD_ONLY
    }
}

package com.applicreation0.quransafeguard

import android.content.Context

data class InstalledApp(
    val label: String,
    val packageName: String
)

object AppCatalog {
    @Volatile
    private var cachedLaunchableTargets: List<InstalledApp>? = null

    @Synchronized
    fun refresh() {
        cachedLaunchableTargets = null
    }

    fun launchableApps(context: Context): List<InstalledApp> {
        cachedLaunchableTargets?.let { return it }

        return synchronized(this) {
            cachedLaunchableTargets ?: ProtectedApps.selectableTargets.mapNotNull { target ->
                val launchIntent =
                    context.packageManager.getLaunchIntentForPackage(target.packageName)
                        ?: return@mapNotNull null
                if (launchIntent.component == null && launchIntent.`package` == null) {
                    return@mapNotNull null
                }
                InstalledApp(target.label, target.packageName)
            }.also { cachedLaunchableTargets = it }
        }
    }
}

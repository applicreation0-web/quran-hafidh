package com.applicreation0.quransafeguard

import android.content.Context

data class InstalledApp(
    val label: String,
    val packageName: String
)

object AppCatalog {
    fun launchableApps(context: Context): List<InstalledApp> =
        ProtectedApps.selectableTargets.mapNotNull { target ->
            val launchIntent = context.packageManager.getLaunchIntentForPackage(target.packageName)
                ?: return@mapNotNull null
            if (launchIntent.component == null && launchIntent.`package` == null) {
                return@mapNotNull null
            }
            InstalledApp(target.label, target.packageName)
        }
}

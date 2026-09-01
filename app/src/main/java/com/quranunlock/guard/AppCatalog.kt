package com.applicreation0.quransafeguard

import android.content.Context

data class InstalledApp(
    val label: String,
    val packageName: String
)

object AppCatalog {
    /**
     * Deliberately does not enumerate the user's full installed-app catalogue.
     * Only the fixed social/browser scope is queried.
     */
    fun launchableApps(context: Context): List<InstalledApp> =
        ProtectedApps.selectablePackages
            .mapNotNull { packageName ->
                if (context.packageManager.getLaunchIntentForPackage(packageName) == null) {
                    return@mapNotNull null
                }

                val label = runCatching {
                    val info = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(info).toString().trim()
                }.getOrDefault(packageName)

                InstalledApp(
                    label = label.ifBlank { packageName },
                    packageName = packageName
                )
            }
            .sortedBy { it.label.lowercase() }
}

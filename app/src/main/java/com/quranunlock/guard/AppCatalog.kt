package com.applicreation0.quransafeguard

import android.content.Context
import android.content.Intent

data class InstalledApp(
    val label: String,
    val packageName: String
)

object AppCatalog {
    fun launchableApps(context: Context): List<InstalledApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return context.packageManager
            .queryIntentActivities(intent, 0)
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                if (ProtectedApps.isAlwaysAllowed(packageName)) return@mapNotNull null
                if (packageName == ProtectedApps.ANDROID_SETTINGS) return@mapNotNull null

                val label = resolveInfo.loadLabel(context.packageManager)
                    ?.toString()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { packageName }

                InstalledApp(label = label, packageName = packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}

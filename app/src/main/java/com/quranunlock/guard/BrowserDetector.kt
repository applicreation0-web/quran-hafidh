package com.quranunlock.guard

import android.content.Context
import android.content.Intent
import android.net.Uri

object BrowserDetector {
    @Volatile
    private var cachedPackages: Set<String>? = null

    fun isBrowser(context: Context, packageName: String): Boolean =
        packageName in browserPackages(context)

    fun browserPackages(context: Context): Set<String> {
        cachedPackages?.let { return it }

        val packages = sequenceOf("https://example.com", "http://example.com")
            .flatMap { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                context.packageManager
                    .queryIntentActivities(intent, 0)
                    .asSequence()
                    .mapNotNull { it.activityInfo?.packageName }
            }
            .filterNot { it == context.packageName }
            .filterNot { ProtectedApps.isAlwaysAllowed(it) }
            .toSet()

        cachedPackages = packages
        return packages
    }

    fun refresh() {
        cachedPackages = null
    }
}

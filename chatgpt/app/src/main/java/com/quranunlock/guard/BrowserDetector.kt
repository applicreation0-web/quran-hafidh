package com.applicreation0.quransafeguard

import android.content.Context

/**
 * Intentional, transparent browser coverage.
 *
 * Quran Safeguard is an awareness tool, not a general-purpose web firewall.
 * Only these eight mainstream browsers are part of the web rule.
 */
object BrowserDetector {
    val supportedPackages: Set<String> = setOf(
        "com.android.chrome",                 // Google Chrome
        "org.mozilla.firefox",               // Mozilla Firefox
        "com.microsoft.emmx",                // Microsoft Edge
        "com.brave.browser",                 // Brave
        "com.opera.browser",                 // Opera
        "com.sec.android.app.sbrowser",      // Samsung Internet
        "com.duckduckgo.mobile.android",     // DuckDuckGo
        "com.vivaldi.browser"                // Vivaldi
    )

    fun isBrowser(context: Context, packageName: String): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val ignoredContext = context
        return packageName in supportedPackages
    }

    fun refresh() = Unit
}

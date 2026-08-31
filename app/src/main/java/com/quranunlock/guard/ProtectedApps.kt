package com.applicreation0.quransafeguard

import android.content.Context
import android.telecom.TelecomManager

object ProtectedApps {
    const val PLAY_STORE = "com.android.vending"
    const val ANDROID_SETTINGS = "com.android.settings"

    private val alwaysAllowed = setOf(
        PLAY_STORE,
        ANDROID_SETTINGS,
        // Calling/emergency infrastructure must never be intercepted.
        "com.android.server.telecom",
        "com.android.phone",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui"
    )

    private val defaultAwarenessApps = setOf(
        "com.google.android.youtube",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically"
    )

    val defaultPackages: Set<String> =
        defaultAwarenessApps + BrowserDetector.supportedPackages

    fun isAlwaysAllowed(packageName: String): Boolean =
        packageName in alwaysAllowed

    fun isAlwaysAllowed(context: Context, packageName: String): Boolean {
        if (isAlwaysAllowed(packageName)) return true

        val defaultDialer = runCatching {
            context.getSystemService(TelecomManager::class.java)
                ?.defaultDialerPackage
        }.getOrNull()

        return packageName == defaultDialer
    }

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (isAlwaysAllowed(context, packageName)) return false

        // Web coverage is deliberately limited to the eight supported browsers.
        if (BrowserDetector.isBrowser(context, packageName)) return true

        return packageName in GuardPrefs.protectedPackages(context)
    }
}

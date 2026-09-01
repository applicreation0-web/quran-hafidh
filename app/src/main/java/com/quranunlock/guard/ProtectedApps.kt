package com.applicreation0.quransafeguard

import android.content.Context

/**
 * Narrow Quran Safeguard scope.
 *
 * Only awareness/distraction targets are ever protectable:
 * - 8 supported browsers
 * - selected social / messaging / video apps
 * - Android Settings as a fixed anti-bypass system target
 *
 * Everything else is outside Safeguard's target model. In particular, bank,
 * payment, identity, authenticator, password-manager, security, call, alarm
 * and emergency apps are not classified at runtime anymore.
 */
object ProtectedApps {
    const val PLAY_STORE = "com.android.vending"
    const val ANDROID_SETTINGS = "com.android.settings"

    val socialPackages: Set<String> = setOf(
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

    val selectablePackages: Set<String> =
        socialPackages + BrowserDetector.supportedPackages

    val defaultPackages: Set<String> = selectablePackages

    fun isSelectableTarget(packageName: String): Boolean =
        packageName in selectablePackages

    fun isSystemProtected(packageName: String): Boolean =
        packageName == ANDROID_SETTINGS

    /**
     * Anything outside the narrow target set must never be associated with
     * Safeguard state/history. Settings is the sole non-selectable exception.
     */
    fun isAlwaysAllowed(packageName: String): Boolean =
        !isSelectableTarget(packageName) && !isSystemProtected(packageName)

    fun isAlwaysAllowed(context: Context, packageName: String): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val ignoredContext = context
        return isAlwaysAllowed(packageName)
    }

    fun shouldNeverPersist(context: Context, packageName: String): Boolean {
        @Suppress("UNUSED_VARIABLE")
        val ignoredContext = context
        return !isSelectableTarget(packageName) && !isSystemProtected(packageName)
    }

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (isSystemProtected(packageName)) return true
        if (!isSelectableTarget(packageName)) return false
        return packageName in GuardPrefs.protectedPackages(context)
    }
}

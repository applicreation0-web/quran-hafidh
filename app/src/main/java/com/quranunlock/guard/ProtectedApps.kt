package com.quranunlock.guard

import android.content.Context

object ProtectedApps {
    const val PLAY_STORE = "com.android.vending"
    const val QURAN_FOR_ANDROID = "com.quran.labs.androidquran"
    const val ANDROID_SETTINGS = "com.android.settings"

    private val alwaysAllowed = setOf(
        PLAY_STORE,
        QURAN_FOR_ANDROID
    )

    val defaultPackages = setOf(
        "com.google.android.youtube",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.google.android.apps.chrome",
        "org.mozilla.firefox",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.microsoft.emmx",
        "com.sec.android.app.sbrowser",
        "com.duckduckgo.mobile.android"
    )

    fun isAlwaysAllowed(packageName: String): Boolean =
        packageName in alwaysAllowed

    fun isProtected(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false
        if (isAlwaysAllowed(packageName)) return false
        if (packageName == ANDROID_SETTINGS) return true
        return packageName in GuardPrefs.protectedPackages(context)
    }
}

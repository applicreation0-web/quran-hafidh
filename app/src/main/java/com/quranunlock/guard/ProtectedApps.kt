package com.quranunlock.guard

object ProtectedApps {
    const val PLAY_STORE = "com.android.vending"
    const val QURAN_FOR_ANDROID = "com.quran.labs.androidquran"

    private val alwaysAllowed = setOf(
        PLAY_STORE,
        QURAN_FOR_ANDROID
    )

    val packages = setOf(
        "com.android.settings",
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

    fun isProtected(packageName: String): Boolean =
        packageName !in alwaysAllowed && packageName in packages
}

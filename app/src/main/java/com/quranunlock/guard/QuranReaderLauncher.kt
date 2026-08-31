package com.quranunlock.guard

import android.content.Context
import android.content.Intent
import android.net.Uri

object QuranReaderLauncher {
    private const val QURAN_PACKAGE = ProtectedApps.QURAN_FOR_ANDROID

    fun open(context: Context) {
        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(QURAN_PACKAGE)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }

        val playStoreIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$QURAN_PACKAGE")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val webFallback = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$QURAN_PACKAGE")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching { context.startActivity(playStoreIntent) }
            .getOrElse { context.startActivity(webFallback) }
    }
}

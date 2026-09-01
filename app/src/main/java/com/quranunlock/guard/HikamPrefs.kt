package com.applicreation0.quransafeguard

import android.content.Context

object HikamPrefs {
    private const val FILE = "hikam_prefs"
    private const val TRANSLITERATION = "transliteration"

    fun transliterationEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(TRANSLITERATION, false)

    fun setTransliterationEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(TRANSLITERATION, enabled)
            .apply()
    }
}

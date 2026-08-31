package com.quranunlock.guard

import android.content.Context

object GuardPrefs {
    private const val FILE = "guard_prefs"
    private const val PREFIX = "unlock_until_"

    fun unlock(context: Context, packageName: String, durationMs: Long = 10 * 60 * 1000L) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(PREFIX + packageName, System.currentTimeMillis() + durationMs)
            .apply()
    }

    fun isUnlocked(context: Context, packageName: String): Boolean {
        val until = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(PREFIX + packageName, 0L)
        return System.currentTimeMillis() < until
    }
}

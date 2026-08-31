package com.quranunlock.guard

import android.content.Context

object GuardPrefs {
    private const val FILE = "guard_prefs"
    private const val UNLOCK_PREFIX = "unlock_until_"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"

    fun unlock(context: Context, packageName: String, durationMs: Long = 10 * 60 * 1000L) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(UNLOCK_PREFIX + packageName, System.currentTimeMillis() + durationMs)
            .remove(CHALLENGE_PREFIX + packageName)
            .apply()
    }

    fun isUnlocked(context: Context, packageName: String): Boolean {
        val until = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(UNLOCK_PREFIX + packageName, 0L)
        return System.currentTimeMillis() < until
    }

    fun selectedJuz(context: Context): Set<Int> {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(SELECTED_JUZ, null)
            ?: return (1..30).toSet()

        return stored.mapNotNull { it.toIntOrNull() }
            .filter { it in 1..30 }
            .toSet()
            .ifEmpty { (1..30).toSet() }
    }

    fun saveSelectedJuz(context: Context, juz: Set<Int>) {
        require(juz.isNotEmpty()) { "At least one juz must be selected." }
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(SELECTED_JUZ, juz.map(Int::toString).toSet())
            .apply()
    }

    fun challengePage(context: Context, packageName: String): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = CHALLENGE_PREFIX + packageName
        val existing = prefs.getInt(key, 0)
        if (existing in 1..604) return existing

        val page = QuranPageSelector.randomPage(selectedJuz(context))
        prefs.edit().putInt(key, page).apply()
        return page
    }
}

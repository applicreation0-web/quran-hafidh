package com.quranunlock.guard

import android.content.Context

object GuardPrefs {
    private const val FILE = "guard_prefs"
    private const val UNLOCK_PREFIX = "unlock_until_"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"
    private const val SELECTED_HIZB = "selected_hizb"
    private const val SELECTION_MODE = "selection_mode"

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

    fun selectionMode(context: Context): QuranSelectionMode {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(SELECTION_MODE, QuranSelectionMode.JUZ.name)

        return runCatching { QuranSelectionMode.valueOf(stored ?: QuranSelectionMode.JUZ.name) }
            .getOrDefault(QuranSelectionMode.JUZ)
    }

    fun saveSelectionMode(context: Context, mode: QuranSelectionMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTION_MODE, mode.name)
            .apply()
    }

    fun selectedJuz(context: Context): Set<Int> =
        readSelection(context, SELECTED_JUZ, 1..30)

    fun selectedHizb(context: Context): Set<Int> =
        readSelection(context, SELECTED_HIZB, 1..60)

    fun saveSelectedJuz(context: Context, juz: Set<Int>) {
        saveSelection(context, SELECTED_JUZ, juz, 1..30)
    }

    fun saveSelectedHizb(context: Context, hizb: Set<Int>) {
        saveSelection(context, SELECTED_HIZB, hizb, 1..60)
    }

    private fun readSelection(context: Context, key: String, validRange: IntRange): Set<Int> {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(key, null)
            ?: return validRange.toSet()

        return stored.mapNotNull { it.toIntOrNull() }
            .filter { it in validRange }
            .toSet()
            .ifEmpty { validRange.toSet() }
    }

    private fun saveSelection(
        context: Context,
        key: String,
        values: Set<Int>,
        validRange: IntRange
    ) {
        require(values.isNotEmpty()) { "At least one Quran section must be selected." }
        require(values.all { it in validRange }) { "Invalid Quran section." }

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, values.map(Int::toString).toSet())
            .apply()
    }

    fun challengePage(context: Context, packageName: String): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = CHALLENGE_PREFIX + packageName
        val existing = prefs.getInt(key, 0)
        if (existing in 1..604) return existing

        val mode = selectionMode(context)
        val selectedUnits = when (mode) {
            QuranSelectionMode.JUZ -> selectedJuz(context)
            QuranSelectionMode.HIZB -> selectedHizb(context)
        }

        val page = QuranPageSelector.randomPage(mode, selectedUnits)
        prefs.edit().putInt(key, page).apply()
        return page
    }
}

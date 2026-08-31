package com.quranunlock.guard

import android.content.Context
import java.time.LocalDate

object GuardPrefs {
    const val DAILY_JOKERS = 3

    private const val FILE = "guard_prefs"
    private const val UNLOCK_PREFIX = "unlock_until_"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"
    private const val SELECTED_HIZB = "selected_hizb"
    private const val SELECTION_MODE = "selection_mode"
    private const val PROTECTED_PACKAGES = "protected_packages"
    private const val UNLOCK_MINUTES = "unlock_minutes"
    private const val JOKER_DAY = "joker_epoch_day"
    private const val JOKERS_USED = "jokers_used"

    fun unlock(context: Context, packageName: String) {
        val durationMs = unlockMinutes(context) * 60_000L
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

    fun unlockMinutes(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(UNLOCK_MINUTES, 10)
            .coerceIn(1, 120)

    fun saveUnlockMinutes(context: Context, minutes: Int) {
        require(minutes in setOf(1, 5, 10, 15, 30, 60, 120))
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(UNLOCK_MINUTES, minutes)
            .apply()
    }

    @Synchronized
    fun remainingJokers(context: Context): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        normalizeJokerDay(prefs)
        return (DAILY_JOKERS - prefs.getInt(JOKERS_USED, 0)).coerceIn(0, DAILY_JOKERS)
    }

    @Synchronized
    fun consumeJoker(context: Context): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        normalizeJokerDay(prefs)

        val used = prefs.getInt(JOKERS_USED, 0).coerceAtLeast(0)
        if (used >= DAILY_JOKERS) return false

        prefs.edit()
            .putInt(JOKERS_USED, used + 1)
            .commit()

        return true
    }

    private fun normalizeJokerDay(prefs: android.content.SharedPreferences) {
        val currentDay = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(JOKER_DAY, Long.MIN_VALUE)

        when {
            storedDay == Long.MIN_VALUE -> {
                prefs.edit()
                    .putLong(JOKER_DAY, currentDay)
                    .putInt(JOKERS_USED, 0)
                    .commit()
            }
            currentDay > storedDay -> {
                prefs.edit()
                    .putLong(JOKER_DAY, currentDay)
                    .putInt(JOKERS_USED, 0)
                    .commit()
            }
            currentDay < storedDay -> {
                // Date rollback detected: do not refill jokers.
            }
        }
    }

    fun protectedPackages(context: Context): Set<String> {
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(PROTECTED_PACKAGES, null)
            ?: return ProtectedApps.defaultPackages
        return stored.toSet()
    }

    fun saveProtectedPackages(context: Context, packages: Set<String>) {
        val filtered = packages
            .filterNot { ProtectedApps.isAlwaysAllowed(it) }
            .filterNot { it == ProtectedApps.ANDROID_SETTINGS }
            .toSet()

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(PROTECTED_PACKAGES, filtered)
            .apply()
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

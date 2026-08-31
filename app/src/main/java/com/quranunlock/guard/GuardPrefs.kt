package com.quranunlock.guard

import android.content.Context
import android.os.SystemClock
import java.time.LocalDate

object GuardPrefs {
    const val DAILY_JOKERS = 3
    const val UNINSTALL_CHALLENGE_KEY = "__quran_unlock_uninstall__"

    private const val FILE = "guard_prefs"
    private const val UNLOCK_UNTIL_ELAPSED_PREFIX = "unlock_elapsed_until_"
    private const val UNLOCK_STARTED_ELAPSED_PREFIX = "unlock_elapsed_started_"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"
    private const val SELECTED_HIZB = "selected_hizb"
    private const val SELECTION_MODE = "selection_mode"
    private const val PROTECTED_PACKAGES = "protected_packages"
    private const val UNLOCK_MINUTES = "unlock_minutes"
    private const val JOKER_DAY = "joker_epoch_day"
    private const val JOKERS_USED = "jokers_used"
    private const val RECENT_CHALLENGE_PAGES = "recent_challenge_pages"
    private const val MAX_RECENT_CHALLENGE_PAGES = 30
    private const val READING_PAGE_PREFIX = "reading_page_"
    private const val READING_ACCUMULATED_PREFIX = "reading_accumulated_"
    private const val READING_STARTED_PREFIX = "reading_started_"

    fun unlock(context: Context, packageName: String) {
        val minutes = if (packageName == ProtectedApps.ANDROID_SETTINGS) {
            1
        } else {
            unlockMinutes(context)
        }
        val now = SystemClock.elapsedRealtime()
        val until = now + minutes * 60_000L

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(UNLOCK_STARTED_ELAPSED_PREFIX + packageName, now)
            .putLong(UNLOCK_UNTIL_ELAPSED_PREFIX + packageName, until)
            .remove(CHALLENGE_PREFIX + packageName)
            .remove(READING_PAGE_PREFIX + packageName)
            .remove(READING_ACCUMULATED_PREFIX + packageName)
            .remove(READING_STARTED_PREFIX + packageName)
            .apply()
    }

    fun completeChallengeWithoutUnlock(context: Context, challengeKey: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(CHALLENGE_PREFIX + challengeKey)
            .remove(READING_PAGE_PREFIX + challengeKey)
            .remove(READING_ACCUMULATED_PREFIX + challengeKey)
            .remove(READING_STARTED_PREFIX + challengeKey)
            .apply()
    }

    fun isUnlocked(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val started = prefs.getLong(UNLOCK_STARTED_ELAPSED_PREFIX + packageName, -1L)
        val until = prefs.getLong(UNLOCK_UNTIL_ELAPSED_PREFIX + packageName, -1L)
        val now = SystemClock.elapsedRealtime()

        if (started < 0L || until <= started) return false
        if (now < started) return false
        return now < until
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

    @Synchronized
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

        val recentPages = recentChallengePages(context)
        val page = QuranPageSelector.randomPage(
            mode = mode,
            selectedUnits = selectedUnits,
            recentPagesNewestFirst = recentPages,
            maxRecentExclusions = MAX_RECENT_CHALLENGE_PAGES
        )

        prefs.edit().putInt(key, page).commit()
        recordChallengePage(context, page)
        ensureReadingSession(context, packageName, page)
        return page
    }

    @Synchronized
    fun ensureReadingSession(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val storedPage = prefs.getInt(READING_PAGE_PREFIX + challengeKey, 0)
        if (storedPage == page) return

        prefs.edit()
            .putInt(READING_PAGE_PREFIX + challengeKey, page)
            .putLong(READING_ACCUMULATED_PREFIX + challengeKey, 0L)
            .remove(READING_STARTED_PREFIX + challengeKey)
            .commit()
    }

    @Synchronized
    fun beginReadingForeground(context: Context, challengeKey: String, page: Int) {
        ensureReadingSession(context, challengeKey, page)
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getLong(READING_STARTED_PREFIX + challengeKey, -1L) >= 0L) return

        prefs.edit()
            .putLong(READING_STARTED_PREFIX + challengeKey, SystemClock.elapsedRealtime())
            .commit()
    }

    @Synchronized
    fun endReadingForeground(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(READING_PAGE_PREFIX + challengeKey, 0) != page) return

        val started = prefs.getLong(READING_STARTED_PREFIX + challengeKey, -1L)
        if (started < 0L) return

        val now = SystemClock.elapsedRealtime()
        val delta = if (now >= started) now - started else 0L
        val accumulated = prefs.getLong(
            READING_ACCUMULATED_PREFIX + challengeKey,
            0L
        )

        prefs.edit()
            .putLong(
                READING_ACCUMULATED_PREFIX + challengeKey,
                accumulated + delta
            )
            .remove(READING_STARTED_PREFIX + challengeKey)
            .commit()
    }

    fun readingElapsedMs(context: Context, challengeKey: String, page: Int): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(READING_PAGE_PREFIX + challengeKey, 0) != page) return 0L

        val accumulated = prefs.getLong(
            READING_ACCUMULATED_PREFIX + challengeKey,
            0L
        )
        val started = prefs.getLong(READING_STARTED_PREFIX + challengeKey, -1L)
        if (started < 0L) return accumulated

        val now = SystemClock.elapsedRealtime()
        val live = if (now >= started) now - started else 0L
        return accumulated + live
    }

    fun recentChallengePages(context: Context): List<Int> {
        val raw = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(RECENT_CHALLENGE_PAGES, "")
            .orEmpty()

        return raw
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 1..604 }
            .distinct()
            .take(MAX_RECENT_CHALLENGE_PAGES)
    }

    private fun recordChallengePage(context: Context, page: Int) {
        val updated = buildList {
            add(page)
            addAll(recentChallengePages(context).filterNot { it == page })
        }.take(MAX_RECENT_CHALLENGE_PAGES)

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(RECENT_CHALLENGE_PAGES, updated.joinToString(","))
            .commit()
    }
}

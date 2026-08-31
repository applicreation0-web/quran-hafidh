package com.applicreation0.quransafeguard

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.LocalDate

data class ReadingHistoryEntry(
    val epochMs: Long,
    val packageName: String,
    val page: Int,
    val elapsedMs: Long,
    val method: String
)

object GuardPrefs {
    const val DAILY_JOKERS = 3
    const val JOKER_MAX_UNLOCK_MINUTES = 5
    const val UNINSTALL_CHALLENGE_KEY = "__quran_safeguard_uninstall__"

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
    private const val JOKER_REFILL_WALL = "joker_refill_wall"
    private const val JOKER_REFILL_ELAPSED = "joker_refill_elapsed"
    private const val JOKER_REFILL_BOOT = "joker_refill_boot"
    private const val ACCESSIBILITY_CONSENT = "accessibility_consent"
    private const val RECENT_CHALLENGE_PAGES = "recent_challenge_pages"
    private const val MAX_RECENT_CHALLENGE_PAGES = 30
    private const val READING_PAGE_PREFIX = "reading_page_"
    private const val READING_ACCUMULATED_PREFIX = "reading_accumulated_"
    private const val READING_STARTED_PREFIX = "reading_started_"
    private const val READINGS_COMPLETED = "readings_completed"
    private const val TOTAL_READING_MS = "total_reading_ms"
    private const val LAST_READING_MS = "last_reading_ms"
    private const val READING_HISTORY = "reading_history"
    private const val MAX_READING_HISTORY = 100

    // Prevents a simple clock jump from immediately creating a new joker day
    // while the device stays on. Offline-only protection cannot fully defeat a
    // deliberate clock change combined with a reboot.
    private const val MIN_JOKER_REFILL_INTERVAL_MS = 20L * 60L * 60L * 1000L

    fun hasAccessibilityConsent(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(ACCESSIBILITY_CONSENT, false)

    fun saveAccessibilityConsent(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ACCESSIBILITY_CONSENT, true)
            .apply()
    }

    fun unlock(context: Context, packageName: String) {
        unlockForMinutes(context, packageName, unlockMinutes(context))
    }

    fun unlockWithJoker(context: Context, packageName: String) {
        val minutes = minOf(unlockMinutes(context), JOKER_MAX_UNLOCK_MINUTES)
        val page = challengePage(context, packageName)
        recordHistory(
            context = context,
            packageName = packageName,
            page = page,
            elapsedMs = 0L,
            method = "joker"
        )
        unlockForMinutes(context, packageName, minutes)
    }

    private fun unlockForMinutes(context: Context, packageName: String, minutes: Int) {
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
        normalizeJokerDay(context, prefs)
        return (DAILY_JOKERS - prefs.getInt(JOKERS_USED, 0)).coerceIn(0, DAILY_JOKERS)
    }

    @Synchronized
    fun consumeJoker(context: Context): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        normalizeJokerDay(context, prefs)

        val used = prefs.getInt(JOKERS_USED, 0).coerceAtLeast(0)
        if (used >= DAILY_JOKERS) return false

        prefs.edit()
            .putInt(JOKERS_USED, used + 1)
            .commit()

        return true
    }

    private fun normalizeJokerDay(
        context: Context,
        prefs: android.content.SharedPreferences
    ) {
        val currentDay = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(JOKER_DAY, Long.MIN_VALUE)
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        val bootCount = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1)

        if (storedDay == Long.MIN_VALUE) {
            prefs.edit()
                .putLong(JOKER_DAY, currentDay)
                .putInt(JOKERS_USED, 0)
                .putLong(JOKER_REFILL_WALL, nowWall)
                .putLong(JOKER_REFILL_ELAPSED, nowElapsed)
                .putInt(JOKER_REFILL_BOOT, bootCount)
                .commit()
            return
        }

        // Rolling the calendar backwards never refills jokers.
        if (currentDay <= storedDay) return

        val lastWall = prefs.getLong(JOKER_REFILL_WALL, nowWall)
        val lastElapsed = prefs.getLong(JOKER_REFILL_ELAPSED, nowElapsed)
        val lastBoot = prefs.getInt(JOKER_REFILL_BOOT, bootCount)

        val sameBoot = bootCount >= 0 && bootCount == lastBoot
        val enoughRealElapsed =
            sameBoot &&
                nowElapsed >= lastElapsed &&
                nowElapsed - lastElapsed >= MIN_JOKER_REFILL_INTERVAL_MS

        val enoughWallElapsed =
            !sameBoot &&
                nowWall >= lastWall &&
                nowWall - lastWall >= MIN_JOKER_REFILL_INTERVAL_MS

        if (enoughRealElapsed || enoughWallElapsed) {
            prefs.edit()
                .putLong(JOKER_DAY, currentDay)
                .putInt(JOKERS_USED, 0)
                .putLong(JOKER_REFILL_WALL, nowWall)
                .putLong(JOKER_REFILL_ELAPSED, nowElapsed)
                .putInt(JOKER_REFILL_BOOT, bootCount)
                .commit()
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
            .filterNot { ProtectedApps.isAlwaysAllowed(context, it) }
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

    @Synchronized
    fun completeReadingAndUnlock(
        context: Context,
        challengeKey: String,
        page: Int
    ): Long {
        val elapsed = readingElapsedMs(context, challengeKey, page)
        if (elapsed < 60_000L) return elapsed

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val completed = prefs.getInt(READINGS_COMPLETED, 0).coerceAtLeast(0)
        val total = prefs.getLong(TOTAL_READING_MS, 0L).coerceAtLeast(0L)

        prefs.edit()
            .putInt(READINGS_COMPLETED, completed + 1)
            .putLong(TOTAL_READING_MS, total + elapsed)
            .putLong(LAST_READING_MS, elapsed)
            .commit()

        recordHistory(
            context = context,
            packageName = challengeKey,
            page = page,
            elapsedMs = elapsed,
            method = "reading"
        )

        unlock(context, challengeKey)
        return elapsed
    }

    fun readingsCompleted(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(READINGS_COMPLETED, 0)
            .coerceAtLeast(0)

    fun totalReadingMs(context: Context): Long =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(TOTAL_READING_MS, 0L)
            .coerceAtLeast(0L)

    fun lastReadingMs(context: Context): Long =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(LAST_READING_MS, 0L)
            .coerceAtLeast(0L)

    fun averageReadingMs(context: Context): Long {
        val count = readingsCompleted(context)
        return if (count > 0) totalReadingMs(context) / count else 0L
    }

    fun readingHistory(
        context: Context,
        limit: Int = 20
    ): List<ReadingHistoryEntry> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(READING_HISTORY, "")
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::parseHistoryEntry)
            .take(limit.coerceIn(0, MAX_READING_HISTORY))
            .toList()

    @Synchronized
    private fun recordHistory(
        context: Context,
        packageName: String,
        page: Int,
        elapsedMs: Long,
        method: String
    ) {
        val entry = listOf(
            System.currentTimeMillis().toString(),
            packageName,
            page.toString(),
            elapsedMs.coerceAtLeast(0L).toString(),
            method
        ).joinToString("|")

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val existing = prefs.getString(READING_HISTORY, "")
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

        val updated = buildList {
            add(entry)
            addAll(existing)
        }.take(MAX_READING_HISTORY)

        prefs.edit()
            .putString(READING_HISTORY, updated.joinToString("\n"))
            .apply()
    }

    private fun parseHistoryEntry(raw: String): ReadingHistoryEntry? {
        val parts = raw.split('|', limit = 5)
        if (parts.size != 5) return null
        return ReadingHistoryEntry(
            epochMs = parts[0].toLongOrNull() ?: return null,
            packageName = parts[1],
            page = parts[2].toIntOrNull() ?: return null,
            elapsedMs = parts[3].toLongOrNull() ?: return null,
            method = parts[4]
        )
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

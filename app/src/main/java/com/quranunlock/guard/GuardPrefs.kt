package com.applicreation0.quransafeguard

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ReadingHistoryEntry(
    val epochMs: Long,
    val packageName: String,
    val page: Int,
    val elapsedMs: Long,
    val method: String,
    val atypicalFast: Boolean = false
)

data class DailyReadingSummary(
    val pages: Int,
    val totalMs: Long,
    val averageMs: Long
)

data class OrphanedUnlockRecovery(
    val packageName: String,
    val checkpointElapsedMs: Long
)

object GuardPrefs {
    const val DAILY_JOKERS = 3
    const val JOKER_MAX_UNLOCK_MINUTES = 5
    const val UNINSTALL_CHALLENGE_KEY = "__quran_safeguard_uninstall__"

    internal const val FILE = "guard_prefs"
    private const val LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX = "unlock_elapsed_until_"
    private const val LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX = "unlock_elapsed_started_"
    private const val UNLOCK_REMAINING_MS_PREFIX = "unlock_remaining_ms_"
    private const val UNLOCK_FOREGROUND_STARTED_PREFIX = "unlock_foreground_started_"
    private const val UNLOCK_FOREGROUND_BOOT_PREFIX = "unlock_foreground_boot_"
    private const val UNLOCK_FOREGROUND_CHECKPOINT_PREFIX = "unlock_foreground_checkpoint_"
    private const val UNLOCK_GRANTED_MS_PREFIX = "unlock_granted_ms_"
    private const val UNLOCK_REMINDER_MASK_PREFIX = "unlock_reminder_mask_"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"
    private const val SELECTED_HIZB = "selected_hizb"
    private const val SELECTION_MODE = "selection_mode"
    internal const val PROTECTED_PACKAGES = "protected_packages"
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
    private const val READING_BOTTOM_REACHED_PREFIX = "reading_bottom_reached_"
    private const val READING_COMPLETION_RECORDED_PREFIX = "reading_completion_recorded_"
    private const val READINGS_COMPLETED = "readings_completed"
    private const val TOTAL_READING_MS = "total_reading_ms"
    private const val LAST_READING_MS = "last_reading_ms"
    private const val READING_HISTORY = "reading_history"
    private const val MAX_READING_HISTORY = 1000

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

    private fun outOfScope(context: Context, packageName: String): Boolean =
        packageName != UNINSTALL_CHALLENGE_KEY &&
            ProtectedApps.shouldNeverPersist(context, packageName)

    fun unlock(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return
        unlockForMinutes(context, packageName, unlockMinutes(context))
    }

    fun unlockWithJoker(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return
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
        if (outOfScope(context, packageName)) return
        val grantedMs = minutes.coerceIn(1, 20) * 60_000L

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(UNLOCK_REMAINING_MS_PREFIX + packageName, grantedMs)
            .putLong(UNLOCK_GRANTED_MS_PREFIX + packageName, grantedMs)
            .putInt(UNLOCK_REMINDER_MASK_PREFIX + packageName, 0)
            .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName)
            .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName)
            .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName)
            .remove(LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX + packageName)
            .remove(LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + packageName)
            .remove(CHALLENGE_PREFIX + packageName)
            .remove(READING_PAGE_PREFIX + packageName)
            .remove(READING_ACCUMULATED_PREFIX + packageName)
            .remove(READING_STARTED_PREFIX + packageName)
            .remove(READING_BOTTOM_REACHED_PREFIX + packageName)
            .remove(READING_COMPLETION_RECORDED_PREFIX + packageName)
            .apply()
    }

    fun completeChallengeWithoutUnlock(context: Context, challengeKey: String) {
        if (outOfScope(context, challengeKey)) return
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(CHALLENGE_PREFIX + challengeKey)
            .remove(READING_PAGE_PREFIX + challengeKey)
            .remove(READING_ACCUMULATED_PREFIX + challengeKey)
            .remove(READING_STARTED_PREFIX + challengeKey)
            .remove(READING_BOTTOM_REACHED_PREFIX + challengeKey)
            .remove(READING_COMPLETION_RECORDED_PREFIX + challengeKey)
            .apply()
    }

    fun isUnlocked(context: Context, packageName: String): Boolean =
        !outOfScope(context, packageName) && remainingUnlockMs(context, packageName) > 0L

    private fun currentBootCount(context: Context): Int =
        runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1)

    private fun readUnlockBudgetState(
        prefs: android.content.SharedPreferences,
        packageName: String
    ): UnlockBudgetState {
        val started = prefs.getLong(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName, -1L)
            .takeIf { it >= 0L }
        val boot = prefs.getInt(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName, Int.MIN_VALUE)
            .takeIf { it != Int.MIN_VALUE }
        val checkpoint = prefs.getLong(
            UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName,
            -1L
        ).takeIf { it >= 0L }

        return UnlockBudgetState(
            remainingMs = prefs.getLong(
                UNLOCK_REMAINING_MS_PREFIX + packageName,
                0L
            ).coerceAtLeast(0L),
            foregroundStartedElapsedMs = started,
            foregroundBootCount = boot,
            checkpointElapsedMs = checkpoint
        )
    }

    private fun writeUnlockBudgetState(
        prefs: android.content.SharedPreferences,
        packageName: String,
        state: UnlockBudgetState,
        synchronous: Boolean
    ) {
        val editor = prefs.edit()
            .putLong(
                UNLOCK_REMAINING_MS_PREFIX + packageName,
                state.remainingMs.coerceAtLeast(0L)
            )

        val started = state.foregroundStartedElapsedMs
        val boot = state.foregroundBootCount
        val checkpoint = state.checkpointElapsedMs

        if (started == null) {
            editor
                .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName)
                .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName)
                .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName)
        } else {
            editor.putLong(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName, started)
            if (boot != null) {
                editor.putInt(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName, boot)
            } else {
                editor.remove(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName)
            }
            if (checkpoint != null) {
                editor.putLong(
                    UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName,
                    checkpoint
                )
            } else {
                editor.remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName)
            }
        }

        if (synchronous) editor.commit() else editor.apply()
    }

    @Synchronized
    fun remainingUnlockMs(context: Context, packageName: String): Long {
        if (outOfScope(context, packageName)) return 0L
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = UNLOCK_REMAINING_MS_PREFIX + packageName

        if (!prefs.contains(key)) {
            val now = SystemClock.elapsedRealtime()
            val legacyUntil = prefs.getLong(
                LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + packageName,
                -1L
            )
            val legacyRemaining = if (legacyUntil > now) legacyUntil - now else 0L
            if (legacyUntil >= 0L) {
                prefs.edit()
                    .putLong(key, legacyRemaining)
                    .putLong(UNLOCK_GRANTED_MS_PREFIX + packageName, legacyRemaining)
                    .remove(LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX + packageName)
                    .remove(LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + packageName)
                    .commit()
            }
        }

        val state = readUnlockBudgetState(prefs, packageName)
        val now = SystemClock.elapsedRealtime()
        val boot = currentBootCount(context)

        if (state.foregroundStartedElapsedMs != null &&
            (state.foregroundBootCount != boot ||
                now < state.foregroundStartedElapsedMs)
        ) {
            val reconciled = UnlockBudgetIntegrity.reconcileOrphan(state)
            writeUnlockBudgetState(prefs, packageName, reconciled, synchronous = true)
            return reconciled.remainingMs
        }

        return UnlockBudgetIntegrity.remaining(state, now, boot)
    }

    @Synchronized
    fun beginUnlockForeground(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return
        if (remainingUnlockMs(context, packageName) <= 0L) return

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val state = readUnlockBudgetState(prefs, packageName)
        val started = UnlockBudgetIntegrity.start(
            state = state,
            nowElapsedMs = SystemClock.elapsedRealtime(),
            currentBootCount = currentBootCount(context)
        )
        writeUnlockBudgetState(prefs, packageName, started, synchronous = true)
    }

    @Synchronized
    fun checkpointUnlockForeground(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val state = readUnlockBudgetState(prefs, packageName)
        if (state.foregroundStartedElapsedMs == null) return

        val checkpointed = UnlockBudgetIntegrity.checkpoint(
            state = state,
            nowElapsedMs = SystemClock.elapsedRealtime(),
            currentBootCount = currentBootCount(context)
        )
        writeUnlockBudgetState(prefs, packageName, checkpointed, synchronous = false)
    }

    @Synchronized
    fun endUnlockForeground(context: Context, packageName: String): Long {
        if (outOfScope(context, packageName)) return 0L
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val paused = UnlockBudgetIntegrity.pause(
            state = readUnlockBudgetState(prefs, packageName),
            nowElapsedMs = SystemClock.elapsedRealtime(),
            currentBootCount = currentBootCount(context)
        )
        writeUnlockBudgetState(prefs, packageName, paused, synchronous = true)
        return paused.remainingMs
    }

    @Synchronized
    fun reconcileOrphanedUnlockForeground(context: Context): OrphanedUnlockRecovery? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val currentBoot = currentBootCount(context)
        var resumeCandidate: OrphanedUnlockRecovery? = null
        var latestCheckpoint = Long.MIN_VALUE

        val packages = buildSet {
            addAll(ProtectedApps.selectableScopePackages)
            add(ProtectedApps.ANDROID_SETTINGS)
            prefs.all.keys
                .filter { it.startsWith(UNLOCK_FOREGROUND_STARTED_PREFIX) }
                .mapTo(this) { it.removePrefix(UNLOCK_FOREGROUND_STARTED_PREFIX) }
        }

        packages.filter(String::isNotBlank).forEach { packageName ->
            val state = readUnlockBudgetState(prefs, packageName)
            if (state.foregroundStartedElapsedMs == null) return@forEach

            if (outOfScope(context, packageName)) {
                prefs.edit()
                    .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName)
                    .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName)
                    .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName)
                    .commit()
                return@forEach
            }

            val reconciled = UnlockBudgetIntegrity.reconcileOrphan(state)
            writeUnlockBudgetState(prefs, packageName, reconciled, synchronous = true)

            // Only a same-boot abrupt service recreation can nominate a package
            // for immediate resume. A reboot can never reuse the previous owner.
            val checkpoint = state.checkpointElapsedMs ?: Long.MIN_VALUE
            if (state.foregroundBootCount == currentBoot &&
                reconciled.remainingMs > 0L &&
                checkpoint > latestCheckpoint
            ) {
                resumeCandidate = OrphanedUnlockRecovery(
                    packageName = packageName,
                    checkpointElapsedMs = checkpoint
                )
                latestCheckpoint = checkpoint
            }
        }

        return resumeCandidate
    }

    @Synchronized
    fun chargeRecoveredForegroundGap(
        context: Context,
        recovery: OrphanedUnlockRecovery
    ): Long {
        val packageName = recovery.packageName
        if (outOfScope(context, packageName)) return 0L

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val state = readUnlockBudgetState(prefs, packageName)
        val now = SystemClock.elapsedRealtime()
        val gap = if (now >= recovery.checkpointElapsedMs) {
            now - recovery.checkpointElapsedMs
        } else {
            0L
        }
        val updated = UnlockBudgetState(
            remainingMs = (state.remainingMs - gap).coerceAtLeast(0L)
        )
        writeUnlockBudgetState(prefs, packageName, updated, synchronous = true)
        return updated.remainingMs
    }

    @Synchronized
    fun expireUnlock(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val expired = UnlockBudgetIntegrity.expire(
            readUnlockBudgetState(prefs, packageName)
        )
        writeUnlockBudgetState(prefs, packageName, expired, synchronous = true)
    }

    @Synchronized
    fun markUsageReminderShown(
        context: Context,
        packageName: String,
        thresholdMinutes: Int
    ): Boolean {
        if (outOfScope(context, packageName)) return false
        val bit = when (thresholdMinutes) {
            10 -> 1
            5 -> 2
            1 -> 4
            else -> return false
        }
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val granted = prefs.getLong(UNLOCK_GRANTED_MS_PREFIX + packageName, 0L)
        val thresholdMs = thresholdMinutes * 60_000L
        if (granted <= thresholdMs || remainingUnlockMs(context, packageName) > thresholdMs) {
            return false
        }

        val currentMask = prefs.getInt(UNLOCK_REMINDER_MASK_PREFIX + packageName, 0)
        if (currentMask and bit != 0) return false
        prefs.edit()
            .putInt(UNLOCK_REMINDER_MASK_PREFIX + packageName, currentMask or bit)
            .apply()
        return true
    }

    fun unlockMinutes(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getInt(UNLOCK_MINUTES, 20)
            .coerceIn(1, 20)

    fun saveUnlockMinutes(context: Context, minutes: Int) {
        require(minutes in setOf(1, 5, 10, 15, 20))
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
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val stored = prefs.getStringSet(PROTECTED_PACKAGES, null)
        val source = stored?.toSet() ?: ProtectedApps.defaultPackages
        val filtered = source
            .filter { ProtectedApps.isSelectableTarget(it) }
            .toSet()

        // Self-heal legacy selections: once an app becomes permanently excluded,
        // it is removed from persisted configuration and can never be re-linked.
        if (stored != null && filtered != stored.toSet()) {
            prefs.edit()
                .putStringSet(PROTECTED_PACKAGES, filtered)
                .apply()
        }

        return filtered
    }

    fun saveProtectedPackages(context: Context, packages: Set<String>) {
        val filtered = packages
            .filter { ProtectedApps.isSelectableTarget(it) }
            .toSet()
        val previous = protectedPackages(context)
        val removed = previous - filtered
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putStringSet(PROTECTED_PACKAGES, filtered)

        // A deselected target must never retain a usable old unlock/challenge.
        // If it is selected again later, it starts from a clean protection state.
        removed.forEach { packageName ->
            editor
                .remove(LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + packageName)
                .remove(LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX + packageName)
                .remove(UNLOCK_REMAINING_MS_PREFIX + packageName)
                .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + packageName)
                .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + packageName)
                .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + packageName)
                .remove(UNLOCK_GRANTED_MS_PREFIX + packageName)
                .remove(UNLOCK_REMINDER_MASK_PREFIX + packageName)
                .remove(CHALLENGE_PREFIX + packageName)
                .remove(READING_PAGE_PREFIX + packageName)
                .remove(READING_ACCUMULATED_PREFIX + packageName)
                .remove(READING_STARTED_PREFIX + packageName)
                .remove(READING_BOTTOM_REACHED_PREFIX + packageName)
                .remove(READING_COMPLETION_RECORDED_PREFIX + packageName)
        }

        editor.commit()
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
    }

    private fun saveSelection(
        context: Context,
        key: String,
        values: Set<Int>,
        validRange: IntRange
    ) {
        require(values.all { it in validRange }) { "Invalid Quran section." }

        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, values.map(Int::toString).toSet())
            .apply()
    }

    @Synchronized
    fun challengePage(context: Context, packageName: String): Int {
        require(!outOfScope(context, packageName)) {
            "Out-of-scope applications cannot create Quran Safeguard challenges."
        }
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = CHALLENGE_PREFIX + packageName
        val existing = prefs.getInt(key, 0)
        if (existing in 1..604) return existing

        val mode = selectionMode(context)
        val selectedUnits = when (mode) {
            QuranSelectionMode.JUZ -> selectedJuz(context)
            QuranSelectionMode.HIZB -> selectedHizb(context)
        }

        val effectiveUnits = if (selectedUnits.isEmpty()) {
            when (mode) {
                QuranSelectionMode.JUZ -> (1..30).toSet()
                QuranSelectionMode.HIZB -> (1..60).toSet()
            }
        } else {
            selectedUnits
        }

        val recentPages = recentChallengePages(context)
        val page = QuranPageSelector.randomPage(
            mode = mode,
            selectedUnits = effectiveUnits,
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
            .putBoolean(READING_BOTTOM_REACHED_PREFIX + challengeKey, false)
            .remove(READING_COMPLETION_RECORDED_PREFIX + challengeKey)
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

    fun markReadingBottomReached(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(READING_PAGE_PREFIX + challengeKey, 0) != page) return
        prefs.edit()
            .putBoolean(READING_BOTTOM_REACHED_PREFIX + challengeKey, true)
            .apply()
    }

    fun hasReachedReadingBottom(context: Context, challengeKey: String, page: Int): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return prefs.getInt(READING_PAGE_PREFIX + challengeKey, 0) == page &&
            prefs.getBoolean(READING_BOTTOM_REACHED_PREFIX + challengeKey, false)
    }

    @Synchronized
    fun completeReadingForSummary(
        context: Context,
        challengeKey: String,
        page: Int
    ): Long {
        val elapsed = readingElapsedMs(context, challengeKey, page)
        if (!hasReachedReadingBottom(context, challengeKey, page)) return elapsed

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.getInt(READING_COMPLETION_RECORDED_PREFIX + challengeKey, 0) == page) {
            return elapsed
        }

        val atypicalFast = isAtypicallyFast(context, elapsed)
        val completed = prefs.getInt(READINGS_COMPLETED, 0).coerceAtLeast(0)
        val total = prefs.getLong(TOTAL_READING_MS, 0L).coerceAtLeast(0L)

        prefs.edit()
            .putInt(READINGS_COMPLETED, completed + 1)
            .putLong(TOTAL_READING_MS, total + elapsed)
            .putLong(LAST_READING_MS, elapsed)
            .putInt(READING_COMPLETION_RECORDED_PREFIX + challengeKey, page)
            .commit()

        recordHistory(
            context = context,
            packageName = challengeKey,
            page = page,
            elapsedMs = elapsed,
            method = "reading",
            atypicalFast = atypicalFast
        )

        if (atypicalFast) {
            GuardDiagnostics.log(
                context,
                "READING_ATYPICAL_FAST",
                challengeKey,
                "page=$page elapsedMs=$elapsed"
            )
        }

        return elapsed
    }

    @Synchronized
    fun unlockAfterReadingSummary(
        context: Context,
        challengeKey: String,
        page: Int
    ): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val recorded = prefs.getInt(READING_COMPLETION_RECORDED_PREFIX + challengeKey, 0)
        if (recorded != page || !hasReachedReadingBottom(context, challengeKey, page)) {
            return false
        }
        unlock(context, challengeKey)
        return true
    }

    @Synchronized
    fun completeReadingAndUnlock(
        context: Context,
        challengeKey: String,
        page: Int
    ): Long {
        val elapsed = completeReadingForSummary(context, challengeKey, page)
        unlockAfterReadingSummary(context, challengeKey, page)
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
        method: String,
        atypicalFast: Boolean = false
    ) {
        if (outOfScope(context, packageName)) return
        val entry = listOf(
            System.currentTimeMillis().toString(),
            packageName,
            page.toString(),
            elapsedMs.coerceAtLeast(0L).toString(),
            method,
            atypicalFast.toString()
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
        val parts = raw.split('|', limit = 6)
        if (parts.size < 5) return null
        return ReadingHistoryEntry(
            epochMs = parts[0].toLongOrNull() ?: return null,
            packageName = parts[1],
            page = parts[2].toIntOrNull() ?: return null,
            elapsedMs = parts[3].toLongOrNull() ?: return null,
            method = parts[4],
            atypicalFast = parts.getOrNull(5) == "true"
        )
    }

    private fun completedReadings(context: Context): List<ReadingHistoryEntry> =
        readingHistory(context, MAX_READING_HISTORY)
            .filter { it.method == "reading" }

    fun dailyReadingSummary(
        context: Context,
        date: LocalDate = LocalDate.now()
    ): DailyReadingSummary {
        val zone = ZoneId.systemDefault()
        val entries = completedReadings(context).filter { entry ->
            Instant.ofEpochMilli(entry.epochMs).atZone(zone).toLocalDate() == date
        }
        val total = entries.sumOf { it.elapsedMs.coerceAtLeast(0L) }
        return DailyReadingSummary(
            pages = entries.size,
            totalMs = total,
            averageMs = if (entries.isNotEmpty()) total / entries.size else 0L
        )
    }

    fun averageReadingMsForWindow(
        context: Context,
        days: Int,
        offsetDays: Int = 0
    ): Long {
        require(days > 0)
        require(offsetDays >= 0)
        val zone = ZoneId.systemDefault()
        val end = LocalDate.now().minusDays(offsetDays.toLong())
        val start = end.minusDays((days - 1).toLong())
        val entries = completedReadings(context).filter { entry ->
            val date = Instant.ofEpochMilli(entry.epochMs).atZone(zone).toLocalDate()
            !date.isBefore(start) && !date.isAfter(end)
        }
        return if (entries.isNotEmpty()) entries.sumOf { it.elapsedMs } / entries.size else 0L
    }

    fun atypicalReadingCount(context: Context, days: Int = 30): Int {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().minusDays((days - 1).coerceAtLeast(0).toLong())
        return completedReadings(context).count { entry ->
            entry.atypicalFast &&
                !Instant.ofEpochMilli(entry.epochMs).atZone(zone).toLocalDate().isBefore(start)
        }
    }

    private fun isAtypicallyFast(context: Context, elapsedMs: Long): Boolean {
        val sample = completedReadings(context)
            .asSequence()
            .map { it.elapsedMs }
            .filter { it > 0L }
            .take(30)
            .sorted()
            .toList()

        if (sample.size < 8) return false
        val median = sample[sample.size / 2]
        return elapsedMs > 0L && elapsedMs * 4L < median
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

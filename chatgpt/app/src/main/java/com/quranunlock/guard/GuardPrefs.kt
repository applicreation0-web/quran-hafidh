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
    const val USAGE_INTERVAL_MINUTES = UsageCyclePolicy.INTERVAL_MINUTES
    const val MIN_READING_MS = ReadingValidationPolicy.MIN_ACTIVE_READING_MS
    internal const val FILE = "guard_prefs"
    private const val LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX = "unlock_elapsed_until_"
    private const val LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX = "unlock_elapsed_started_"
    private const val UNLOCK_REMAINING_MS_PREFIX = "unlock_remaining_ms_"
    private const val UNLOCK_FOREGROUND_STARTED_PREFIX = "unlock_foreground_started_"
    private const val UNLOCK_FOREGROUND_BOOT_PREFIX = "unlock_foreground_boot_"
    private const val UNLOCK_FOREGROUND_CHECKPOINT_PREFIX = "unlock_foreground_checkpoint_"
    private const val UNLOCK_GRANTED_MS_PREFIX = "unlock_granted_ms_"
    private const val UNLOCK_REMINDER_MASK_PREFIX = "unlock_reminder_mask_"
    private const val GLOBAL_USAGE_KEY = "__all_protected_targets__"
    private const val GLOBAL_ACTIVE_TARGET = "unlock_global_active_target"
    private const val CHALLENGE_PREFIX = "challenge_page_"
    private const val SELECTED_JUZ = "selected_juz"
    private const val SELECTED_HIZB = "selected_hizb"
    private const val SELECTION_MODE = "selection_mode"
    internal const val PROTECTED_PACKAGES = "protected_packages"
    private const val PENDING_PROTECTED_REMOVALS = "pending_protected_removals"
    private const val PENDING_PROTECTED_REMOVAL_DAY = "pending_protected_removal_epoch_day"
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
        ProtectedApps.shouldNeverPersist(context, packageName)

    private fun budgetKey(packageName: String): String =
        if (ProtectedApps.isSelectableTarget(packageName)) GLOBAL_USAGE_KEY else packageName

    private fun readingKey(challengeKey: String): String =
        if (ProtectedApps.isSelectableTarget(challengeKey)) GLOBAL_USAGE_KEY else challengeKey

    private fun appendUsageIntervalGrant(
        editor: android.content.SharedPreferences.Editor,
        packageName: String
    ) {
        val key = budgetKey(packageName)
        val reading = readingKey(packageName)
        val grantedMs = UsageCyclePolicy.INTERVAL_MS
        editor
            .putLong(UNLOCK_REMAINING_MS_PREFIX + key, grantedMs)
            .putLong(UNLOCK_GRANTED_MS_PREFIX + key, grantedMs)
            .putInt(UNLOCK_REMINDER_MASK_PREFIX + key, 0)
            .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + key)
            .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + key)
            .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + key)
            .remove(LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX + key)
            .remove(LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + key)
            .remove(GLOBAL_ACTIVE_TARGET)
            .remove(CHALLENGE_PREFIX + reading)
            .remove(READING_PAGE_PREFIX + reading)
            .remove(READING_ACCUMULATED_PREFIX + reading)
            .remove(READING_STARTED_PREFIX + reading)
            .remove(READING_BOTTOM_REACHED_PREFIX + reading)
            .remove(READING_COMPLETION_RECORDED_PREFIX + reading)
    }


    fun isUnlocked(context: Context, packageName: String): Boolean {
        if (outOfScope(context, packageName)) return false
        val progress = SafeguardCyclePrefs.progress(context)
        return progress.morningCompleted &&
            progress.pendingLevel == null &&
            remainingUnlockMs(context, packageName) > 0L
    }

    private fun currentBootCount(context: Context): Int =
        runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        }.getOrDefault(-1)

    private fun readUnlockBudgetState(
        prefs: android.content.SharedPreferences,
        packageName: String
    ): UnlockBudgetState {
        val key = budgetKey(packageName)
        val started = prefs.getLong(UNLOCK_FOREGROUND_STARTED_PREFIX + key, -1L)
            .takeIf { it >= 0L }
        val boot = prefs.getInt(UNLOCK_FOREGROUND_BOOT_PREFIX + key, Int.MIN_VALUE)
            .takeIf { it != Int.MIN_VALUE }
        val checkpoint = prefs.getLong(
            UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + key,
            -1L
        ).takeIf { it >= 0L }

        return UnlockBudgetState(
            remainingMs = prefs.getLong(
                UNLOCK_REMAINING_MS_PREFIX + key,
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
        val key = budgetKey(packageName)
        val editor = prefs.edit()
            .putLong(
                UNLOCK_REMAINING_MS_PREFIX + key,
                state.remainingMs.coerceAtLeast(0L)
            )

        val started = state.foregroundStartedElapsedMs
        val boot = state.foregroundBootCount
        val checkpoint = state.checkpointElapsedMs

        if (started == null) {
            editor
                .remove(UNLOCK_FOREGROUND_STARTED_PREFIX + key)
                .remove(UNLOCK_FOREGROUND_BOOT_PREFIX + key)
                .remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + key)
        } else {
            editor.putLong(UNLOCK_FOREGROUND_STARTED_PREFIX + key, started)
            if (boot != null) {
                editor.putInt(UNLOCK_FOREGROUND_BOOT_PREFIX + key, boot)
            } else {
                editor.remove(UNLOCK_FOREGROUND_BOOT_PREFIX + key)
            }
            if (checkpoint != null) {
                editor.putLong(
                    UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + key,
                    checkpoint
                )
            } else {
                editor.remove(UNLOCK_FOREGROUND_CHECKPOINT_PREFIX + key)
            }
        }

        if (synchronous) editor.commit() else editor.apply()
    }

    @Synchronized
    fun remainingUnlockMs(context: Context, packageName: String): Long {
        if (outOfScope(context, packageName)) return 0L
        SafeguardCyclePrefs.ensureDailyState(context)
        if (!SafeguardCyclePrefs.isMorningCompleted(context)) return 0L
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val budget = budgetKey(packageName)
        val key = UNLOCK_REMAINING_MS_PREFIX + budget

        if (!prefs.contains(key)) {
            val legacyUntilKey = LEGACY_UNLOCK_UNTIL_ELAPSED_PREFIX + budget
            val legacyStartedKey = LEGACY_UNLOCK_STARTED_ELAPSED_PREFIX + budget
            if (prefs.contains(legacyUntilKey) || prefs.contains(legacyStartedKey)) {
                // Legacy elapsedRealtime values have no boot identity. Converting
                // them after reboot can create a phantom credit, so fail closed:
                // invalidate the old session and require one fresh Quran reading.
                prefs.edit()
                    .putLong(key, 0L)
                    .putLong(UNLOCK_GRANTED_MS_PREFIX + budget, 0L)
                    .remove(legacyStartedKey)
                    .remove(legacyUntilKey)
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
        prefs.edit().putString(GLOBAL_ACTIVE_TARGET, packageName).commit()
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
        if (prefs.getString(GLOBAL_ACTIVE_TARGET, null) == packageName) {
            prefs.edit().remove(GLOBAL_ACTIVE_TARGET).commit()
        }
        return paused.remainingMs
    }

    @Synchronized
    fun reconcileOrphanedUnlockForeground(context: Context): OrphanedUnlockRecovery? {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val packageName = prefs.getString(GLOBAL_ACTIVE_TARGET, null)
            ?.takeIf { ProtectedApps.isProtected(context, it) }
        val state = readUnlockBudgetState(prefs, GLOBAL_USAGE_KEY)
        if (state.foregroundStartedElapsedMs == null) {
            prefs.edit().remove(GLOBAL_ACTIVE_TARGET).commit()
            return null
        }

        val checkpoint = state.checkpointElapsedMs
        val safeRecovery = packageName != null &&
            UnlockBudgetIntegrity.isSafeSameBootRecovery(
                storedBootCount = state.foregroundBootCount,
                currentBootCount = currentBootCount(context),
                checkpointElapsedMs = checkpoint,
                nowElapsedMs = SystemClock.elapsedRealtime()
            ) &&
            checkpoint != null &&
            state.remainingMs > 0L

        val reconciled = UnlockBudgetIntegrity.reconcileOrphan(state)
        writeUnlockBudgetState(
            prefs,
            GLOBAL_USAGE_KEY,
            reconciled,
            synchronous = true
        )
        prefs.edit().remove(GLOBAL_ACTIVE_TARGET).commit()

        return if (safeRecovery) {
            OrphanedUnlockRecovery(
                packageName = packageName!!,
                checkpointElapsedMs = checkpoint!!
            )
        } else {
            null
        }
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
        val boundedCharge = UnlockBudgetIntegrity.boundedRecoveryChargeMs(
            checkpointElapsedMs = recovery.checkpointElapsedMs,
            nowElapsedMs = now
        )
        val updated = UnlockBudgetState(
            remainingMs = (state.remainingMs - boundedCharge).coerceAtLeast(0L)
        )
        writeUnlockBudgetState(prefs, packageName, updated, synchronous = true)
        return updated.remainingMs
    }

    @Synchronized
    fun expireUnlock(context: Context, packageName: String) {
        if (outOfScope(context, packageName)) return

        // Persist the pending level first. If the process stops between commits,
        // isUnlocked still fails closed instead of leaking the old credit.
        SafeguardCyclePrefs.onIntervalExpired(context)

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val expired = UnlockBudgetIntegrity.expire(
            readUnlockBudgetState(prefs, packageName)
        )
        writeUnlockBudgetState(prefs, packageName, expired, synchronous = true)
        prefs.edit().remove(GLOBAL_ACTIVE_TARGET).commit()
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
        val key = budgetKey(packageName)
        val granted = prefs.getLong(UNLOCK_GRANTED_MS_PREFIX + key, 0L)
        val thresholdMs = thresholdMinutes * 60_000L
        if (granted <= thresholdMs || remainingUnlockMs(context, packageName) > thresholdMs) {
            return false
        }

        val currentMask = prefs.getInt(UNLOCK_REMINDER_MASK_PREFIX + key, 0)
        if (currentMask and bit != 0) return false
        prefs.edit()
            .putInt(UNLOCK_REMINDER_MASK_PREFIX + key, currentMask or bit)
            .apply()
        return true
    }


    fun globalRemainingUnlockMs(context: Context): Long {
        val target = protectedPackages(context).firstOrNull() ?: return 0L
        return remainingUnlockMs(context, target)
    }

    fun currentIntervalTargetPresenceMs(context: Context): Long {
        val progress = SafeguardCyclePrefs.progress(context)
        if (!progress.morningCompleted) return 0L
        if (progress.pendingLevel != null) {
            return if (progress.pendingLevel == ChallengeLevel.MORNING) {
                0L
            } else {
                UsageCyclePolicy.INTERVAL_MS
            }
        }

        val remaining = globalRemainingUnlockMs(context)
        return (UsageCyclePolicy.INTERVAL_MS - remaining)
            .coerceIn(0L, UsageCyclePolicy.INTERVAL_MS)
    }

    fun currentCycleTargetPresenceMs(context: Context): Long {
        val progress = SafeguardCyclePrefs.progress(context)
        return UsageCyclePolicy.currentCyclePresenceMs(
            state = UsageCycleState(
                morningCompleted = progress.morningCompleted,
                completedIntervals = progress.completedIntervals,
                completedNinetyMinuteCycles = progress.completedNinetyMinuteCycles,
                pendingLevel = progress.pendingLevel
            ),
            currentIntervalPresenceMs = currentIntervalTargetPresenceMs(context)
        )
    }

    fun completedTargetUsageMs(context: Context): Long {
        val progress = SafeguardCyclePrefs.progress(context)
        return progress.completedNinetyMinuteCycles.coerceAtLeast(0) *
            UsageCyclePolicy.CUMULATIVE_MS +
            currentCycleTargetPresenceMs(context)
    }

    @Synchronized
    fun remainingJokers(context: Context): Int {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        normalizeJokerDay(context, prefs)
        return (DAILY_JOKERS - prefs.getInt(JOKERS_USED, 0)).coerceIn(0, DAILY_JOKERS)
    }

    @Synchronized
    fun consumeJokerAndUnlock(
        context: Context,
        packageName: String
    ): ChallengeLevel? {
        if (outOfScope(context, packageName)) return null
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        normalizeJokerDay(context, prefs)

        val used = prefs.getInt(JOKERS_USED, 0).coerceAtLeast(0)
        if (used >= DAILY_JOKERS) return null

        val level = SafeguardCyclePrefs.currentLevel(context)
        val page = challengePage(context, packageName)
        val historyEntry = listOf(
            System.currentTimeMillis().toString(),
            packageName,
            page.toString(),
            "0",
            "joker_" + level.name.lowercase(),
            "false"
        ).joinToString("|")
        val history = buildList {
            add(historyEntry)
            addAll(
                prefs.getString(READING_HISTORY, "")
                    .orEmpty()
                    .lineSequence()
                    .filter { it.isNotBlank() }
            )
        }.take(MAX_READING_HISTORY)

        SafeguardCyclePrefs.skipWithJoker(context) { editor ->
            editor.putInt(JOKERS_USED, used + 1)
            appendUsageIntervalGrant(editor, packageName)
            editor.putString(READING_HISTORY, history.joinToString("\n"))
        }
        return level
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

    @Synchronized
    fun protectedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val installedTargets = AppCatalog.launchableApps(context)
            .map(InstalledApp::packageName)
            .filter(ProtectedApps::isSelectableTarget)
            .toSet()
        val storedActive = prefs.getStringSet(
            PROTECTED_PACKAGES,
            null
        )?.toSet() ?: installedTargets
        val storedPending = prefs.getStringSet(
            PENDING_PROTECTED_REMOVALS,
            emptySet()
        ).orEmpty().toSet()
        val storedEffectiveDay = if (
            prefs.contains(PENDING_PROTECTED_REMOVAL_DAY)
        ) {
            prefs.getLong(PENDING_PROTECTED_REMOVAL_DAY, Long.MIN_VALUE)
        } else {
            null
        }

        val state = ProtectedSelectionPolicy.reconcile(
            active = storedActive,
            pendingRemoval = storedPending,
            removalEffectiveEpochDay = storedEffectiveDay,
            installedTargets = installedTargets,
            todayEpochDay = LocalDate.now().toEpochDay()
        )

        if (state.active != storedActive ||
            state.pendingRemoval != storedPending ||
            state.removalEffectiveEpochDay != storedEffectiveDay ||
            !prefs.contains(PROTECTED_PACKAGES)
        ) {
            persistProtectedSelection(prefs, state)
        }

        return state.active
    }

    @Synchronized
    fun pendingProtectedRemovals(context: Context): Set<String> {
        val active = protectedPackages(context)
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getStringSet(PENDING_PROTECTED_REMOVALS, emptySet())
            .orEmpty()
            .intersect(active)
    }

    /**
     * Additions and cancellations are immediate. Removing a target only creates
     * a reversible request, effective when the next local day begins.
     */
    @Synchronized
    fun saveProtectedPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val installedTargets = AppCatalog.launchableApps(context)
            .map(InstalledApp::packageName)
            .filter(ProtectedApps::isSelectableTarget)
            .toSet()
        val active = protectedPackages(context)
        val pending = prefs.getStringSet(
            PENDING_PROTECTED_REMOVALS,
            emptySet()
        ).orEmpty().intersect(active)
        val effectiveDay = if (
            prefs.contains(PENDING_PROTECTED_REMOVAL_DAY)
        ) {
            prefs.getLong(PENDING_PROTECTED_REMOVAL_DAY, Long.MIN_VALUE)
        } else {
            null
        }
        val today = LocalDate.now().toEpochDay()

        val state = ProtectedSelectionPolicy.update(
            state = ProtectedSelectionState(
                active = active,
                pendingRemoval = pending,
                removalEffectiveEpochDay = effectiveDay
            ),
            requested = packages,
            installedTargets = installedTargets,
            todayEpochDay = today
        )
        persistProtectedSelection(prefs, state)
    }

    private fun persistProtectedSelection(
        prefs: android.content.SharedPreferences,
        state: ProtectedSelectionState
    ) {
        val editor = prefs.edit()
            .putStringSet(PROTECTED_PACKAGES, state.active)
            .putStringSet(
                PENDING_PROTECTED_REMOVALS,
                state.pendingRemoval
            )

        state.removalEffectiveEpochDay?.let {
            editor.putLong(PENDING_PROTECTED_REMOVAL_DAY, it)
        } ?: editor.remove(PENDING_PROTECTED_REMOVAL_DAY)

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
        val page = SafeguardCyclePrefs.currentPage(context)
        recordChallengePage(context, page)
        ensureReadingSession(context, packageName, page)
        return page
    }

    fun challengeLevel(context: Context): ChallengeLevel =
        SafeguardCyclePrefs.currentLevel(context)

    fun challengePagePosition(context: Context): Pair<Int, Int> =
        SafeguardCyclePrefs.currentPagePosition(context)

    @Synchronized
    fun ensureReadingSession(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        val storedPage = prefs.getInt(READING_PAGE_PREFIX + key, 0)
        if (storedPage == page) return

        prefs.edit()
            .putInt(READING_PAGE_PREFIX + key, page)
            .putLong(READING_ACCUMULATED_PREFIX + key, 0L)
            .remove(READING_STARTED_PREFIX + key)
            .putBoolean(READING_BOTTOM_REACHED_PREFIX + key, false)
            .remove(READING_COMPLETION_RECORDED_PREFIX + key)
            .commit()
    }

    @Synchronized
    fun beginReadingForeground(context: Context, challengeKey: String, page: Int) {
        ensureReadingSession(context, challengeKey, page)
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        if (prefs.getLong(READING_STARTED_PREFIX + key, -1L) >= 0L) return

        prefs.edit()
            .putLong(READING_STARTED_PREFIX + key, SystemClock.elapsedRealtime())
            .commit()
    }

    @Synchronized
    fun endReadingForeground(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        if (prefs.getInt(READING_PAGE_PREFIX + key, 0) != page) return

        val started = prefs.getLong(READING_STARTED_PREFIX + key, -1L)
        if (started < 0L) return

        val now = SystemClock.elapsedRealtime()
        val delta = if (now >= started) now - started else 0L
        val accumulated = prefs.getLong(
            READING_ACCUMULATED_PREFIX + key,
            0L
        )

        prefs.edit()
            .putLong(
                READING_ACCUMULATED_PREFIX + key,
                accumulated + delta
            )
            .remove(READING_STARTED_PREFIX + key)
            .commit()
    }

    fun readingElapsedMs(context: Context, challengeKey: String, page: Int): Long {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        if (prefs.getInt(READING_PAGE_PREFIX + key, 0) != page) return 0L

        val accumulated = prefs.getLong(
            READING_ACCUMULATED_PREFIX + key,
            0L
        )
        val started = prefs.getLong(READING_STARTED_PREFIX + key, -1L)
        if (started < 0L) return accumulated

        val now = SystemClock.elapsedRealtime()
        val live = if (now >= started) now - started else 0L
        return accumulated + live
    }

    fun markReadingBottomReached(context: Context, challengeKey: String, page: Int) {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        if (prefs.getInt(READING_PAGE_PREFIX + key, 0) != page) return
        prefs.edit()
            .putBoolean(READING_BOTTOM_REACHED_PREFIX + key, true)
            .apply()
    }

    fun hasReachedReadingBottom(context: Context, challengeKey: String, page: Int): Boolean {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        return prefs.getInt(READING_PAGE_PREFIX + key, 0) == page &&
            prefs.getBoolean(READING_BOTTOM_REACHED_PREFIX + key, false)
    }

    @Synchronized
    fun completeReadingForSummary(
        context: Context,
        challengeKey: String,
        page: Int
    ): Long {
        val elapsed = readingElapsedMs(context, challengeKey, page)
        if (!ReadingValidationPolicy.canValidate(activeReadingMs = elapsed)) return elapsed

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val key = readingKey(challengeKey)
        if (prefs.getInt(READING_COMPLETION_RECORDED_PREFIX + key, 0) == page) {
            return elapsed
        }

        val atypicalFast = isAtypicallyFast(context, elapsed)
        val completed = prefs.getInt(READINGS_COMPLETED, 0).coerceAtLeast(0)
        val total = prefs.getLong(TOTAL_READING_MS, 0L).coerceAtLeast(0L)

        prefs.edit()
            .putInt(READINGS_COMPLETED, completed + 1)
            .putLong(TOTAL_READING_MS, total + elapsed)
            .putLong(LAST_READING_MS, elapsed)
            .putInt(READING_COMPLETION_RECORDED_PREFIX + key, page)
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
        val key = readingKey(challengeKey)
        val recorded = prefs.getInt(READING_COMPLETION_RECORDED_PREFIX + key, 0)
        if (recorded != page ||
            !ReadingValidationPolicy.canValidate(
                activeReadingMs = readingElapsedMs(context, challengeKey, page)
            )
        ) {
            return false
        }

        val completed = SafeguardCyclePrefs.completePage(context, page) { editor ->
            // The completed level and its next 15-minute credit share one
            // SharedPreferences transaction: neither can survive without the other.
            appendUsageIntervalGrant(editor, challengeKey)
        }
        if (!completed) {
            val nextPage = SafeguardCyclePrefs.currentPage(context)
            ensureReadingSession(context, challengeKey, nextPage)
        }
        return completed
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

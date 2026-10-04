package com.applicreation0.quransafeguard

import android.content.Context
import java.time.LocalDate

data class UsageProgress(
    val morningCompleted: Boolean,
    val completedIntervals: Int,
    val completedNinetyMinuteCycles: Int,
    val pendingLevel: ChallengeLevel?,
    val currentPageNumber: Int,
    val totalPages: Int
)

object SafeguardCyclePrefs {
    private const val USAGE_DAY = "usage_cycle_epoch_day"
    private const val MORNING_COMPLETED = "usage_morning_completed"
    private const val COMPLETED_INTERVALS = "usage_completed_intervals"
    private const val COMPLETED_NINETY_CYCLES = "usage_completed_ninety_cycles"
    private const val PENDING_LEVEL = "usage_pending_level"
    private const val PLAN_PAGES = "usage_plan_pages"
    private const val PLAN_INDEX = "usage_plan_index"
    private const val PLAN_NEXT_CURSOR = "usage_plan_next_cursor"
    private const val MORNING_CURSOR = "usage_morning_hizb_cursor"
    private const val HIZB_CURSOR = "usage_hizb_cursor"

    @Synchronized
    fun ensureDailyState(context: Context) {
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(USAGE_DAY, Long.MIN_VALUE)

        if (storedDay == today || storedDay > today) return

        prefs.edit()
            .putLong(USAGE_DAY, today)
            .putBoolean(MORNING_COMPLETED, false)
            .putInt(COMPLETED_INTERVALS, 0)
            .putInt(COMPLETED_NINETY_CYCLES, 0)
            .remove(PENDING_LEVEL)
            .remove(PLAN_PAGES)
            .remove(PLAN_INDEX)
            .remove(PLAN_NEXT_CURSOR)
            .commit()
    }

    @Synchronized
    fun progress(context: Context): UsageProgress {
        ensureDailyState(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val pages = readPlan(prefs)
        val index = prefs.getInt(PLAN_INDEX, 0).coerceAtLeast(0)
        val state = readState(prefs)
        return UsageProgress(
            morningCompleted = state.morningCompleted,
            completedIntervals = state.completedIntervals,
            completedNinetyMinuteCycles = state.completedNinetyMinuteCycles,
            pendingLevel = UsageCyclePolicy.requiredLevel(state),
            currentPageNumber = if (pages.isEmpty()) 0 else (index + 1).coerceAtMost(pages.size),
            totalPages = pages.size
        )
    }

    @Synchronized
    fun isMorningCompleted(context: Context): Boolean {
        ensureDailyState(context)
        return readState(
            context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        ).morningCompleted
    }

    @Synchronized
    fun onIntervalExpired(context: Context) {
        ensureDailyState(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val current = readState(prefs)
        val updated = UsageCyclePolicy.onIntervalExpired(current)
        writeState(
            prefs = prefs,
            state = updated,
            clearPlan = updated != current
        )
    }

    @Synchronized
    fun currentLevel(context: Context): ChallengeLevel {
        ensurePlan(context)
        val state = readState(
            context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        )
        return UsageCyclePolicy.requiredLevel(state)
            ?: error("Safeguard challenge plan has no pending level.")
    }

    @Synchronized
    fun currentPage(context: Context): Int {
        ensurePlan(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val pages = readPlan(prefs)
        val index = prefs.getInt(PLAN_INDEX, 0).coerceIn(0, pages.lastIndex)
        return pages[index]
    }

    @Synchronized
    fun currentPagePosition(context: Context): Pair<Int, Int> {
        ensurePlan(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val pages = readPlan(prefs)
        val index = prefs.getInt(PLAN_INDEX, 0).coerceIn(0, pages.lastIndex)
        return (index + 1) to pages.size
    }

    @Synchronized
    fun currentPlan(context: Context): Pair<List<Int>, Int> {
        ensurePlan(context)
        val prefs = context.getSharedPreferences(
            GuardPrefs.FILE,
            Context.MODE_PRIVATE
        )
        val pages = readPlan(prefs)
        val index = prefs.getInt(PLAN_INDEX, 0)
            .coerceIn(0, pages.lastIndex)
        return pages.toList() to index
    }

    @Synchronized
    fun completePage(
        context: Context,
        page: Int,
        onChallengeCompleted: (android.content.SharedPreferences.Editor) -> Unit = {}
    ): Boolean {
        ensurePlan(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val pages = readPlan(prefs)
        val index = prefs.getInt(PLAN_INDEX, 0).coerceIn(0, pages.lastIndex)
        // Midnight or a pool change can replace the persisted plan while a reader
        // screen is still open. Fail safely and let the UI reopen the new page.
        if (pages[index] != page) return false

        if (index < pages.lastIndex) {
            check(prefs.edit().putInt(PLAN_INDEX, index + 1).commit())
            return false
        }

        completePendingChallenge(prefs, onChallengeCompleted)
        return true
    }

    @Synchronized
    fun skipWithJoker(
        context: Context,
        onChallengeCompleted: (android.content.SharedPreferences.Editor) -> Unit
    ): ChallengeLevel {
        ensurePlan(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)
        val level = currentLevel(context)
        val updated = UsageCyclePolicy.skipWithJoker(readState(prefs))
        finishChallenge(prefs, level, updated, onChallengeCompleted)
        return level
    }

    private fun ensurePlan(context: Context) {
        ensureDailyState(context)
        val prefs = context.getSharedPreferences(GuardPrefs.FILE, Context.MODE_PRIVATE)

        val mode = GuardPrefs.selectionMode(context)
        val selectedUnits = when (mode) {
            QuranSelectionMode.JUZ -> GuardPrefs.selectedJuz(context)
                .filter { it in 1..30 }
                .toSet()
                .ifEmpty { (1..30).toSet() }
            QuranSelectionMode.HIZB -> GuardPrefs.selectedHizb(context)
                .filter { it in 1..60 }
                .toSet()
                .ifEmpty { (1..60).toSet() }
        }
        val canonicalPool = QuranPageSelector.availablePages(mode, selectedUnits).toSet()

        val existing = readPlan(prefs)
        val existingIndex = prefs.getInt(PLAN_INDEX, 0)
        if (
            existing.isNotEmpty() &&
            existingIndex in existing.indices &&
            existing.all { it in canonicalPool }
        ) {
            return
        }

        var state = readState(prefs)
        var level = UsageCyclePolicy.requiredLevel(state)
        if (level == null) {
            // Recover an interrupted expiration without weakening the sixth
            // interval: the policy reconstructs MICRO versus HIZB from state.
            state = UsageCyclePolicy.onIntervalExpired(state)
            level = UsageCyclePolicy.requiredLevel(state)
                ?: error("Unable to reconstruct the pending Safeguard level.")
        }

        val plan = when (level) {
            ChallengeLevel.MORNING -> QuranPageSelector.sequentialCanonicalQuotaPages(
                mode = mode,
                selectedUnits = selectedUnits,
                cursor = prefs.getInt(MORNING_CURSOR, 0),
                pageCount = UsageCyclePolicy.MORNING_PAGE_COUNT
            )
            ChallengeLevel.HIZB -> QuranPageSelector.sequentialCanonicalQuotaPages(
                mode = mode,
                selectedUnits = selectedUnits,
                cursor = prefs.getInt(HIZB_CURSOR, 0),
                pageCount = UsageCyclePolicy.HIZB_PAGE_COUNT
            )
            ChallengeLevel.MICRO -> HizbPagePlan(
                pages = listOf(
                    QuranPageSelector.randomPage(
                        mode = mode,
                        selectedUnits = selectedUnits,
                        recentPagesNewestFirst = GuardPrefs.recentChallengePages(context)
                    )
                ),
                hizbNumbers = emptyList(),
                nextCursor = 0
            )
        }

        val requestedPageCount = when (level) {
            ChallengeLevel.MORNING -> UsageCyclePolicy.MORNING_PAGE_COUNT
            ChallengeLevel.MICRO -> 1
            ChallengeLevel.HIZB -> UsageCyclePolicy.HIZB_PAGE_COUNT
        }
        require(plan.pages.isNotEmpty()) { "Canonical Quran reading plan is empty." }
        require(plan.pages.size <= requestedPageCount) {
            "Canonical Quran reading plan exceeds the Safeguard quota."
        }
        if (level == ChallengeLevel.MICRO) {
            require(plan.pages.size == 1)
        }
        require(plan.pages.distinct().size == plan.pages.size) {
            "Canonical Quran reading plan must not repeat a page."
        }
        require(plan.pages.all { it in canonicalPool }) {
            "Canonical Quran reading plan escaped the selected Juz/Hizb pool."
        }

        writeState(prefs, state.copy(pendingLevel = level), clearPlan = true)
        check(
            prefs.edit()
                .putString(PLAN_PAGES, plan.pages.joinToString(","))
                .putInt(PLAN_INDEX, 0)
                .putInt(PLAN_NEXT_CURSOR, plan.nextCursor)
                .commit()
        )
    }

    private fun completePendingChallenge(
        prefs: android.content.SharedPreferences,
        onChallengeCompleted: (android.content.SharedPreferences.Editor) -> Unit
    ) {
        val state = readState(prefs)
        val level = UsageCyclePolicy.requiredLevel(state)
            ?: error("No pending challenge to complete.")
        val updated = UsageCyclePolicy.completeChallenge(state, level)
        finishChallenge(prefs, level, updated, onChallengeCompleted)
    }

    private fun finishChallenge(
        prefs: android.content.SharedPreferences,
        level: ChallengeLevel,
        updated: UsageCycleState,
        onChallengeCompleted: (android.content.SharedPreferences.Editor) -> Unit
    ) {
        val nextCursor = prefs.getInt(PLAN_NEXT_CURSOR, 0)
        val editor = prefs.edit()
            .putBoolean(MORNING_COMPLETED, updated.morningCompleted)
            .putInt(COMPLETED_INTERVALS, updated.completedIntervals)
            .putInt(COMPLETED_NINETY_CYCLES, updated.completedNinetyMinuteCycles)
            .remove(PENDING_LEVEL)
            .remove(PLAN_PAGES)
            .remove(PLAN_INDEX)
            .remove(PLAN_NEXT_CURSOR)

        when (level) {
            ChallengeLevel.MORNING -> editor.putInt(MORNING_CURSOR, nextCursor)
            ChallengeLevel.HIZB -> editor.putInt(HIZB_CURSOR, nextCursor)
            ChallengeLevel.MICRO -> Unit
        }
        onChallengeCompleted(editor)
        check(editor.commit())
    }

    private fun readState(
        prefs: android.content.SharedPreferences
    ): UsageCycleState {
        val pending = prefs.getString(PENDING_LEVEL, null)?.let { raw ->
            runCatching { ChallengeLevel.valueOf(raw) }.getOrNull()
        }
        return UsageCycleState(
            morningCompleted = prefs.getBoolean(MORNING_COMPLETED, false),
            completedIntervals = prefs.getInt(COMPLETED_INTERVALS, 0)
                .coerceIn(0, UsageCyclePolicy.INTERVALS_PER_HIZB),
            completedNinetyMinuteCycles = prefs.getInt(COMPLETED_NINETY_CYCLES, 0)
                .coerceAtLeast(0),
            pendingLevel = pending
        )
    }

    private fun writeState(
        prefs: android.content.SharedPreferences,
        state: UsageCycleState,
        clearPlan: Boolean
    ) {
        val editor = prefs.edit()
            .putBoolean(MORNING_COMPLETED, state.morningCompleted)
            .putInt(COMPLETED_INTERVALS, state.completedIntervals)
            .putInt(COMPLETED_NINETY_CYCLES, state.completedNinetyMinuteCycles)

        if (state.pendingLevel == null) editor.remove(PENDING_LEVEL)
        else editor.putString(PENDING_LEVEL, state.pendingLevel.name)

        if (clearPlan) {
            editor.remove(PLAN_PAGES)
                .remove(PLAN_INDEX)
                .remove(PLAN_NEXT_CURSOR)
        }
        check(editor.commit())
    }

    private fun readPlan(prefs: android.content.SharedPreferences): List<Int> =
        prefs.getString(PLAN_PAGES, "")
            .orEmpty()
            .split(',')
            .mapNotNull { raw -> raw.toIntOrNull() }
            .filter { it in 1..604 }
}

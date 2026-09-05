package com.applicreation0.quransafeguard

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

/**
 * Preserved Taddabur core for regression tests and future reactivation.
 * The 0.10.5 release runtime is intentionally disabled in TaddaburEdition.
 */
internal object TaddaburPolicy {
    const val FIRST_HIZB = 1
    const val LAST_HIZB = 60
    const val START_HOUR = 7
    const val DEADLINE_HOUR = 20
    const val MIN_PAGE_MS = 90_000L

    fun nextHizb(current: Int): Int =
        if (current >= LAST_HIZB) FIRST_HIZB else current + 1

    fun mayAccumulate(time: LocalTime): Boolean = time.hour >= START_HOUR

    fun deadlinePassed(time: LocalTime): Boolean = time.hour >= DEADLINE_HOUR
}

internal data class TaddaburProgress(
    val epochDay: Long,
    val hizb: Int,
    val startPage: Int,
    val endPage: Int,
    val completedPages: Set<Int>,
    val bookmarkPage: Int,
    val completedAtEpochMs: Long?,
    val penaltyActive: Boolean
) {
    val totalPages: Int get() = endPage - startPage + 1
    val completedCount: Int get() = completedPages.count { it in startPage..endPage }
    val complete: Boolean get() = completedCount >= totalPages
    val fraction: Float get() =
        if (totalPages <= 0) 0f else completedCount.toFloat() / totalPages.toFloat()
}

internal data class TaddaburDailyHistory(
    val epochDay: Long,
    val hizb: Int,
    val completedPages: Int,
    val totalPages: Int,
    val complete: Boolean,
    val completedAtEpochMs: Long?,
    val totalReadingMs: Long
)

internal object TaddaburPrefs {
    private const val FILE = "taddabur_prefs"
    private const val ACTIVE_DAY = "active_day"
    private const val CURRENT_HIZB = "current_hizb"
    private const val COMPLETED_PAGES = "completed_pages"
    private const val BOOKMARK_PAGE = "bookmark_page"
    private const val COMPLETED_DAY = "completed_day"
    private const val COMPLETED_AT = "completed_at"
    private const val PENALTY_DAY = "penalty_day"
    private const val HISTORY_PREFIX = "history_"
    private const val ELAPSED_PREFIX = "elapsed_"

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun division(hizb: Int): QuranDivision =
        QuranStructureMetadata.division(QuranSelectionMode.HIZB, hizb)

    @Synchronized
    fun progress(context: Context): TaddaburProgress {
        syncDay(context)
        val prefs = prefs(context)
        val day = LocalDate.now().toEpochDay()
        val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val division = division(hizb)
        val completed = prefs.getStringSet(COMPLETED_PAGES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in division.pageRange }
            .toSet()
        val bookmark = prefs.getInt(BOOKMARK_PAGE, division.startPage)
            .coerceIn(division.startPage, division.endPage)
        val completedDay = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE)
        val completedAt = if (completedDay == day) {
            prefs.getLong(COMPLETED_AT, 0L).takeIf { it > 0L }
        } else {
            null
        }
        val penalty = prefs.getLong(PENALTY_DAY, Long.MIN_VALUE) == day
        return TaddaburProgress(
            epochDay = day,
            hizb = hizb,
            startPage = division.startPage,
            endPage = division.endPage,
            completedPages = completed,
            bookmarkPage = bookmark,
            completedAtEpochMs = completedAt,
            penaltyActive = penalty
        )
    }

    @Synchronized
    fun setBookmark(context: Context, page: Int) {
        val state = progress(context)
        if (page !in state.startPage..state.endPage) return
        prefs(context).edit().putInt(BOOKMARK_PAGE, page).commit()
        writeSnapshot(context)
    }

    @Synchronized
    fun elapsedMs(context: Context, page: Int): Long {
        val state = progress(context)
        if (page !in state.startPage..state.endPage) return 0L
        return prefs(context).getLong(elapsedKey(state.hizb, page), 0L).coerceAtLeast(0L)
    }

    @Synchronized
    fun recordActiveMs(context: Context, page: Int, deltaMs: Long): TaddaburProgress {
        var state = progress(context)
        if (page !in state.startPage..state.endPage || deltaMs <= 0L) return state
        val prefs = prefs(context)
        val key = elapsedKey(state.hizb, page)
        val next = (prefs.getLong(key, 0L).coerceAtLeast(0L) + deltaMs)
            .coerceAtMost(24L * 60L * 60L * 1000L)
        val editor = prefs.edit().putLong(key, next).putInt(BOOKMARK_PAGE, page)

        if (next >= TaddaburPolicy.MIN_PAGE_MS && page !in state.completedPages) {
            val pages = state.completedPages.map(Int::toString).toMutableSet()
            pages += page.toString()
            editor.putStringSet(COMPLETED_PAGES, pages)
            val total = state.totalPages
            if (pages.mapNotNull { it.toIntOrNull() }
                    .count { it in state.startPage..state.endPage } >= total
            ) {
                val today = LocalDate.now().toEpochDay()
                if (TaddaburPolicy.deadlinePassed(LocalTime.now())) {
                    editor.putLong(PENALTY_DAY, today)
                }
                editor.putLong(COMPLETED_DAY, today)
                    .putLong(COMPLETED_AT, System.currentTimeMillis())
            }
        }
        check(editor.commit()) { "Unable to persist Taddabur reading progress" }
        state = progress(context)
        writeSnapshot(context)
        return state
    }

    @Synchronized
    fun shouldBlockNow(context: Context): Boolean {
        val state = progress(context)
        val now = LocalTime.now()
        if (!TaddaburPolicy.deadlinePassed(now)) return false
        if (state.penaltyActive) return true
        if (state.complete && state.completedAtEpochMs != null) return false

        val today = LocalDate.now().toEpochDay()
        check(prefs(context).edit().putLong(PENALTY_DAY, today).commit()) {
            "Unable to arm Taddabur deadline"
        }
        writeSnapshot(context)
        return true
    }

    @Synchronized
    fun history(context: Context, limit: Int = 7): List<TaddaburDailyHistory> {
        syncDay(context)
        writeSnapshot(context)
        return prefs(context).all.entries
            .asSequence()
            .filter { it.key.startsWith(HISTORY_PREFIX) && it.value is String }
            .mapNotNull { entry ->
                val day = entry.key.removePrefix(HISTORY_PREFIX).toLongOrNull()
                    ?: return@mapNotNull null
                parseHistory(day, entry.value as String)
            }
            .sortedByDescending { it.epochDay }
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    @Synchronized
    private fun syncDay(context: Context) {
        val prefs = prefs(context)
        val today = LocalDate.now().toEpochDay()
        val storedDay = prefs.getLong(ACTIVE_DAY, Long.MIN_VALUE)
        if (storedDay == Long.MIN_VALUE) {
            val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
                .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
            val division = division(hizb)
            prefs.edit()
                .putLong(ACTIVE_DAY, today)
                .putInt(CURRENT_HIZB, hizb)
                .putInt(BOOKMARK_PAGE, division.startPage)
                .commit()
            return
        }
        if (storedDay == today) return

        writeSnapshotForDay(context, storedDay)
        val oldHizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val completedYesterday = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE) == storedDay
        val nextHizb = if (completedYesterday) TaddaburPolicy.nextHizb(oldHizb) else oldHizb
        val nextDivision = division(nextHizb)
        val editor = prefs.edit()
            .putLong(ACTIVE_DAY, today)
            .putInt(CURRENT_HIZB, nextHizb)
            .putStringSet(COMPLETED_PAGES, emptySet())
            .putInt(BOOKMARK_PAGE, nextDivision.startPage)
            .remove(COMPLETED_DAY)
            .remove(COMPLETED_AT)
            .remove(PENALTY_DAY)
        prefs.all.keys.filter { it.startsWith(ELAPSED_PREFIX) }.forEach(editor::remove)
        check(editor.commit()) { "Unable to roll Taddabur to the next day" }
    }

    private fun writeSnapshot(context: Context) {
        val day = prefs(context).getLong(ACTIVE_DAY, LocalDate.now().toEpochDay())
        writeSnapshotForDay(context, day)
    }

    private fun writeSnapshotForDay(context: Context, day: Long) {
        val prefs = prefs(context)
        val hizb = prefs.getInt(CURRENT_HIZB, TaddaburPolicy.FIRST_HIZB)
            .coerceIn(TaddaburPolicy.FIRST_HIZB, TaddaburPolicy.LAST_HIZB)
        val division = division(hizb)
        val completed = prefs.getStringSet(COMPLETED_PAGES, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .count { it in division.pageRange }
        val total = division.pageRange.count()
        val complete = prefs.getLong(COMPLETED_DAY, Long.MIN_VALUE) == day && completed >= total
        val completedAt = if (complete) prefs.getLong(COMPLETED_AT, 0L) else 0L
        val totalMs = division.pageRange.sumOf { page ->
            prefs.getLong(elapsedKey(hizb, page), 0L).coerceAtLeast(0L)
        }
        val encoded = listOf(
            hizb.toString(),
            completed.toString(),
            total.toString(),
            if (complete) "1" else "0",
            completedAt.toString(),
            totalMs.toString()
        ).joinToString("|")
        prefs.edit().putString(HISTORY_PREFIX + day, encoded).commit()
    }

    private fun parseHistory(day: Long, raw: String): TaddaburDailyHistory? {
        val p = raw.split('|')
        if (p.size != 6) return null
        val hizb = p[0].toIntOrNull() ?: return null
        val completed = p[1].toIntOrNull() ?: return null
        val total = p[2].toIntOrNull() ?: return null
        val complete = p[3] == "1"
        val completedAt = p[4].toLongOrNull()?.takeIf { it > 0L }
        val totalMs = p[5].toLongOrNull() ?: return null
        return TaddaburDailyHistory(day, hizb, completed, total, complete, completedAt, totalMs)
    }

    private fun elapsedKey(hizb: Int, page: Int): String =
        "${ELAPSED_PREFIX}h${hizb}_p$page"
}

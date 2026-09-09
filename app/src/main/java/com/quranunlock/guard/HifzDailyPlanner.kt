package com.applicreation0.quransafeguard

import java.time.LocalDate

/** Exact planned Quran passage plus its conservative real-Mushaf page-equivalent volume. */
data class HifzPlannedPassage(
    val cursor: HifzCursor,
    val pageEquivalent: Double
) {
    init {
        require(pageEquivalent > 0.0 && pageEquivalent.isFinite())
    }
}

object HifzPassagePlanningPolicy {
    /**
     * A physical line touched by the target counts as a full line of work. This is
     * deliberately conservative when an off-target verse shares that line.
     */
    fun pageEquivalent(index: HifzGeometryIndex, target: HifzVerseRange): Double {
        val lines = HifzGeometryPolicy.targetLines(index, target)
        require(lines.isNotEmpty()) { "Target has no Mushaf geometry lines." }
        return lines
            .groupBy { it.ref.page }
            .entries
            .sumOf { (page, targetLines) ->
                val pageLineCount = requireNotNull(index.linesByPage[page]).size
                require(pageLineCount > 0)
                targetLines.map { it.ref.geometryId }.distinct().size.toDouble() / pageLineCount
            }
    }

    fun cursor(index: HifzGeometryIndex, target: HifzVerseRange): HifzCursor {
        val startPage = pagesForVerse(index, target.start).minOrNull()
            ?: error("Missing Mushaf page for ${target.start.label}")
        val endPage = pagesForVerse(index, target.end).maxOrNull()
            ?: error("Missing Mushaf page for ${target.end.label}")
        return HifzCursor(target.start, target.end, startPage, endPage)
    }

    /**
     * Selects whole verses only. If even the first verse exceeds the estimated capacity,
     * that single verse is still the task: its internal line segments may span sessions,
     * but no fraction is ever credited as an acquired Quran verse.
     */
    fun takeContiguousWithinCapacity(
        index: HifzGeometryIndex,
        allowed: HifzVerseRange,
        start: QuranVerseRef,
        pageEquivalentCapacity: Double
    ): HifzPlannedPassage? {
        require(pageEquivalentCapacity > 0.0 && pageEquivalentCapacity.isFinite())
        if (!allowed.contains(start)) return null

        var current = start
        var bestEnd: QuranVerseRef? = null
        var bestVolume = 0.0
        while (allowed.contains(current)) {
            val candidate = HifzVerseRange(start, current)
            val volume = pageEquivalent(index, candidate)
            if (bestEnd == null || volume <= pageEquivalentCapacity + 1e-9) {
                bestEnd = current
                bestVolume = volume
            } else {
                break
            }
            current = HifzItqanTraversalPolicy.nextCanonicalVerse(current) ?: break
        }

        val end = bestEnd ?: return null
        val range = HifzVerseRange(start, end)
        return HifzPlannedPassage(cursor(index, range), bestVolume)
    }

    /** One page/partial-page revision portion, preserving mid-page Quran boundaries. */
    fun murajaahPortions(
        index: HifzGeometryIndex,
        source: HifzCursor
    ): List<HifzPlannedPassage> {
        val sourceRange = HifzVerseRange(source.start, source.end)
        return HifzGeometryPolicy.targetLines(index, sourceRange)
            .groupBy { it.ref.page }
            .toSortedMap()
            .map { (page, lines) ->
                val verses = lines.flatMap { it.targetVerses }.distinct()
                    .sortedWith(::compareQuranVerseRefs)
                require(verses.isNotEmpty())
                val range = HifzVerseRange(verses.first(), verses.last())
                HifzPlannedPassage(
                    cursor = HifzCursor(range.start, range.end, page, page),
                    pageEquivalent = pageEquivalent(index, range)
                )
            }
    }

    private fun pagesForVerse(index: HifzGeometryIndex, verse: QuranVerseRef): Set<Int> =
        index.linesByPage.entries
            .asSequence()
            .filter { (_, lines) -> lines.any { verse in it.verses } }
            .map { it.key }
            .toSet()
}

/**
 * Idempotent daily planner. A date is recorded even when backlog or missing measurements
 * prevent new work, so reopening the app later the same day can never create a second
 * automatic quota after an overdue task was completed.
 */
object HifzDailyPlanner {
    fun planDate(
        state: HifzState,
        today: LocalDate,
        geometry: HifzGeometryIndex
    ): HifzState {
        if (today in state.planningDates) return state

        val resumed = HifzResumePolicy.resume(state, today).state
        val considered = resumed.copy(planningDates = resumed.planningDates + today)
        val bounds = considered.journeyConfig.bounds ?: return considered

        // Existing backlog/today work has priority. Never create an automatic double load.
        if (HifzSchedulePolicy.nextTask(today, considered.tasks) != null) return considered

        val track = HifzSchedulePolicy.defaultTrackFor(today.dayOfWeek)
        if (considered.tasks.any { it.track == track && it.status != HifzTaskStatus.COMPLETED }) {
            return considered
        }
        val availableMinutes = considered.journeyConfig.availableMinutes.forTrack(track)
            ?: return considered
        val capacity = HifzTimeQuotaPolicy.pageEquivalentCapacity(
            track,
            availableMinutes.toDouble(),
            considered.journeyConfig.pace
        ) ?: return considered
        if (capacity <= 0.0) return considered

        val passage = when (track) {
            HifzTrack.SABQI -> planSabqi(considered, bounds, geometry, capacity)
            HifzTrack.ITQAN -> planItqan(considered, bounds, geometry, capacity)
            HifzTrack.MURAJAAH -> planMurajaah(considered, geometry, capacity)
        } ?: return considered

        val task = HifzTask(
            id = stableTaskId(track, today, passage.cursor),
            track = track,
            originalScheduledDate = today,
            scheduledDate = today,
            cursor = passage.cursor,
            quota = availableMinutes,
            status = HifzTaskStatus.PLANNED
        )
        return considered.copy(tasks = (considered.tasks + task).sortedBy { it.id })
    }

    private fun planSabqi(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex,
        capacity: Double
    ): HifzPlannedPassage? {
        val last = state.tasks.filter { it.track == HifzTrack.SABQI }
            .maxWithOrNull(compareBy { it.cursor.end.surah }.thenBy { it.cursor.end.ayah })
        val start = if (last == null) {
            bounds.sabqi.start
        } else {
            HifzItqanTraversalPolicy.nextCanonicalVerse(last.cursor.end) ?: return null
        }
        if (!bounds.sabqi.contains(start)) return null
        return HifzPassagePlanningPolicy.takeContiguousWithinCapacity(
            geometry, bounds.sabqi, start, capacity
        )
    }

    private fun planItqan(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex,
        capacity: Double
    ): HifzPlannedPassage? {
        val last = state.tasks.filter { it.track == HifzTrack.ITQAN }
            .maxWithOrNull(compareBy { it.cursor.end.surah }.thenBy { it.cursor.end.ayah })
        val start = if (last == null) {
            bounds.itqan.first().start
        } else {
            HifzItqanTraversalPolicy.nextAfter(bounds.itqan, last.cursor.end) ?: return null
        }
        val interval = bounds.itqan.firstOrNull { it.contains(start) } ?: return null
        return HifzPassagePlanningPolicy.takeContiguousWithinCapacity(
            geometry, interval, start, capacity
        )
    }

    private fun planMurajaah(
        state: HifzState,
        geometry: HifzGeometryIndex,
        capacity: Double
    ): HifzPlannedPassage? {
        val completedSources = state.tasks.filter {
            it.status == HifzTaskStatus.COMPLETED && it.track != HifzTrack.MURAJAAH
        }
        if (completedSources.isEmpty()) return null

        data class Ranked(
            val passage: HifzPlannedPassage,
            val errors: Int,
            val reveals: Int,
            val lastReviewed: LocalDate?,
            val sourceDate: LocalDate,
            val stable: String
        )

        val completedReviews = state.tasks.filter {
            it.status == HifzTaskStatus.COMPLETED && it.track == HifzTrack.MURAJAAH
        }
        val candidates = completedSources.flatMap { source ->
            val sourceProgress = state.progressByTask[source.id]
            HifzPassagePlanningPolicy.murajaahPortions(geometry, source.cursor).map { portion ->
                val lastReviewed = completedReviews
                    .filter { it.cursor == portion.cursor }
                    .maxOfOrNull { it.scheduledDate }
                Ranked(
                    passage = portion,
                    errors = sourceProgress?.totalIncorrectAttempts ?: 0,
                    reveals = sourceProgress?.totalRevealCount ?: 0,
                    lastReviewed = lastReviewed,
                    sourceDate = source.scheduledDate,
                    stable = "${source.id}:${portion.cursor.label}"
                )
            }
        }

        // No arbitrary weighted score and no fixed Sabqi/Itqan ratio: explicit lexicographic
        // priorities use the observed fragility signals, then least-recent review and age.
        return candidates.sortedWith(
            compareByDescending<Ranked> { it.errors }
                .thenByDescending { it.reveals }
                .thenBy { it.lastReviewed ?: LocalDate.MIN }
                .thenBy { it.sourceDate }
                .thenBy { it.stable }
        ).firstOrNull { it.passage.pageEquivalent <= capacity + 1e-9 }
            ?.passage
            ?: candidates.firstOrNull()?.passage
    }

    private fun stableTaskId(track: HifzTrack, date: LocalDate, cursor: HifzCursor): String =
        buildString {
            append(track.name.lowercase()).append('-').append(date)
            append('-').append(cursor.start.surah).append('_').append(cursor.start.ayah)
            append('-').append(cursor.end.surah).append('_').append(cursor.end.ayah)
        }
}

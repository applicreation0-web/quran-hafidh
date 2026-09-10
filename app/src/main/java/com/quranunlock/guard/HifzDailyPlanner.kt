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
    const val SABQI_REAL_LINE_TARGET = 5

    /** A physical line touched by the target counts as a full line of work. */
    fun pageEquivalent(index: HifzGeometryIndex, target: HifzVerseRange): Double {
        val lines = HifzGeometryPolicy.targetLines(index, target)
        require(lines.isNotEmpty()) { "Target has no Mushaf geometry lines." }
        return pageEquivalentFromLines(index, lines)
    }

    /** Task-page-aware volume, used by exact physical-page Itqan and pace tracking. */
    fun pageEquivalent(index: HifzGeometryIndex, cursor: HifzCursor): Double {
        val lines = HifzGeometryPolicy.targetLines(index, cursor)
        require(lines.isNotEmpty()) { "Task has no Mushaf geometry lines." }
        return pageEquivalentFromLines(index, lines)
    }

    private fun pageEquivalentFromLines(
        index: HifzGeometryIndex,
        lines: List<HifzTargetLine>
    ): Double = lines
        .groupBy { it.ref.page }
        .entries
        .sumOf { (page, targetLines) ->
            val pageLineCount = requireNotNull(index.linesByPage[page]).size
            require(pageLineCount > 0)
            targetLines.map { it.ref.geometryId }.distinct().size.toDouble() / pageLineCount
        }

    fun cursor(index: HifzGeometryIndex, target: HifzVerseRange): HifzCursor {
        val startPage = pagesForVerse(index, target.start).minOrNull()
            ?: error("Missing Mushaf page for ${target.start.label}")
        val endPage = pagesForVerse(index, target.end).maxOrNull()
            ?: error("Missing Mushaf page for ${target.end.label}")
        return HifzCursor(target.start, target.end, startPage, endPage)
    }

    /**
     * Sabqi is an exact real-Mushaf-line method, not a time-derived quantity. Whole
     * verses remain the canonical persistence unit. We stop before the first verse that
     * would make the passage exceed five real lines; if the first verse itself occupies
     * more than five lines, that indivisible verse is the only allowed oversize case.
     */
    fun takeSabqiFiveLines(
        index: HifzGeometryIndex,
        allowed: HifzVerseRange,
        start: QuranVerseRef
    ): HifzPlannedPassage? {
        if (!allowed.contains(start)) return null
        var current = start
        var bestEnd: QuranVerseRef? = null
        var bestLineCount = 0
        while (allowed.contains(current)) {
            val candidate = HifzVerseRange(start, current)
            val lineCount = HifzGeometryPolicy.targetLines(index, candidate)
                .map { it.ref.geometryId }
                .distinct()
                .size
            require(lineCount > 0) { "Sabqi candidate has no Mushaf line geometry." }
            if (bestEnd == null || lineCount <= SABQI_REAL_LINE_TARGET) {
                bestEnd = current
                bestLineCount = lineCount
            } else {
                break
            }
            if (bestLineCount >= SABQI_REAL_LINE_TARGET) break
            current = HifzItqanTraversalPolicy.nextCanonicalVerse(current) ?: break
        }
        val end = bestEnd ?: return null
        val range = HifzVerseRange(start, end)
        return HifzPlannedPassage(cursor(index, range), pageEquivalent(index, range))
    }

    /**
     * Itqan is exactly one physical Madinah-Mushaf page x30. At a configured corpus
     * boundary the first/last task may be a partial page; it never borrows verses beyond
     * the declared interval. Verse references remain canonical while rendering is clamped
     * to this page by the task-aware geometry policy.
     */
    fun takeItqanPage(
        index: HifzGeometryIndex,
        allowed: HifzVerseRange,
        start: QuranVerseRef,
        afterPage: Int? = null
    ): HifzPlannedPassage? {
        if (!allowed.contains(start)) return null
        val pages = pagesForVerse(index, start).sorted()
        val page = pages.firstOrNull { afterPage == null || it > afterPage }
            ?: pages.firstOrNull()
            ?: return null
        val pageLines = index.linesByPage[page].orEmpty()
        val pageVerses = pageLines
            .flatMap { it.verses }
            .filter { allowed.contains(it) && compareQuranVerseRefs(start, it) <= 0 }
            .distinct()
            .sortedWith(Comparator(::compareQuranVerseRefs))
        if (pageVerses.isEmpty()) return null
        val cursor = HifzCursor(
            start = start,
            end = pageVerses.last(),
            startPage = page,
            endPage = page
        )
        return HifzPlannedPassage(cursor, pageEquivalent(index, cursor))
    }

    /**
     * Selects whole verses only for adaptive time-based work (Murajaah support). If even
     * the first verse exceeds capacity, that indivisible verse remains the task.
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

    /** One page/partial-page revision portion, preserving source task page boundaries. */
    fun murajaahPortions(
        index: HifzGeometryIndex,
        source: HifzCursor
    ): List<HifzPlannedPassage> = HifzGeometryPolicy.targetLines(index, source)
        .groupBy { it.ref.page }
        .toSortedMap()
        .map { (page, lines) ->
            val verses = lines.flatMap { it.targetVerses }.distinct()
                .sortedWith(Comparator(::compareQuranVerseRefs))
            require(verses.isNotEmpty())
            val cursor = HifzCursor(verses.first(), verses.last(), page, page)
            HifzPlannedPassage(cursor = cursor, pageEquivalent = pageEquivalent(index, cursor))
        }

    internal fun pagesForVerse(index: HifzGeometryIndex, verse: QuranVerseRef): Set<Int> =
        index.linesByPage.entries
            .asSequence()
            .filter { (_, lines) -> lines.any { verse in it.verses } }
            .map { it.key }
            .toSet()
}

/**
 * Idempotent daily planner. Sabqi quantity is five real Mushaf lines; Itqan quantity is
 * one physical page; only Murajaah converts available time through the adjustable pace.
 */
object HifzDailyPlanner {
    fun planDate(
        state: HifzState,
        today: LocalDate,
        geometry: HifzGeometryIndex,
        audioAvailableFor: (HifzCursor) -> Boolean = { true }
    ): HifzState {
        if (today in state.planningDates) return state

        val resumed = HifzResumePolicy.resume(state, today).state
        val bounds = resumed.journeyConfig.bounds ?: return resumed

        if (HifzSchedulePolicy.nextTask(today, resumed.tasks) != null) {
            return markConsidered(resumed, today)
        }

        val track = resumed.journeyConfig.schedule.trackFor(today.dayOfWeek)
        if (resumed.tasks.any { it.track == track && it.status != HifzTaskStatus.COMPLETED }) {
            return markConsidered(resumed, today)
        }
        val availableMinutes = resumed.journeyConfig.availableMinutes.forTrack(track)
            ?: return resumed

        val passage = when (track) {
            HifzTrack.SABQI -> planSabqi(resumed, bounds, geometry)
            HifzTrack.ITQAN -> planItqan(resumed, bounds, geometry)
            HifzTrack.MURAJAAH -> {
                val capacity = HifzTimeQuotaPolicy.pageEquivalentCapacity(
                    track,
                    availableMinutes.toDouble(),
                    resumed.journeyConfig.pace
                ) ?: return resumed
                if (capacity <= 0.0) return resumed
                planMurajaah(resumed, geometry, capacity)
            }
        } ?: return markConsidered(resumed, today)

        val task = HifzTask(
            id = stableTaskId(track, today, passage.cursor),
            track = track,
            originalScheduledDate = today,
            scheduledDate = today,
            cursor = passage.cursor,
            quota = availableMinutes,
            status = HifzTaskStatus.PLANNED,
            audioPhasesIncluded =
                track != HifzTrack.SABQI || audioAvailableFor(passage.cursor)
        )
        return markConsidered(
            resumed.copy(tasks = (resumed.tasks + task).sortedBy { it.id }),
            today
        )
    }

    private fun planSabqi(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex
    ): HifzPlannedPassage? {
        val last = state.tasks.filter { it.track == HifzTrack.SABQI }
            .maxWithOrNull(compareBy<HifzTask> { it.cursor.end.surah }.thenBy { it.cursor.end.ayah })
        val start = if (last == null) bounds.sabqi.start
        else HifzItqanTraversalPolicy.nextCanonicalVerse(last.cursor.end) ?: return null
        if (!bounds.sabqi.contains(start)) return null
        return HifzPassagePlanningPolicy.takeSabqiFiveLines(geometry, bounds.sabqi, start)
    }

    private fun planItqan(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex
    ): HifzPlannedPassage? {
        val last = state.tasks.filter { it.track == HifzTrack.ITQAN }
            .maxWithOrNull(compareBy<HifzTask> { it.cursor.end.surah }.thenBy { it.cursor.end.ayah })
        val start = if (last == null) bounds.itqan.first().start
        else HifzItqanTraversalPolicy.nextAfter(bounds.itqan, last.cursor.end) ?: return null
        val interval = bounds.itqan.firstOrNull { it.contains(start) } ?: return null
        val sameIntervalAsLast = last != null && interval.contains(last.cursor.end)
        return HifzPassagePlanningPolicy.takeItqanPage(
            geometry,
            interval,
            start,
            afterPage = last?.cursor?.endPage?.takeIf { sameIntervalAsLast }
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

        val ranked = candidates.sortedWith(
            compareByDescending<Ranked> { it.errors }
                .thenByDescending { it.reveals }
                .thenBy { it.lastReviewed ?: LocalDate.MIN }
                .thenBy { it.sourceDate }
                .thenBy { it.stable }
        )
        return ranked.firstOrNull { it.passage.pageEquivalent <= capacity + 1e-9 }?.passage
            ?: ranked.firstOrNull()?.passage
    }

    private fun markConsidered(state: HifzState, date: LocalDate): HifzState =
        state.copy(planningDates = state.planningDates + date)

    private fun stableTaskId(track: HifzTrack, date: LocalDate, cursor: HifzCursor): String =
        buildString {
            append(track.name.lowercase()).append('-').append(date)
            append('-').append(cursor.start.surah).append('_').append(cursor.start.ayah)
            append('-').append(cursor.end.surah).append('_').append(cursor.end.ayah)
            append('-').append(cursor.startPage).append('_').append(cursor.endPage)
        }
}
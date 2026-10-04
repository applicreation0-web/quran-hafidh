package com.applicreation0.quransafeguard

import java.time.LocalDate

/** Exact planned Quran passage plus its conservative real-Mushaf page-equivalent volume. */
data class HifzPlannedPassage(
    val cursor: HifzCursor,
    val pageEquivalent: Double
) {
    init { require(pageEquivalent > 0.0 && pageEquivalent.isFinite()) }
}

object HifzPassagePlanningPolicy {
    const val SABQI_REAL_LINE_TARGET = 5

    /** A physical line touched by the target counts as a full line of work. */
    fun pageEquivalent(index: HifzGeometryIndex, target: HifzVerseRange): Double {
        val lines = HifzGeometryPolicy.targetLines(index, target)
        require(lines.isNotEmpty()) { "Target has no Mushaf geometry lines." }
        return pageEquivalentFromLines(index, lines)
    }

    fun pageEquivalent(index: HifzGeometryIndex, cursor: HifzCursor): Double {
        val lines = HifzGeometryPolicy.targetLines(index, cursor)
        require(lines.isNotEmpty()) { "Task has no Mushaf geometry lines." }
        return pageEquivalentFromLines(index, lines)
    }

    private fun pageEquivalentFromLines(index: HifzGeometryIndex, lines: List<HifzTargetLine>): Double =
        lines.groupBy { it.ref.page }.entries.sumOf { (page, targetLines) ->
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
     * Frozen Sabqi rule: exactly five consecutive real Quran text lines. The end may
     * lie inside a verse. Exact start/end geometry ids are persisted so the next Sabqi
     * resumes at the immediately following physical line, not the following verse.
     */
    fun takeSabqiFiveLines(
        index: HifzGeometryIndex,
        allowed: HifzVerseRange,
        start: QuranVerseRef,
        exactStartLineId: String? = null
    ): HifzPlannedPassage? {
        if (!allowed.contains(start)) return null
        val all = HifzGeometryPolicy.orderedLines(index)
        val startIndex = if (exactStartLineId != null) {
            all.indexOfFirst { it.ref.geometryId == exactStartLineId }
        } else {
            all.indexOfFirst { line -> line.verses.any { it == start } }
        }
        if (startIndex < 0 || startIndex + SABQI_REAL_LINE_TARGET > all.size) return null

        val selected = all.subList(startIndex, startIndex + SABQI_REAL_LINE_TARGET)
        val activeByLine = selected.mapIndexed { relativeIndex, line ->
            line.verses
                .filter { ref ->
                    allowed.contains(ref) &&
                        (relativeIndex != 0 || compareQuranVerseRefs(ref, start) >= 0)
                }
                .sortedWith(Comparator(::compareQuranVerseRefs))
        }
        if (activeByLine.any { it.isEmpty() }) return null

        val actualStart = activeByLine.first().first()
        val actualEnd = activeByLine.last().last()
        val nextPhysicalLine = all.getOrNull(startIndex + SABQI_REAL_LINE_TARGET)
        val endPartial = nextPhysicalLine?.verses?.contains(actualEnd) == true
        val cursor = HifzCursor(
            start = actualStart,
            end = actualEnd,
            startPage = selected.first().ref.page,
            endPage = selected.last().ref.page,
            startLineId = selected.first().ref.geometryId,
            endLineId = selected.last().ref.geometryId,
            endVersePartial = endPartial
        )
        val targetLines = HifzGeometryPolicy.targetLines(index, cursor)
        require(targetLines.map { it.ref.geometryId }.distinct().size == SABQI_REAL_LINE_TARGET) {
            "Sabqi must resolve to exactly five real Mushaf lines."
        }
        return HifzPlannedPassage(cursor, pageEquivalent(index, cursor))
    }

    /** Exact continuation after an exact-line Sabqi task. */
    fun nextSabqiStart(index: HifzGeometryIndex, cursor: HifzCursor): Pair<QuranVerseRef, String>? {
        val endLineId = cursor.endLineId ?: return null
        val next = HifzGeometryPolicy.nextLine(index, endLineId) ?: return null
        val sorted = next.verses.sortedWith(Comparator(::compareQuranVerseRefs))
        val verse = if (cursor.endVersePartial && cursor.end in next.verses) {
            cursor.end
        } else {
            sorted.firstOrNull { compareQuranVerseRefs(it, cursor.end) > 0 }
                ?: sorted.firstOrNull()
                ?: return null
        }
        return verse to next.ref.geometryId
    }

    /**
     * Legacy preview Itqan task: a physical page/partial page. Verse references remain
     * canonical. The definitive duration-to-target formula is intentionally not invented
     * here; fixed session duration and x30 persistence are enforced elsewhere.
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
            ?: pages.firstOrNull() ?: return null
        val pageLines = index.linesByPage[page].orEmpty()
        val pageVerses = pageLines.flatMap { it.verses }
            .filter { allowed.contains(it) && compareQuranVerseRefs(start, it) <= 0 }
            .distinct().sortedWith(Comparator(::compareQuranVerseRefs))
        if (pageVerses.isEmpty()) return null
        val cursor = HifzCursor(start, pageVerses.last(), page, page)
        return HifzPlannedPassage(cursor, pageEquivalent(index, cursor))
    }

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
            } else break
            current = HifzItqanTraversalPolicy.nextCanonicalVerse(current) ?: break
        }
        val end = bestEnd ?: return null
        val range = HifzVerseRange(start, end)
        return HifzPlannedPassage(cursor(index, range), bestVolume)
    }

    fun murajaahPortions(index: HifzGeometryIndex, source: HifzCursor): List<HifzPlannedPassage> =
        HifzGeometryPolicy.targetLines(index, source)
            .groupBy { it.ref.page }.toSortedMap().map { (page, lines) ->
                val verses = lines.flatMap { it.targetVerses }.distinct()
                    .sortedWith(Comparator(::compareQuranVerseRefs))
                require(verses.isNotEmpty())
                val cursor = HifzCursor(verses.first(), verses.last(), page, page)
                HifzPlannedPassage(cursor, pageEquivalent(index, cursor))
            }

    internal fun pagesForVerse(index: HifzGeometryIndex, verse: QuranVerseRef): Set<Int> =
        index.linesByPage.entries.asSequence()
            .filter { (_, lines) -> lines.any { verse in it.verses } }
            .map { it.key }.toSet()
}

/** Daily planner. Fixed duration is stored as quota; algorithms select work, never time. */
object HifzDailyPlanner {
    fun planDate(state: HifzState, today: LocalDate, geometry: HifzGeometryIndex): HifzState {
        if (today in state.planningDates) return state
        val resumed = HifzResumePolicy.resume(state, today).state
        val bounds = resumed.journeyConfig.bounds ?: return resumed
        if (HifzSchedulePolicy.nextTask(today, resumed.tasks) != null) return markConsidered(resumed, today)

        val track = resumed.journeyConfig.schedule.trackFor(today.dayOfWeek)
        if (resumed.tasks.any { it.track == track && it.status != HifzTaskStatus.COMPLETED }) {
            return markConsidered(resumed, today)
        }
        val availableMinutes = resumed.journeyConfig.availableMinutes.forTrack(track) ?: return resumed

        val passage = when (track) {
            HifzTrack.SABQI -> planSabqi(resumed, bounds, geometry)
            HifzTrack.ITQAN -> planItqan(resumed, bounds, geometry)
            HifzTrack.MURAJAAH -> {
                val capacity = HifzTimeQuotaPolicy.pageEquivalentCapacity(
                    track, availableMinutes.toDouble(), resumed.journeyConfig.pace
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
            status = HifzTaskStatus.PLANNED
        )
        return markConsidered(resumed.copy(tasks = (resumed.tasks + task).sortedBy { it.id }), today)
    }

    private fun latestTask(state: HifzState, track: HifzTrack): HifzTask? =
        state.tasks.filter { it.track == track }
            .maxWithOrNull(compareBy<HifzTask> { it.originalScheduledDate }.thenBy { it.id })

    private fun planSabqi(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex
    ): HifzPlannedPassage? {
        val last = latestTask(state, HifzTrack.SABQI)
        if (last == null) {
            return HifzPassagePlanningPolicy.takeSabqiFiveLines(geometry, bounds.sabqi, bounds.sabqi.start)
        }
        val exact = HifzPassagePlanningPolicy.nextSabqiStart(geometry, last.cursor)
        val start = exact?.first ?: HifzItqanTraversalPolicy.nextCanonicalVerse(last.cursor.end) ?: return null
        if (!bounds.sabqi.contains(start)) return null
        return HifzPassagePlanningPolicy.takeSabqiFiveLines(
            geometry,
            bounds.sabqi,
            start,
            exactStartLineId = exact?.second
        )
    }

    private fun planItqan(
        state: HifzState,
        bounds: HifzJourneyBounds,
        geometry: HifzGeometryIndex
    ): HifzPlannedPassage? {
        val last = latestTask(state, HifzTrack.ITQAN)
        // Reference setup has lower Baqarah interval first and current cycle beginning at
        // the upper/tail interval. Using the last configured interval start preserves that
        // intended initial cursor while remaining deterministic for other setups.
        val start = if (last == null) bounds.itqan.last().start
        else HifzItqanTraversalPolicy.nextAfter(bounds.itqan, last.cursor.end)
        val interval = bounds.itqan.firstOrNull { it.contains(start) } ?: return null
        val sameIntervalAsLast = last != null && interval.contains(last.cursor.end)
        return HifzPassagePlanningPolicy.takeItqanPage(
            geometry,
            interval,
            start,
            afterPage = last?.cursor?.endPage?.takeIf { sameIntervalAsLast }
        )
    }

    /** Existing Claude Murajaah ranking retained temporarily until separate-cursor planner is green. */
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
                val lastReviewed = completedReviews.filter { it.cursor == portion.cursor }
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
            cursor.startLineId?.let { append('-').append(it.replace(':', '_')) }
            cursor.endLineId?.let { append('-').append(it.replace(':', '_')) }
        }
}

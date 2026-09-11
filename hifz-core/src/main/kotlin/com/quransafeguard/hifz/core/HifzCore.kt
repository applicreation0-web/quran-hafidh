package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.floor

/**
 * Pure Kotlin domain engine for Quran Hifz.
 *
 * The engine is deliberately independent from Android, Compose, BOOX and Safeguard.
 * It can therefore be tested as an oracle and reused by any Hifz UI implementation.
 */

data class VerseRef(val surah: Int, val ayah: Int) : Comparable<VerseRef> {
    init {
        QuranCanon.requireValid(this)
    }

    override fun compareTo(other: VerseRef): Int =
        QuranCanon.ordinal(this).compareTo(QuranCanon.ordinal(other))

    override fun toString(): String = "$surah:$ayah"
}

object QuranCanon {
    private val ayahCounts = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109,
        123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60,
        34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45,
        60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44,
        28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20,
        15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3,
        5, 4, 5, 6
    )

    private val cumulativeBeforeSurah: IntArray = IntArray(ayahCounts.size + 1).also { cumulative ->
        var running = 0
        ayahCounts.indices.forEach { index ->
            cumulative[index + 1] = running
            running += ayahCounts[index]
        }
    }

    val totalVerses: Int = ayahCounts.sum()
    val firstVerse = VerseRef(1, 1)
    val lastVerse = VerseRef(114, 6)

    fun ayahCount(surah: Int): Int {
        require(surah in 1..ayahCounts.size) { "Invalid surah: $surah" }
        return ayahCounts[surah - 1]
    }

    fun isValid(ref: VerseRef): Boolean =
        ref.surah in 1..ayahCounts.size && ref.ayah in 1..ayahCounts[ref.surah - 1]

    fun requireValid(ref: VerseRef) {
        require(ref.surah in 1..ayahCounts.size) { "Invalid surah: ${ref.surah}" }
        require(ref.ayah in 1..ayahCounts[ref.surah - 1]) {
            "Invalid ayah ${ref.ayah} for surah ${ref.surah}"
        }
    }

    /** 1-based canonical verse ordinal, from 1 to 6236. */
    fun ordinal(ref: VerseRef): Int {
        requireValid(ref)
        return cumulativeBeforeSurah[ref.surah] + ref.ayah
    }

    fun fromOrdinal(ordinal: Int): VerseRef {
        require(ordinal in 1..totalVerses) { "Invalid Quran verse ordinal: $ordinal" }
        var remaining = ordinal
        ayahCounts.forEachIndexed { index, count ->
            if (remaining <= count) return VerseRef(index + 1, remaining)
            remaining -= count
        }
        error("Unreachable ordinal: $ordinal")
    }

    fun next(ref: VerseRef): VerseRef? {
        requireValid(ref)
        return when {
            ref == lastVerse -> null
            ref.ayah < ayahCount(ref.surah) -> VerseRef(ref.surah, ref.ayah + 1)
            else -> VerseRef(ref.surah + 1, 1)
        }
    }

    fun previous(ref: VerseRef): VerseRef? {
        requireValid(ref)
        return when {
            ref == firstVerse -> null
            ref.ayah > 1 -> VerseRef(ref.surah, ref.ayah - 1)
            else -> VerseRef(ref.surah - 1, ayahCount(ref.surah - 1))
        }
    }
}

data class VerseRange(val start: VerseRef, val endInclusive: VerseRef) {
    init {
        require(start <= endInclusive) { "Range start $start must not follow end $endInclusive" }
    }

    fun contains(ref: VerseRef): Boolean = ref >= start && ref <= endInclusive
}

/**
 * Ordered, normalized, non-overlapping eligible Quran ranges.
 * Navigation is cyclic by design because Itqan and Murajaah never terminate at An-Nas.
 */
class EligibleCorpus private constructor(val ranges: List<VerseRange>) {
    init {
        require(ranges.isNotEmpty()) { "Eligible corpus cannot be empty" }
    }

    val first: VerseRef get() = ranges.first().start
    val last: VerseRef get() = ranges.last().endInclusive

    fun contains(ref: VerseRef): Boolean = ranges.any { it.contains(ref) }

    fun next(current: VerseRef): VerseRef {
        require(contains(current)) { "Cursor $current is outside the eligible corpus" }
        val ordinal = QuranCanon.ordinal(current)
        val rangeIndex = ranges.indexOfFirst { it.contains(current) }
        val range = ranges[rangeIndex]
        val endOrdinal = QuranCanon.ordinal(range.endInclusive)

        if (ordinal < endOrdinal) {
            return QuranCanon.fromOrdinal(ordinal + 1)
        }
        if (rangeIndex + 1 < ranges.size) {
            return ranges[rangeIndex + 1].start
        }
        return ranges.first().start
    }

    fun advance(current: VerseRef, steps: Int): VerseRef {
        require(steps >= 0) { "steps must be non-negative" }
        var cursor = current
        repeat(steps) { cursor = next(cursor) }
        return cursor
    }

    companion object {
        fun of(vararg ranges: VerseRange): EligibleCorpus = of(ranges.toList())

        fun of(ranges: List<VerseRange>): EligibleCorpus {
            require(ranges.isNotEmpty()) { "Eligible corpus cannot be empty" }
            val sorted = ranges.sortedBy { QuranCanon.ordinal(it.start) }
            val merged = mutableListOf<VerseRange>()

            sorted.forEach { range ->
                val previous = merged.lastOrNull()
                if (previous == null) {
                    merged += range
                } else {
                    val previousEnd = QuranCanon.ordinal(previous.endInclusive)
                    val nextStart = QuranCanon.ordinal(range.start)
                    if (nextStart <= previousEnd + 1) {
                        val mergedEnd = if (range.endInclusive > previous.endInclusive) {
                            range.endInclusive
                        } else {
                            previous.endInclusive
                        }
                        merged[merged.lastIndex] = VerseRange(previous.start, mergedEnd)
                    } else {
                        merged += range
                    }
                }
            }
            return EligibleCorpus(merged.toList())
        }

        /**
         * Reference model used by the current Hifz plan:
         * [lowerBound..promotedFrontier] U [upperTailStart..114:6].
         */
        fun dynamic(
            lowerBound: VerseRef,
            promotedFrontier: VerseRef,
            upperTailStart: VerseRef
        ): EligibleCorpus {
            require(lowerBound <= promotedFrontier) {
                "Promoted frontier $promotedFrontier cannot precede lower bound $lowerBound"
            }
            require(upperTailStart <= QuranCanon.lastVerse)
            return of(
                VerseRange(lowerBound, promotedFrontier),
                VerseRange(upperTailStart, QuranCanon.lastVerse)
            )
        }
    }
}

data class MushafPosition(
    val verse: VerseRef,
    val page: Int,
    val line: Int,
    /** Stable fragment number inside a line/verse when a five-line boundary cuts a verse. */
    val fragmentIndex: Int = 0
) {
    init {
        require(page in 1..604) { "Mushaf page must be 1..604" }
        require(line in 1..15) { "Mushaf line must be 1..15" }
        require(fragmentIndex >= 0) { "fragmentIndex must be non-negative" }
    }
}

/**
 * Adapter boundary to the exact geometry derived from the shipped Hafs/KFQC SVG corpus.
 * Implementations must never infer line counts from words or reflowed text.
 */
interface LineGeometry {
    fun advanceQuranLines(start: MushafPosition, lineCount: Int): MushafPosition
}

data class SabqiBlock(
    val start: MushafPosition,
    val endInclusive: MushafPosition,
    val lineCount: Int = REQUIRED_LINES,
    val endsInsideVerse: Boolean = false
) {
    companion object {
        const val REQUIRED_LINES = 5
    }
}

class SabqiPlanner(private val geometry: LineGeometry) {
    fun plan(start: MushafPosition): SabqiBlock {
        val end = geometry.advanceQuranLines(start, SabqiBlock.REQUIRED_LINES)
        return SabqiBlock(
            start = start,
            endInclusive = end,
            endsInsideVerse = end.fragmentIndex > 0
        )
    }
}

enum class MaskStage(val hiddenPercent: Int) {
    VISIBLE(0),
    MASK_25(25),
    MASK_50(50),
    MASK_75(75),
    MASK_100(100)
}

object SabqiRepetitionPlan {
    const val VISIBLE_REPS = 15
    const val MASK_25_REPS = 5
    const val MASK_50_REPS = 5
    const val MASK_75_REPS = 5
    const val MASK_100_REPS = 7
    const val TOTAL_REPS = VISIBLE_REPS + MASK_25_REPS + MASK_50_REPS + MASK_75_REPS + MASK_100_REPS

    fun stageForNextRepetition(completedRepetitions: Int): MaskStage? {
        require(completedRepetitions in 0..TOTAL_REPS)
        if (completedRepetitions == TOTAL_REPS) return null
        return when (completedRepetitions + 1) {
            in 1..VISIBLE_REPS -> MaskStage.VISIBLE
            in (VISIBLE_REPS + 1)..(VISIBLE_REPS + MASK_25_REPS) -> MaskStage.MASK_25
            in (VISIBLE_REPS + MASK_25_REPS + 1)..(VISIBLE_REPS + MASK_25_REPS + MASK_50_REPS) -> MaskStage.MASK_50
            in (VISIBLE_REPS + MASK_25_REPS + MASK_50_REPS + 1)..(VISIBLE_REPS + MASK_25_REPS + MASK_50_REPS + MASK_75_REPS) -> MaskStage.MASK_75
            else -> MaskStage.MASK_100
        }
    }
}

data class SabqiProgress(
    val block: SabqiBlock,
    val completedRepetitions: Int = 0,
    val assistedRepetitions: Int = 0
) {
    init {
        require(completedRepetitions in 0..SabqiRepetitionPlan.TOTAL_REPS)
        require(assistedRepetitions in 0..completedRepetitions)
    }

    val isComplete: Boolean get() = completedRepetitions == SabqiRepetitionPlan.TOTAL_REPS
    val nextStage: MaskStage? get() = SabqiRepetitionPlan.stageForNextRepetition(completedRepetitions)
    val nextRepetitionNumber: Int? get() = if (isComplete) null else completedRepetitions + 1

    fun completeRepetition(assisted: Boolean = false): SabqiProgress {
        require(!isComplete) { "Sabqi block already has all required repetitions" }
        return copy(
            completedRepetitions = completedRepetitions + 1,
            assistedRepetitions = assistedRepetitions + if (assisted) 1 else 0
        )
    }
}

data class ItqanMaskPlan(
    val visible: Int,
    val mask25: Int,
    val mask50: Int,
    val mask75: Int,
    val mask100: Int
) {
    init {
        require(listOf(visible, mask25, mask50, mask75, mask100).all { it >= 0 })
        require(total == TOTAL_REPS) { "Itqan mask plan must total $TOTAL_REPS repetitions" }
    }

    val total: Int get() = visible + mask25 + mask50 + mask75 + mask100

    fun stageForNextRepetition(completed: Int): MaskStage? {
        require(completed in 0..TOTAL_REPS)
        if (completed == TOTAL_REPS) return null
        val next = completed + 1
        val a = visible
        val b = a + mask25
        val c = b + mask50
        val d = c + mask75
        return when (next) {
            in 1..a -> MaskStage.VISIBLE
            in (a + 1)..b -> MaskStage.MASK_25
            in (b + 1)..c -> MaskStage.MASK_50
            in (c + 1)..d -> MaskStage.MASK_75
            else -> MaskStage.MASK_100
        }
    }

    companion object {
        const val TOTAL_REPS = 30

        /** Working value only; product split is intentionally centralized here. */
        val WORKING_DEFAULT = ItqanMaskPlan(
            visible = 10,
            mask25 = 5,
            mask50 = 5,
            mask75 = 5,
            mask100 = 5
        )
    }
}

data class ItqanUnit(
    val start: MushafPosition,
    val endInclusive: MushafPosition
)

data class ItqanProgress(
    val unit: ItqanUnit,
    val completedRepetitions: Int = 0,
    val assistedRepetitions: Int = 0
) {
    init {
        require(completedRepetitions in 0..ItqanMaskPlan.TOTAL_REPS)
        require(assistedRepetitions in 0..completedRepetitions)
    }

    val isComplete: Boolean get() = completedRepetitions == ItqanMaskPlan.TOTAL_REPS
    val nextRepetitionNumber: Int? get() = if (isComplete) null else completedRepetitions + 1

    fun nextStage(plan: ItqanMaskPlan): MaskStage? =
        plan.stageForNextRepetition(completedRepetitions)

    fun completeRepetition(assisted: Boolean = false): ItqanProgress {
        require(!isComplete) { "Itqan unit already has 30 repetitions" }
        return copy(
            completedRepetitions = completedRepetitions + 1,
            assistedRepetitions = assistedRepetitions + if (assisted) 1 else 0
        )
    }
}

data class HifzState(
    val lowerEligibleBound: VerseRef,
    val promotedFrontier: VerseRef,
    val upperTailStart: VerseRef,
    val itqanCursor: VerseRef,
    val murajaahItqanCursor: VerseRef
) {
    init {
        require(lowerEligibleBound <= promotedFrontier)
        val corpus = corpus()
        require(corpus.contains(itqanCursor)) { "Itqan cursor $itqanCursor must be eligible" }
        require(corpus.contains(murajaahItqanCursor)) {
            "Murajaah cursor $murajaahItqanCursor must be eligible"
        }
    }

    fun corpus(): EligibleCorpus = EligibleCorpus.dynamic(
        lowerBound = lowerEligibleBound,
        promotedFrontier = promotedFrontier,
        upperTailStart = upperTailStart
    )

    /** Corpus grows; both live cursors deliberately remain unchanged. */
    fun promoteTo(newFrontier: VerseRef): HifzState {
        require(newFrontier >= promotedFrontier) {
            "Promotion cannot move backwards from $promotedFrontier to $newFrontier"
        }
        return copy(promotedFrontier = newFrontier)
    }

    fun advanceItqanVerse(): HifzState = copy(itqanCursor = corpus().next(itqanCursor))
    fun advanceMurajaahVerse(): HifzState = copy(murajaahItqanCursor = corpus().next(murajaahItqanCursor))
}

enum class SessionKind {
    SABQI,
    ITQAN,
    MURAJAAH,
    FREE_MEMORIZATION
}

object HifzSchedule {
    fun scheduledKind(date: LocalDate, programStartDate: LocalDate): SessionKind? {
        if (date.isBefore(programStartDate)) return null
        return when (date.dayOfWeek) {
            DayOfWeek.MONDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.FRIDAY -> SessionKind.SABQI

            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY -> SessionKind.ITQAN

            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY -> SessionKind.MURAJAAH
        }
    }
}

enum class SessionStatus {
    DUE,
    COMPLETED,
    OVERDUE
}

data class ScheduleEntry(
    val date: LocalDate,
    val kind: SessionKind,
    val status: SessionStatus
)

object HifzCalendar {
    fun entries(
        programStartDate: LocalDate,
        today: LocalDate,
        completedDates: Set<LocalDate>
    ): List<ScheduleEntry> {
        if (today.isBefore(programStartDate)) return emptyList()
        val entries = mutableListOf<ScheduleEntry>()
        var date = programStartDate
        while (!date.isAfter(today)) {
            val kind = HifzSchedule.scheduledKind(date, programStartDate)
            if (kind != null) {
                val status = when {
                    date in completedDates -> SessionStatus.COMPLETED
                    date == today -> SessionStatus.DUE
                    else -> SessionStatus.OVERDUE
                }
                entries += ScheduleEntry(date, kind, status)
            }
            date = date.plusDays(1)
        }
        return entries
    }
}

data class SessionDurations(
    val sabqiMinutes: Int,
    val itqanMinutes: Int,
    val murajaahMinutes: Int,
    val freeMemorizationMinutes: Int
) {
    init {
        require(listOf(sabqiMinutes, itqanMinutes, murajaahMinutes, freeMemorizationMinutes).all { it > 0 })
    }

    fun minutesFor(kind: SessionKind): Int = when (kind) {
        SessionKind.SABQI -> sabqiMinutes
        SessionKind.ITQAN -> itqanMinutes
        SessionKind.MURAJAAH -> murajaahMinutes
        SessionKind.FREE_MEMORIZATION -> freeMemorizationMinutes
    }

    companion object {
        /** Working values, not irreversible product constants. */
        val WORKING_DEFAULT = SessionDurations(
            sabqiMinutes = 90,
            itqanMinutes = 60,
            murajaahMinutes = 45,
            freeMemorizationMinutes = 45
        )
    }
}

data class SpeedSample(
    val activeMillis: Long,
    val completedLines: Int
) {
    init {
        require(activeMillis > 0)
        require(completedLines > 0)
    }

    val secondsPerLine: Double get() = activeMillis / 1000.0 / completedLines
}

data class MurajaahSpeedPolicy(
    val initialSecondsPerLine: Double = 9.0,
    val minValidSessions: Int = 3,
    val minValidLines: Int = 60,
    val windowLines: Int = 100,
    val maxChangeRatio: Double = 0.10
) {
    init {
        require(initialSecondsPerLine > 0)
        require(minValidSessions > 0)
        require(minValidLines > 0)
        require(windowLines > 0)
        require(maxChangeRatio in 0.0..1.0)
    }

    fun plannedLines(durationMillis: Long, secondsPerLine: Double): Int {
        require(durationMillis >= 0)
        require(secondsPerLine > 0)
        val durationSeconds = durationMillis / 1000.0
        return floor(durationSeconds / secondsPerLine).toInt().coerceAtLeast(0)
    }

    fun calibratedSecondsPerLine(
        currentSecondsPerLine: Double,
        allSamplesOldestToNewest: List<SpeedSample>
    ): Double {
        require(currentSecondsPerLine > 0)
        if (allSamplesOldestToNewest.size < minValidSessions) return currentSecondsPerLine
        if (allSamplesOldestToNewest.sumOf { it.completedLines } < minValidLines) return currentSecondsPerLine

        val recent = recentWindow(allSamplesOldestToNewest)
        val ratios = recent.map { it.secondsPerLine }.sorted()
        val observedMedian = median(ratios)
        val lower = currentSecondsPerLine * (1.0 - maxChangeRatio)
        val upper = currentSecondsPerLine * (1.0 + maxChangeRatio)
        return observedMedian.coerceIn(lower, upper)
    }

    private fun recentWindow(samples: List<SpeedSample>): List<SpeedSample> {
        val selectedReversed = mutableListOf<SpeedSample>()
        var lines = 0
        for (sample in samples.asReversed()) {
            selectedReversed += sample
            lines += sample.completedLines
            if (lines >= windowLines) break
        }
        return selectedReversed.asReversed()
    }

    private fun median(sorted: List<Double>): Double {
        require(sorted.isNotEmpty())
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }
}

interface MonotonicClock {
    fun nowMillis(): Long
}

/** Android adapter must use SystemClock.elapsedRealtime(). */
class ActiveDurationTracker(
    private val clock: MonotonicClock,
    initialAccumulatedMillis: Long = 0L
) {
    private var accumulatedMillis: Long = initialAccumulatedMillis.also {
        require(it >= 0) { "initial duration must be non-negative" }
    }
    private var runningSinceMillis: Long? = null

    val isRunning: Boolean get() = runningSinceMillis != null

    fun start() {
        if (isRunning) return
        runningSinceMillis = clock.nowMillis()
    }

    fun pause(): Long {
        val started = runningSinceMillis ?: return accumulatedMillis
        val now = clock.nowMillis()
        require(now >= started) { "Monotonic clock moved backwards" }
        accumulatedMillis += now - started
        runningSinceMillis = null
        return accumulatedMillis
    }

    fun snapshotMillis(): Long {
        val started = runningSinceMillis ?: return accumulatedMillis
        val now = clock.nowMillis()
        require(now >= started) { "Monotonic clock moved backwards" }
        return accumulatedMillis + (now - started)
    }
}

enum class EinkRefreshAction {
    LOCAL_COUNTER,
    MASK_LAYER,
    LOCAL_VERSE_HIGHLIGHT,
    PAGE_BODY_PARTIAL,
    FULL_CLEAN
}

data class EinkRefreshPolicy(
    val fullCleanPageInterval: Int = 8
) {
    init {
        require(fullCleanPageInterval > 0)
    }

    fun repetitionChanged(): EinkRefreshAction = EinkRefreshAction.LOCAL_COUNTER
    fun maskStageChanged(): EinkRefreshAction = EinkRefreshAction.MASK_LAYER
    fun audioVerseChanged(): EinkRefreshAction = EinkRefreshAction.LOCAL_VERSE_HIGHLIGHT

    fun pageChanged(pagesSinceFullClean: Int): EinkRefreshAction {
        require(pagesSinceFullClean >= 0)
        return if (pagesSinceFullClean + 1 >= fullCleanPageInterval) {
            EinkRefreshAction.FULL_CLEAN
        } else {
            EinkRefreshAction.PAGE_BODY_PARTIAL
        }
    }
}

package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.floor

/** Pure deterministic Hifz domain model. Android/UI/BOOX independent. */
data class VerseRef(val surah: Int, val ayah: Int) : Comparable<VerseRef> {
    init { QuranCanon.requireValid(this) }
    override fun compareTo(other: VerseRef): Int = QuranCanon.ordinal(this).compareTo(QuranCanon.ordinal(other))
    override fun toString(): String = "$surah:$ayah"
}

object QuranCanon {
    private val counts = intArrayOf(
        7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
        112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,
        59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,
        52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,
        21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6
    )
    private val before = IntArray(115).also { out ->
        var running = 0
        for (s in 1..114) {
            out[s] = running
            running += counts[s - 1]
        }
    }
    const val TOTAL_VERSES = 6236

    fun ayahCount(surah: Int): Int {
        require(surah in 1..114)
        return counts[surah - 1]
    }
    fun requireValid(ref: VerseRef) {
        require(ref.surah in 1..114) { "invalid surah ${ref.surah}" }
        require(ref.ayah in 1..counts[ref.surah - 1]) { "invalid ayah $ref" }
    }
    fun ordinal(ref: VerseRef): Int { requireValid(ref); return before[ref.surah] + ref.ayah }
    fun fromOrdinal(value: Int): VerseRef {
        require(value in 1..TOTAL_VERSES)
        var remaining = value
        for (i in counts.indices) {
            if (remaining <= counts[i]) return VerseRef(i + 1, remaining)
            remaining -= counts[i]
        }
        error("unreachable")
    }
    fun next(ref: VerseRef): VerseRef? = when {
        ref == VerseRef(114, 6) -> null
        ref.ayah < ayahCount(ref.surah) -> VerseRef(ref.surah, ref.ayah + 1)
        else -> VerseRef(ref.surah + 1, 1)
    }
}

data class VerseRange(val start: VerseRef, val endInclusive: VerseRef) {
    init { require(start <= endInclusive) }
    fun contains(ref: VerseRef) = ref >= start && ref <= endInclusive
}

class EligibleCorpus private constructor(val ranges: List<VerseRange>) {
    init { require(ranges.isNotEmpty()) }
    fun contains(ref: VerseRef) = ranges.any { it.contains(ref) }
    fun next(current: VerseRef): VerseRef {
        require(contains(current)) { "$current outside eligible corpus" }
        val index = ranges.indexOfFirst { it.contains(current) }
        val range = ranges[index]
        if (current < range.endInclusive) return QuranCanon.fromOrdinal(QuranCanon.ordinal(current) + 1)
        return if (index + 1 < ranges.size) ranges[index + 1].start else ranges.first().start
    }
    fun advance(current: VerseRef, steps: Int): VerseRef {
        require(steps >= 0)
        var p = current
        repeat(steps) { p = next(p) }
        return p
    }
    companion object {
        fun of(ranges: List<VerseRange>): EligibleCorpus {
            require(ranges.isNotEmpty())
            val sorted = ranges.sortedBy { QuranCanon.ordinal(it.start) }
            val merged = mutableListOf<VerseRange>()
            for (r in sorted) {
                val previous = merged.lastOrNull()
                if (previous == null) merged += r
                else {
                    val touches = QuranCanon.ordinal(r.start) <= QuranCanon.ordinal(previous.endInclusive) + 1
                    if (touches) {
                        val end = if (r.endInclusive > previous.endInclusive) r.endInclusive else previous.endInclusive
                        merged[merged.lastIndex] = VerseRange(previous.start, end)
                    } else merged += r
                }
            }
            return EligibleCorpus(merged)
        }
        fun dynamic(lowerBound: VerseRef, promotedFrontier: VerseRef, upperTailStart: VerseRef): EligibleCorpus {
            require(lowerBound <= promotedFrontier)
            return of(listOf(
                VerseRange(lowerBound, promotedFrontier),
                VerseRange(upperTailStart, VerseRef(114, 6))
            ))
        }
    }
}

data class MushafPosition(
    val verse: VerseRef,
    val page: Int,
    val line: Int,
    val fragmentIndex: Int = 0
) {
    init {
        require(page in 1..604)
        require(line in 1..15)
        require(fragmentIndex >= 0)
    }
}

interface LineGeometry {
    /** Must use geometry derived from the exact displayed KFQC Mushaf, never reflowed text. */
    fun advanceQuranLines(start: MushafPosition, lineCount: Int): MushafPosition
}

data class SabqiBlock(val start: MushafPosition, val endInclusive: MushafPosition, val lineCount: Int = 5) {
    init { require(lineCount == 5) }
    val endsInsideVerse: Boolean get() = endInclusive.fragmentIndex > 0
}

class SabqiPlanner(private val geometry: LineGeometry) {
    fun plan(start: MushafPosition) = SabqiBlock(start, geometry.advanceQuranLines(start, 5))
}

enum class MaskStage(val hiddenPercent: Int) { VISIBLE(0), MASK_25(25), MASK_50(50), MASK_75(75), MASK_100(100) }

object SabqiPlan {
    const val VISIBLE = 15
    const val MASK25 = 5
    const val MASK50 = 5
    const val MASK75 = 5
    const val MASK100 = 7
    const val TOTAL = VISIBLE + MASK25 + MASK50 + MASK75 + MASK100

    fun stageForNext(completed: Int): MaskStage? {
        require(completed in 0..TOTAL)
        if (completed == TOTAL) return null
        val n = completed + 1
        return when (n) {
            in 1..VISIBLE -> MaskStage.VISIBLE
            in (VISIBLE + 1)..(VISIBLE + MASK25) -> MaskStage.MASK_25
            in (VISIBLE + MASK25 + 1)..(VISIBLE + MASK25 + MASK50) -> MaskStage.MASK_50
            in (VISIBLE + MASK25 + MASK50 + 1)..(VISIBLE + MASK25 + MASK50 + MASK75) -> MaskStage.MASK_75
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
        require(completedRepetitions in 0..SabqiPlan.TOTAL)
        require(assistedRepetitions in 0..completedRepetitions)
    }
    val nextRepetition: Int? get() = if (completedRepetitions == SabqiPlan.TOTAL) null else completedRepetitions + 1
    val nextMask: MaskStage? get() = SabqiPlan.stageForNext(completedRepetitions)
    fun complete(assisted: Boolean = false) = copy(
        completedRepetitions = completedRepetitions + 1,
        assistedRepetitions = assistedRepetitions + if (assisted) 1 else 0
    )
}

data class ItqanMaskPlan(val visible: Int, val mask25: Int, val mask50: Int, val mask75: Int, val mask100: Int) {
    init {
        require(listOf(visible, mask25, mask50, mask75, mask100).all { it >= 0 })
        require(total == 30)
    }
    val total: Int get() = visible + mask25 + mask50 + mask75 + mask100
    companion object {
        /** Working value only, intentionally centralized and replaceable. */
        val WORKING_DEFAULT = ItqanMaskPlan(10, 5, 5, 5, 5)
    }
}

data class ItqanProgress(
    val verse: VerseRef,
    val completedRepetitions: Int = 0,
    val assistedRepetitions: Int = 0
) {
    init {
        require(completedRepetitions in 0..30)
        require(assistedRepetitions in 0..completedRepetitions)
    }
    val nextRepetition: Int? get() = if (completedRepetitions == 30) null else completedRepetitions + 1
    fun complete(assisted: Boolean = false) = copy(
        completedRepetitions = completedRepetitions + 1,
        assistedRepetitions = assistedRepetitions + if (assisted) 1 else 0
    )
}

data class HifzState(
    val lowerEligibleBound: VerseRef,
    val promotedFrontier: VerseRef,
    val upperTailStart: VerseRef,
    val itqanCursor: VerseRef,
    val murajaahCursor: VerseRef
) {
    init {
        val c = corpus()
        require(c.contains(itqanCursor))
        require(c.contains(murajaahCursor))
    }
    fun corpus() = EligibleCorpus.dynamic(lowerEligibleBound, promotedFrontier, upperTailStart)
    fun advanceItqan(): HifzState = copy(itqanCursor = corpus().next(itqanCursor))
    fun advanceMurajaah(): HifzState = copy(murajaahCursor = corpus().next(murajaahCursor))
    fun promoteTo(newFrontier: VerseRef): HifzState {
        require(newFrontier >= promotedFrontier)
        val candidate = copy(promotedFrontier = newFrontier)
        require(candidate.corpus().contains(itqanCursor))
        require(candidate.corpus().contains(murajaahCursor))
        return candidate
    }
    companion object {
        fun referenceScenario() = HifzState(
            lowerEligibleBound = VerseRef(2, 1),
            promotedFrontier = VerseRef(2, 74),
            upperTailStart = VerseRef(49, 1),
            itqanCursor = VerseRef(49, 1),
            murajaahCursor = VerseRef(49, 1)
        )
    }
}

enum class SessionType { SABQI, ITQAN, MURAJAAH }

data class ScheduledSession(val date: LocalDate, val type: SessionType, val overdue: Boolean = false)

object HifzSchedule {
    fun typeFor(day: DayOfWeek): SessionType = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> SessionType.SABQI
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY -> SessionType.ITQAN
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SessionType.MURAJAAH
    }
    fun scheduled(date: LocalDate, programStartDate: LocalDate, today: LocalDate): ScheduledSession? {
        if (date < programStartDate) return null
        return ScheduledSession(date, typeFor(date.dayOfWeek), overdue = date < today)
    }
}

data class SessionDurations(
    val sabqiMinutes: Int = 90,
    val itqanMinutes: Int = 60,
    val murajaahMinutes: Int = 45,
    val freeMemMinutes: Int = 45
) {
    init { require(listOf(sabqiMinutes, itqanMinutes, murajaahMinutes, freeMemMinutes).all { it > 0 }) }
}

/** Wall clock is deliberately absent; Android adapter must feed elapsedRealtime deltas. */
class ActiveDurationCounter(private val targetMs: Long) {
    init { require(targetMs > 0) }
    var activeMs: Long = 0; private set
    fun addActive(deltaMs: Long) { require(deltaMs >= 0); activeMs = (activeMs + deltaMs).coerceAtMost(targetMs) }
    val remainingMs: Long get() = (targetMs - activeMs).coerceAtLeast(0)
    val expired: Boolean get() = activeMs >= targetMs
}

data class SpeedCalibration(
    val secondsPerLine: Double = 9.0,
    val validSessions: Int = 0,
    val validLines: Int = 0
)

object MurajaahPlanner {
    const val INITIAL_SECONDS_PER_LINE = 9.0
    const val MIN_VALID_SESSIONS = 3
    const val MIN_VALID_LINES = 60
    const val MAX_CHANGE_RATIO = 0.10

    fun plannedLines(seconds: Int, secondsPerLine: Double): Int {
        require(seconds >= 0 && secondsPerLine > 0)
        return floor(seconds / secondsPerLine).toInt()
    }
    fun recalibrate(old: SpeedCalibration, observedMedian: Double): SpeedCalibration {
        require(observedMedian > 0)
        if (old.validSessions < MIN_VALID_SESSIONS || old.validLines < MIN_VALID_LINES) return old
        val low = old.secondsPerLine * (1.0 - MAX_CHANGE_RATIO)
        val high = old.secondsPerLine * (1.0 + MAX_CHANGE_RATIO)
        return old.copy(secondsPerLine = observedMedian.coerceIn(low, high))
    }
}

enum class RefreshRegion { COUNTER, MASK, VERSE_HIGHLIGHT, TAFSIR, FULL_PAGE }

object EinkPolicy {
    const val WORKING_FULL_CLEAN_PAGE_INTERVAL = 8
    fun onRepetition(maskChanged: Boolean): Set<RefreshRegion> =
        if (maskChanged) setOf(RefreshRegion.COUNTER, RefreshRegion.MASK) else setOf(RefreshRegion.COUNTER)
    fun onAudioVerseChanged(): Set<RefreshRegion> = setOf(RefreshRegion.VERSE_HIGHLIGHT)
}

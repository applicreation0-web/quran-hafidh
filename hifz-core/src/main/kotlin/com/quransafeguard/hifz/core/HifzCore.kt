package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Shared pure domain primitives actually consumed by Quran Hifz.
 *
 * Session/repetition/masking state intentionally lives in the Android Hifz engine
 * (PreviewConfig/HifzPrefs/HifzSessionActivity). A parallel session-scheduling engine
 * (SessionType/DailyPlan/typeFor/planFor/scheduled) used to live here too, unreachable
 * from the APK; it was removed rather than kept as untested-by-runtime documentation.
 */
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

    fun ordinal(ref: VerseRef): Int {
        requireValid(ref)
        return before[ref.surah] + ref.ayah
    }

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

    /**
     * Advances the single cyclic Itqan cursor while making the configured anchor explicit.
     * The corpus itself stays in canonical order: starting at the anchor naturally visits the
     * later Quran first, wraps after the final eligible range, then visits earlier eligible
     * material until the cursor reaches the same anchor again. Growing the corpus never
     * teleports the current cursor.
     */
    fun nextAnchored(current: VerseRef, anchor: VerseRef): VerseRef {
        require(contains(anchor)) { "$anchor outside eligible corpus" }
        return next(current)
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
            for (range in sorted) {
                val previous = merged.lastOrNull()
                if (previous == null) {
                    merged += range
                } else {
                    val touches = QuranCanon.ordinal(range.start) <= QuranCanon.ordinal(previous.endInclusive) + 1
                    if (touches) {
                        val end = if (range.endInclusive > previous.endInclusive) range.endInclusive else previous.endInclusive
                        merged[merged.lastIndex] = VerseRange(previous.start, end)
                    } else {
                        merged += range
                    }
                }
            }
            return EligibleCorpus(merged)
        }
    }
}

enum class SessionKind {
    SABQI_NEW,
    SABQI_TODAY_REVIEW,
    ITQAN,
    RECENT_SABQI_REVIEW,
    OLD_ITQAN_MURAJAAH,
    ACTIVE_MURAJAAH
}

enum class CadenceAction { LEARNING, STABILIZATION, REVISION }

data class ScheduledCadence(
    val scheduledDate: LocalDate,
    val action: CadenceAction,
    val overdue: Boolean = false
)

object HifzSchedule {
    const val EVENING_REVIEW_MINUTES = 30
    const val CONSOLIDATION_MINUTES = 30
    const val ANCHORING_ENVELOPE_MINUTES = 60
    const val MAINTENANCE_MINUTES = 45
    /** Daily, mandatory, masked-by-default recall test that runs before the passive Entretien. */
    const val ACTIVE_REVIEW_MINUTES = 15

    /**
     * Sunday is always reserved for Révision, so only the other six days can be reassigned — the
     * full 0..6 range is allowed, including dedicating the whole week to one family (e.g. 6
     * Apprentissage days means Stabilisation simply never comes due that week, and vice versa).
     */
    const val MIN_LEARNING_DAYS_PER_WEEK = 0
    const val MAX_LEARNING_DAYS_PER_WEEK = 6
    const val DEFAULT_LEARNING_DAYS_PER_WEEK = 3

    fun targetMinutesFor(kind: SessionKind): Int = when (kind) {
        SessionKind.SABQI_NEW -> 0
        SessionKind.SABQI_TODAY_REVIEW -> EVENING_REVIEW_MINUTES
        SessionKind.ITQAN -> ANCHORING_ENVELOPE_MINUTES
        SessionKind.RECENT_SABQI_REVIEW -> CONSOLIDATION_MINUTES
        SessionKind.OLD_ITQAN_MURAJAAH -> MAINTENANCE_MINUTES
        SessionKind.ACTIVE_MURAJAAH -> ACTIVE_REVIEW_MINUTES
    }

    /**
     * Canonical weekly cadence: Monday..Saturday split between Apprentissage (LEARNING) and
     * Stabilisation (STABILIZATION) mornings, Sunday always the reserved Révision day.
     * learningDaysPerWeek (default 3, the original Mon/Wed/Fri split) sets how many of those six
     * days go to Apprentissage instead of Stabilisation; the rest go to Stabilisation. The days are
     * spread as evenly as possible across the week — the same even-distribution formula used for
     * scheduling N items over M slots — rather than clustered at the start, and it reproduces the
     * exact legacy Mon/Wed/Fri-vs-Tue/Thu/Sat split at the default of 3.
     */
    fun actionFor(day: DayOfWeek, learningDaysPerWeek: Int = DEFAULT_LEARNING_DAYS_PER_WEEK): CadenceAction {
        if (day == DayOfWeek.SUNDAY) return CadenceAction.REVISION
        val n = learningDaysPerWeek.coerceIn(MIN_LEARNING_DAYS_PER_WEEK, MAX_LEARNING_DAYS_PER_WEEK)
        val index = day.value - 1 // Monday=0 .. Saturday=5
        return if ((index * n) % 6 < n) CadenceAction.LEARNING else CadenceAction.STABILIZATION
    }

    /**
     * Returns exactly one automatic task: the oldest incomplete scheduled day from the programme
     * start through today. This is the soft carry-over contract: missed work stays due, but a
     * later day never creates a doubled automatic quota. Cursor/progression state remains owned by
     * the Android session engine and is therefore not modified here.
     */
    fun nextDue(
        programStartDate: LocalDate,
        today: LocalDate,
        completedDates: Set<LocalDate>,
        learningDaysPerWeek: Int = DEFAULT_LEARNING_DAYS_PER_WEEK
    ): ScheduledCadence? {
        if (today < programStartDate) return null
        var date = programStartDate
        while (!date.isAfter(today)) {
            if (!completedDates.contains(date)) {
                return ScheduledCadence(
                    scheduledDate = date,
                    action = actionFor(date.dayOfWeek, learningDaysPerWeek),
                    overdue = date < today
                )
            }
            date = date.plusDays(1)
        }
        return null
    }
}

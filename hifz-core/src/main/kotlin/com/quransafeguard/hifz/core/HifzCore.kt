package com.quransafeguard.hifz.core

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Shared pure domain primitives actually consumed by Quran Hifz.
 *
 * Session/repetition/masking state intentionally lives in the Android Hifz engine
 * (PreviewConfig/HifzPrefs/HifzSessionActivity). Keeping a second unused session
 * engine here previously allowed CI to validate rules that the APK did not execute.
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

enum class SessionType { SABQI, ITQAN, MURAJAAH }

enum class SessionKind {
    SABQI_NEW,
    SABQI_TODAY_REVIEW,
    ITQAN,
    RECENT_SABQI_REVIEW,
    OLD_ITQAN_MURAJAAH
}

enum class CadenceAction { LEARNING, STABILIZATION, REVISION }

data class PlannedSession(val kind: SessionKind, val targetMinutes: Int)
data class DailyPlan(val morning: PlannedSession, val evening: PlannedSession)

data class ScheduledSession(val date: LocalDate, val type: SessionType, val overdue: Boolean = false)
data class ScheduledCadence(
    val scheduledDate: LocalDate,
    val action: CadenceAction,
    val overdue: Boolean = false
)

object HifzSchedule {
    const val EVENING_REVIEW_MINUTES = 30
    const val CONSOLIDATION_MINUTES = 30
    const val ANCHORING_ENVELOPE_MINUTES = 60
    const val MAINTENANCE_MINUTES = 30

    fun targetMinutesFor(kind: SessionKind): Int = when (kind) {
        SessionKind.SABQI_NEW -> 0
        SessionKind.SABQI_TODAY_REVIEW -> EVENING_REVIEW_MINUTES
        SessionKind.ITQAN -> ANCHORING_ENVELOPE_MINUTES
        SessionKind.RECENT_SABQI_REVIEW -> CONSOLIDATION_MINUTES
        SessionKind.OLD_ITQAN_MURAJAAH -> MAINTENANCE_MINUTES
    }

    /**
     * Canonical weekly cadence: three Leçon-neuve mornings (Mon/Wed/Fri) pair with three
     * Ancrage mornings (Tue/Thu/Sat) to form the weekly groups of three the retention
     * redesign relies on (S1/S2/S3 and A1/A2/A3). Sunday is the sole reserved Révision day.
     */
    fun actionFor(day: DayOfWeek): CadenceAction = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> CadenceAction.LEARNING
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY -> CadenceAction.STABILIZATION
        DayOfWeek.SUNDAY -> CadenceAction.REVISION
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
        completedDates: Set<LocalDate>
    ): ScheduledCadence? {
        if (today < programStartDate) return null
        var date = programStartDate
        while (!date.isAfter(today)) {
            if (!completedDates.contains(date)) {
                return ScheduledCadence(
                    scheduledDate = date,
                    action = actionFor(date.dayOfWeek),
                    overdue = date < today
                )
            }
            date = date.plusDays(1)
        }
        return null
    }

    fun typeFor(day: DayOfWeek): SessionType = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> SessionType.SABQI
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY -> SessionType.ITQAN
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SessionType.MURAJAAH
    }

    /**
     * A zero target means repetition-driven with no time envelope. Entretien (Murajaah) is a
     * fixed nightly touch every evening, Sunday included: Sunday morning instead carries the
     * weekly snowball's ×5 final review (also repetition-driven, not time-boxed), but Sunday
     * evening still gets its own ordinary 30-minute Entretien like every other day.
     */
    fun planFor(day: DayOfWeek): DailyPlan = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> DailyPlan(
            PlannedSession(SessionKind.SABQI_NEW, targetMinutesFor(SessionKind.SABQI_NEW)),
            PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, targetMinutesFor(SessionKind.OLD_ITQAN_MURAJAAH))
        )
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY -> DailyPlan(
            PlannedSession(SessionKind.ITQAN, targetMinutesFor(SessionKind.ITQAN)),
            PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, targetMinutesFor(SessionKind.OLD_ITQAN_MURAJAAH))
        )
        DayOfWeek.SUNDAY -> DailyPlan(
            PlannedSession(SessionKind.RECENT_SABQI_REVIEW, 0),
            PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, targetMinutesFor(SessionKind.OLD_ITQAN_MURAJAAH))
        )
    }

    fun scheduled(date: LocalDate, programStartDate: LocalDate, today: LocalDate): ScheduledSession? {
        if (date < programStartDate) return null
        return ScheduledSession(date, typeFor(date.dayOfWeek), overdue = date < today)
    }
}

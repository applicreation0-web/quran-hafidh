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

data class ScheduledSession(val date: LocalDate, val type: SessionType, val overdue: Boolean = false)

object HifzSchedule {
    fun typeFor(day: DayOfWeek): SessionType = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> SessionType.SABQI
        DayOfWeek.TUESDAY, DayOfWeek.THURSDAY -> SessionType.ITQAN
        DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> SessionType.MURAJAAH
    }

    fun hasEveningMurajaah(day: DayOfWeek): Boolean =
        day == DayOfWeek.TUESDAY || day == DayOfWeek.THURSDAY

    fun scheduled(date: LocalDate, programStartDate: LocalDate, today: LocalDate): ScheduledSession? {
        if (date < programStartDate) return null
        return ScheduledSession(date, typeFor(date.dayOfWeek), overdue = date < today)
    }
}

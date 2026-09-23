package com.applicreation0.quransafeguard

/**
 * Grows the eligible Itqan corpus only from explicitly completed Sabqi work.
 * The policy edits corpus bounds only; it never mutates or teleports any Itqan task/cursor.
 */
object HifzPromotionPolicy {

    fun promoteCompletedSabqi(bounds: HifzJourneyBounds, completed: HifzCursor): HifzJourneyBounds {
        val promotionEnd = if (completed.endVersePartial) {
            previousCanonicalVerse(completed.end)
        } else completed.end

        // A five-line slice entirely inside one long verse has not completed that verse.
        if (promotionEnd == null || compareQuranVerseRefs(promotionEnd, completed.start) < 0) {
            return bounds
        }

        val mutable = bounds.itqan.toMutableList()
        val growIndex = mutable.indexOfLast { range ->
            range.contains(completed.start) || nextCanonicalVerse(range.end) == completed.start
        }
        if (growIndex < 0) return bounds

        val current = mutable[growIndex]
        val newEnd = if (compareQuranVerseRefs(promotionEnd, current.end) > 0) promotionEnd else current.end
        mutable[growIndex] = HifzVerseRange(current.start, newEnd)

        return bounds.copy(itqan = mergeTouchingEligibleRanges(mutable))
    }

    private fun mergeTouchingEligibleRanges(ranges: List<HifzVerseRange>): List<HifzVerseRange> {
        if (ranges.isEmpty()) return ranges
        val ordered = ranges.sortedWith(Comparator { a, b -> compareQuranVerseRefs(a.start, b.start) })
        val out = mutableListOf<HifzVerseRange>()
        ordered.forEach { next ->
            val previous = out.lastOrNull()
            if (previous == null) {
                out += next
            } else {
                val touchesOrOverlaps =
                    previous.contains(next.start) || nextCanonicalVerse(previous.end) == next.start
                if (touchesOrOverlaps) {
                    val end = if (compareQuranVerseRefs(next.end, previous.end) > 0) next.end else previous.end
                    out[out.lastIndex] = HifzVerseRange(previous.start, end)
                } else {
                    out += next
                }
            }
        }
        return out
    }

    internal fun previousCanonicalVerse(ref: QuranVerseRef): QuranVerseRef? {
        QuranCanonicalBounds.requireValid(ref)
        return when {
            ref.ayah > 1 -> QuranVerseRef(ref.surah, ref.ayah - 1)
            ref.surah > 1 -> {
                val previousSurah = ref.surah - 1
                QuranVerseRef(previousSurah, requireNotNull(QuranCanonicalBounds.ayahCount(previousSurah)))
            }
            else -> null
        }
    }

    private fun nextCanonicalVerse(ref: QuranVerseRef): QuranVerseRef? =
        HifzItqanTraversalPolicy.nextCanonicalVerse(ref)
}

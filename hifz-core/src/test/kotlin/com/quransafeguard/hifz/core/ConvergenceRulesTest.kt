package com.quransafeguard.hifz.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConvergenceRulesTest {
    @Test fun arbitraryItqanRangesAreNormalizedAndGapsStayIneligible() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(50, 1), VerseRef(50, 10)),
            VerseRange(VerseRef(2, 1), VerseRef(2, 10)),
            VerseRange(VerseRef(2, 8), VerseRef(2, 20)),
            VerseRange(VerseRef(50, 11), VerseRef(50, 12))
        ))
        assertEquals(2, corpus.ranges.size)
        assertEquals(VerseRange(VerseRef(2, 1), VerseRef(2, 20)), corpus.ranges[0])
        assertEquals(VerseRange(VerseRef(50, 1), VerseRef(50, 12)), corpus.ranges[1])
        assertFalse(corpus.contains(VerseRef(2, 21)))
        assertFalse(corpus.contains(VerseRef(49, 1)))
    }

    @Test fun rotationCanStartInsideAnyEligibleRangeAndWrapCanonically() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(2, 1), VerseRef(2, 3)),
            VerseRange(VerseRef(49, 1), VerseRef(49, 2)),
            VerseRange(VerseRef(114, 5), VerseRef(114, 6))
        ))
        var cursor = VerseRef(49, 2)
        val seen = mutableListOf<VerseRef>()
        repeat(7) {
            seen += cursor
            cursor = corpus.next(cursor)
        }
        assertEquals(listOf(
            VerseRef(49,2), VerseRef(114,5), VerseRef(114,6),
            VerseRef(2,1), VerseRef(2,2), VerseRef(2,3), VerseRef(49,1)
        ), seen)
        assertEquals(VerseRef(49,2), cursor)
    }

    @Test fun corpusExtensionDoesNotRequireCursorTeleportation() {
        val before = EligibleCorpus.of(listOf(VerseRange(VerseRef(49,1), VerseRef(114,6))))
        val cursor = VerseRef(52,10)
        assertTrue(before.contains(cursor))
        val after = EligibleCorpus.of(before.ranges + VerseRange(VerseRef(2,75), VerseRef(2,90)))
        assertTrue(after.contains(cursor))
        assertEquals(VerseRef(52,10), cursor)
    }
}

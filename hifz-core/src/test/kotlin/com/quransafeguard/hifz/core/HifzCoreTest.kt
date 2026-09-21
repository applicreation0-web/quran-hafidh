package com.quransafeguard.hifz.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HifzCoreTest {
    @Test fun quranCanonHas6236VersesAndCrossesSurahBoundary() {
        assertEquals(6236, QuranCanon.TOTAL_VERSES)
        assertEquals(VerseRef(3,1), QuranCanon.next(VerseRef(2,286)))
        assertNull(QuranCanon.next(VerseRef(114,6)))
    }

    @Test fun eligibleCorpusMergesAdjacencyPreservesGapsAndWraps() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(50,1), VerseRef(50,10)),
            VerseRange(VerseRef(2,1), VerseRef(2,10)),
            VerseRange(VerseRef(2,8), VerseRef(2,20)),
            VerseRange(VerseRef(50,11), VerseRef(50,12)),
            VerseRange(VerseRef(114,5), VerseRef(114,6))
        ))
        assertEquals(3, corpus.ranges.size)
        assertEquals(VerseRange(VerseRef(2,1), VerseRef(2,20)), corpus.ranges[0])
        assertEquals(VerseRange(VerseRef(50,1), VerseRef(50,12)), corpus.ranges[1])
        assertFalse(corpus.contains(VerseRef(2,21)))
        assertEquals(VerseRef(2,1), corpus.next(VerseRef(114,6)))
        assertEquals(VerseRef(50,1), corpus.next(VerseRef(2,20)))
    }

    @Test fun anchoredCycleTraversesAfterAnchorThenEarlierCorpusBeforeWrapping() {
        val corpus = EligibleCorpus.of(listOf(
            VerseRange(VerseRef(2,1), VerseRef(2,2)),
            VerseRange(VerseRef(49,1), VerseRef(49,2)),
            VerseRange(VerseRef(114,6), VerseRef(114,6))
        ))
        val anchor = VerseRef(49,1)
        assertEquals(VerseRef(49,2), corpus.nextAnchored(VerseRef(49,1), anchor))
        assertEquals(VerseRef(114,6), corpus.nextAnchored(VerseRef(49,2), anchor))
        assertEquals(VerseRef(2,1), corpus.nextAnchored(VerseRef(114,6), anchor))
        assertEquals(VerseRef(2,2), corpus.nextAnchored(VerseRef(2,1), anchor))
        assertEquals(anchor, corpus.nextAnchored(VerseRef(2,2), anchor))
    }

    @Test fun corpusExtensionNeverRequiresCursorTeleportation() {
        val before = EligibleCorpus.of(listOf(VerseRange(VerseRef(49,1), VerseRef(114,6))))
        val cursor = VerseRef(52,10)
        assertTrue(before.contains(cursor))
        val after = EligibleCorpus.of(before.ranges + VerseRange(VerseRef(2,75), VerseRef(2,90)))
        assertTrue(after.contains(cursor))
        assertEquals(VerseRef(52,10), cursor)
    }

    @Test fun directSessionTargetsDoNotDependOnTodaysSchedule() {
        assertEquals(0, HifzSchedule.targetMinutesFor(SessionKind.SABQI_NEW))
        assertEquals(30, HifzSchedule.targetMinutesFor(SessionKind.SABQI_TODAY_REVIEW))
        assertEquals(60, HifzSchedule.targetMinutesFor(SessionKind.ITQAN))
        assertEquals(30, HifzSchedule.targetMinutesFor(SessionKind.RECENT_SABQI_REVIEW))
        assertEquals(45, HifzSchedule.targetMinutesFor(SessionKind.OLD_ITQAN_MURAJAAH))
        assertEquals(15, HifzSchedule.targetMinutesFor(SessionKind.ACTIVE_MURAJAAH))
    }
}

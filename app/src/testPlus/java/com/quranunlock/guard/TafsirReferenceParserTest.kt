package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirReferenceParserTest {
    @Test
    fun parsesExplicitSingleReferencesWithoutInferringProse() {
        val text = "Allah says this (8:24), while another passage is [24:35]."
        val matches = TafsirReferenceParser.find(text)

        assertEquals(
            listOf(
                QuranReferenceRef(8, 24),
                QuranReferenceRef(24, 35)
            ),
            matches.map { it.reference }
        )
        assertEquals(listOf("8:24", "24:35"), matches.map { text.substring(it.start, it.endExclusive) })
    }

    @Test
    fun parsesSourceRangesAndKeepsRangeBoundary() {
        val text = "Surely mankind goes too far [96:6–7]."
        val match = TafsirReferenceParser.find(text).single()

        assertEquals(QuranReferenceRef(96, 6, 7), match.reference)
        assertEquals("96:6–7", text.substring(match.start, match.endExclusive))
    }

    @Test
    fun parsesMultipleReferencesInsideOneSourceBracket() {
        val text = "You could never reckon it [14:34 and 16:18]."
        assertEquals(
            listOf(QuranReferenceRef(14, 34), QuranReferenceRef(16, 18)),
            TafsirReferenceParser.find(text).map { it.reference }
        )
    }

    @Test
    fun rejectsImpossibleQurtubiSourceReferencesRatherThanGuessingCorrections() {
        val sourceAnomalies = "21:480 2:293 4:181 58:29 4:186"
        assertTrue(TafsirReferenceParser.find(sourceAnomalies).isEmpty())
    }

    @Test
    fun doesNotInventReferencesFromOrdinaryNumbers() {
        val text = "There are seven points, 1989 pages, and chapter twenty in the discussion."
        assertTrue(TafsirReferenceParser.find(text).isEmpty())
    }
}

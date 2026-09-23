package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirSourceRenderingTest {
    @Test
    fun sourceTranslationStaysBeforeCommentaryWithoutGeneratedVerseHeading() {
        val runs = renderSourceBackedTafsirSegments(
            listOf(
                SourceBackedTafsirSegment(
                    verseStart = 68,
                    verseEnd = 68,
                    segment = 1,
                    translation = "They said: Call upon your Lord for us...",
                    commentary = "The source commentary follows the verse translation."
                )
            )
        )

        assertEquals(TafsirRunStyle.BOLD_ITALIC, runs[0].style)
        assertEquals("They said: Call upon your Lord for us...", runs[0].text)
        assertEquals("\n", runs[1].text)
        assertEquals(TafsirRunStyle.REGULAR, runs[2].style)
        assertEquals("The source commentary follows the verse translation.", runs[2].text)

        val rendered = runs.joinToString("") { it.text }
        assertFalse(rendered.startsWith("68"))
        assertFalse(rendered.contains("Verse 68", ignoreCase = true))
        assertFalse(rendered.contains("Ayah 68", ignoreCase = true))
        assertFalse(rendered.contains("\n\n"))
    }

    @Test
    fun structuralVerseNumbersNeverLeakIntoDisplayedText() {
        val runs = renderSourceBackedTafsirSegments(
            listOf(
                SourceBackedTafsirSegment(
                    verseStart = 68,
                    verseEnd = 71,
                    segment = 3,
                    translation = "Source translation",
                    commentary = "Source commentary"
                )
            )
        )

        assertTrue(runs.any { it.text == "Source translation" })
        assertTrue(runs.any { it.text == "Source commentary" })
        assertFalse(runs.any { it.text == "68" || it.text == "71" || it.text == "3" })
    }

    @Test
    fun emptyTranslationDoesNotCreateSyntheticTextBeforeCommentary() {
        val runs = renderSourceBackedTafsirSegments(
            listOf(
                SourceBackedTafsirSegment(
                    verseStart = 1,
                    verseEnd = 1,
                    segment = 1,
                    translation = "",
                    commentary = "Only the source commentary is available."
                )
            )
        )

        assertEquals(1, runs.size)
        assertEquals(TafsirRunStyle.REGULAR, runs.single().style)
        assertEquals("Only the source commentary is available.", runs.single().text)
    }
}

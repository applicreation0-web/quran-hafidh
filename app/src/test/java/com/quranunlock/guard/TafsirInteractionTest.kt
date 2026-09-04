package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TafsirInteractionTest {
    @Test
    fun extractsEveryPolygonOfTheSameVerseAsOneReference() {
        val svg = """
            <svg>
              <path class="ayahPolygon" number="002013" ayah="13" surah="2" d="A" />
              <path class="ayahPolygon extra" surah="2" ayah="13" d="B" />
              <path class="ayahPolygon" surah="2" ayah="14" d="C" />
            </svg>
        """.trimIndent()

        assertEquals(
            setOf(VerseRef(2, 13), VerseRef(2, 14)),
            MushafVerseIndex.fromSvg(svg)
        )
    }

    @Test
    fun horizontalSwipesFollowArabicBookDirection() {
        val classifier = ReaderGestureClassifier(72f)
        classifier.onDown(100f, 100f, 1)
        assertEquals(ReaderSwipe.NEXT, classifier.onUp(200f, 105f, true))

        classifier.onDown(200f, 100f, 1)
        assertEquals(ReaderSwipe.PREVIOUS, classifier.onUp(100f, 95f, true))
    }

    @Test
    fun verticalMovementPinchCancelAndOpenPanelNeverNavigate() {
        val classifier = ReaderGestureClassifier(72f)
        classifier.onDown(100f, 100f, 1)
        classifier.onMove(105f, 210f, 1)
        assertNull(classifier.onUp(108f, 230f, true))

        classifier.onDown(200f, 100f, 1)
        classifier.onAdditionalPointer()
        assertNull(classifier.onUp(50f, 100f, true))

        classifier.onDown(200f, 100f, 1)
        classifier.onCancel()
        assertNull(classifier.onUp(50f, 100f, true))

        classifier.onDown(200f, 100f, 1)
        assertNull(classifier.onUp(50f, 100f, false))
        assertTrue(true)
    }

    @Test
    fun jalalaynIsTheDefaultTafsirEdition() {
        assertEquals(TafsirEditionId.JALALAYN, TafsirEditionId.fromStableId(null))
        assertEquals(TafsirEditionId.JALALAYN, TafsirEditionId.fromStableId("unknown"))
    }

    @Test
    fun partialTafsirCoverageFailsClosedAtExactBoundaries() {
        assertTrue(TafsirEditionId.QURTUBI.covers(VerseRef(4, 23)))
        assertFalse(TafsirEditionId.QURTUBI.covers(VerseRef(4, 24)))
        assertTrue(TafsirEditionId.QUSHAYRI.covers(VerseRef(4, 176)))
        assertFalse(TafsirEditionId.QUSHAYRI.covers(VerseRef(5, 1)))
    }

    @Test
    fun tafsirRequestIdentityIncludesEdition() {
        val verse = VerseRef(2, 85)
        assertNotEquals(
            TafsirRequestKey(verse, TafsirEditionId.QUSHAYRI),
            TafsirRequestKey(verse, TafsirEditionId.QURTUBI)
        )
    }

    @Test
    fun rangeCommentaryKeepsItsSourceRangeLabel() {
        val entry = TafsirEntry(
            verse = VerseRef(4, 12),
            commentaryRuns = listOf(TafsirRun(TafsirRunStyle.REGULAR, "body")),
            notes = emptyList(),
            editionId = TafsirEditionId.QURTUBI,
            verseStart = 11,
            verseEnd = 14
        )
        assertEquals("Commentary on 4:11–14", entry.rangeLabel)
    }
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
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
    fun explicitPoetryStaysSeparateFromJustifiedProse() {
        val blocks = splitTafsirRenderBlocks(
            listOf(
                TafsirRun(TafsirRunStyle.REGULAR, "Prose A"),
                TafsirRun(TafsirRunStyle.POETRY, "Line 1\nLine 2"),
                TafsirRun(TafsirRunStyle.POETRY, "\nLine 3"),
                TafsirRun(TafsirRunStyle.REGULAR, "Prose B")
            )
        )
        assertEquals(
            listOf(TafsirBlockKind.PROSE, TafsirBlockKind.POETRY, TafsirBlockKind.PROSE),
            blocks.map { it.kind }
        )
        assertEquals(
            "Line 1\nLine 2\nLine 3",
            blocks[1].runs.joinToString(separator = "") { it.text }
        )
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
}
package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HifzMushafCursorVerifierTest {
    private val page2 = """
        <svg xmlns="http://www.w3.org/2000/svg">
          <path ayah="1" class="ayahPolygon selected" surah="2" />
          <path class="ayahPolygon" surah="2" ayah="2" />
          <path class="decoration" surah="99" ayah="1" />
        </svg>
    """.trimIndent()

    private val page3 = """
        <svg xmlns="http://www.w3.org/2000/svg">
          <path class='ayahPolygon' ayah='6' surah='2' />
          <path class='ayahPolygon' surah='2' ayah='7' />
        </svg>
    """.trimIndent()

    @Test
    fun coherentEndpointsMustExistOnDeclaredMushafPages() {
        val cursor = HifzCursor.range(2, 1, 2, 7, 2, 3)

        assertTrue(
            HifzMushafCursorVerifier.allCoherent(listOf(cursor)) { page ->
                when (page) {
                    2 -> page2
                    3 -> page3
                    else -> null
                }
            }
        )
    }

    @Test
    fun validVerseAndPageNumbersStillFailWhenTheirPairingIsWrong() {
        val wrong = HifzCursor.page(2, 1, 1, 604)

        assertFalse(
            HifzMushafCursorVerifier.allCoherent(listOf(wrong)) { page ->
                if (page == 604) "<svg><path class=\"ayahPolygon\" surah=\"114\" ayah=\"6\" /></svg>" else null
            }
        )
    }

    @Test
    fun nonAyahPolygonMetadataCannotFakeCoherence() {
        val cursor = HifzCursor.page(99, 1, 1, 2)

        assertFalse(HifzMushafCursorVerifier.allCoherent(listOf(cursor)) { page2 })
    }

    @Test
    fun missingPageAssetFailsClosed() {
        val cursor = HifzCursor.page(2, 1, 1, 2)

        assertFalse(HifzMushafCursorVerifier.allCoherent(listOf(cursor)) { null })
    }
}

package com.applicreation0.quransafeguard

import org.brotli.dec.BrotliInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MushafAssetCompletenessTest {
    private fun mushafDir(): File {
        val candidates = listOf(
            File("src/main/assets/mushaf/hafs/kfqc/svg-br"),
            File("app/src/main/assets/mushaf/hafs/kfqc/svg-br")
        )
        return candidates.firstOrNull(File::isDirectory)
            ?: error("Medina Mushaf assets are missing; run scripts/fetch_mushaf_pages.sh")
    }

    private fun readPage(page: Int): String {
        val file = mushafDir().resolve("%03d.svg.br".format(page))
        assertTrue("Mushaf page %03d is missing or empty".format(page), file.isFile && file.length() > 0L)
        return try {
            BrotliInputStream(file.inputStream()).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: Throwable) {
            throw AssertionError("Mushaf page %03d cannot be Brotli-decoded".format(page), error)
        }
    }

    @Test
    fun all604CanonicalPagesExistDecodeAndContainVerseSvg() {
        val dir = mushafDir()
        val canonical = dir.listFiles { file ->
            file.isFile && file.name.matches(Regex("\\d{3}\\.svg\\.br"))
        }.orEmpty().map { it.name }.toSet()
        val expected = (1..604).map { "%03d.svg.br".format(it) }.toSet()
        assertEquals("The packaged Mushaf must contain exactly pages 001..604", expected, canonical)

        for (page in 1..604) {
            val svg = readPage(page)
            assertTrue("Mushaf page %03d has no SVG root".format(page), svg.contains("<svg"))
            assertTrue("Mushaf page %03d has no closing SVG element".format(page), svg.contains("</svg>"))
            assertTrue(
                "Mushaf page %03d exposes no ayah metadata".format(page),
                MushafVerseIndex.fromSvg(svg).isNotEmpty()
            )
        }
    }

    @Test
    fun page552IsPresentAndContainsSurahAsSaff() {
        val verses = MushafVerseIndex.fromSvg(readPage(552))
        assertTrue("Page 552 must contain verses from Surah as-Saff (61)", verses.any { it.surah == 61 })
        assertTrue("Page 552 must not decode as an empty verse page", verses.isNotEmpty())
    }
}

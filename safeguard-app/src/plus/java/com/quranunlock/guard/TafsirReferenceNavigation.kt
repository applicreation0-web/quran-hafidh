package com.applicreation0.quransafeguard

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.brotli.dec.BrotliInputStream

/**
 * Resolves an already validated Tafsir Quran reference to the exact pinned
 * Medina Mushaf page. Only the canonical Juz page window containing the verse
 * is scanned, so a click never requires reading all 604 pages.
 */
internal object TafsirReferenceNavigation {
    suspend fun pageFor(
        context: Context,
        reference: QuranReferenceRef
    ): Int? = withContext(Dispatchers.IO) {
        if (!TafsirReferenceParser.isCanonical(reference.surah, reference.startAyah) ||
            !TafsirReferenceParser.isCanonical(reference.surah, reference.endAyah) ||
            reference.endAyah < reference.startAyah
        ) {
            return@withContext null
        }

        val target = reference.startVerse
        val division = (1..30)
            .asSequence()
            .map { QuranStructureMetadata.division(QuranSelectionMode.JUZ, it) }
            .firstOrNull { contains(it, target) }
            ?: return@withContext null

        division.pageRange.firstOrNull { page ->
            val assetPath = "mushaf/hafs/kfqc/svg-br/%03d.svg.br".format(page)
            val svg = runCatching {
                context.assets.open(assetPath).use { compressed ->
                    BrotliInputStream(compressed)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                }
            }.getOrNull() ?: return@firstOrNull false
            target in MushafVerseIndex.fromSvg(svg)
        }
    }

    private fun contains(division: QuranDivision, verse: VerseRef): Boolean {
        fun compare(surah: Int, ayah: Int, other: QuranVerseRef): Int =
            when {
                surah != other.surah -> surah.compareTo(other.surah)
                else -> ayah.compareTo(other.ayah)
            }
        return compare(verse.surah, verse.ayah, division.start) >= 0 &&
            compare(verse.surah, verse.ayah, division.end) <= 0
    }
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamSharhIntegrityTest {
    private fun entry(
        commentator: HikamCommentator = HikamCommentator.SHARNUBI,
        workId: String = commentator.expectedWorkId,
        canonicalNumbers: Set<Int> = setOf(16),
        groupId: String = "${commentator.stableId}-group-16",
        arabicSpans: List<HikamRichSpan> = emptyList()
    ) = HikamSharhEntry(
        hikmaNumber = canonicalNumbers.minOrNull() ?: 16,
        commentator = commentator,
        workId = workId,
        workTitle = "verified work",
        arabicText = "نص شرح موثق",
        frenchText = "Traduction vérifiée du commentaire.",
        sourceUrl = "https://example.test/source",
        printLocator = "p. 10",
        verified = true,
        canonicalHikmaNumbers = canonicalNumbers,
        sourceHikmaLocator = "source locator",
        commentaryGroupId = groupId,
        richSpansArabic = arabicSpans
    )

    @Test
    fun oneClassicalBlockMayMapToSeveralCanonicalHikamWithoutDuplication() {
        val model = entry(canonicalNumbers = setOf(16, 17))
        assertTrue(model.displayEligible)
        HikamSharhIntegrity.requireUnique(listOf(model))
    }

    @Test
    fun twoCommentatorsMayIndependentlyCommentTheSameCanonicalHikma() {
        val sharnubi = entry(
            commentator = HikamCommentator.SHARNUBI,
            groupId = "sharnubi-16"
        )
        val ibnAbbad = entry(
            commentator = HikamCommentator.IBN_ABBAD,
            groupId = "ibn-abbad-16"
        )
        HikamSharhIntegrity.requireUnique(listOf(sharnubi, ibnAbbad))
    }

    @Test
    fun duplicateCanonicalMappingForSameCommentatorFailsClosed() {
        val first = entry(canonicalNumbers = setOf(16, 17), groupId = "group-a")
        val second = entry(canonicalNumbers = setOf(17), groupId = "group-b")
        assertThrows(IllegalArgumentException::class.java) {
            HikamSharhIntegrity.requireUnique(listOf(first, second))
        }
    }

    @Test
    fun crossWorkAttributionFailsClosed() {
        val wrong = entry(workId = HikamCommentator.IBN_ABBAD.expectedWorkId)
        assertThrows(IllegalArgumentException::class.java) {
            HikamSharhIntegrity.requireUnique(listOf(wrong))
        }
    }

    @Test
    fun invalidRichSpanCannotRemainDisplayEligible() {
        val invalid = entry(
            arabicSpans = listOf(
                HikamRichSpan(
                    start = 0,
                    endExclusive = 10_000,
                    role = HikamRichRole.COMMENTATOR_EMPHASIS
                )
            )
        )
        assertThrows(IllegalArgumentException::class.java) {
            HikamSharhIntegrity.requireUnique(listOf(invalid))
        }
    }

    @Test
    fun duplicateSourceGroupIdFailsClosedEvenAcrossDifferentCanonicalNumbers() {
        val first = entry(canonicalNumbers = setOf(16), groupId = "same-source-block")
        val second = entry(canonicalNumbers = setOf(18), groupId = "same-source-block")
        assertThrows(IllegalArgumentException::class.java) {
            HikamSharhIntegrity.requireUnique(listOf(first, second))
        }
    }
}

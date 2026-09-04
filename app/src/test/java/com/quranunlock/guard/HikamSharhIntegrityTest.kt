package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamSharhIntegrityTest {
    private fun entry(
        hikma: Int = 16,
        commentator: HikamCommentator = HikamCommentator.SHARNUBI,
        workId: String = commentator.expectedWorkId
    ) = HikamSharhEntry(
        hikmaNumber = hikma,
        commentator = commentator,
        workId = workId,
        workTitle = "Verified classical sharh",
        arabicText = "نص الشرح",
        frenchText = "Traduction vérifiée du commentaire.",
        sourceUrl = "https://example.test/source",
        printLocator = "p. 10",
        verified = true
    )

    @Test
    fun validCommentatorWorkPairCanDisplay() {
        assertTrue(entry().displayEligible)
        assertTrue(
            entry(commentator = HikamCommentator.IBN_ABBAD).displayEligible
        )
    }

    @Test
    fun crossAttributedWorkIsRejected() {
        val wrong = entry(
            commentator = HikamCommentator.SHARNUBI,
            workId = HikamCommentator.IBN_ABBAD.expectedWorkId
        )
        assertFalse(wrong.attributionMatchesSource)
        assertFalse(wrong.displayEligible)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateHikmaAndCommentatorFailsClosed() {
        val one = entry()
        HikamSharhIntegrity.requireUnique(listOf(one, one.copy()))
    }
}

package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamSharhContractTest {
    @Test
    fun fullyVerifiedDocumentaryEntryIsDisplayEligible() {
        val entry = sampleEntry()

        assertTrue(entry.verified)
        assertTrue(entry.displayEligible)
        assertTrue(entry.sourceEdition.isNotBlank())
        assertTrue(entry.translationCredit.isNotBlank())
        assertTrue(entry.boundaryLocator.isNotBlank())
    }

    @Test
    fun blankBoundaryLocatorIsRejected() {
        val entry = sampleEntry(boundaryLocator = "")

        assertFalse(entry.displayEligible)
    }

    @Test
    fun unverifiedEntryIsRejected() {
        val entry = sampleEntry(verified = false)

        assertFalse(entry.displayEligible)
    }

    @Test
    fun missingTranslationCreditIsRejected() {
        val entry = sampleEntry(translationCredit = "")

        assertFalse(entry.displayEligible)
    }

    private fun sampleEntry(
        boundaryLocator: String = "matn A → next matn B",
        translationCredit: String = "Quran Safeguard — test",
        verified: Boolean = true
    ) = HikamSharhEntry(
        hikmaNumber = 1,
        commentator = HikamCommentator.SHARNUBI,
        workTitle = "شرح الحكم العطائية",
        sourceEdition = "Dar Ibn Kathir, 2e éd., 1410/1989",
        arabicText = "هذا نص عربي تجريبي لا يمثل محتوى منشورا.",
        frenchText = "Texte de test ne constituant pas un commentaire publié.",
        translationCredit = translationCredit,
        sourceUrl = "https://archive.org/example",
        printLocator = "p. 12",
        boundaryLocator = boundaryLocator,
        verified = verified
    )
}

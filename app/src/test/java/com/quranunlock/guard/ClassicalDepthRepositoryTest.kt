package com.applicreation0.quransafeguard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicalDepthRepositoryTest {
    @Test
    fun unverifiedOrRightsUnresolvedContentCannotDisplay() {
        val unresolved = ClassicalDepthEntry(
            reminderId = "hikma_12",
            kind = ClassicalDepthKind.HIKAM_CLASSICAL_COMMENTARY,
            sourceAuthor = "Ibn Ajiba",
            sourceWork = "Iqaz al-Himam fi Sharh al-Hikam",
            arabicText = "نص عربي موثق",
            frenchText = "Traduction française correspondante.",
            sourceUrl = "https://example.invalid/source",
            sourceReference = "p. 58",
            verificationDate = "2026-09-01",
            translationRightsStatus = TranslationRightsStatus.UNRESOLVED,
            humanVerified = true,
            completePassage = true
        )
        assertFalse(unresolved.isDisplayReady)

        val notHumanVerified = unresolved.copy(
            translationRightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_CLEARED,
            humanVerified = false
        )
        assertFalse(notHumanVerified.isDisplayReady)
    }

    @Test
    fun verifiedRightsClearedContentCanDisplay() {
        val ready = ClassicalDepthEntry(
            reminderId = "ghazali_test",
            kind = ClassicalDepthKind.GHAZALI_SAME_AUTHOR_CONTEXT,
            sourceAuthor = "Abu Hamid al-Ghazali",
            sourceWork = "Bidayat al-Hidaya",
            arabicText = "نص عربي موثق",
            frenchText = "Traduction française correspondante.",
            sourceUrl = "https://example.invalid/source",
            sourceReference = "section test",
            verificationDate = "2026-09-01",
            translationRightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_CLEARED,
            humanVerified = true,
            completePassage = false
        )
        assertTrue(ready.isDisplayReady)
        assertTrue(ready.buttonLabel.contains("contexte"))
    }

    @Test
    fun repositoryDoesNotExposePendingContent() {
        assertNull(ClassicalDepthRepository.displayEntry("hikma_12"))
    }
}

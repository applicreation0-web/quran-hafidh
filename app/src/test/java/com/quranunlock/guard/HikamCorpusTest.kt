package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamCorpusTest {
    private fun entry(
        french: String = "Traduction française vérifiée.",
        translationStatus: HikamTranslationStatus = HikamTranslationStatus.VERIFIED,
        verificationStatus: HikamVerificationStatus = HikamVerificationStatus.VERIFIED,
        translationSources: List<String> = listOf("source")
    ) = HikamEntry(
        id = "hikam_001",
        collection = "al_hikam_al_ataiyya",
        author = "Ibn Ata Allah al-Iskandari",
        arabic = "من علامات الاعتماد على العمل نقصان الرجاء عند وجود الزلل",
        french = french,
        translationStatus = translationStatus,
        translationSources = translationSources,
        translationMethod = "editorial_translation_from_verified_arabic",
        explanation = null,
        sourceTitle = "Al-Hikam al-Ata'iyya",
        sourceEdition = "édition de test",
        sourceNumber = "1",
        sourcePage = "95",
        verificationStatus = verificationStatus,
        verificationSources = listOf("https://example.test/source"),
        textType = "author_wisdom",
        themes = setOf("hope"),
        estimatedReadingSeconds = 8,
        alternateNumbering = emptyMap(),
        textualVariant = null,
        translatorNote = null,
        verificationNotes = null
    )

    @Test
    fun verifiedTranslatedHikmaMapsToScholarWisdomReminder() {
        val reminder = entry().toDailyReminder()
        assertEquals(ReminderType.HIKAM, reminder.type)
        assertEquals("Ibn ‘Atâ’ Allâh al-Iskandarî", reminder.author)
        assertEquals("Al-Hikam al-‘Atâ’iyya", reminder.book)
        assertNull(reminder.authenticity)
        assertTrue(reminder.frenchText.isNotBlank())
    }

    @Test
    fun missingFrenchTranslationCanNeverBecomeActiveReminder() {
        assertThrows(IllegalStateException::class.java) {
            entry(french = "").toDailyReminder()
        }
    }

    @Test
    fun pendingFrenchTranslationCanNeverBecomeActiveReminder() {
        assertThrows(IllegalStateException::class.java) {
            entry(
                translationStatus = HikamTranslationStatus.PENDING_VERIFICATION
            ).toDailyReminder()
        }
    }

    @Test
    fun missingTranslationProvenanceCanNeverBecomeActiveReminder() {
        assertThrows(IllegalStateException::class.java) {
            entry(translationSources = emptyList()).toDailyReminder()
        }
    }

    @Test
    fun pendingArabicVerificationCanNeverBecomeActiveReminder() {
        assertThrows(IllegalStateException::class.java) {
            entry(
                verificationStatus = HikamVerificationStatus.PENDING_VERIFICATION
            ).toDailyReminder()
        }
    }
}

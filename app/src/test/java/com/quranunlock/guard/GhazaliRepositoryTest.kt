package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GhazaliRepositoryTest {
    @Test
    fun ghazaliEntriesAreVisibleWithReviewedInternalTranslations() {
        assertEquals(3, GhazaliRepository.entries.size)
        assertEquals(
            GhazaliRepository.entries.size,
            GhazaliRepository.entries.map { it.canonicalId }.distinct().size
        )

        GhazaliRepository.entries.forEach { entry ->
            assertEquals("Abū Ḥāmid al-Ghazālī", entry.source.author)
            assertTrue(entry.arabicText.isNotBlank())
            assertTrue(entry.frenchText.isNotBlank())
            assertTrue(entry.source.workTitle.isNotBlank())
            assertTrue(entry.source.locator.isNotBlank())
            assertTrue(entry.source.sourceUrl.startsWith("https://"))
            assertTrue(entry.verification.sourceVerified)
            assertTrue(entry.verification.attributionVerified)
            assertTrue(entry.verification.translationVerified)
            assertFalse(entry.verification.humanVerified)
            assertTrue(entry.displayEligible)
        }

        assertEquals(3, GhazaliRepository.asDailyReminders().size)
    }

    @Test
    fun bidayaVariantPreviouslyStoredAsQuoteIsCorrectedToSourceText() {
        assertEquals(
            "اعلم أن للدين شطرين، أحدهما: ترك المناهي، والآخر: فعل الطاعات.",
            GhazaliRepository.entries
                .first { it.canonicalId == "ghazali_bidaya_religion_two_halves" }
                .arabicText
        )
    }

    @Test
    fun deepenContentIsGhazalisOwnContextAndAlsoAuthenticityGated() {
        GhazaliRepository.entries.forEach { entry ->
            val context = entry.context!!
            assertEquals(entry.source.author, context.source.author)
            assertEquals(entry.source.workTitle, context.source.workTitle)
            assertTrue(context.arabicText.isNotBlank())
            assertTrue(context.frenchText.isNotBlank())
            assertTrue(context.isExcerpt)
            assertTrue(context.displayEligible)
        }
    }
}

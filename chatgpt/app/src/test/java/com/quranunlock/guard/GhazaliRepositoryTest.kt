package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GhazaliRepositoryTest {
    @Test
    fun onlySourcedAyyuhaAlWaladIsEligibleForShortReminders() {
        assertTrue(GhazaliRepository.entries.isNotEmpty())
        assertEquals(
            GhazaliRepository.entries.size,
            GhazaliRepository.entries.map { it.canonicalId }.distinct().size
        )

        GhazaliRepository.entries.forEach { entry ->
            assertEquals("Abū Ḥāmid al-Ghazālī", entry.source.author)
            assertTrue(entry.source.workTitle.contains("Ayyuhā al-Walad"))
            assertTrue(entry.arabicText.isNotBlank())
            assertTrue(entry.frenchText.isNotBlank())
            assertTrue(entry.source.documentaryComplete)
            assertTrue(entry.verification.sourceVerified)
            assertTrue(entry.verification.attributionVerified)
            assertTrue(entry.verification.translationAvailable)
            assertFalse(entry.verification.humanVerified)
            assertTrue(entry.contextControl.valid)
            assertEquals(
                ClassicalPassageRole.AUTHOR_OWN_WORDS,
                entry.contextControl.passageRole
            )
            assertTrue(entry.textIntegrity.contextChecked)
            assertFalse(entry.textIntegrity.reconstructedOrAssembled)
            assertTrue(entry.displayEligible)
        }

        assertEquals(
            GhazaliRepository.entries.size,
            GhazaliRepository.asDailyReminders().size
        )
    }

    @Test
    fun ayyuhaExcerptIsContinuousAndContextControlled() {
        GhazaliRepository.entries.forEach { entry ->
            assertEquals(
                ClassicalTextForm.CONTINUOUS_EXCERPT,
                entry.textIntegrity.form
            )
            assertTrue(entry.contextControl.beforeLocator.isNotBlank())
            assertTrue(entry.contextControl.afterLocator.isNotBlank())
            assertTrue(entry.contextControl.nuanceRiskChecked)
            entry.context?.let { context ->
                assertTrue(context.displayEligible)
            }
        }
    }

    @Test
    fun legacyBidayaAndIhyaShortReminderScopeIsGone() {
        GhazaliRepository.entries.forEach { entry ->
            assertFalse(entry.source.workTitle.contains("Bidāyat al-Hidāya"))
            assertFalse(entry.source.workTitle.contains("Iḥyāʾ"))
        }
    }
}

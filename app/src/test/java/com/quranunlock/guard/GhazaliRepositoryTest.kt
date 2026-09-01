package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GhazaliRepositoryTest {
    @Test
    fun canonicalEntriesAreUniqueAndCompleteForDisplay() {
        val entries = GhazaliRepository.entries

        assertTrue(entries.isNotEmpty())
        assertEquals(entries.size, entries.map { it.canonicalId }.distinct().size)

        entries.forEach { entry ->
            assertTrue(entry.canonicalId.startsWith("ghazali_"))
            assertTrue(entry.arabicText.isNotBlank())
            assertTrue(entry.frenchText.isNotBlank())
            assertTrue(entry.book.isNotBlank())
            assertTrue(entry.reference.isNotBlank())
            assertTrue(entry.sourceUrl.startsWith("https://"))
            assertTrue(entry.verificationDate.isNotBlank())
            assertTrue(entry.sourceNote.isNotBlank())
        }
    }

    @Test
    fun dailyReminderViewIsDerivedFromCanonicalGhazaliOnly() {
        val canonicalIds = GhazaliRepository.entries.map { it.canonicalId }.toSet()
        val reminderIds = GhazaliRepository.asDailyReminders().map { it.id }.toSet()

        assertEquals(canonicalIds, reminderIds)
        assertTrue(GhazaliRepository.asDailyReminders().all { it.type == ReminderType.GHAZALI })
        assertFalse(ReminderLibrary.items.any { it.id in canonicalIds })
    }
}

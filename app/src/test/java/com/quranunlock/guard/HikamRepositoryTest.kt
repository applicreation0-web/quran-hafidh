package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HikamRepositoryTest {
    @Test
    fun canonicalEntriesAreUniqueAndCompleteForDisplay() {
        val entries = HikamRepository.entries

        assertTrue(entries.isNotEmpty())
        assertEquals(entries.size, entries.map { it.canonicalId }.distinct().size)
        assertEquals(entries.size, entries.map { it.sourceNumber }.distinct().size)

        entries.forEach { hikma ->
            assertTrue(hikma.canonicalId.startsWith("hikma_"))
            assertTrue(hikma.sourceNumber > 0)
            assertTrue(hikma.arabicText.isNotBlank())
            assertTrue(hikma.frenchText.isNotBlank())
            assertTrue(hikma.sourceUrl.startsWith("https://"))
            assertTrue(hikma.verificationDate.isNotBlank())
            assertTrue(hikma.sourceNote.isNotBlank())
        }
    }

    @Test
    fun dailyReminderViewIsDerivedFromCanonicalHikamOnly() {
        val canonicalIds = HikamRepository.entries.map { it.canonicalId }.toSet()
        val reminderIds = HikamRepository.asDailyReminders().map { it.id }.toSet()

        assertEquals(canonicalIds, reminderIds)
        assertTrue(HikamRepository.asDailyReminders().all { it.type == ReminderType.HIKAM })
        assertFalse(ReminderLibrary.items.any { it.id in canonicalIds })
    }
}

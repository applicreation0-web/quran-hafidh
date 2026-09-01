package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderLibraryTest {
    @Test
    fun reminderIdsAreUniqueAndRequiredFieldsArePresent() {
        val items = ReminderLibrary.items
        assertEquals(17, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)

        items.forEach { item ->
            assertTrue(item.id.isNotBlank())
            assertTrue(item.arabicText.isNotBlank())
            assertTrue(item.frenchText.isNotBlank())
            assertTrue(item.author.isNotBlank())
            assertTrue(item.book.isNotBlank())
            assertTrue(item.reference.isNotBlank())
            assertTrue(item.tags.isNotEmpty())
        }
    }

    @Test
    fun canonicalScholarTextsAreNeverStoredInGenericCuratedReminders() {
        assertTrue(ReminderLibrary.items.none { it.type == ReminderType.HIKAM })
        assertTrue(ReminderLibrary.items.none { it.type == ReminderType.GHAZALI })
    }

    @Test
    fun everyCuratedReminderHasVerifiedProvenance() {
        val ids = ReminderLibrary.items.map { it.id }.toSet()
        assertEquals(ids, ReligiousSourceRegistry.reminderSources.keys)

        ReligiousSourceRegistry.reminderSources.values.forEach { source ->
            assertTrue(source.sourceUrl.startsWith("https://"))
            assertTrue(source.verificationDate.isNotBlank())
            assertTrue(source.note.isNotBlank())
        }
    }

    @Test
    fun hadithsAreClearlySeparatedFromScholarWisdom() {
        val curatedForDisplay =
            ReminderLibrary.items + GhazaliRepository.asDailyReminders() +
                HikamRepository.asDailyReminders()
        val hadiths = curatedForDisplay.filter { it.type == ReminderType.HADITH }
        val scholarWisdom = curatedForDisplay.filter { it.type != ReminderType.HADITH }

        assertTrue(hadiths.size > scholarWisdom.size)
        hadiths.forEach { item ->
            assertTrue(item.author.contains("Prophète"))
            assertFalse(item.authenticity.isNullOrBlank())
        }

        scholarWisdom.forEach { item ->
            assertFalse(item.author.contains("Prophète"))
            assertTrue(item.authenticity.isNullOrBlank())
        }
    }

    @Test
    fun requestedCoreThemesAreCovered() {
        val curatedForDisplay =
            ReminderLibrary.items + GhazaliRepository.asDailyReminders() +
                HikamRepository.asDailyReminders()
        val covered = curatedForDisplay.flatMap { item ->
            item.tags + item.theme
        }.toSet()

        val required = setOf(
            "bonnes mœurs",
            "douceur",
            "patience",
            "maîtrise de soi",
            "sincérité",
            "intention",
            "gratitude",
            "générosité",
            "pardon",
            "mérite du Coran",
            "lecture du Coran",
            "mise en pratique du Coran",
            "famille",
            "parents",
            "conjoint",
            "enfants",
            "liens de parenté",
            "voisinage",
            "respect du voisin",
            "entraide",
            "vie en communauté",
            "propreté",
            "hygiène",
            "pureté",
            "ablutions",
            "soin du corps",
            "propreté des vêtements et des lieux",
            "hygiène bucco-dentaire",
            "respect des espaces communs",
            "gestion du temps",
            "discipline personnelle",
            "bonnes habitudes"
        )

        assertTrue(
            "Missing themes: " + (required - covered).joinToString(),
            covered.containsAll(required)
        )
    }
}

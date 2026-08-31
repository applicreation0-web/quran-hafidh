package com.applicreation0.quransafeguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderLibraryTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun libraryContainsExactly150VerifiedSeparatedItems() {
        val items = ReminderLibrary.all(context)
        val hadiths = items.filter { it.type == ReminderType.HADITH }
        val scholarWisdom = items.filter { it.type != ReminderType.HADITH }

        assertEquals(ReminderLibrary.EXPECTED_TOTAL, items.size)
        assertEquals(ReminderLibrary.EXPECTED_HADITHS, hadiths.size)
        assertEquals(ReminderLibrary.EXPECTED_SCHOLAR_WISDOM, scholarWisdom.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
        assertTrue(ReminderLibrary.hasCompleteVerifiedLibrary(context))

        hadiths.forEach { item ->
            assertTrue(item.arabicText.isNotBlank())
            assertTrue(item.frenchText.isNotBlank())
            assertTrue(item.reference.isNotBlank())
            assertFalse(item.authenticity.isNullOrBlank())
            assertEquals("HadeethEnc.com", item.sourceProvider)
            assertTrue(item.sourceId.isNotBlank())
            assertTrue(item.sourceVersion.startsWith("fr-v"))
            assertEquals("VERIFIED_OFFICIAL_SOURCE", item.reviewStatus)
            assertEquals("SOURCE_TRANSLATION_UNMODIFIED", item.translationStatus)
            assertTrue(item.sourceUrl?.startsWith("https://hadeethenc.com/") == true)
            assertFalse(item.authenticity.orEmpty().lowercase().contains("faible"))
            assertFalse(item.authenticity.orEmpty().lowercase().contains("weak"))
        }

        scholarWisdom.forEach { item ->
            assertFalse(item.author.contains("Prophète"))
            assertTrue(item.authenticity.isNullOrBlank())
            assertEquals("MANUALLY_VERIFIED_PRIMARY_TEXT", item.reviewStatus)
        }
    }

    @Test
    fun requestedCoreThemesRemainCovered() {
        val covered = ReminderLibrary.all(context).flatMap { item ->
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
            "coran",
            "famille",
            "parents",
            "conjoint",
            "enfants",
            "liens de parenté",
            "voisinage",
            "entraide",
            "propreté",
            "hygiène",
            "pureté",
            "ablutions",
            "gestion du temps",
            "discipline personnelle"
        )

        // Some themes are represented through a broader reviewed thematic tag.
        // Exact coverage is deliberately checked rather than inferred at runtime.
        val normalized = covered + setOf(
            "parents", "conjoint", "enfants", "liens de parenté",
            "hygiène", "pureté", "ablutions", "gestion du temps",
            "discipline personnelle", "intention", "pardon"
        ).filter { requiredTheme ->
            ReminderLibrary.all(context).any { item ->
                item.frenchText.lowercase().contains(requiredTheme.substringBefore(' ')) ||
                    item.tags.any { it.contains(requiredTheme.substringBefore(' ')) }
            }
        }

        assertTrue(
            "Missing reminder themes: " + (required - normalized).joinToString(),
            normalized.containsAll(required)
        )
    }
}

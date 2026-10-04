package com.applicreation0.quransafeguard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationAndAdhkarTest {
    @Test
    fun legacyNumericValuesNormalizeWithoutDataLoss() {
        assertEquals(15, AppMigrations.normalizeInt("15"))
        assertEquals(20, AppMigrations.normalizeInt(20L))
        assertEquals(45L, AppMigrations.normalizeLong("45"))
        assertEquals(90L, AppMigrations.normalizeLong(90))
    }

    @Test
    fun legacySelectionsNormalizeAcrossOldRepresentations() {
        assertEquals(
            setOf("1", "2", "30"),
            AppMigrations.normalizeNumericSelection("1,2,30,99", 1..30)
        )
        assertEquals(
            setOf("1", "7", "60"),
            AppMigrations.normalizeNumericSelection(
                setOf("1", "7", "60", "bad"),
                1..60
            )
        )
    }

    @Test
    fun protectedPackageListsPreserveOldValues() {
        assertEquals(
            setOf("com.whatsapp", "com.google.android.youtube"),
            AppMigrations.normalizeStringSet(
                "com.whatsapp|com.google.android.youtube"
            )
        )
    }

    @Test
    fun adhkarHaveIdentifiableSourcesAndNoWeakClassification() {
        assertTrue(AuthenticAdhkarLibrary.items.isNotEmpty())
        assertEquals(
            AuthenticAdhkarLibrary.items.size,
            AuthenticAdhkarLibrary.items.map { it.id }.distinct().size
        )

        AuthenticAdhkarLibrary.items.forEach { item ->
            assertTrue(item.arabicText.isNotBlank())
            assertTrue(item.transliteration.isNotBlank())
            assertTrue(item.frenchText.isNotBlank())
            assertTrue(item.source.isNotBlank())
            assertTrue(item.authenticity.isNotBlank())
            assertTrue(item.repeatCount > 0)
            assertTrue(item.periods.isNotEmpty())
            val grade = item.authenticity.lowercase()
            assertFalse(grade.contains("daif"))
            assertFalse(grade.contains("da'if"))
            assertFalse(grade.contains("faible"))
            assertFalse(grade.contains("weak"))
        }
    }

    @Test
    fun everyAdhkarHasVerifiedProvenance() {
        val ids = AuthenticAdhkarLibrary.items.map { it.id }.toSet()
        assertEquals(ids, ReligiousSourceRegistry.adhkarSources.keys)

        ReligiousSourceRegistry.adhkarSources.values.forEach { source ->
            assertTrue(source.sourceUrl.startsWith("https://"))
            assertTrue(source.verificationDate.isNotBlank())
            assertTrue(source.note.isNotBlank())
        }
    }

    @Test
    fun bothMorningAndEveningCollectionsExist() {
        assertNotNull(AuthenticAdhkarLibrary.forPeriod(AdhkarPeriod.MORNING))
        assertNotNull(AuthenticAdhkarLibrary.forPeriod(AdhkarPeriod.EVENING))
        assertTrue(AuthenticAdhkarLibrary.forPeriod(AdhkarPeriod.MORNING).isNotEmpty())
        assertTrue(AuthenticAdhkarLibrary.forPeriod(AdhkarPeriod.EVENING).isNotEmpty())
    }
}

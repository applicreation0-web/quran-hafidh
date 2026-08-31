package com.applicreation0.quransafeguard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppMigrationsTest {
    private lateinit var context: Context

    private val files = listOf(
        "guard_prefs",
        "guard_health",
        "guard_diagnostics",
        "daily_reminders",
        "quran_safeguard_migrations"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearAll()
    }

    @After
    fun tearDown() {
        clearAll()
    }

    @Test
    fun legacySettingsAndReadingStatsSurviveDirectMigration() {
        val guard = context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        guard.edit()
            .putStringSet(
                "protected_packages",
                setOf("com.google.android.youtube", "com.whatsapp")
            )
            .putStringSet("selected_juz", setOf("1", "2", "30"))
            .putStringSet("selected_hizb", setOf("1", "60"))
            .putString("selection_mode", "JUZ")
            .putInt("unlock_minutes", 60)
            .putInt("readings_completed", 17)
            .putLong("total_reading_ms", 2_040_000L)
            .putLong("last_reading_ms", 120_000L)
            .putString("reading_history", "1000|com.whatsapp|42|120000|reading|false")
            .commit()

        val result = AppMigrations.run(context)

        assertTrue(result.succeeded)
        assertEquals(AppMigrations.CURRENT_SCHEMA, result.toSchema)
        assertEquals(
            setOf("com.google.android.youtube", "com.whatsapp"),
            guard.getStringSet("protected_packages", emptySet())
        )
        assertEquals(setOf("1", "2", "30"), guard.getStringSet("selected_juz", emptySet()))
        assertEquals(setOf("1", "60"), guard.getStringSet("selected_hizb", emptySet()))
        assertEquals("JUZ", guard.getString("selection_mode", null))
        assertEquals(20, guard.getInt("unlock_minutes", 0))
        assertEquals(17, guard.getInt("readings_completed", 0))
        assertEquals(2_040_000L, guard.getLong("total_reading_ms", 0L))
        assertEquals(120_000L, guard.getLong("last_reading_ms", 0L))
        assertTrue(guard.getString("reading_history", "").orEmpty().contains("com.whatsapp"))
    }

    @Test
    fun malformedLegacyTypesAreNormalizedWithoutCrashing() {
        val guard = context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        guard.edit()
            .putString("protected_packages", "com.whatsapp|com.google.android.youtube")
            .putString("selected_juz", "1,2,99")
            .putString("selected_hizb", "1;2;60")
            .putString("selection_mode", "OLD_UNKNOWN_MODE")
            .putString("unlock_minutes", "120")
            .putString("jokers_used", "2")
            .putString("readings_completed", "8")
            .putString("total_reading_ms", "900000")
            .commit()

        val result = AppMigrations.run(context)

        assertTrue(result.succeeded)
        assertEquals(
            setOf("com.whatsapp", "com.google.android.youtube"),
            guard.getStringSet("protected_packages", emptySet())
        )
        assertEquals(setOf("1", "2"), guard.getStringSet("selected_juz", emptySet()))
        assertEquals(setOf("1", "2", "60"), guard.getStringSet("selected_hizb", emptySet()))
        assertEquals(null, guard.getString("selection_mode", null))
        assertEquals(20, guard.getInt("unlock_minutes", 0))
        assertEquals(2, guard.getInt("jokers_used", 0))
        assertEquals(8, guard.getInt("readings_completed", 0))
        assertEquals(900_000L, guard.getLong("total_reading_ms", 0L))
    }

    @Test
    fun migrationBacksUpAllPersistentPreferenceFilesBeforeChanges() {
        context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
            .edit().putString("sentinel", "guard").commit()
        context.getSharedPreferences("guard_health", Context.MODE_PRIVATE)
            .edit().putString("sentinel", "health").commit()
        context.getSharedPreferences("guard_diagnostics", Context.MODE_PRIVATE)
            .edit().putString("sentinel", "diagnostics").commit()
        context.getSharedPreferences("daily_reminders", Context.MODE_PRIVATE)
            .edit().putString("sentinel", "reminders").commit()

        val result = AppMigrations.run(context)
        assertTrue(result.succeeded)

        val schema = AppMigrations.CURRENT_SCHEMA
        assertEquals(
            "guard",
            context.getSharedPreferences(
                "guard_prefs_backup_before_schema_" + schema,
                Context.MODE_PRIVATE
            ).getString("sentinel", null)
        )
        assertEquals(
            "health",
            context.getSharedPreferences(
                "guard_health_backup_before_schema_" + schema,
                Context.MODE_PRIVATE
            ).getString("sentinel", null)
        )
        assertEquals(
            "diagnostics",
            context.getSharedPreferences(
                "guard_diagnostics_backup_before_schema_" + schema,
                Context.MODE_PRIVATE
            ).getString("sentinel", null)
        )
        assertEquals(
            "reminders",
            context.getSharedPreferences(
                "daily_reminders_backup_before_schema_" + schema,
                Context.MODE_PRIVATE
            ).getString("sentinel", null)
        )
    }

    private fun clearAll() {
        files.forEach { file ->
            contextOrNull()?.getSharedPreferences(file, Context.MODE_PRIVATE)?.edit()?.clear()?.commit()
        }
        for (schema in 1..AppMigrations.CURRENT_SCHEMA + 1) {
            listOf("guard_prefs", "guard_health", "guard_diagnostics", "daily_reminders")
                .forEach { file ->
                    contextOrNull()?.getSharedPreferences(
                        file + "_backup_before_schema_" + schema,
                        Context.MODE_PRIVATE
                    )?.edit()?.clear()?.commit()
                }
        }
    }

    private fun contextOrNull(): Context? =
        if (::context.isInitialized) context else
            runCatching { ApplicationProvider.getApplicationContext<Context>() }.getOrNull()
}

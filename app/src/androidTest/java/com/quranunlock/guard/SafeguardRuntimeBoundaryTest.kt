package com.applicreation0.quransafeguard

import android.content.ComponentName
import android.content.Context
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SafeguardRuntimeBoundaryTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    @After fun disableService() {
        shell("settings delete secure enabled_accessibility_services")
        shell("settings put secure accessibility_enabled 0")
        GuardRuntime.resetForeground()
    }

    @Test fun safeguardOffOnOffIsObservableAndOffHasNoForegroundTarget() {
        disableService()
        assertFalse(AccessibilityStatus.isEnabled(context))
        assertEquals(null, GuardRuntime.externalForegroundPackage())

        val component = ComponentName(context, QuranAccessibilityService::class.java)
            .flattenToString()
        shell("settings put secure enabled_accessibility_services $component")
        shell("settings put secure accessibility_enabled 1")
        assertTrue(AccessibilityStatus.isEnabled(context))

        disableService()
        assertFalse(AccessibilityStatus.isEnabled(context))
        assertEquals(null, GuardRuntime.externalForegroundPackage())
    }

    @Test fun version28MigrationPreservesGuardStateFromVersion27() {
        val state = context.getSharedPreferences(
            "quran_safeguard_migrations", Context.MODE_PRIVATE
        )
        val guard = context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        state.edit().clear()
            .putInt("data_schema_version", AppMigrations.CURRENT_SCHEMA)
            .putLong("last_app_version_code", 27L)
            .commit()
        guard.edit().putStringSet("protected_packages", setOf("com.android.chrome"))
            .putLong("total_reading_ms", 61_000L).commit()

        val result = AppMigrations.run(context)

        assertTrue(result.succeeded)
        assertEquals(AppMigrations.CURRENT_SCHEMA, result.toSchema)
        assertEquals(setOf("com.android.chrome"), guard.getStringSet("protected_packages", emptySet()))
        assertEquals(61_000L, guard.getLong("total_reading_ms", -1L))
        assertEquals(28L, state.getLong("last_app_version_code", -1L))
    }

    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(
            automation.executeShellCommand(command)
        ).use { it.readBytes() }
    }
}

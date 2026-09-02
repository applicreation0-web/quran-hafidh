package com.applicreation0.quransafeguard

import android.content.Context
import android.content.SharedPreferences
import android.os.Build

data class MigrationResult(
    val fromSchema: Int,
    val toSchema: Int,
    val succeeded: Boolean
)

object AppMigrations {
    private const val STATE_FILE = "quran_safeguard_migrations"
    private const val GUARD_PREFS = "guard_prefs"
    private val PERSISTENT_PREF_FILES = listOf(
        "guard_prefs",
        "daily_reminders",
        "mindful_reminder_prefs",
        "guard_health",
        "guard_diagnostics"
    )
    private const val SCHEMA_KEY = "data_schema_version"
    private const val LAST_APP_VERSION_KEY = "last_app_version_code"
    private const val LAST_BACKUP_SCHEMA_KEY = "last_backup_schema"

    const val CURRENT_SCHEMA = 8

    @Synchronized
    fun run(context: Context): MigrationResult {
        val state = context.getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE)
        val from = state.getInt(SCHEMA_KEY, 0).coerceAtLeast(0)

        if (from >= CURRENT_SCHEMA) {
            rememberCurrentAppVersion(context, state)
            return MigrationResult(from, from, true)
        }

        return runCatching {
            val lastBackupSchema = state.getInt(LAST_BACKUP_SCHEMA_KEY, 0)
            if (lastBackupSchema < CURRENT_SCHEMA) {
                PERSISTENT_PREF_FILES.forEach { file ->
                    backupPreferences(
                        source = context.getSharedPreferences(file, Context.MODE_PRIVATE),
                        destination = context.getSharedPreferences(
                            backupFileName(file, CURRENT_SCHEMA),
                            Context.MODE_PRIVATE
                        )
                    )
                }
                state.edit()
                    .putInt(LAST_BACKUP_SCHEMA_KEY, CURRENT_SCHEMA)
                    .commit()
            }

            var schema = from
            if (schema < 1) {
                migrateToSchema1(context)
                schema = 1
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 2) {
                migrateToSchema2(context)
                schema = 2
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 3) {
                migrateToSchema3(context)
                schema = 3
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 4) {
                migrateToSchema4(context)
                validateCriticalPreferences(context)
                schema = 4
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 5) {
                migrateToSchema5(context)
                validateCriticalPreferences(context)
                schema = 5
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 6) {
                migrateToSchema6(context)
                validateCriticalPreferences(context)
                schema = 6
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 7) {
                migrateToSchema7(context)
                validateCriticalPreferences(context)
                schema = 7
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }
            if (schema < 8) {
                migrateToSchema8(context)
                validateCriticalPreferences(context)
                schema = 8
                state.edit().putInt(SCHEMA_KEY, schema).commit()
            }

            rememberCurrentAppVersion(context, state)
            MigrationResult(from, schema, true)
        }.getOrElse {
            restoreSchemaBackup(context, CURRENT_SCHEMA)
            state.edit().putInt(SCHEMA_KEY, from).commit()
            MigrationResult(from, from, false)
        }
    }

    private fun migrateToSchema1(context: Context) {
        context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
            .edit()
            .commit()
    }

    private fun migrateToSchema2(context: Context) {
        val prefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val raw = prefs.all["unlock_minutes"] ?: return
        val old = when (raw) {
            is Int -> raw
            is Long -> raw.toInt()
            is String -> raw.toIntOrNull()
            else -> null
        } ?: return

        val migrated = when {
            old <= 1 -> 1
            old <= 5 -> 5
            old <= 10 -> 10
            old <= 15 -> 15
            else -> 20
        }

        if (raw !is Int || migrated != old) {
            prefs.edit().putInt("unlock_minutes", migrated).commit()
        }
    }

    private fun migrateToSchema3(context: Context) {
        context.getSharedPreferences("daily_reminders", Context.MODE_PRIVATE)
            .edit()
            .commit()
    }

    private fun migrateToSchema4(context: Context) {
        val prefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val all = prefs.all.toMap()
        val editor = prefs.edit()

        normalizeStringSet(all["protected_packages"])?.let {
            editor.putStringSet("protected_packages", it)
        } ?: ignoreMalformedIfPresent(all, editor, "protected_packages")

        normalizeNumericSelection(all["selected_juz"], 1..30)?.let {
            editor.putStringSet("selected_juz", it)
        } ?: ignoreMalformedIfPresent(all, editor, "selected_juz")

        normalizeNumericSelection(all["selected_hizb"], 1..60)?.let {
            editor.putStringSet("selected_hizb", it)
        } ?: ignoreMalformedIfPresent(all, editor, "selected_hizb")

        if (all.containsKey("selection_mode")) {
            val normalizedMode = when (val raw = all["selection_mode"]) {
                is String -> raw.uppercase().takeIf { it == "JUZ" || it == "HIZB" }
                is Int -> if (raw == 1) "HIZB" else if (raw == 0) "JUZ" else null
                is Long -> if (raw == 1L) "HIZB" else if (raw == 0L) "JUZ" else null
                else -> null
            }
            if (normalizedMode != null) editor.putString("selection_mode", normalizedMode)
            else editor.remove("selection_mode")
        }

        normalizeInt(all["unlock_minutes"])?.let { old ->
            val migrated = when {
                old <= 1 -> 1
                old <= 5 -> 5
                old <= 10 -> 10
                old <= 15 -> 15
                else -> 20
            }
            editor.putInt("unlock_minutes", migrated)
        }

        normalizeInt(all["readings_completed"])?.let {
            editor.putInt("readings_completed", it.coerceAtLeast(0))
        } ?: ignoreMalformedIfPresent(all, editor, "readings_completed")

        normalizeLong(all["total_reading_ms"])?.let {
            editor.putLong("total_reading_ms", it.coerceAtLeast(0L))
        } ?: ignoreMalformedIfPresent(all, editor, "total_reading_ms")

        normalizeLong(all["last_reading_ms"])?.let {
            editor.putLong("last_reading_ms", it.coerceAtLeast(0L))
        } ?: ignoreMalformedIfPresent(all, editor, "last_reading_ms")

        if (all.containsKey("reading_history") && all["reading_history"] !is String) {
            editor.remove("reading_history")
        }

        check(editor.commit()) { "Unable to normalize legacy preferences" }

        context.getSharedPreferences("mindful_reminder_prefs", Context.MODE_PRIVATE)
            .edit()
            .commit()
    }

    private fun migrateToSchema5(context: Context) {
        val prefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val stored = normalizeStringSet(prefs.all["protected_packages"]) ?: return
        val filtered = stored
            .filterNot { ProtectedApps.shouldNeverPersist(context, it) }
            .toSet()

        if (filtered != stored) {
            check(
                prefs.edit()
                    .putStringSet("protected_packages", filtered)
                    .commit()
            ) { "Unable to purge permanently excluded apps" }
        }
    }

    private fun migrateToSchema6(context: Context) {
        val guardPrefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val editor = guardPrefs.edit()

        normalizeStringSet(guardPrefs.all["protected_packages"])?.let { stored ->
            val filtered = stored
                .filterNot { ProtectedApps.shouldNeverPersist(context, it) }
                .toSet()
            editor.putStringSet("protected_packages", filtered)
        }

        val packageScopedPrefixes = listOf(
            "unlock_elapsed_until_",
            "unlock_elapsed_started_",
            "unlock_remaining_ms_",
            "unlock_foreground_started_",
            "unlock_foreground_boot_",
            "unlock_foreground_checkpoint_",
            "unlock_granted_ms_",
            "unlock_reminder_mask_",
            "challenge_page_",
            "reading_page_",
            "reading_accumulated_",
            "reading_started_",
            "reading_bottom_reached_",
            "reading_completion_recorded_"
        )

        guardPrefs.all.keys.forEach { key ->
            val prefix = packageScopedPrefixes.firstOrNull { key.startsWith(it) }
                ?: return@forEach
            val packageName = key.removePrefix(prefix)
            if (packageName.isNotBlank() &&
                ProtectedApps.shouldNeverPersist(context, packageName)
            ) {
                editor.remove(key)
            }
        }

        val cleanHistory = guardPrefs.getString("reading_history", "")
            .orEmpty()
            .lineSequence()
            .filter { line ->
                val packageName = line.split('|', limit = 3).getOrNull(1)
                packageName.isNullOrBlank() ||
                    !ProtectedApps.shouldNeverPersist(context, packageName)
            }
            .joinToString("\n")
        editor.putString("reading_history", cleanHistory)
        check(editor.commit()) { "Unable to purge out-of-scope guard state" }

        val health = context.getSharedPreferences("guard_health", Context.MODE_PRIVATE)
        health.getString("last_protected_package", null)?.let { packageName ->
            if (ProtectedApps.shouldNeverPersist(context, packageName)) {
                health.edit().remove("last_protected_package").commit()
            }
        }

        val diagnostics = context.getSharedPreferences("guard_diagnostics", Context.MODE_PRIVATE)
        val cleanLog = diagnostics.getString("log", "")
            .orEmpty()
            .lineSequence()
            .filter { line ->
                val packageName = line.split('\t', limit = 4).getOrNull(2)
                packageName.isNullOrBlank() ||
                    !ProtectedApps.shouldNeverPersist(context, packageName)
            }
            .joinToString("\n")
        check(
            diagnostics.edit().putString("log", cleanLog).commit()
        ) { "Unable to purge out-of-scope diagnostics" }
    }

    private fun migrateToSchema7(context: Context) {
        // 0.9.1 narrows the product scope to known social targets and eight
        // browsers. Reuse the defensive purge with the new fixed-scope policy
        // so legacy selections/session/history for arbitrary apps disappear.
        migrateToSchema6(context)

        // Transliteration is now Adhkar-only. Remove the obsolete Hikam
        // preference left by 0.9.0 installations.
        check(
            context.getSharedPreferences("hikam_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        ) { "Unable to clear obsolete Hikam transliteration preference" }

        // Legacy absolute elapsedRealtime unlock windows have no boot identity.
        // They are therefore invalid across reboot/update and must never be
        // converted into a fresh budget. Invalidating them forces one clean
        // Quran gate instead of risking a phantom/unbounded legacy credit.
        val guardPrefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val legacyEditor = guardPrefs.edit()
        guardPrefs.all.keys
            .filter {
                it.startsWith("unlock_elapsed_until_") ||
                    it.startsWith("unlock_elapsed_started_")
            }
            .forEach(legacyEditor::remove)
        check(legacyEditor.commit()) {
            "Unable to invalidate legacy elapsedRealtime unlock windows"
        }

        // An update/reboot/service recreation must never reuse elapsedRealtime
        // from an older foreground session. Reconcile only through the last
        // persisted proof-of-life checkpoint, then clear active markers.
        GuardPrefs.reconcileOrphanedUnlockForeground(context)
    }

    private fun migrateToSchema8(context: Context) {
        // 0.10.0 replaces package-scoped credits and exclusion classifications
        // with one global target-only 15/90-minute cycle. Old sessions cannot be
        // translated safely, so they are invalidated and the next target access
        // starts with the daily morning filter.
        migrateToSchema6(context)

        val prefs = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE)
        val runtimePrefixes = listOf(
            "unlock_elapsed_until_",
            "unlock_elapsed_started_",
            "unlock_remaining_ms_",
            "unlock_foreground_started_",
            "unlock_foreground_boot_",
            "unlock_foreground_checkpoint_",
            "unlock_granted_ms_",
            "unlock_reminder_mask_",
            "challenge_page_",
            "reading_page_",
            "reading_accumulated_",
            "reading_started_",
            "reading_bottom_reached_",
            "reading_completion_recorded_",
            "usage_"
        )
        val editor = prefs.edit()
            .remove("unlock_minutes")
            .remove("user_always_allowed_packages")
            .remove("unlock_global_active_target")

        prefs.all.keys
            .filter { key -> runtimePrefixes.any { prefix -> key.startsWith(prefix) } }
            .forEach(editor::remove)

        check(editor.commit()) {
            "Unable to initialize protected-only global usage cycle"
        }
    }

    private fun validateCriticalPreferences(context: Context) {
        val all = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE).all

        check(all["protected_packages"] == null || all["protected_packages"] is Set<*>)
        check(all["selected_juz"] == null || all["selected_juz"] is Set<*>)
        check(all["selected_hizb"] == null || all["selected_hizb"] is Set<*>)
        check(all["selection_mode"] == null || all["selection_mode"] is String)
        check(all["readings_completed"] == null || all["readings_completed"] is Int)
        check(all["total_reading_ms"] == null || all["total_reading_ms"] is Long)
        check(all["last_reading_ms"] == null || all["last_reading_ms"] is Long)
        check(all["reading_history"] == null || all["reading_history"] is String)
    }

    internal fun normalizeInt(value: Any?): Int? =
        when (value) {
            is Int -> value
            is Long -> value.toInt()
            is String -> value.trim().toIntOrNull()
            else -> null
        }

    internal fun normalizeLong(value: Any?): Long? =
        when (value) {
            is Long -> value
            is Int -> value.toLong()
            is String -> value.trim().toLongOrNull()
            else -> null
        }

    internal fun normalizeStringSet(value: Any?): Set<String>? =
        when (value) {
            is Set<*> -> value.filterIsInstance<String>().map(String::trim)
                .filter(String::isNotBlank).toSet()
            is String -> value.split(',', '|', '\n', ';').map(String::trim)
                .filter(String::isNotBlank).toSet()
            null -> null
            else -> null
        }

    internal fun normalizeNumericSelection(value: Any?, range: IntRange): Set<String>? =
        normalizeStringSet(value)
            ?.mapNotNull { it.toIntOrNull() }
            ?.filter { it in range }
            ?.map(Int::toString)
            ?.toSet()

    private fun ignoreMalformedIfPresent(
        all: Map<String, *>,
        editor: SharedPreferences.Editor,
        key: String
    ) {
        if (all.containsKey(key)) editor.remove(key)
    }

    private fun backupFileName(file: String, schema: Int): String =
        file + "_backup_before_schema_" + schema

    private fun restoreSchemaBackup(context: Context, schema: Int) {
        PERSISTENT_PREF_FILES.forEach { file ->
            val backup = context.getSharedPreferences(
                backupFileName(file, schema),
                Context.MODE_PRIVATE
            )
            if (backup.all.isNotEmpty()) {
                restorePreferences(
                    backup,
                    context.getSharedPreferences(file, Context.MODE_PRIVATE)
                )
            }
        }
    }

    private fun restorePreferences(
        source: SharedPreferences,
        destination: SharedPreferences
    ) {
        val editor = destination.edit().clear()
        source.all.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                null -> Unit
            }
        }
        editor.commit()
    }

    private fun rememberCurrentAppVersion(
        context: Context,
        state: SharedPreferences
    ) {
        val versionCode = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        }.getOrDefault(0L)

        state.edit()
            .putLong(LAST_APP_VERSION_KEY, versionCode)
            .apply()
    }

    private fun backupPreferences(
        source: SharedPreferences,
        destination: SharedPreferences
    ) {
        val editor = destination.edit().clear()
        source.all.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(
                    key,
                    value.filterIsInstance<String>().toSet()
                )
                null -> Unit
            }
        }
        check(editor.commit()) { "Unable to create migration backup" }
    }
}

class QuranSafeguardApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val result = AppMigrations.run(this)
        GuardDiagnostics.log(
            this,
            code = if (result.succeeded) "MIGRATION_OK" else "MIGRATION_DEFERRED",
            detail = "schema=" + result.fromSchema + "->" + result.toSchema
        )
        MindfulReminderScheduler.scheduleAll(this)
    }
}

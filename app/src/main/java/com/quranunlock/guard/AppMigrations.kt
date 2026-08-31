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
    private const val SCHEMA_KEY = "data_schema_version"
    private const val LAST_APP_VERSION_KEY = "last_app_version_code"
    private const val LAST_BACKUP_SCHEMA_KEY = "last_backup_schema"

    private val dataFiles = listOf(
        "guard_prefs",
        "guard_health",
        "guard_diagnostics",
        "daily_reminders"
    )

    const val CURRENT_SCHEMA = 4

    @Synchronized
    fun run(context: Context): MigrationResult {
        val state = context.getSharedPreferences(STATE_FILE, Context.MODE_PRIVATE)
        val from = readIntSafely(state, SCHEMA_KEY, 0).coerceAtLeast(0)

        if (from >= CURRENT_SCHEMA) {
            rememberCurrentAppVersion(context, state)
            return MigrationResult(from, from, true)
        }

        val backupSchema = readIntSafely(state, LAST_BACKUP_SCHEMA_KEY, 0)
        var completeBackupAvailable = backupSchema >= CURRENT_SCHEMA

        return runCatching {
            if (!completeBackupAvailable) {
                backupAllPersistentData(context, CURRENT_SCHEMA)
                completeBackupAvailable = true
                check(
                    state.edit()
                        .putInt(LAST_BACKUP_SCHEMA_KEY, CURRENT_SCHEMA)
                        .commit()
                ) { "Unable to record migration backup state" }
            }

            if (from < 1) migrateToSchema1(context)
            if (from < 2) migrateToSchema2(context)
            if (from < 3) migrateToSchema3(context)
            if (from < 4) migrateToSchema4(context)

            validateCoherence(context)

            check(
                state.edit()
                    .putInt(SCHEMA_KEY, CURRENT_SCHEMA)
                    .commit()
            ) { "Unable to persist migration schema" }

            rememberCurrentAppVersion(context, state)
            MigrationResult(from, CURRENT_SCHEMA, true)
        }.getOrElse { error ->
            if (completeBackupAvailable) {
                runCatching { restoreAllPersistentData(context, CURRENT_SCHEMA) }
            }
            state.edit()
                .putInt(SCHEMA_KEY, from)
                .apply()

            GuardDiagnostics.log(
                context,
                "MIGRATION_ROLLBACK",
                detail = error.javaClass.simpleName
            )
            MigrationResult(from, from, false)
        }
    }

    private fun migrateToSchema1(context: Context) {
        // Historical SharedPreferences layout becomes the formal schema.
        // No existing key is renamed or deleted.
        context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
            .edit()
            .commit()
    }

    private fun migrateToSchema2(context: Context) {
        val prefs = context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        val raw = prefs.all["unlock_minutes"] ?: return
        val old = coerceInt(raw) ?: return
        val migrated = when {
            old <= 1 -> 1
            old <= 5 -> 5
            old <= 10 -> 10
            old <= 15 -> 15
            else -> 20
        }
        if (raw !is Int || migrated != old) {
            check(prefs.edit().putInt("unlock_minutes", migrated).commit())
        }
    }

    private fun migrateToSchema3(context: Context) {
        context.getSharedPreferences("daily_reminders", Context.MODE_PRIVATE)
            .edit()
            .commit()
    }

    private fun migrateToSchema4(context: Context) {
        normalizeGuardPreferences(
            context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        )
        normalizeHealthPreferences(
            context.getSharedPreferences("guard_health", Context.MODE_PRIVATE)
        )
        normalizeDiagnosticsPreferences(
            context.getSharedPreferences("guard_diagnostics", Context.MODE_PRIVATE)
        )
        normalizeReminderPreferences(
            context.getSharedPreferences("daily_reminders", Context.MODE_PRIVATE)
        )
    }

    private fun normalizeGuardPreferences(prefs: SharedPreferences) {
        normalizeStringSet(prefs, "protected_packages")
        normalizeNumericStringSet(prefs, "selected_juz", 1..30)
        normalizeNumericStringSet(prefs, "selected_hizb", 1..60)

        val mode = prefs.all["selection_mode"]
        if (mode != null && mode !is String) {
            prefs.edit().remove("selection_mode").commit()
        } else if (mode is String && mode !in setOf("JUZ", "HIZB")) {
            prefs.edit().remove("selection_mode").commit()
        }

        normalizeBoolean(prefs, "accessibility_consent")
        normalizeInt(prefs, "jokers_used", min = 0, max = GuardPrefs.DAILY_JOKERS)
        normalizeLong(prefs, "joker_epoch_day")
        normalizeLong(prefs, "joker_refill_wall")
        normalizeLong(prefs, "joker_refill_elapsed")
        normalizeInt(prefs, "joker_refill_boot")
        normalizeInt(prefs, "readings_completed", min = 0)
        normalizeLong(prefs, "total_reading_ms", min = 0L)
        normalizeLong(prefs, "last_reading_ms", min = 0L)

        val rawUnlock = prefs.all["unlock_minutes"]
        if (rawUnlock != null) {
            val old = coerceInt(rawUnlock)
            if (old == null) {
                prefs.edit().remove("unlock_minutes").commit()
            } else {
                val migrated = when {
                    old <= 1 -> 1
                    old <= 5 -> 5
                    old <= 10 -> 10
                    old <= 15 -> 15
                    else -> 20
                }
                if (rawUnlock !is Int || old != migrated) {
                    prefs.edit().putInt("unlock_minutes", migrated).commit()
                }
            }
        }

        val fixedStringKeys = setOf("recent_challenge_pages", "reading_history")
        fixedStringKeys.forEach { key ->
            val raw = prefs.all[key]
            if (raw != null && raw !is String) {
                prefs.edit().remove(key).commit()
            }
        }

        prefs.all.keys.forEach { key ->
            when {
                key.startsWith("unlock_remaining_ms_") ||
                    key.startsWith("unlock_foreground_started_") ||
                    key.startsWith("unlock_granted_ms_") ||
                    key.startsWith("unlock_elapsed_until_") ||
                    key.startsWith("unlock_elapsed_started_") ||
                    key.startsWith("reading_accumulated_") ||
                    key.startsWith("reading_started_") ->
                    normalizeLong(prefs, key, min = 0L)

                key.startsWith("unlock_reminder_mask_") ||
                    key.startsWith("challenge_page_") ||
                    key.startsWith("reading_page_") ||
                    key.startsWith("reading_completed_pending_page_") ->
                    normalizeInt(prefs, key, min = 0)

                key.startsWith("reading_completed_pending_elapsed_") ->
                    normalizeLong(prefs, key, min = 0L)

                key.startsWith("reading_bottom_reached_") ->
                    normalizeBoolean(prefs, key)
            }
        }
    }

    private fun normalizeHealthPreferences(prefs: SharedPreferences) {
        normalizeBoolean(prefs, "service_connected")
        normalizeLong(prefs, "last_heartbeat_wall", min = 0L)
        normalizeLong(prefs, "last_event_wall", min = 0L)
        val lastPackage = prefs.all["last_protected_package"]
        if (lastPackage != null && lastPackage !is String) {
            prefs.edit().remove("last_protected_package").commit()
        }
    }

    private fun normalizeDiagnosticsPreferences(prefs: SharedPreferences) {
        val log = prefs.all["log"]
        if (log != null && log !is String) {
            prefs.edit().remove("log").commit()
        }
    }

    private fun normalizeReminderPreferences(prefs: SharedPreferences) {
        normalizeLong(prefs, "selected_epoch_day")
        normalizeLong(prefs, "last_notification_epoch_day")
        normalizeBoolean(prefs, "notification_enabled", defaultValue = true)

        listOf("selected_id", "recent_ids").forEach { key ->
            val raw = prefs.all[key]
            if (raw != null && raw !is String) {
                prefs.edit().remove(key).commit()
            }
        }

        if (!prefs.contains("notification_enabled")) {
            prefs.edit().putBoolean("notification_enabled", true).commit()
        }
    }

    private fun validateCoherence(context: Context) {
        val guard = context.getSharedPreferences("guard_prefs", Context.MODE_PRIVATE)
        guard.all["protected_packages"]?.let {
            check(it is Set<*> && it.all { item -> item is String })
        }
        guard.all["selected_juz"]?.let {
            check(it is Set<*> && it.all { item -> item is String })
        }
        guard.all["selected_hizb"]?.let {
            check(it is Set<*> && it.all { item -> item is String })
        }
        guard.all["unlock_minutes"]?.let {
            check(it is Int && it in setOf(1, 5, 10, 15, 20))
        }

        val reminders = context.getSharedPreferences("daily_reminders", Context.MODE_PRIVATE)
        reminders.all["notification_enabled"]?.let { check(it is Boolean) }
    }

    private fun backupAllPersistentData(context: Context, schema: Int) {
        dataFiles.forEach { file ->
            backupPreferences(
                source = context.getSharedPreferences(file, Context.MODE_PRIVATE),
                destination = context.getSharedPreferences(
                    file + "_backup_before_schema_" + schema,
                    Context.MODE_PRIVATE
                )
            )
        }
    }

    private fun restoreAllPersistentData(context: Context, schema: Int) {
        dataFiles.forEach { file ->
            val backup = context.getSharedPreferences(
                file + "_backup_before_schema_" + schema,
                Context.MODE_PRIVATE
            )
            restorePreferences(
                source = backup,
                destination = context.getSharedPreferences(file, Context.MODE_PRIVATE)
            )
        }
    }

    private fun backupPreferences(
        source: SharedPreferences,
        destination: SharedPreferences
    ) {
        copyPreferences(source, destination)
    }

    private fun restorePreferences(
        source: SharedPreferences,
        destination: SharedPreferences
    ) {
        copyPreferences(source, destination)
    }

    private fun copyPreferences(
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
        check(editor.commit()) { "Unable to copy persistent preferences" }
    }

    private fun normalizeStringSet(prefs: SharedPreferences, key: String) {
        val raw = prefs.all[key] ?: return
        val values = when (raw) {
            is Set<*> -> raw.filterIsInstance<String>().filter { it.isNotBlank() }.toSet()
            is String -> raw.split(',', '|', '\n')
                .map(String::trim)
                .filter(String::isNotBlank)
                .toSet()
            else -> null
        }

        if (values == null) {
            prefs.edit().remove(key).commit()
        } else if (raw !is Set<*> || raw.any { it !is String }) {
            prefs.edit().putStringSet(key, values).commit()
        }
    }

    private fun normalizeNumericStringSet(
        prefs: SharedPreferences,
        key: String,
        range: IntRange
    ) {
        val raw = prefs.all[key] ?: return
        val values = when (raw) {
            is Set<*> -> raw.mapNotNull {
                when (it) {
                    is String -> it.toIntOrNull()
                    is Int -> it
                    is Long -> it.toInt()
                    else -> null
                }
            }
            is String -> raw.split(',', '|', ';', ' ', '\n')
                .mapNotNull { it.trim().toIntOrNull() }
            else -> emptyList()
        }.filter { it in range }.map(Int::toString).toSet()

        if (values.isEmpty() && raw !is Set<*> && raw !is String) {
            prefs.edit().remove(key).commit()
        } else {
            prefs.edit().putStringSet(key, values).commit()
        }
    }

    private fun normalizeBoolean(
        prefs: SharedPreferences,
        key: String,
        defaultValue: Boolean? = null
    ) {
        val raw = prefs.all[key]
        if (raw == null) {
            if (defaultValue != null) {
                prefs.edit().putBoolean(key, defaultValue).commit()
            }
            return
        }

        val value = when (raw) {
            is Boolean -> raw
            is String -> when (raw.trim().lowercase()) {
                "true", "1", "yes", "oui" -> true
                "false", "0", "no", "non" -> false
                else -> null
            }
            is Int -> raw != 0
            is Long -> raw != 0L
            else -> null
        }

        if (value == null) {
            prefs.edit().remove(key).commit()
        } else if (raw !is Boolean) {
            prefs.edit().putBoolean(key, value).commit()
        }
    }

    private fun normalizeInt(
        prefs: SharedPreferences,
        key: String,
        min: Int? = null,
        max: Int? = null
    ) {
        val raw = prefs.all[key] ?: return
        val value = coerceInt(raw)
        if (value == null) {
            prefs.edit().remove(key).commit()
            return
        }
        val safe = value
            .let { if (min != null) it.coerceAtLeast(min) else it }
            .let { if (max != null) it.coerceAtMost(max) else it }

        if (raw !is Int || safe != value) {
            prefs.edit().putInt(key, safe).commit()
        }
    }

    private fun normalizeLong(
        prefs: SharedPreferences,
        key: String,
        min: Long? = null
    ) {
        val raw = prefs.all[key] ?: return
        val value = when (raw) {
            is Long -> raw
            is Int -> raw.toLong()
            is String -> raw.toLongOrNull()
            else -> null
        }
        if (value == null) {
            prefs.edit().remove(key).commit()
            return
        }
        val safe = if (min != null) value.coerceAtLeast(min) else value
        if (raw !is Long || safe != value) {
            prefs.edit().putLong(key, safe).commit()
        }
    }

    private fun coerceInt(value: Any?): Int? = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    }

    private fun readIntSafely(
        prefs: SharedPreferences,
        key: String,
        defaultValue: Int
    ): Int = coerceInt(prefs.all[key]) ?: defaultValue

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
}

class QuranSafeguardApp : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        val result = AppMigrations.run(this)
        GuardDiagnostics.log(
            this,
            code = if (result.succeeded) "MIGRATION_OK" else "MIGRATION_ROLLBACK",
            detail = "schema=" + result.fromSchema + "->" + result.toSchema
        )

        if (result.succeeded) {
            DailyReminderScheduler.scheduleNext(this)
        }
    }
}

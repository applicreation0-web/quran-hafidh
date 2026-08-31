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
    private const val SCHEMA_KEY = "data_schema_version"
    private const val LAST_APP_VERSION_KEY = "last_app_version_code"
    private const val LAST_BACKUP_SCHEMA_KEY = "last_backup_schema"

    const val CURRENT_SCHEMA = 3

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
                backupPreferences(
                    source = context.getSharedPreferences(GUARD_PREFS, Context.MODE_PRIVATE),
                    destination = context.getSharedPreferences(
                        "guard_prefs_backup_before_schema_" + CURRENT_SCHEMA,
                        Context.MODE_PRIVATE
                    )
                )
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

            rememberCurrentAppVersion(context, state)
            MigrationResult(from, schema, true)
        }.getOrElse {
            MigrationResult(from, state.getInt(SCHEMA_KEY, from), false)
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
    }
}

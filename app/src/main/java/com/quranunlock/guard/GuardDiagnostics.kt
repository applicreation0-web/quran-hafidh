package com.applicreation0.quransafeguard

import android.content.Context
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DiagnosticEntry(
    val epochMs: Long,
    val code: String,
    val packageName: String?,
    val detail: String
)

object GuardDiagnostics {
    private const val FILE = "guard_diagnostics"
    private const val LOG = "log"
    private const val MAX_ENTRIES = 80
    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    @Synchronized
    fun log(
        context: Context,
        code: String,
        packageName: String? = null,
        detail: String = ""
    ) {
        val excluded = packageName?.let {
            ProtectedApps.shouldNeverPersist(context, it)
        } == true
        val safePackageName = if (excluded) "" else packageName.orEmpty()
        val safeDetail = if (excluded) "" else detail

        val entry = listOf(
            System.currentTimeMillis().toString(),
            sanitize(code),
            sanitize(safePackageName),
            sanitize(safeDetail)
        ).joinToString("\t")

        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val existing = prefs.getString(LOG, "").orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()

        val updated = buildList {
            add(entry)
            addAll(existing)
        }.take(MAX_ENTRIES)

        prefs.edit().putString(LOG, updated.joinToString("\n")).apply()
    }

    fun recent(context: Context, limit: Int = 12): List<DiagnosticEntry> =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(LOG, "")
            .orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull(::parse)
            .take(limit.coerceAtLeast(0))
            .toList()

    fun formatTime(entry: DiagnosticEntry): String =
        Instant.ofEpochMilli(entry.epochMs)
            .atZone(ZoneId.systemDefault())
            .format(formatter)

    @Synchronized
    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .remove(LOG)
            .apply()
    }

    private fun parse(raw: String): DiagnosticEntry? {
        val parts = raw.split('\t', limit = 4)
        if (parts.size < 4) return null
        val epoch = parts[0].toLongOrNull() ?: return null
        return DiagnosticEntry(
            epochMs = epoch,
            code = parts[1],
            packageName = parts[2].ifBlank { null },
            detail = parts[3]
        )
    }

    private fun sanitize(value: String): String =
        value.replace('\t', ' ').replace('\n', ' ')
}

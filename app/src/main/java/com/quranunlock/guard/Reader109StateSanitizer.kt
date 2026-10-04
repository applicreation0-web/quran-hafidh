package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Defensive migration boundary for reader109 state written by 0.10.9.
 * Rendering safety wins over retaining malformed fields, while valid bookmarks,
 * sessions and UI state are preserved whenever their container is structurally safe.
 */
object Reader109StateSanitizer {
    private const val MIN_PAGE = 1
    private const val MAX_PAGE = 604
    private const val SAFE_PAGE = 1

    data class Result(
        val json: String,
        val page: Int,
        val changed: Boolean
    )

    fun sanitize(raw: String?, fallbackPage: Int = SAFE_PAGE): Result {
        val safeFallback = fallbackPage.takeIf { it in MIN_PAGE..MAX_PAGE } ?: SAFE_PAGE
        val parsed = runCatching { raw?.let(::JSONObject) }.getOrNull()
        val sourceSchemaValid = parsed?.optInt("schema", -1) == 1
        val root = if (sourceSchemaValid) parsed!! else JSONObject()
        val before = raw

        root.put("schema", 1)
        val page = strictInt(root.opt("page"))
            ?.takeIf { it in MIN_PAGE..MAX_PAGE }
            ?: safeFallback
        root.put("page", page)

        val zoom = when (val candidate = root.opt("zoom")) {
            is Number -> candidate.toDouble().takeIf { it.isFinite() && it in 1.0..3.0 }
            else -> null
        } ?: 1.0
        root.put("zoom", zoom)

        root.put("marks", sanitizeMarks(root.opt("marks")))
        val sessions = sanitizeSessions(root.opt("sessions"))
        root.put("sessions", sessions)

        val active = root.opt("active") as? String
        val activeStillExists = active != null && (0 until sessions.length()).any { index ->
            sessions.optJSONObject(index)?.optString("id") == active
        }
        if (activeStillExists) root.put("active", active) else root.put("active", JSONObject.NULL)

        val ui = root.optJSONObject("ui") ?: JSONObject()
        val mode = if (ui.optString("mode") == "MEMORIZATION") "MEMORIZATION" else "READING"
        ui.put("mode", mode)
        if (ui.opt("selectStart") !is String) ui.put("selectStart", JSONObject.NULL)
        if (ui.opt("selectEnd") !is String) ui.put("selectEnd", JSONObject.NULL)
        root.put("ui", ui)

        val json = root.toString()
        return Result(json = json, page = page, changed = before != json)
    }

    /** Run once on voluntary reader entry as an upgrade-safe 0.10.9 -> 10.10 repair. */
    fun migrateOnReaderEntry(context: Context) {
        val reader = context.getSharedPreferences(
            QuranPersistenceNamespaces.FREE_READER_MEMORIZATION,
            Context.MODE_PRIVATE
        )
        val lastPagePrefs = context.getSharedPreferences(
            QuranPersistenceNamespaces.FREE_READER_LAST_PAGE,
            Context.MODE_PRIVATE
        )
        val storedLastPage = strictInt(lastPagePrefs.all["last_page"])
            ?.takeIf { it in MIN_PAGE..MAX_PAGE }
            ?: SAFE_PAGE
        if (lastPagePrefs.all["last_page"] != storedLastPage) {
            lastPagePrefs.edit().putInt("last_page", storedLastPage).commit()
        }

        val rawAny = reader.all["state"]
        val raw = rawAny as? String
        if (rawAny == null) return
        val sanitized = sanitize(raw, storedLastPage)
        if (raw !is String || sanitized.changed) {
            reader.edit().putString("state", sanitized.json).commit()
        }
    }

    internal fun strictInt(value: Any?): Int? = when (value) {
        is Int -> value
        is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
        else -> null
    }

    private fun sanitizeMarks(value: Any?): JSONArray {
        val input = value as? JSONArray ?: return JSONArray()
        val output = JSONArray()
        for (index in 0 until input.length()) {
            val mark = input.optJSONObject(index) ?: continue
            val page = strictInt(mark.opt("page"))?.takeIf { it in MIN_PAGE..MAX_PAGE } ?: continue
            mark.put("page", page)
            output.put(mark)
        }
        return output
    }

    private fun sanitizeSessions(value: Any?): JSONArray {
        val input = value as? JSONArray ?: return JSONArray()
        val output = JSONArray()
        for (index in 0 until input.length()) {
            input.optJSONObject(index)?.let(output::put)
        }
        return output
    }
}

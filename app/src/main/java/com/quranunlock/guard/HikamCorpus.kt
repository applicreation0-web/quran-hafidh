package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

enum class HikamVerificationStatus {
    VERIFIED,
    PENDING_VERIFICATION
}

data class HikamEntry(
    val id: String,
    val collection: String,
    val author: String,
    val arabic: String,
    val french: String,
    val explanation: String?,
    val sourceTitle: String,
    val sourceEdition: String,
    val sourceNumber: String,
    val sourcePage: String?,
    val verificationStatus: HikamVerificationStatus,
    val verificationSources: List<String>,
    val textType: String,
    val themes: Set<String>,
    val estimatedReadingSeconds: Int,
    val alternateNumbering: Map<String, String>,
    val textualVariant: String?,
    val translatorNote: String?,
    val verificationNotes: String?
) {
    fun toDailyReminder(): DailyReminder {
        check(verificationStatus == HikamVerificationStatus.VERIFIED)
        check(textType == "author_wisdom")
        return DailyReminder(
            id = "hikam_corpus_" + id,
            type = ReminderType.HIKAM,
            theme = themes.firstOrNull() ?: "spiritual_presence",
            arabicText = arabic,
            frenchText = french,
            author = "Ibn ‘Atâ’ Allâh al-Iskandarî",
            book = "Al-Hikam al-‘Atâ’iyya",
            reference = buildString {
                append("Ḥikma n° ")
                append(sourceNumber)
                append(" selon ")
                append(sourceEdition)
            },
            authenticity = null,
            tags = themes
        )
    }
}

object HikamCorpus {
    private const val ASSET = "hikam/al_hikam_verified.json"
    private var cached: List<HikamEntry>? = null

    @Synchronized
    fun all(context: Context): List<HikamEntry> {
        cached?.let { return it }
        val loaded = runCatching {
            context.assets.open(ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { reader -> parse(reader.readText()) }
        }.getOrDefault(emptyList())
        cached = loaded
        return loaded
    }

    fun verified(context: Context): List<HikamEntry> =
        all(context).filter {
            it.verificationStatus == HikamVerificationStatus.VERIFIED
        }

    fun pending(context: Context): List<HikamEntry> =
        all(context).filter {
            it.verificationStatus != HikamVerificationStatus.VERIFIED
        }

    fun bySourceNumber(context: Context, number: String): HikamEntry? =
        all(context).firstOrNull { it.sourceNumber == number }

    fun byTheme(context: Context, theme: String): List<HikamEntry> =
        verified(context).filter { theme in it.themes }

    internal fun parse(raw: String): List<HikamEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(parseOne(obj))
            }
        }
    }

    private fun parseOne(obj: JSONObject): HikamEntry {
        val status = when (obj.getString("verification_status")) {
            "verified" -> HikamVerificationStatus.VERIFIED
            else -> HikamVerificationStatus.PENDING_VERIFICATION
        }
        return HikamEntry(
            id = obj.getString("id"),
            collection = obj.getString("collection"),
            author = obj.getString("author"),
            arabic = obj.getString("arabic"),
            french = obj.optString("french"),
            explanation = obj.optString("explanation").takeIf { it.isNotBlank() },
            sourceTitle = obj.getString("source_title"),
            sourceEdition = obj.getString("source_edition"),
            sourceNumber = obj.getString("source_number"),
            sourcePage = obj.optString("source_page").takeIf { it.isNotBlank() },
            verificationStatus = status,
            verificationSources = obj.stringList("verification_sources"),
            textType = obj.getString("text_type"),
            themes = obj.stringList("themes").toSet(),
            estimatedReadingSeconds = obj.optInt("estimated_reading_seconds", 0),
            alternateNumbering = obj.stringMap("alternate_numbering"),
            textualVariant = obj.optString("textual_variant").takeIf { it.isNotBlank() },
            translatorNote = obj.optString("translator_note").takeIf { it.isNotBlank() },
            verificationNotes = obj.optString("verification_notes").takeIf { it.isNotBlank() }
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject.stringMap(key: String): Map<String, String> {
        val obj = optJSONObject(key) ?: return emptyMap()
        return buildMap {
            val keys = obj.keys()
            while (keys.hasNext()) {
                val childKey = keys.next()
                obj.optString(childKey).takeIf { it.isNotBlank() }
                    ?.let { put(childKey, it) }
            }
        }
    }
}

data class ReligiousContentCounts(
    val verifiedHadiths: Int,
    val verifiedHikam: Int,
    val verifiedGhazali: Int,
    val adhkar: Int,
    val other: Int
)

object ReligiousContentCatalog {
    fun counts(context: Context): ReligiousContentCounts {
        val reminders = ReminderLibrary.all(context)
        return ReligiousContentCounts(
            verifiedHadiths = reminders.count { it.type == ReminderType.HADITH },
            verifiedHikam = HikamCorpus.verified(context).size,
            verifiedGhazali = reminders.count { it.type == ReminderType.GHAZALI },
            adhkar = AuthenticAdhkarLibrary.items.size,
            other = 0
        )
    }
}

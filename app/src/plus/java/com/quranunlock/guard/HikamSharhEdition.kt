package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Personal Plus-only commentary layer.
 *
 * The asset is optional while the corpus is being verified. The UI can therefore
 * ship the dual-commentator interaction before all 264 pairs are ready, without
 * inventing missing text.
 */
object HikamSharhEdition {
    const val isEnabled: Boolean = true
    private const val ASSET = "hikam/hikam_sharh_dual.json"

    private var cached: Map<Int, List<HikamSharhEntry>>? = null

    @Synchronized
    fun forHikma(
        context: Context,
        hikmaNumber: Int
    ): List<HikamSharhAvailability> {
        val entries = load(context)[hikmaNumber].orEmpty()
        return HikamCommentator.entries.map { commentator ->
            HikamSharhAvailability(
                commentator = commentator,
                entry = entries.singleOrNull {
                    it.commentator == commentator && it.displayEligible
                }
            )
        }
    }

    @Synchronized
    private fun load(context: Context): Map<Int, List<HikamSharhEntry>> {
        cached?.let { return it }
        val raw = runCatching {
            context.assets.open(ASSET)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            return emptyMap<Int, List<HikamSharhEntry>>().also { cached = it }
        }

        val parsed = runCatching { parse(raw) }.getOrElse {
            return emptyMap<Int, List<HikamSharhEntry>>().also { cached = it }
        }
        val byCanonical = mutableMapOf<Int, MutableList<HikamSharhEntry>>()
        parsed.forEach { entry ->
            entry.canonicalHikmaNumbers.forEach { number ->
                byCanonical.getOrPut(number) { mutableListOf() }.add(entry)
            }
        }
        cached = byCanonical.mapValues { (_, entries) -> entries.toList() }
        return cached.orEmpty()
    }

    internal fun parse(raw: String): List<HikamSharhEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(parseOne(obj))
            }
        }.also(HikamSharhIntegrity::requireUnique)
    }

    private fun parseOne(obj: JSONObject): HikamSharhEntry {
        val commentator = when (obj.getString("commentator_id")) {
            HikamCommentator.SHARNUBI.stableId -> HikamCommentator.SHARNUBI
            HikamCommentator.IBN_ABBAD.stableId -> HikamCommentator.IBN_ABBAD
            else -> error("Unsupported Hikam commentator")
        }
        val canonicalNumbers = obj.requiredIntSet("canonical_hikma_numbers")
        val primaryCanonical = canonicalNumbers.minOrNull()
            ?: error("canonical_hikma_numbers must not be empty")

        return HikamSharhEntry(
            hikmaNumber = primaryCanonical,
            commentator = commentator,
            workId = obj.getString("commentary_work_id").trim(),
            workTitle = obj.getString("commentary_work").trim(),
            arabicText = obj.getString("commentary_arabic").trim(),
            frenchText = obj.getString("commentary_french").trim(),
            sourceUrl = obj.getString("commentary_source_url").trim(),
            printLocator = obj.getString("commentary_print_locator").trim(),
            verified = obj.optString("commentary_status") == "verified" &&
                obj.optString("translation_status") == "verified",
            canonicalHikmaNumbers = canonicalNumbers,
            sourceHikmaLocator = obj.getString("source_hikma_locator").trim(),
            commentaryGroupId = obj.getString("commentary_group_id").trim(),
            richSpansArabic = obj.richSpans("rich_spans_arabic"),
            richSpansFrench = obj.richSpans("rich_spans_french"),
            technicalTerms = obj.stringSet("technical_terms")
        )
    }

    private fun JSONObject.richSpans(key: String): List<HikamRichSpan> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val span = array.getJSONObject(index)
                val role = runCatching {
                    HikamRichRole.valueOf(span.getString("role").uppercase())
                }.getOrNull() ?: error("Unsupported rich span role in $key")
                add(
                    HikamRichSpan(
                        start = span.getInt("start"),
                        endExclusive = span.getInt("end"),
                        role = role,
                        lexiconKey = span.optString("lexicon_key")
                            .trim()
                            .takeIf(String::isNotBlank)
                    )
                )
            }
        }
    }

    private fun JSONObject.stringSet(key: String): Set<String> {
        val array = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                array.optString(index)
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }

    private fun JSONObject.requiredIntSet(key: String): Set<Int> {
        val array = getJSONArray(key)
        return buildSet {
            for (index in 0 until array.length()) {
                add(array.getInt(index))
            }
        }.also {
            require(it.isNotEmpty()) { "$key must not be empty" }
        }
    }
}

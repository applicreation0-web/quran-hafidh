package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Personal Plus-only commentary layer.
 *
 * The asset is optional while the corpus is being verified. The UI can therefore
 * ship the dual-commentator interaction before all 264 pairs are ready, without
 * inventing missing text. The 0.10.5 release gate separately requires the full
 * verified asset before publication.
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
                entry = entries.firstOrNull {
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

        val parsed = parse(raw)
        cached = parsed.groupBy(HikamSharhEntry::hikmaNumber)
        return cached.orEmpty()
    }

    internal fun parse(raw: String): List<HikamSharhEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val obj = array.getJSONObject(index)
                add(parseOne(obj))
            }
        }
    }

    private fun parseOne(obj: JSONObject): HikamSharhEntry {
        val commentator = when (obj.getString("commentator_id")) {
            HikamCommentator.SHARNUBI.stableId -> HikamCommentator.SHARNUBI
            HikamCommentator.IBN_ABBAD.stableId -> HikamCommentator.IBN_ABBAD
            else -> error("Unsupported Hikam commentator")
        }

        return HikamSharhEntry(
            hikmaNumber = obj.getInt("source_number"),
            commentator = commentator,
            workTitle = obj.getString("commentary_work").trim(),
            sourceEdition = obj.getString("commentary_source_edition").trim(),
            arabicText = obj.getString("commentary_arabic").trim(),
            frenchText = obj.getString("commentary_french").trim(),
            translationCredit = obj.getString("commentary_translation_credit").trim(),
            sourceUrl = obj.getString("commentary_source_url").trim(),
            printLocator = obj.getString("commentary_print_locator").trim(),
            verified = obj.optString("commentary_status") == "verified" &&
                obj.optString("translation_status") == "verified" &&
                obj.optString("hikma_alignment_status") == "verified",
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
                }.getOrNull() ?: continue
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
}

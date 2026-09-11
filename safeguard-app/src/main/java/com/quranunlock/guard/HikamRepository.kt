package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Canonical in-app source for the 264 Al-Hikam al-ʿAṭāʾiyya.
 *
 * Only the Hikma itself is exposed: verified Arabic matn with source-aligned
 * tashkīl, internal French translation and documentary metadata. No commentary,
 * summary, synthesis or generated explanation is stored or displayed.
 */
data class HikmaEntry(
    val canonicalId: String,
    val sourceNumber: Int,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val textIntegrity: ClassicalTextIntegrity,
    val canonicalArabicText: String = arabicText,
    val vocalizationSourceUrl: String? = null
) {
    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                sourceNumber in 1..264 &&
                arabicText.isNotBlank() &&
                canonicalArabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(canonicalArabicText, frenchText)
}

object HikamRepository {
    private const val ASSET = "hikam/al_hikam_verified.json"
    private const val VOCALIZATION_STATUS =
        "source_aligned_no_automatic_generation"

    private var cached: List<HikmaEntry>? = null

    @Synchronized
    fun entries(context: Context): List<HikmaEntry> {
        cached?.let { return it }
        val raw = context.assets.open(ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val parsed = parse(raw)

        require(parsed.size == 264) {
            "Expected exactly 264 verified Hikam, got ${parsed.size}"
        }
        require(parsed.map { it.sourceNumber }.toSet() == (1..264).toSet()) {
            "Hikam numbering must cover exactly 1..264"
        }
        require(parsed.all { it.displayEligible }) {
            "Every bundled Hikma must satisfy the classical authenticity contract"
        }
        require(parsed.all { !it.vocalizationSourceUrl.isNullOrBlank() }) {
            "Every bundled Hikma must retain its vocalization source"
        }

        cached = parsed
        return parsed
    }

    fun byId(context: Context, id: String): HikmaEntry? =
        entries(context).firstOrNull {
            it.canonicalId == id && it.displayEligible
        }

    fun asDailyReminders(context: Context): List<DailyReminder> =
        entries(context)
            .filter(HikmaEntry::displayEligible)
            .map { hikma ->
                DailyReminder(
                    id = hikma.canonicalId,
                    type = ReminderType.HIKAM,
                    theme = hikma.theme,
                    arabicText = hikma.arabicText,
                    frenchText = hikma.frenchText,
                    author = hikma.source.author,
                    book = hikma.source.workTitle,
                    reference = hikma.source.locator,
                    authenticity = null,
                    tags = hikma.tags
                )
            }

    internal fun parse(raw: String): List<HikmaEntry> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                add(parseOne(array.getJSONObject(index)))
            }
        }
    }

    private fun parseOne(obj: JSONObject): HikmaEntry {
        val sourceNumber = obj.getString("source_number").toInt()
        val canonicalArabic = obj.getString("arabic").trim()
        val vocalizedArabic = obj.getString("arabic_vocalized").trim()
        val french = obj.getString("french").trim()
        val verificationSources = obj.stringList("verification_sources")
        val translationSources = obj.stringList("translation_sources")
        val themes = obj.stringList("themes").toSet()
        val sourcePage = obj.optString("source_page")
            .trim()
            .takeIf(String::isNotBlank)
        val vocalizationSource = obj.optString("vocalization_source")
            .trim()
            .takeIf(String::isNotBlank)
        val vocalizationVerified =
            obj.optString("vocalization_status") == VOCALIZATION_STATUS &&
                vocalizationSource != null &&
                vocalizedArabic.isNotBlank()

        val sourceVerified =
            obj.optString("verification_status") == "verified" &&
                verificationSources.isNotEmpty() &&
                canonicalArabic.isNotBlank() &&
                vocalizationVerified
        val translationVerified =
            obj.optString("translation_status") == "verified" &&
                french.isNotBlank() &&
                translationSources.isNotEmpty()
        val attributionVerified =
            obj.optString("author") == "Ibn Ata Allah al-Iskandari" &&
                obj.optString("text_type") == "author_wisdom"

        val baseVerificationNote = obj.optString("verification_notes")
            .ifBlank {
                "Matn arabe et traduction interne contrôlés dans le corpus 1–264."
            }
        val vocalizationNote = obj.optString("vocalization_notes")
            .ifBlank {
                "Tashkīl recopié uniquement sur les lettres alignées de l’édition vocalisée."
            }

        val verification = ClassicalVerification(
            sourceVerified = sourceVerified,
            attributionVerified = attributionVerified,
            translationAvailable = translationVerified,
            humanVerified = false,
            rightsStatus =
                TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
            authenticityStatus =
                if (sourceVerified &&
                    attributionVerified &&
                    translationVerified
                ) {
                    ClassicalAuthenticityStatus.VERIFIED_SOURCE
                } else {
                    ClassicalAuthenticityStatus.NOT_VERIFIED
                },
            verificationNote = "$baseVerificationNote $vocalizationNote"
        )

        val locator = buildString {
            append("Hikma ")
            append(sourceNumber)
            sourcePage?.let {
                append(" • p. ")
                append(it)
            }
        }

        return HikmaEntry(
            canonicalId = "hikma_$sourceNumber",
            sourceNumber = sourceNumber,
            arabicText = vocalizedArabic,
            canonicalArabicText = canonicalArabic,
            frenchText = french,
            theme = themes.firstOrNull() ?: "spiritual_presence",
            tags = themes.ifEmpty { setOf("Al-Hikam") },
            source = ClassicalSource(
                author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                workTitle = "Al-Hikam al-ʿAṭāʾiyya",
                edition = obj.getString("source_edition"),
                editor = "ʿĀṣim Ibrāhīm al-Kayyālī",
                volume = null,
                locator = locator,
                sourceUrl = verificationSources.firstOrNull().orEmpty(),
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verification,
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            ),
            vocalizationSourceUrl = vocalizationSource
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index)
                    .trim()
                    .takeIf(String::isNotBlank)
                    ?.let(::add)
            }
        }
    }
}

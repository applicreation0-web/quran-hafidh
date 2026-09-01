package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Canonical in-app source for the 264 Al-Hikam al-ʿAṭāʾiyya retained for 0.9.1.
 *
 * The Arabic matn and the internal French translations are loaded from the frozen,
 * release-audited asset hikam/al_hikam_verified.json. No AI summary or explanation
 * is stored or generated. Ibn ʿAjība commentary is attached only where a separately
 * sourced commentary excerpt has already been verified.
 */
data class HikmaCommentary(
    val arabicText: String,
    val frenchText: String,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val isExcerpt: Boolean,
    val textIntegrity: ClassicalTextIntegrity
) {
    val displayEligible: Boolean
        get() =
            arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(arabicText, frenchText)
}

data class HikmaEntry(
    val canonicalId: String,
    val sourceNumber: Int,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val commentary: HikmaCommentary?,
    val additionalCommentaries: List<HikmaCommentary> = emptyList(),
    val textIntegrity: ClassicalTextIntegrity
) {
    /**
     * Classical commentaries are intentionally kept as independent source units.
     * Never merge, synthesize or paraphrase multiple commentators into one text.
     */
    val commentaries: List<HikmaCommentary>
        get() = listOfNotNull(commentary) + additionalCommentaries

    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                sourceNumber in 1..264 &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(arabicText, frenchText)
}

object HikamRepository {
    private const val ASSET = "hikam/al_hikam_verified.json"

    private const val AJIBA_EDITION =
        "Ibn ʿAjība, Īqāẓ al-Himam fī Sharḥ al-Ḥikam, éd./corr. " +
            "Muḥammad ʿAbd al-Qādir Naṣṣār, Dār Jawāmiʿ al-Kalim, Le Caire, 632 p."

    private var cached: List<HikmaEntry>? = null

    private fun verifiedInternalTranslation(note: String) = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = note
    )

    private val commentaryByNumber: Map<Int, HikmaCommentary> by lazy {
        mapOf(
            5 to HikmaCommentary(
                arabicText = "قلت : الاجتهاد في الشيء استفراغ الجهد والطاقة في طلبه ، والتقصير هو التفريط والتضييع والبصيرة ناظر القلب […]",
                frenchText = "J’ai dit : l’effort appliqué à une chose consiste à déployer toute son énergie pour la rechercher ; la négligence est le relâchement et l’abandon, et la clairvoyance est le regard du cœur. […]",
                source = ClassicalSource(
                    author = "Ibn ʿAjība",
                    workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                    edition = AJIBA_EDITION,
                    editor = "Muḥammad ʿAbd al-Qādir Naṣṣār",
                    volume = null,
                    locator = "Hikma 5 • p. 39",
                    sourceUrl = "https://ablibrary.net/book_content/b/9684/39",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Extrait arabe retrouvé à la p. 39 ; traduction interne du même passage."
                ),
                isExcerpt = true,
                textIntegrity = ClassicalTextIntegrity(
                    form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                    reconstructedOrAssembled = false,
                    hasInternalOmissions = true,
                    contextChecked = true,
                    passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
                )
            ),
            10 to HikmaCommentary(
                arabicText = "قلت : الأعمال كلها أشباح وأجساد وأرواحها وجود الإخلاص فيها فكما لا قيام للأشباح إلا بالأرواح […]",
                frenchText = "J’ai dit : toutes les œuvres sont des formes et des corps, et leurs âmes sont la présence de la sincérité en elles ; de même que les formes ne subsistent que par les âmes […].",
                source = ClassicalSource(
                    author = "Ibn ʿAjība",
                    workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                    edition = AJIBA_EDITION,
                    editor = "Muḥammad ʿAbd al-Qādir Naṣṣār",
                    volume = null,
                    locator = "Hikma 10 • p. 50",
                    sourceUrl = "https://ablibrary.net/book_content/b/9684/50",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Extrait arabe retrouvé à la p. 50 ; traduction interne du même passage."
                ),
                isExcerpt = true,
                textIntegrity = ClassicalTextIntegrity(
                    form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                    reconstructedOrAssembled = false,
                    hasInternalOmissions = true,
                    contextChecked = true,
                    passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
                )
            ),
            12 to HikmaCommentary(
                arabicText = "قلت : لا شيء أنفع للقلب من عزلة مصحوبة بفكرة لأن العزلة كالحمية والفكرة كالدواء […]",
                frenchText = "J’ai dit : rien n’est plus bénéfique au cœur qu’une retraite accompagnée de réflexion, car la retraite est comme une diète et la réflexion comme un remède. […]",
                source = ClassicalSource(
                    author = "Ibn ʿAjība",
                    workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                    edition = AJIBA_EDITION,
                    editor = "Muḥammad ʿAbd al-Qādir Naṣṣār",
                    volume = null,
                    locator = "Hikma 12 • p. 58",
                    sourceUrl = "https://ablibrary.net/book_content/b/9684/58",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Extrait arabe retrouvé à la p. 58 ; traduction interne du même passage."
                ),
                isExcerpt = true,
                textIntegrity = ClassicalTextIntegrity(
                    form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                    reconstructedOrAssembled = false,
                    hasInternalOmissions = true,
                    contextChecked = true,
                    passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
                )
            )
        )
    }

    @Synchronized
    fun entries(context: Context): List<HikmaEntry> {
        cached?.let { return it }
        val raw = context.assets.open(ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val parsed = parse(raw)
        require(parsed.size == 264) { "Expected exactly 264 verified Hikam, got ${parsed.size}" }
        require(parsed.map { it.sourceNumber }.toSet() == (1..264).toSet()) {
            "Hikam numbering must cover exactly 1..264"
        }
        require(parsed.all { it.displayEligible }) {
            "Every bundled Hikma must satisfy the classical authenticity contract"
        }
        cached = parsed
        return parsed
    }

    fun byId(context: Context, id: String): HikmaEntry? =
        entries(context).firstOrNull { it.canonicalId == id && it.displayEligible }

    fun asDailyReminders(context: Context): List<DailyReminder> =
        entries(context).filter { it.displayEligible }.map { hikma ->
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
        val arabic = obj.getString("arabic").trim()
        val french = obj.getString("french").trim()
        val verificationSources = obj.stringList("verification_sources")
        val translationSources = obj.stringList("translation_sources")
        val themes = obj.stringList("themes").toSet()
        val sourcePage = obj.optString("source_page").trim().takeIf { it.isNotBlank() }

        val sourceVerified =
            obj.optString("verification_status") == "verified" &&
                verificationSources.isNotEmpty()
        val translationVerified =
            obj.optString("translation_status") == "verified" &&
                french.isNotBlank() &&
                translationSources.isNotEmpty()
        val attributionVerified =
            obj.optString("author") == "Ibn Ata Allah al-Iskandari" &&
                obj.optString("text_type") == "author_wisdom"

        val verification = ClassicalVerification(
            sourceVerified = sourceVerified,
            attributionVerified = attributionVerified,
            translationAvailable = translationVerified,
            humanVerified = false,
            rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
            authenticityStatus =
                if (sourceVerified && attributionVerified && translationVerified) {
                    ClassicalAuthenticityStatus.VERIFIED_SOURCE
                } else {
                    ClassicalAuthenticityStatus.NOT_VERIFIED
                },
            verificationNote = obj.optString("verification_notes").ifBlank {
                "Matn arabe et traduction interne contrôlés dans le corpus gelé 1–264."
            }
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
            canonicalId = "hikma_" + sourceNumber,
            sourceNumber = sourceNumber,
            arabicText = arabic,
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
            commentary = commentaryByNumber[sourceNumber],
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            )
        )
    }

    private fun JSONObject.stringList(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}

package com.applicreation0.quransafeguard

/**
 * Canonical in-app source for Al-Hikam al-ʿAṭāʾiyya.
 *
 * Authenticity before quantity:
 * no entry is display-eligible until the exact Arabic, attribution, source/locator
 * and a French translation are available. Human review of the translation is
 * informative metadata, not a display requirement.
 *
 * The repository may grow toward the numbering of the retained source, but the
 * app must never claim a complete corpus until every entry has been checked.
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
                source.author.isNotBlank() &&
                source.workTitle.isNotBlank() &&
                source.edition.isNotBlank() &&
                source.locator.isNotBlank() &&
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
    val textIntegrity: ClassicalTextIntegrity
) {
    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                sourceNumber > 0 &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.author.isNotBlank() &&
                source.workTitle.isNotBlank() &&
                source.edition.isNotBlank() &&
                source.locator.isNotBlank() &&
                source.documentaryComplete &&
                verification.displayEligible &&
                textIntegrity.allows(arabicText, frenchText)
}

object HikamRepository {
    private const val HIKAM_DIGITAL_EDITION =
        "Al-Hikam al-ʿAṭāʾiyya — transcription arabe numérique, ibnalarabi.com; " +
            "numérotation retenue pour l’identification interne. Édition imprimée de contrôle humain : à verrouiller."

    private const val AJIBA_EDITION =
        "Ibn ʿAjība, Īqāẓ al-Himam fī Sharḥ al-Ḥikam, éd./corr. " +
            "Muḥammad ʿAbd al-Qādir Naṣṣār, Dār Jawāmiʿ al-Kalim, Le Caire, 632 p."

    private fun verifiedInternalTranslation(note: String) = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = note
    )

    val entries: List<HikmaEntry> = listOf(
        HikmaEntry(
            canonicalId = "hikma_5",
            sourceNumber = 5,
            arabicText = "اجتهادك فيما ضمن لك وتقصيرك فيما طلب منك دليل على انطماس البصيرة منك.",
            frenchText = "Ton effort dans ce qui t’est garanti et ta négligence dans ce qui t’est demandé sont une preuve de l’obscurcissement de ta clairvoyance.",
            theme = "discipline",
            tags = setOf("gestion du temps", "discipline personnelle", "priorités", "bonnes habitudes"),
            source = ClassicalSource(
                author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                workTitle = "Al-Hikam al-ʿAṭāʾiyya",
                edition = HIKAM_DIGITAL_EDITION,
                editor = null,
                volume = null,
                locator = "Hikma 5",
                sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=5",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Arabe et attribution retrouvés dans la source numérique retenue. " +
                    "Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
            ),
            commentary = HikmaCommentary(
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
                    "Extrait arabe retrouvé à la p. 39. Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
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
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            )
        ),
        HikmaEntry(
            canonicalId = "hikma_10",
            sourceNumber = 10,
            arabicText = "الأعمال صور قائمة، وأرواحها وجود سر الإخلاص فيها.",
            frenchText = "Les œuvres sont des formes dressées, et leurs âmes sont la présence en elles du secret de la sincérité.",
            theme = "sincérité",
            tags = setOf("sincérité", "intention", "bonnes habitudes", "discipline personnelle"),
            source = ClassicalSource(
                author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                workTitle = "Al-Hikam al-ʿAṭāʾiyya",
                edition = HIKAM_DIGITAL_EDITION,
                editor = null,
                volume = null,
                locator = "Hikma 10",
                sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=10",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Arabe et attribution retrouvés dans la source numérique retenue. " +
                    "Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
            ),
            commentary = HikmaCommentary(
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
                    "Extrait arabe retrouvé à la p. 50. Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
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
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            )
        ),
        HikmaEntry(
            canonicalId = "hikma_12",
            sourceNumber = 12,
            arabicText = "ما نفع القلب شئ مثل عزلة يدخل بها ميدان فكرة.",
            frenchText = "Rien n’est plus bénéfique au cœur qu’une retraite par laquelle il entre dans le champ de la réflexion.",
            theme = "réflexion",
            tags = setOf("réflexion", "gestion du temps", "discipline personnelle", "maîtrise de soi"),
            source = ClassicalSource(
                author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
                workTitle = "Al-Hikam al-ʿAṭāʾiyya",
                edition = HIKAM_DIGITAL_EDITION,
                editor = null,
                volume = null,
                locator = "Hikma 12",
                sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=12",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Arabe et attribution retrouvés dans la source numérique retenue. " +
                    "Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
            ),
            commentary = HikmaCommentary(
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
                    "Extrait arabe retrouvé à la p. 58. Traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
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
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.COMPLETE_TEXT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            )
        )
    )

    init {
        require(entries.map { it.canonicalId }.distinct().size == entries.size)
        require(entries.map { it.sourceNumber }.distinct().size == entries.size)
    }

    fun byId(id: String): HikmaEntry? =
        entries.firstOrNull { it.canonicalId == id && it.displayEligible }

    fun asDailyReminders(): List<DailyReminder> =
        entries.filter { it.displayEligible }.map { hikma ->
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
}

package com.applicreation0.quransafeguard

data class GhazaliContextControl(
    val beforeLocator: String,
    val afterLocator: String,
    val passageRole: ClassicalPassageRole,
    val continuityChecked: Boolean,
    val nuanceRiskChecked: Boolean
) {
    val valid: Boolean
        get() =
            beforeLocator.isNotBlank() &&
                afterLocator.isNotBlank() &&
                continuityChecked &&
                nuanceRiskChecked
}

data class GhazaliContext(
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

data class GhazaliEntry(
    val canonicalId: String,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val contextControl: GhazaliContextControl,
    val textIntegrity: ClassicalTextIntegrity,
    val context: GhazaliContext?
) {
    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.documentaryComplete &&
                source.workTitle.contains("Ayyuhā al-Walad") &&
                verification.displayEligible &&
                contextControl.valid &&
                contextControl.passageRole == ClassicalPassageRole.AUTHOR_OWN_WORDS &&
                textIntegrity.allows(arabicText, frenchText)
}

object GhazaliRepository {
    private const val AYYUHA_SOURCE =
        "Ayyuhā al-Walad — transcription arabe Wikisource; texte attribué à Abū Ḥāmid al-Ghazālī. " +
            "La traduction française affichée est interne à Quran Safeguard."

    private const val AYYUHA_URL =
        "https://ar.wikisource.org/w/index.php?title=أيها_الولد&oldid=426675"

    private fun verifiedInternalTranslation(note: String) = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationAvailable = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = note
    )

    /**
     * Issue #31 deliberately narrows short al-Ghazālī reminders to Ayyuhā al-Walad.
     * This is a continuous prose work, not a maxim collection. Entries are added
     * only after checking the immediate before/after context and the role of the
     * selected passage.
     */
    val entries: List<GhazaliEntry> = listOf(
        GhazaliEntry(
            canonicalId = "ghazali_ayyuhalwalad_works_not_bankrupt",
            arabicText =
                "لا تكنْ مِنَ الأَعْمالِ مُفْلِساً، ولا مِنَ الأَحْوالِ خَالِياً، " +
                    "وتَيَقَّنْ أَنَّ العِلْمَ المُجَرَّدَ لا يَأخُذُ بِاليَدِ.",
            frenchText =
                "Ne sois pas démuni d’œuvres ni vide d’états spirituels, et sois certain " +
                    "que la science purement théorique, à elle seule, ne te prend pas par la main.",
            theme = "discipline",
            tags = setOf(
                "discipline personnelle",
                "bonnes habitudes",
                "mise en pratique",
                "connaissance",
                "action"
            ),
            source = ClassicalSource(
                author = "Abū Ḥāmid al-Ghazālī",
                workTitle = "Ayyuhā al-Walad (أيها الولد)",
                edition = AYYUHA_SOURCE,
                editor = null,
                volume = null,
                locator =
                    "Section « أيها الولد » — paragraphe commençant " +
                        "« لا تكن من الأعمال مفلسا، ولا من الأحوال خاليا »",
                sourceUrl = AYYUHA_URL,
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Texte arabe, attribution et emplacement retrouvés dans la transcription Wikisource. " +
                    "Traduction interne non certifiée humainement; elle ne conditionne pas " +
                    "l’authenticité documentaire du texte arabe."
            ),
            contextControl = GhazaliContextControl(
                beforeLocator =
                    "Passage immédiatement précédent : discussion de la science sans mise en pratique, " +
                        "puis récit rapporté concernant al-Junayd",
                afterLocator =
                    "Suite immédiate du même paragraphe : exemple des armes et du lion, puis " +
                        "« فكذا لو قرأ رجل مائة ألف مسألة علمية… »",
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS,
                continuityChecked = true,
                nuanceRiskChecked = true
            ),
            textIntegrity = ClassicalTextIntegrity(
                form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                reconstructedOrAssembled = false,
                hasInternalOmissions = false,
                contextChecked = true,
                passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
            ),
            context = GhazaliContext(
                arabicText =
                    "لا تكنْ مِنَ الأَعْمالِ مُفْلِساً، ولا مِنَ الأَحْوالِ خَالِياً، " +
                        "وتَيَقَّنْ أَنَّ العِلْمَ المُجَرَّدَ لا يَأخُذُ بِاليَدِ. " +
                        "مِثَالُهُ: لَوْ كانَ على رَجُلٍ في بَرِّيَّةٍ عَشْرَةُ أَسْيافٍ هِنْدِيَّةٍ " +
                        "مَعَ أَسْلِحَةٍ أُخْرَى، وَكَاَن الرَّجُلُ شُجَاعاً وأَهْلَ حَرْب، " +
                        "فَحَمَلَ عَلَيْهِ أَسَدٌ عَظِيمٌ مَهيبٌ فَمَا ظَنُّكَ؟ " +
                        "هَلْ تَدْفَعُ الأَسْلِحَةُ شَرَّهُ عَنْهُ بِلا اسْتِعْمالِها وضَرْبِها؟ " +
                        "ومِنَ المَعْلُومِ أَنَّهَا لَا تَدْفَعُ إِلَّا بالتَّحْرِيكِ والضَّرْبِ.",
                frenchText =
                    "Ne sois pas démuni d’œuvres ni vide d’états spirituels, et sois certain " +
                        "que la science purement théorique ne te prend pas par la main. Par exemple, " +
                        "si un homme se trouvait dans un désert avec dix sabres indiens et d’autres armes, " +
                        "qu’il soit courageux et expérimenté au combat, puis qu’un lion énorme et redoutable " +
                        "l’attaque, que penses-tu qu’il arriverait ? Les armes repousseraient-elles son mal " +
                        "sans qu’il les utilise et frappe avec elles ? Il est évident qu’elles ne le repoussent " +
                        "que par leur mise en mouvement et leur emploi.",
                source = ClassicalSource(
                    author = "Abū Ḥāmid al-Ghazālī",
                    workTitle = "Ayyuhā al-Walad (أيها الولد)",
                    edition = AYYUHA_SOURCE,
                    editor = null,
                    volume = null,
                    locator =
                        "Section « أيها الولد » — passage continu commençant " +
                            "« لا تكن من الأعمال مفلسا » et poursuivi par l’exemple des armes et du lion",
                    sourceUrl = AYYUHA_URL,
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Contexte continu du même paragraphe, sans assemblage ni résumé."
                ),
                isExcerpt = true,
                textIntegrity = ClassicalTextIntegrity(
                    form = ClassicalTextForm.CONTINUOUS_EXCERPT,
                    reconstructedOrAssembled = false,
                    hasInternalOmissions = false,
                    contextChecked = true,
                    passageRole = ClassicalPassageRole.AUTHOR_OWN_WORDS
                )
            )
        )
    )

    fun byId(id: String): GhazaliEntry? =
        entries.firstOrNull { it.canonicalId == id && it.displayEligible }

    fun asDailyReminders(): List<DailyReminder> =
        entries.filter { it.displayEligible }.map { entry ->
            DailyReminder(
                id = entry.canonicalId,
                type = ReminderType.GHAZALI,
                theme = entry.theme,
                arabicText = entry.arabicText,
                frenchText = entry.frenchText,
                author = entry.source.author,
                book = entry.source.workTitle,
                reference = entry.source.locator,
                authenticity = null,
                tags = entry.tags
            )
        }
}

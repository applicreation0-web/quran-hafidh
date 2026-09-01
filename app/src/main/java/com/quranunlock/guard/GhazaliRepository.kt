package com.applicreation0.quransafeguard

data class GhazaliContext(
    val arabicText: String,
    val frenchText: String,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val isExcerpt: Boolean
) {
    val displayEligible: Boolean
        get() =
            arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.author.isNotBlank() &&
                source.workTitle.isNotBlank() &&
                source.edition.isNotBlank() &&
                source.locator.isNotBlank() &&
                source.sourceUrl.isNotBlank() &&
                verification.displayEligible
}

data class GhazaliEntry(
    val canonicalId: String,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val source: ClassicalSource,
    val verification: ClassicalVerification,
    val context: GhazaliContext?
) {
    val displayEligible: Boolean
        get() =
            canonicalId.isNotBlank() &&
                arabicText.isNotBlank() &&
                frenchText.isNotBlank() &&
                source.author.isNotBlank() &&
                source.workTitle.isNotBlank() &&
                source.edition.isNotBlank() &&
                source.locator.isNotBlank() &&
                source.sourceUrl.isNotBlank() &&
                verification.displayEligible
}

object GhazaliRepository {
    private const val BIDAYAT_SOURCE =
        "Bidāyat al-Hidāya — transcription arabe Wikisource, contrôlée contre une seconde lecture numérique; " +
            "édition imprimée/page de référence humaine à verrouiller."
    private const val IHYA_SOURCE =
        "Iḥyāʾ ʿUlūm al-Dīn — Kitāb Ādāb al-Maʿīsha wa-Akhlāq al-Nubuwwa, " +
            "transcription arabe Wikisource; édition imprimée/page de référence humaine à verrouiller."

    private fun verifiedInternalTranslation(note: String) = ClassicalVerification(
        sourceVerified = true,
        attributionVerified = true,
        translationVerified = true,
        humanVerified = false,
        rightsStatus = TranslationRightsStatus.INTERNAL_TRANSLATION_ALLOWED,
        authenticityStatus = ClassicalAuthenticityStatus.VERIFIED_SOURCE,
        verificationNote = note
    )

    val entries: List<GhazaliEntry> = listOf(
        GhazaliEntry(
            canonicalId = "ghazali_bidaya_religion_two_halves",
            arabicText = "اعلم أن للدين شطرين، أحدهما: ترك المناهي، والآخر: فعل الطاعات.",
            frenchText = "Sache que la religion comporte deux parts : l’une consiste à délaisser les interdits, et l’autre à accomplir les actes d’obéissance.",
            theme = "discipline",
            tags = setOf("discipline personnelle", "bonnes habitudes", "maîtrise de soi"),
            source = ClassicalSource(
                author = "Abū Ḥāmid al-Ghazālī",
                workTitle = "Bidāyat al-Hidāya",
                edition = BIDAYAT_SOURCE,
                editor = null,
                volume = null,
                locator = "Section II — القول في اجتناب المعاصي — transcription Wikisource, ligne 207",
                sourceUrl = "https://ar.wikisource.org/wiki/بداية_الهداية",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "La version précédente avait une variante non exacte (« الدين شطران »). " +
                    "Le texte canonique interne est corrigé sur la transcription retrouvée."
            ),
            context = GhazaliContext(
                arabicText = "القسم الثاني القول في اجتناب المعاصى توطئة اعلم ان للدين شطرين، أحدهما: ترك المناهي، والآخر: فعل الطاعات.. وترك المناهي هو الأشد؛ فإن الطاعات يقدر عليها كل واحد، وترك الشهوات لا يقدر عليه إلا الصديقون، […]",
                frenchText = "Deuxième partie : propos sur l’évitement des désobéissances. Préambule. Sache que la religion comporte deux parts : l’une consiste à délaisser les interdits, et l’autre à accomplir les actes d’obéissance. Délaisser les interdits est le plus difficile ; car chacun peut accomplir les actes d’obéissance, tandis que délaisser les passions n’est à la portée que des véridiques. […]",
                source = ClassicalSource(
                    author = "Abū Ḥāmid al-Ghazālī",
                    workTitle = "Bidāyat al-Hidāya",
                    edition = BIDAYAT_SOURCE,
                    editor = null,
                    volume = null,
                    locator = "Section II — Wikisource, ligne 207",
                    sourceUrl = "https://ar.wikisource.org/wiki/بداية_الهداية",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Contexte continu retrouvé dans le texte d’al-Ghazālī; traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
                ),
                isExcerpt = true
            )
        ),
        GhazaliEntry(
            canonicalId = "ghazali_bidaya_limb_guardianship",
            arabicText = "فأعضاؤك رعاياك، فانظر كيف ترعاها.",
            frenchText = "Tes membres sont ceux dont tu as la charge ; regarde donc comment tu en prends soin.",
            theme = "discipline",
            tags = setOf("soin du corps", "discipline personnelle", "bonnes habitudes", "comportement"),
            source = ClassicalSource(
                author = "Abū Ḥāmid al-Ghazālī",
                workTitle = "Bidāyat al-Hidāya",
                edition = BIDAYAT_SOURCE,
                editor = null,
                volume = null,
                locator = "Section II — القول في اجتناب المعاصي — transcription Wikisource, ligne 208",
                sourceUrl = "https://ar.wikisource.org/wiki/بداية_الهداية",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Texte arabe et attribution retrouvés dans le passage continu de Bidāyat al-Hidāya."
            ),
            context = GhazaliContext(
                arabicText = "واعلم أنك إنما تعصي الله بجوارحك، وهي نعمة من الله عليك وأمانة لديك، فاستعانتك بنعمة الله على معصيته غاية الكفران، وخيانتك في أمانة استودعها الله غاية الطغيان؛ فأعضاؤك رعاياك، فانظر كيف ترعاها؛ فكلكم راع، وكلكم مسؤول عن رعيته. […]",
                frenchText = "Sache que tu ne désobéis à Dieu qu’au moyen de tes membres : ils sont un bienfait de Dieu envers toi et un dépôt confié à toi. Employer le bienfait de Dieu dans la désobéissance est le comble de l’ingratitude, et trahir le dépôt qu’Il t’a confié est le comble de la transgression. Tes membres sont ceux dont tu as la charge ; regarde donc comment tu en prends soin. Chacun de vous est gardien et chacun sera interrogé sur ce dont il a la charge. […]",
                source = ClassicalSource(
                    author = "Abū Ḥāmid al-Ghazālī",
                    workTitle = "Bidāyat al-Hidāya",
                    edition = BIDAYAT_SOURCE,
                    editor = null,
                    volume = null,
                    locator = "Section II — Wikisource, ligne 208",
                    sourceUrl = "https://ar.wikisource.org/wiki/بداية_الهداية",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Contexte continu retrouvé dans le texte d’al-Ghazālī; traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
                ),
                isExcerpt = true
            )
        ),
        GhazaliEntry(
            canonicalId = "ghazali_ihya_outer_inner_adab",
            arabicText = "فإن آداب الظواهر عنوان آداب البواطن، وحركات الجوارح ثمرات الخواطر.",
            frenchText = "Les règles de conduite extérieures sont l’indice des règles de conduite intérieures, et les mouvements des membres sont les fruits des pensées.",
            theme = "comportement",
            tags = setOf("bonnes mœurs", "comportement", "sincérité", "bonnes habitudes"),
            source = ClassicalSource(
                author = "Abū Ḥāmid al-Ghazālī",
                workTitle = "Iḥyāʾ ʿUlūm al-Dīn",
                edition = IHYA_SOURCE,
                editor = null,
                volume = "Rubʿ al-ʿĀdāt, livre 10",
                locator = "Kitāb Ādāb al-Maʿīsha wa-Akhlāq al-Nubuwwa — transcription Wikisource, ligne 97",
                sourceUrl = "https://ar.wikisource.org/wiki/إحياء_علوم_الدين/كتاب_آداب_المعيشة_وأخلاق_النبوة",
                translator = "Traduction interne Quran Safeguard"
            ),
            verification = verifiedInternalTranslation(
                "Texte arabe et attribution retrouvés dans l’ouverture du livre indiqué."
            ),
            context = GhazaliContext(
                arabicText = "أما بعد: فإن آداب الظواهر عنوان آداب البواطن، وحركات الجوارح ثمرات الخواطر، والأعمال نتيجة الأخلاق والآداب رشح المعارف، وسرائر القلوب هي مغارس الأفعال ومنابعها، وأنوار السرائر هي التي تشرق على الظواهر فتزينها وتجليها، وتبدل بالمحاسن مكارهها ومساويها. […]",
                frenchText = "Ensuite : les règles de conduite extérieures sont l’indice des règles de conduite intérieures, les mouvements des membres sont les fruits des pensées, les œuvres sont le résultat des caractères, et les règles de conduite sont l’émanation des connaissances. Les secrets des cœurs sont les lieux où se plantent les actes et leurs sources ; les lumières des réalités intérieures rayonnent sur les apparences, les embellissent et les rendent manifestes, et remplacent par des beautés leurs aspects déplaisants. […]",
                source = ClassicalSource(
                    author = "Abū Ḥāmid al-Ghazālī",
                    workTitle = "Iḥyāʾ ʿUlūm al-Dīn",
                    edition = IHYA_SOURCE,
                    editor = null,
                    volume = "Rubʿ al-ʿĀdāt, livre 10",
                    locator = "Kitāb Ādāb al-Maʿīsha wa-Akhlāq al-Nubuwwa — Wikisource, lignes 97-98",
                    sourceUrl = "https://ar.wikisource.org/wiki/إحياء_علوم_الدين/كتاب_آداب_المعيشة_وأخلاق_النبوة",
                    translator = "Traduction interne Quran Safeguard"
                ),
                verification = verifiedInternalTranslation(
                    "Contexte continu retrouvé dans le texte d’al-Ghazālī; traduction française interne relue contre le passage arabe; pas de certification éditoriale externe."
                ),
                isExcerpt = true
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

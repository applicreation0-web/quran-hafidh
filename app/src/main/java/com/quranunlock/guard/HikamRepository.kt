package com.applicreation0.quransafeguard

/**
 * Canonical in-app source for Al-Hikam al-'Ata'iyya.
 *
 * This is intentionally a verified partial corpus. Never label it as the
 * complete Al-Hikam corpus unless the retained edition has been fully audited.
 * A Hikma is display-eligible only when Arabic, French translation, source and
 * a stable reference are present.
 */
data class HikmaCommentary(
    val commentator: String,
    val workTitle: String,
    val arabicExcerpt: String,
    val frenchTranslation: String,
    val edition: String,
    val locator: String,
    val sourceUrl: String,
    val isExcerpt: Boolean = true
)

data class HikmaEntry(
    val canonicalId: String,
    val sourceNumber: Int,
    val arabicText: String,
    val frenchText: String,
    val transliteration: String,
    val theme: String,
    val tags: Set<String>,
    val sourceUrl: String,
    val verificationDate: String,
    val sourceNote: String,
    val commentary: HikmaCommentary?
)

object HikamRepository {
    private const val VERIFIED_ON = "2026-09-01"
    private const val COMMENTARY_EDITION =
        "Ibn ʿAjība, Īqāẓ al-Himam fī Sharḥ al-Ḥikam, éd./corr. " +
            "Muḥammad ʿAbd al-Qādir Naṣṣār, Dār Jawāmiʿ al-Kalim, Le Caire, 632 p."

    val entries: List<HikmaEntry> = listOf(
        HikmaEntry(
            canonicalId = "hikma_5",
            sourceNumber = 5,
            arabicText = "اجْتِهَادُكَ فِيمَا ضُمِنَ لَكَ وَتَقْصِيرُكَ فِيمَا طُلِبَ مِنْكَ دَلِيلٌ عَلَى انْطِمَاسِ الْبَصِيرَةِ مِنْكَ.",
            frenchText = "T’épuiser pour ce qui t’est garanti tout en négligeant ce qui t’est demandé est un signe d’obscurcissement de la clairvoyance.",
            transliteration = "Ijtihāduka fīmā ḍumina laka wa-taqṣīruka fīmā ṭuliba minka dalīlun ʿalā inṭimāsi l-baṣīrati minka.",
            theme = "discipline",
            tags = setOf("gestion du temps", "discipline personnelle", "priorités", "bonnes habitudes"),
            sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=5",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-ʿAṭāʾiyya • Hikma 5 • numérotation de la source retenue",
            commentary = HikmaCommentary(
                commentator = "Ibn ʿAjība",
                workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                arabicExcerpt = "قلت : الاجتهاد في الشيء استفراغ الجهد والطاقة في طلبه ، والتقصير هو التفريط والتضييع والبصيرة ناظر القلب […]",
                frenchTranslation = "J’ai dit : l’effort appliqué à une chose consiste à déployer toute son énergie pour la rechercher ; la négligence est le relâchement et l’abandon. La clairvoyance est le regard du cœur. […]",
                edition = COMMENTARY_EDITION,
                locator = "Ḥikma 5 • p. 39",
                sourceUrl = "https://ablibrary.net/book_content/b/9684/39"
            )
        ),
        HikmaEntry(
            canonicalId = "hikma_10",
            sourceNumber = 10,
            arabicText = "الأَعْمَالُ صُوَرٌ قَائِمَةٌ، وَأَرْوَاحُهَا وُجُودُ سِرِّ الإِخْلَاصِ فِيهَا.",
            frenchText = "Les œuvres sont des formes dressées ; leur âme est la présence du secret de la sincérité en elles.",
            transliteration = "Al-aʿmālu ṣuwarun qāʾimatun, wa-arwāḥuhā wujūdu sirri l-ikhlāṣi fīhā.",
            theme = "sincérité",
            tags = setOf("sincérité", "intention", "bonnes habitudes", "discipline personnelle"),
            sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=10",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-ʿAṭāʾiyya • Hikma 10 • numérotation de la source retenue",
            commentary = HikmaCommentary(
                commentator = "Ibn ʿAjība",
                workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                arabicExcerpt = "قلت : الأعمال كلها أشباح وأجساد وأرواحها وجود الإخلاص فيها فكما لا قيام للأشباح إلا بالأرواح […]",
                frenchTranslation = "J’ai dit : toutes les œuvres sont comme des formes et des corps, et leur âme est la présence de la sincérité. De même que les formes ne subsistent que par les âmes […].",
                edition = COMMENTARY_EDITION,
                locator = "Ḥikma 10 • p. 50",
                sourceUrl = "https://ablibrary.net/book_content/b/9684/50"
            )
        ),
        HikmaEntry(
            canonicalId = "hikma_12",
            sourceNumber = 12,
            arabicText = "مَا نَفَعَ الْقَلْبَ شَيْءٌ مِثْلُ عُزْلَةٍ يَدْخُلُ بِهَا مَيْدَانَ فِكْرَةٍ.",
            frenchText = "Rien n’est plus bénéfique au cœur qu’un moment de retrait qui ouvre un espace à la réflexion.",
            transliteration = "Mā nafaʿa l-qalba shayʾun mithlu ʿuzlatin yadkhulu bihā maydāna fikrah.",
            theme = "réflexion",
            tags = setOf("réflexion", "gestion du temps", "discipline personnelle", "maîtrise de soi"),
            sourceUrl = "https://ablibrary.net/book_content/8865/58",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-ʿAṭāʾiyya • Hikma 12 • numérotation de la source retenue",
            commentary = HikmaCommentary(
                commentator = "Ibn ʿAjība",
                workTitle = "Īqāẓ al-Himam fī Sharḥ al-Ḥikam",
                arabicExcerpt = "قلت : لا شيء أنفع للقلب من عزلة مصحوبة بفكرة لأن العزلة كالحمية والفكرة كالدواء […]",
                frenchTranslation = "J’ai dit : rien n’est plus bénéfique au cœur qu’un retrait accompagné de réflexion, car le retrait est comme une diète et la réflexion comme un remède. […]",
                edition = COMMENTARY_EDITION,
                locator = "Ḥikma 12 • p. 58",
                sourceUrl = "https://ablibrary.net/book_content/b/9684/58"
            )
        )
    )

    init {
        require(entries.map { it.canonicalId }.distinct().size == entries.size) {
            "Duplicate Hikam canonical IDs"
        }
        require(entries.map { it.sourceNumber }.distinct().size == entries.size) {
            "Duplicate Hikam source numbers"
        }
        entries.forEach { hikma ->
            require(hikma.arabicText.isNotBlank())
            require(hikma.frenchText.isNotBlank())
            require(hikma.transliteration.isNotBlank())
            require(hikma.sourceUrl.startsWith("https://"))
            require(hikma.sourceNote.isNotBlank())
        }
    }

    fun byId(id: String): HikmaEntry? =
        entries.firstOrNull { it.canonicalId == id }

    fun asDailyReminders(): List<DailyReminder> = entries.map { hikma ->
        DailyReminder(
            id = hikma.canonicalId,
            type = ReminderType.HIKAM,
            theme = hikma.theme,
            arabicText = hikma.arabicText,
            frenchText = hikma.frenchText,
            author = "Ibn ʿAṭāʾ Allāh al-Iskandarī",
            book = "Al-Hikam al-ʿAṭāʾiyya",
            reference = "Hikma " + hikma.sourceNumber + " (numérotation de la source retenue)",
            authenticity = null,
            tags = hikma.tags
        )
    }
}

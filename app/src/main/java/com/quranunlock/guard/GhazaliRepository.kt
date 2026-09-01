package com.applicreation0.quransafeguard

/**
 * Canonical in-app source for direct excerpts from Abu Hamid al-Ghazali.
 *
 * The entries in this repository are source texts attributed to al-Ghazali,
 * not summaries and not later commentary. Any future "Approfondir" layer for
 * these entries must use contiguous context from the same work unless a
 * separately identified classical commentator is explicitly selected.
 *
 * As with Al-Hikam, a displayed entry must keep Arabic, French and provenance
 * together so that content cannot be detached from its source.
 */
data class GhazaliEntry(
    val canonicalId: String,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val book: String,
    val reference: String,
    val sourceUrl: String,
    val verificationDate: String,
    val sourceNote: String
)

object GhazaliRepository {
    private const val VERIFIED_ON = "2026-08-31"

    val entries: List<GhazaliEntry> = listOf(
        GhazaliEntry(
            canonicalId = "ghazali_bidaya_religion_two_halves",
            arabicText = "اعْلَمْ أَنَّ الدِّينَ شَطْرَانِ: أَحَدُهُمَا تَرْكُ الْمَنَاهِي، وَالْآخَرُ فِعْلُ الطَّاعَاتِ.",
            frenchText = "Sache que la religion comporte deux volets : délaisser les interdits et accomplir les actes d’obéissance.",
            theme = "discipline",
            tags = setOf("discipline personnelle", "bonnes habitudes", "maîtrise de soi"),
            book = "Bidâyat al-Hidâya",
            reference = "Section : éviter les désobéissances",
            sourceUrl = "https://islamweb.net/ar/library/content/60/4822/",
            verificationDate = VERIFIED_ON,
            sourceNote = "Attributed in Siyar A‘lam an-Nubala’ to Abu Hamid al-Ghazali; also present in Bidayat al-Hidaya"
        ),
        GhazaliEntry(
            canonicalId = "ghazali_bidaya_limb_guardianship",
            arabicText = "فَأَعْضَاؤُكَ رَعَايَاكَ، فَانْظُرْ كَيْفَ تَرْعَاهَا.",
            frenchText = "Tes membres sont sous ta responsabilité : veille donc à la manière dont tu en prends soin.",
            theme = "discipline",
            tags = setOf("soin du corps", "discipline personnelle", "bonnes habitudes", "comportement"),
            book = "Bidâyat al-Hidâya",
            reference = "Section : éviter les désobéissances",
            sourceUrl = "https://baheth.ieasybooks.com/ar/media/شرح-بداية-الهداية-للإمام-الغزالي-رحمه-الله-تعالى-15",
            verificationDate = VERIFIED_ON,
            sourceNote = "Bidayat al-Hidaya • section on avoiding disobedience"
        ),
        GhazaliEntry(
            canonicalId = "ghazali_ihya_outer_inner_adab",
            arabicText = "آدَابُ الظَّوَاهِرِ عُنْوَانُ آدَابِ الْبَوَاطِنِ، وَحَرَكَاتُ الْجَوَارِحِ ثَمَرَاتُ الْخَوَاطِرِ.",
            frenchText = "Les bonnes manières extérieures révèlent celles de l’intérieur, et les gestes du corps sont les fruits des pensées.",
            theme = "comportement",
            tags = setOf("bonnes mœurs", "comportement", "sincérité", "bonnes habitudes"),
            book = "Ihyâ’ ‘Ulûm ad-Dîn",
            reference = "Livre des règles de vie et des caractères prophétiques",
            sourceUrl = "https://ar.wikisource.org/wiki/إحياء_علوم_الدين/كتاب_آداب_المعيشة_وأخلاق_النبوة",
            verificationDate = VERIFIED_ON,
            sourceNote = "Ihya’ ‘Ulum ad-Din • Book of conduct and Prophetic character"
        )
    )

    init {
        require(entries.map { it.canonicalId }.distinct().size == entries.size) {
            "Duplicate Ghazali canonical IDs"
        }
        entries.forEach { entry ->
            require(entry.arabicText.isNotBlank()) { "Missing Ghazali Arabic text" }
            require(entry.frenchText.isNotBlank()) { "Missing Ghazali French text" }
            require(entry.sourceUrl.startsWith("https://")) { "Missing Ghazali source URL" }
            require(entry.sourceNote.isNotBlank()) { "Missing Ghazali source note" }
        }
    }

    fun asDailyReminders(): List<DailyReminder> = entries.map { entry ->
        DailyReminder(
            id = entry.canonicalId,
            type = ReminderType.GHAZALI,
            theme = entry.theme,
            arabicText = entry.arabicText,
            frenchText = entry.frenchText,
            author = "Abû Hâmid al-Ghazâlî",
            book = entry.book,
            reference = entry.reference,
            authenticity = null,
            tags = entry.tags
        )
    }
}

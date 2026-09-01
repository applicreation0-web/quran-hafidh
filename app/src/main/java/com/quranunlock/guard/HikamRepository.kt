package com.applicreation0.quransafeguard

/**
 * Canonical in-app source for Al-Hikam al-'Ata'iyya.
 *
 * Hikam must never be duplicated in ReminderLibrary or in another content store.
 * Any feature that wants to display a Hikma reads from this repository.
 *
 * The corpus is intentionally partial for now. Only entries whose Arabic text
 * and French translation have both been verified are admitted here.
 */
data class HikmaEntry(
    val canonicalId: String,
    val sourceNumber: Int,
    val arabicText: String,
    val frenchText: String,
    val theme: String,
    val tags: Set<String>,
    val sourceUrl: String,
    val verificationDate: String,
    val sourceNote: String
)

object HikamRepository {
    private const val VERIFIED_ON = "2026-08-31"

    val entries: List<HikmaEntry> = listOf(
        HikmaEntry(
            canonicalId = "hikma_5",
            sourceNumber = 5,
            arabicText = "اجْتِهَادُكَ فِيمَا ضُمِنَ لَكَ وَتَقْصِيرُكَ فِيمَا طُلِبَ مِنْكَ دَلِيلٌ عَلَى انْطِمَاسِ الْبَصِيرَةِ مِنْكَ.",
            frenchText = "T’épuiser pour ce qui t’est garanti tout en négligeant ce qui t’est demandé est un signe d’obscurcissement de la clairvoyance.",
            theme = "discipline",
            tags = setOf("gestion du temps", "discipline personnelle", "priorités", "bonnes habitudes"),
            sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=5",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-‘Ata’iyya • Hikma 5"
        ),
        HikmaEntry(
            canonicalId = "hikma_10",
            sourceNumber = 10,
            arabicText = "الأَعْمَالُ صُوَرٌ قَائِمَةٌ، وَأَرْوَاحُهَا وُجُودُ سِرِّ الإِخْلَاصِ فِيهَا.",
            frenchText = "Les œuvres sont des formes dressées ; leur âme est la présence du secret de la sincérité en elles.",
            theme = "sincérité",
            tags = setOf("sincérité", "intention", "bonnes habitudes", "discipline personnelle"),
            sourceUrl = "https://www.ibnalarabi.com/books/hikam-ataiya.php?id=10",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-‘Ata’iyya • Hikma 10"
        ),
        HikmaEntry(
            canonicalId = "hikma_12",
            sourceNumber = 12,
            arabicText = "مَا نَفَعَ الْقَلْبَ شَيْءٌ مِثْلُ عُزْلَةٍ يَدْخُلُ بِهَا مَيْدَانَ فِكْرَةٍ.",
            frenchText = "Rien n’est plus bénéfique au cœur qu’un moment de retrait qui ouvre un espace à la réflexion.",
            theme = "réflexion",
            tags = setOf("réflexion", "gestion du temps", "discipline personnelle", "maîtrise de soi"),
            sourceUrl = "https://ablibrary.net/book_content/8865/58",
            verificationDate = VERIFIED_ON,
            sourceNote = "Al-Hikam al-‘Ata’iyya • Hikma 12, attested in Ibn ‘Ajiba’s commentary"
        )
    )

    init {
        require(entries.map { it.canonicalId }.distinct().size == entries.size) {
            "Duplicate Hikam canonical IDs"
        }
        require(entries.map { it.sourceNumber }.distinct().size == entries.size) {
            "Duplicate Hikam source numbers"
        }
    }

    fun asDailyReminders(): List<DailyReminder> = entries.map { hikma ->
        DailyReminder(
            id = hikma.canonicalId,
            type = ReminderType.HIKAM,
            theme = hikma.theme,
            arabicText = hikma.arabicText,
            frenchText = hikma.frenchText,
            author = "Ibn ‘Atâ’ Allâh al-Iskandarî",
            book = "Al-Hikam al-‘Atâ’iyya",
            reference = "Hikma " + hikma.sourceNumber + " (numérotation de la source retenue)",
            authenticity = null,
            tags = hikma.tags
        )
    }
}

package com.applicreation0.quransafeguard

/**
 * Editorial terminology aid for Al-Hikam.
 *
 * The Hikma matn and its French translation remain untouched. This layer only
 * adds clearly separated bracketed terminology notes when a technical term is
 * actually present in the Arabic matn. The English Islamic Pearls page and its
 * glossary are used as a secondary terminology control; Arabic remains the
 * authority.
 */
data class HikamTechnicalTerm(
    val key: String,
    val displayTerm: String,
    val frenchMeaning: String,
    val arabicNeedles: List<String>
)

object HikamTechnicalLexicon {
    private val terms = listOf(
        HikamTechnicalTerm(
            key = "arif",
            displayTerm = "ʿārif",
            frenchMeaning = "connaissant de Dieu ; gnostique",
            arabicNeedles = listOf("عارف", "العارفين", "عارفين")
        ),
        HikamTechnicalTerm(
            key = "abd",
            displayTerm = "ʿabd",
            frenchMeaning = "serviteur ; adorateur",
            arabicNeedles = listOf("العبد", "عبد", "عباد")
        ),
        HikamTechnicalTerm(
            key = "adab",
            displayTerm = "adab",
            frenchMeaning = "juste convenance ; comportement spirituel approprié",
            arabicNeedles = listOf("الأدب", "ادب", "أدب")
        ),
        HikamTechnicalTerm(
            key = "aghyar",
            displayTerm = "aghyār",
            frenchMeaning = "les autres que Dieu ; les réalités qui détournent de Sa présence",
            arabicNeedles = listOf("الأغيار", "اغيار", "أغيار")
        ),
        HikamTechnicalTerm(
            key = "al_haqq",
            displayTerm = "al-Ḥaqq",
            frenchMeaning = "le Réel ; la Vérité, Nom divin selon le contexte",
            arabicNeedles = listOf("الحق")
        ),
        HikamTechnicalTerm(
            key = "ihsan",
            displayTerm = "iḥsān",
            frenchMeaning = "excellence spirituelle ; adoration avec présence à Dieu",
            arabicNeedles = listOf("الإحسان", "احسان", "إحسان")
        ),
        HikamTechnicalTerm(
            key = "hadra",
            displayTerm = "ḥaḍra",
            frenchMeaning = "présence spirituelle ; Présence divine, sans sens spatial",
            arabicNeedles = listOf("الحضرة", "حضرته", "حضرة")
        ),
        HikamTechnicalTerm(
            key = "madad",
            displayTerm = "madad",
            frenchMeaning = "soutien ; influx ou secours spirituel",
            arabicNeedles = listOf("مدد", "أمداد", "امداد", "إمداد")
        ),
        HikamTechnicalTerm(
            key = "majdhub",
            displayTerm = "majdhūb",
            frenchMeaning = "attiré vers Dieu par attraction spirituelle",
            arabicNeedles = listOf("المجذوب", "مجذوب", "المجذوبين", "المجاذيب")
        ),
        HikamTechnicalTerm(
            key = "murid",
            displayTerm = "murīd",
            frenchMeaning = "aspirant ; disciple engagé sur la voie",
            arabicNeedles = listOf("المريد", "مريد", "المريدين")
        ),
        HikamTechnicalTerm(
            key = "qabd",
            displayTerm = "qabḍ",
            frenchMeaning = "contraction ou resserrement spirituel du cœur",
            arabicNeedles = listOf("القبض", "قبض")
        ),
        HikamTechnicalTerm(
            key = "bast",
            displayTerm = "basṭ",
            frenchMeaning = "dilatation ou expansion spirituelle du cœur",
            arabicNeedles = listOf("البسط", "بسط")
        ),
        HikamTechnicalTerm(
            key = "salik",
            displayTerm = "sālik",
            frenchMeaning = "cheminant ; voyageur sur la voie spirituelle",
            arabicNeedles = listOf("السالك", "سالك", "السالكين")
        ),
        HikamTechnicalTerm(
            key = "shawq",
            displayTerm = "shawq",
            frenchMeaning = "désir ardent ; nostalgie ou aspiration du cœur vers Dieu",
            arabicNeedles = listOf("الشوق", "شوق")
        ),
        HikamTechnicalTerm(
            key = "wali",
            displayTerm = "walī",
            frenchMeaning = "ami rapproché de Dieu ; saint",
            arabicNeedles = listOf("ولي", "الأولياء", "اوليا", "أولياء")
        ),
        HikamTechnicalTerm(
            key = "warid",
            displayTerm = "wārid",
            frenchMeaning = "influx ou inspiration spirituelle survenant dans le cœur",
            arabicNeedles = listOf("الوارد", "وارد", "الواردات", "واردات")
        ),
        HikamTechnicalTerm(
            key = "wird",
            displayTerm = "wird",
            frenchMeaning = "pratique ou litanie spirituelle régulière",
            arabicNeedles = listOf("الورد", "ورد", "أوراد", "اوراد")
        ),
        HikamTechnicalTerm(
            key = "yaqin",
            displayTerm = "yaqīn",
            frenchMeaning = "certitude spirituelle",
            arabicNeedles = listOf("اليقين", "يقين")
        ),
        HikamTechnicalTerm(
            key = "dhikr",
            displayTerm = "dhikr",
            frenchMeaning = "rappel ou invocation de Dieu",
            arabicNeedles = listOf("الذكر", "ذكره", "ذكر")
        ),
        HikamTechnicalTerm(
            key = "zuhd",
            displayTerm = "zuhd",
            frenchMeaning = "détachement ascétique à l’égard du monde",
            arabicNeedles = listOf("الزهد", "زهد")
        ),
        HikamTechnicalTerm(
            key = "tajrid",
            displayTerm = "tajrīd",
            frenchMeaning = "dépouillement ; état de détachement des moyens ordinaires",
            arabicNeedles = listOf("التجريد", "تجريد")
        ),
        HikamTechnicalTerm(
            key = "asbab",
            displayTerm = "asbāb",
            frenchMeaning = "causes ou moyens ordinaires par lesquels une chose advient",
            arabicNeedles = listOf("الأسباب", "اسباب", "أسباب")
        ),
        HikamTechnicalTerm(
            key = "himma",
            displayTerm = "himma",
            frenchMeaning = "aspiration ou résolution spirituelle",
            arabicNeedles = listOf("الهمة", "همم", "همة")
        ),
        HikamTechnicalTerm(
            key = "maqam",
            displayTerm = "maqām",
            frenchMeaning = "station spirituelle relativement stable",
            arabicNeedles = listOf("المقام", "مقام", "المقامات")
        ),
        HikamTechnicalTerm(
            key = "hal",
            displayTerm = "ḥāl",
            frenchMeaning = "état spirituel reçu ou traversé",
            arabicNeedles = listOf("الحال", "حال", "الأحوال", "احوال", "أحوال")
        ),
        HikamTechnicalTerm(
            key = "ma_rifa",
            displayTerm = "maʿrifa",
            frenchMeaning = "connaissance spirituelle directe",
            arabicNeedles = listOf("المعرفة", "معرفة", "المعارف")
        ),
        HikamTechnicalTerm(
            key = "fana",
            displayTerm = "fanāʾ",
            frenchMeaning = "effacement ou extinction de la considération de soi",
            arabicNeedles = listOf("الفناء", "فناء")
        ),
        HikamTechnicalTerm(
            key = "baqa",
            displayTerm = "baqāʾ",
            frenchMeaning = "subsistance spirituelle après l’effacement",
            arabicNeedles = listOf("البقاء", "بقاء")
        )
    )

    fun forHikma(hikma: HikmaEntry): List<HikamTechnicalTerm> {
        val arabic = hikma.canonicalArabicText
        return terms.filter { term ->
            term.arabicNeedles.any(arabic::contains)
        }
    }
}

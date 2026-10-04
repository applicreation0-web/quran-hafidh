package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONArray
import java.time.LocalDate

enum class ReminderType {
    HADITH,
    GHAZALI,
    HIKAM
}

data class DailyReminder(
    val id: String,
    val type: ReminderType,
    val theme: String,
    val arabicText: String,
    val frenchText: String,
    val author: String,
    val book: String,
    val reference: String,
    val authenticity: String?,
    val tags: Set<String>
)

object ReminderLibrary {
    val items: List<DailyReminder> = listOf(
        DailyReminder(
            id = "hadith_bukhari_1",
            type = ReminderType.HADITH,
            theme = "intention",
            arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى.",
            frenchText = "Les actes ne valent que par les intentions, et chacun obtient selon ce qu’il a eu comme intention.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 1 • Livre 1, hadith 1",
            authenticity = "Sahih",
            tags = setOf("sincérité", "intention", "discipline personnelle", "bonnes habitudes")
        ),
        DailyReminder(
            id = "hadith_muslim_2593",
            type = ReminderType.HADITH,
            theme = "douceur",
            arabicText = "إِنَّ اللَّهَ رَفِيقٌ يُحِبُّ الرِّفْقَ، وَيُعْطِي عَلَى الرِّفْقِ مَا لَا يُعْطِي عَلَى الْعُنْفِ.",
            frenchText = "Allah est Doux et Il aime la douceur. Il accorde par la douceur ce qu’Il n’accorde pas par la rudesse.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 2593 • Livre 45, hadith 99",
            authenticity = "Sahih",
            tags = setOf("douceur", "comportement", "famille", "vie en communauté")
        ),
        DailyReminder(
            id = "hadith_bukhari_6018",
            type = ReminderType.HADITH,
            theme = "voisinage",
            arabicText = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلَا يُؤْذِ جَارَهُ.",
            frenchText = "Que celui qui croit en Allah et au Jour dernier ne fasse pas de tort à son voisin.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 6018 • Livre 78, hadith 49",
            authenticity = "Sahih",
            tags = setOf("voisinage", "respect du voisin", "entraide", "espaces communs")
        ),
        DailyReminder(
            id = "hadith_bukhari_5027",
            type = ReminderType.HADITH,
            theme = "coran",
            arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ.",
            frenchText = "Les meilleurs d’entre vous sont ceux qui apprennent le Coran et l’enseignent.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 5027 • Livre 66, hadith 49",
            authenticity = "Sahih",
            tags = setOf("mérite du Coran", "lecture du Coran", "apprentissage du Coran", "transmission")
        ),
        DailyReminder(
            id = "hadith_bukhari_6114",
            type = ReminderType.HADITH,
            theme = "maîtrise de soi",
            arabicText = "لَيْسَ الشَّدِيدُ بِالصُّرَعَةِ، إِنَّمَا الشَّدِيدُ الَّذِي يَمْلِكُ نَفْسَهُ عِنْدَ الْغَضَبِ.",
            frenchText = "Le véritable fort n’est pas celui qui terrasse les autres, mais celui qui se maîtrise lorsqu’il est en colère.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 6114 • Livre 78, hadith 141",
            authenticity = "Sahih",
            tags = setOf("maîtrise de soi", "patience", "comportement", "bonnes mœurs")
        ),
        DailyReminder(
            id = "hadith_muslim_223",
            type = ReminderType.HADITH,
            theme = "propreté",
            arabicText = "الطُّهُورُ شَطْرُ الإِيمَانِ.",
            frenchText = "La purification est la moitié de la foi.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 223 • Livre 2, hadith 1",
            authenticity = "Sahih",
            tags = setOf("propreté", "hygiène", "pureté", "ablutions", "soin du corps", "propreté des vêtements et des lieux")
        ),
        DailyReminder(
            id = "hadith_nasai_5",
            type = ReminderType.HADITH,
            theme = "propreté",
            arabicText = "السِّوَاكُ مَطْهَرَةٌ لِلْفَمِ مَرْضَاةٌ لِلرَّبِّ.",
            frenchText = "Le siwâk purifie la bouche et est une cause d’agrément du Seigneur.",
            author = "Prophète Muhammad ﷺ",
            book = "Sunan an-Nasa’i",
            reference = "Hadith 5 • Livre de la purification, hadith 5",
            authenticity = "Sahih (Darussalam)",
            tags = setOf("hygiène bucco-dentaire", "propreté", "soin du corps", "bonnes habitudes")
        ),
        DailyReminder(
            id = "hadith_bukhari_5971",
            type = ReminderType.HADITH,
            theme = "famille",
            arabicText = "أُمُّكَ، ثُمَّ أُمُّكَ، ثُمَّ أُمُّكَ، ثُمَّ أَبُوكَ.",
            frenchText = "Ta mère, puis ta mère, puis ta mère, puis ton père.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 5971 • Livre 78, hadith 2",
            authenticity = "Sahih",
            tags = setOf("parents", "famille", "liens de parenté", "bon comportement")
        ),
        DailyReminder(
            id = "hadith_bukhari_5997",
            type = ReminderType.HADITH,
            theme = "famille",
            arabicText = "مَنْ لَا يَرْحَمُ لَا يُرْحَمُ.",
            frenchText = "Celui qui ne fait pas miséricorde ne recevra pas de miséricorde.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 5997 • Livre 78, hadith 28",
            authenticity = "Sahih",
            tags = setOf("enfants", "famille", "douceur", "miséricorde")
        ),
        DailyReminder(
            id = "hadith_tirmidhi_3895",
            type = ReminderType.HADITH,
            theme = "famille",
            arabicText = "خَيْرُكُمْ خَيْرُكُمْ لِأَهْلِهِ، وَأَنَا خَيْرُكُمْ لِأَهْلِي.",
            frenchText = "Les meilleurs d’entre vous sont les meilleurs envers leur famille, et je suis le meilleur d’entre vous envers ma famille.",
            author = "Prophète Muhammad ﷺ",
            book = "Jami’ at-Tirmidhi",
            reference = "Hadith 3895",
            authenticity = "Hasan gharîb sahih (at-Tirmidhi)",
            tags = setOf("conjoint", "famille", "bonnes mœurs", "comportement")
        ),
        DailyReminder(
            id = "hadith_muslim_2588",
            type = ReminderType.HADITH,
            theme = "pardon",
            arabicText = "مَا نَقَصَتْ صَدَقَةٌ مِنْ مَالٍ، وَمَا زَادَ اللَّهُ عَبْدًا بِعَفْوٍ إِلَّا عِزًّا.",
            frenchText = "L’aumône ne diminue pas les biens, et Allah n’augmente un serviteur qui pardonne qu’en dignité.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 2588 • Livre 45, hadith 90",
            authenticity = "Sahih",
            tags = setOf("pardon", "générosité", "humilité", "entraide")
        ),
        DailyReminder(
            id = "hadith_muslim_2999",
            type = ReminderType.HADITH,
            theme = "patience",
            arabicText = "إِنْ أَصَابَتْهُ سَرَّاءُ شَكَرَ فَكَانَ خَيْرًا لَهُ، وَإِنْ أَصَابَتْهُ ضَرَّاءُ صَبَرَ فَكَانَ خَيْرًا لَهُ.",
            frenchText = "Quand un bien l’atteint, le croyant remercie et c’est un bien pour lui ; quand une épreuve l’atteint, il patiente et c’est un bien pour lui.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 2999 • Livre 55, hadith 82",
            authenticity = "Sahih",
            tags = setOf("gratitude", "patience", "maîtrise de soi", "discipline personnelle")
        ),
        DailyReminder(
            id = "hadith_tirmidhi_1956",
            type = ReminderType.HADITH,
            theme = "communauté",
            arabicText = "تَبَسُّمُكَ فِي وَجْهِ أَخِيكَ لَكَ صَدَقَةٌ.",
            frenchText = "Ton sourire au visage de ton frère est une aumône.",
            author = "Prophète Muhammad ﷺ",
            book = "Jami’ at-Tirmidhi",
            reference = "Hadith 1956",
            authenticity = "Hasan (Darussalam)",
            tags = setOf("bonnes mœurs", "vie en communauté", "entraide", "douceur")
        ),
        DailyReminder(
            id = "hadith_bukhari_6138",
            type = ReminderType.HADITH,
            theme = "liens de parenté",
            arabicText = "مَنْ كَانَ يُؤْمِنُ بِاللَّهِ وَالْيَوْمِ الآخِرِ فَلْيَصِلْ رَحِمَهُ.",
            frenchText = "Que celui qui croit en Allah et au Jour dernier entretienne ses liens de parenté.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih al-Bukhari",
            reference = "Hadith 6138 • Livre 78, hadith 165 • également Muslim 47",
            authenticity = "Sahih",
            tags = setOf("liens de parenté", "famille", "entraide", "vie en communauté")
        ),
        DailyReminder(
            id = "hadith_muslim_223_quran",
            type = ReminderType.HADITH,
            theme = "coran",
            arabicText = "وَالْقُرْآنُ حُجَّةٌ لَكَ أَوْ عَلَيْكَ.",
            frenchText = "Le Coran est une preuve en ta faveur ou contre toi.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 223 • Livre 2, hadith 1",
            authenticity = "Sahih",
            tags = setOf("mise en pratique du Coran", "lecture du Coran", "discipline personnelle")
        ),
        DailyReminder(
            id = "hadith_tirmidhi_1956_common_space",
            type = ReminderType.HADITH,
            theme = "communauté",
            arabicText = "وَإِمَاطَتُكَ الْحَجَرَ وَالشَّوْكَةَ وَالْعَظْمَ عَنِ الطَّرِيقِ لَكَ صَدَقَةٌ.",
            frenchText = "Retirer de la route une pierre, une épine ou un os est pour toi une aumône.",
            author = "Prophète Muhammad ﷺ",
            book = "Jami’ at-Tirmidhi",
            reference = "Hadith 1956",
            authenticity = "Hasan (Darussalam)",
            tags = setOf("respect des espaces communs", "propreté des lieux", "entraide", "vie en communauté")
        ),
        DailyReminder(
            id = "hadith_muslim_2699_help",
            type = ReminderType.HADITH,
            theme = "entraide",
            arabicText = "وَاللَّهُ فِي عَوْنِ الْعَبْدِ مَا كَانَ الْعَبْدُ فِي عَوْنِ أَخِيهِ.",
            frenchText = "Allah vient en aide au serviteur tant que le serviteur vient en aide à son frère.",
            author = "Prophète Muhammad ﷺ",
            book = "Sahih Muslim",
            reference = "Hadith 2699a • Livre 48, hadith 48",
            authenticity = "Sahih",
            tags = setOf("entraide", "vie en communauté", "générosité", "liens de parenté")
        ),


    )

    private var cachedBundled: List<DailyReminder>? = null

    fun all(context: Context): List<DailyReminder> =
        items +
            HikamRepository.asDailyReminders(context) +
            bundled(context)

    fun byId(context: Context, id: String): DailyReminder? =
        all(context).firstOrNull { it.id == id }

    fun totalCount(context: Context): Int = all(context).size

    @Synchronized
    private fun bundled(context: Context): List<DailyReminder> {
        cachedBundled?.let { return it }
        val loaded = runCatching {
            val raw = context.assets.open("reminders/hadeethenc_snapshot.json")
                .bufferedReader(Charsets.UTF_8).use { it.readText() }
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.getJSONObject(index)
                    val tagsJson = obj.optJSONArray("tags")
                    val tags = buildSet {
                        if (tagsJson != null) {
                            for (tagIndex in 0 until tagsJson.length()) {
                                tagsJson.optString(tagIndex)
                                    .takeIf { it.isNotBlank() }
                                    ?.let(::add)
                            }
                        }
                    }
                    if (!obj.optBoolean("displayEligible", false)) {
                        continue
                    }
                    add(
                        DailyReminder(
                            id = obj.getString("id"),
                            type = ReminderType.HADITH,
                            theme = obj.optString("theme", "bonnes_moeurs"),
                            arabicText = obj.getString("arabicText"),
                            frenchText = obj.getString("frenchText"),
                            author = obj.optString("author", "Prophète Muhammad ﷺ"),
                            book = obj.optString(
                                "book",
                                "Encyclopédie des hadiths traduits (HadeethEnc)"
                            ),
                            reference = obj.getString("reference"),
                            authenticity = obj.optString("authenticity")
                                .takeIf { it.isNotBlank() },
                            tags = tags.ifEmpty { setOf("bonnes mœurs") }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
        cachedBundled = loaded
        return loaded
    }
}

object ThoughtOfDayPolicy {
    const val REMINDER_HOUR = 20
    const val NOTIFICATION_ID = 8200
    const val FULL_CARD_ACTIVITY_SIMPLE_NAME = "ThoughtOfDayActivity"

    fun shouldNotify(lastEpochDay: Long, currentEpochDay: Long): Boolean =
        lastEpochDay != currentEpochDay
}

object DailyReminderManager {
    private const val FILE = "daily_reminders"
    private const val DAY_KEY = "selected_epoch_day"
    private const val ID_KEY = "selected_id"
    private const val RECENT_KEY = "recent_ids"
    private const val MAX_RECENT = 30

    private val weeklyThemes = listOf(
        "voisinage",
        "famille",
        "propreté",
        "douceur",
        "coran",
        "patience",
        "discipline",
        "communauté",
        "sincérité",
        "liens de parenté"
    )

    // Hadith remains the main source. Hikam slots are used only when
    // authenticity-gated entries are display-eligible; otherwise the
    // selection tiers fall back to verified available content.
    private val dailyTypeCycle = listOf(
        ReminderType.HADITH,
        ReminderType.HADITH,
        ReminderType.HIKAM,
        ReminderType.HADITH,
        ReminderType.HIKAM,
        ReminderType.HADITH,
        ReminderType.HADITH
    )

    @Synchronized
    fun today(context: Context, date: LocalDate = LocalDate.now()): DailyReminder {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val epochDay = date.toEpochDay()
        val storedDay = prefs.getLong(DAY_KEY, Long.MIN_VALUE)
        val storedId = prefs.getString(ID_KEY, null)

        if (storedDay == epochDay && storedId != null) {
            ReminderLibrary.byId(context, storedId)?.let { return it }
        }

        val recent = prefs.getString(RECENT_KEY, "")
            .orEmpty()
            .split('|')
            .filter { it.isNotBlank() }

        val theme = weeklyThemes[Math.floorMod((epochDay / 7L).toInt(), weeklyThemes.size)]
        val desiredType = dailyTypeCycle[Math.floorMod(date.dayOfWeek.value - 1, dailyTypeCycle.size)]

        val selected = chooseReminderFromList(
            all = ReminderLibrary.all(context),
            epochDay = epochDay,
            theme = theme,
            desiredType = desiredType,
            recentIds = recent.toSet()
        )

        val updatedRecent = buildList {
            add(selected.id)
            addAll(recent.filterNot { it == selected.id })
        }.take(MAX_RECENT)

        prefs.edit()
            .putLong(DAY_KEY, epochDay)
            .putString(ID_KEY, selected.id)
            .putString(RECENT_KEY, updatedRecent.joinToString("|"))
            .apply()

        return selected
    }

    internal fun chooseReminderFromList(
        all: List<DailyReminder>,
        epochDay: Long,
        theme: String,
        desiredType: ReminderType,
        recentIds: Set<String>
    ): DailyReminder {
        require(all.isNotEmpty()) { "Thought-of-day corpus must not be empty." }
        val fresh = all.filterNot { it.id in recentIds }

        val tiers = listOf(
            fresh.filter { it.type == desiredType && (it.theme == theme || theme in it.tags) },
            fresh.filter { it.theme == theme || theme in it.tags },
            fresh.filter { it.type == desiredType },
            fresh,
            all.filter { it.theme == theme || theme in it.tags },
            all.filter { it.type == desiredType },
            all
        )

        val candidates = tiers.firstOrNull { it.isNotEmpty() }.orEmpty()
        val seed = (epochDay xor (theme.hashCode().toLong() shl 1)).toInt()
        return candidates[Math.floorMod(seed, candidates.size)]
    }
}

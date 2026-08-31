package com.applicreation0.quransafeguard

import android.content.Context
import org.json.JSONObject
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
    val tags: Set<String>,
    val sourceProvider: String,
    val sourceId: String,
    val sourceVersion: String,
    val sourceFetchedAt: String,
    val reviewStatus: String,
    val translationStatus: String,
    val sourceUrl: String?
)

object ReminderLibrary {
    const val EXPECTED_TOTAL = 150
    const val EXPECTED_HADITHS = 144
    const val EXPECTED_SCHOLAR_WISDOM = 6
    private const val HADITH_ASSET = "reminders/verified_hadiths.json"

    @Volatile
    private var cachedHadiths: List<DailyReminder>? = null

    val scholarItems: List<DailyReminder> = listOf(
        DailyReminder(
            id = "ghazali_bidaya_religion_two_halves",
            type = ReminderType.GHAZALI,
            theme = "discipline",
            arabicText = "اعْلَمْ أَنَّ الدِّينَ شَطْرَانِ: أَحَدُهُمَا تَرْكُ الْمَنَاهِي، وَالْآخَرُ فِعْلُ الطَّاعَاتِ.",
            frenchText = "Sache que la religion comporte deux volets : délaisser les interdits et accomplir les actes d’obéissance.",
            author = "Abû Hâmid al-Ghazâlî",
            book = "Bidâyat al-Hidâya",
            reference = "Section : éviter les désobéissances",
            authenticity = null,
            tags = setOf("discipline personnelle", "bonnes habitudes", "maîtrise de soi"),
            sourceProvider = "Texte primaire vérifié",
            sourceId = "bidaya-avoid-disobedience-1",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        ),
        DailyReminder(
            id = "ghazali_bidaya_limb_guardianship",
            type = ReminderType.GHAZALI,
            theme = "discipline",
            arabicText = "فَأَعْضَاؤُكَ رَعَايَاكَ، فَانْظُرْ كَيْفَ تَرْعَاهَا.",
            frenchText = "Tes membres sont sous ta responsabilité : veille donc à la manière dont tu en prends soin.",
            author = "Abû Hâmid al-Ghazâlî",
            book = "Bidâyat al-Hidâya",
            reference = "Section : éviter les désobéissances",
            authenticity = null,
            tags = setOf("soin du corps", "discipline personnelle", "bonnes habitudes", "comportement"),
            sourceProvider = "Texte primaire vérifié",
            sourceId = "bidaya-avoid-disobedience-2",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        ),
        DailyReminder(
            id = "ghazali_ihya_outer_inner_adab",
            type = ReminderType.GHAZALI,
            theme = "bonnes mœurs",
            arabicText = "آدَابُ الظَّوَاهِرِ عُنْوَانُ آدَابِ الْبَوَاطِنِ، وَحَرَكَاتُ الْجَوَارِحِ ثَمَرَاتُ الْخَوَاطِرِ.",
            frenchText = "Les bonnes manières extérieures révèlent celles de l’intérieur, et les gestes du corps sont les fruits des pensées.",
            author = "Abû Hâmid al-Ghazâlî",
            book = "Ihyâ’ ‘Ulûm ad-Dîn",
            reference = "Livre des règles de vie et des caractères prophétiques",
            authenticity = null,
            tags = setOf("bonnes mœurs", "comportement", "sincérité", "bonnes habitudes"),
            sourceProvider = "Texte primaire vérifié",
            sourceId = "ihya-adab-living-outward-inward",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        ),
        DailyReminder(
            id = "hikma_5",
            type = ReminderType.HIKAM,
            theme = "discipline",
            arabicText = "اجْتِهَادُكَ فِيمَا ضُمِنَ لَكَ وَتَقْصِيرُكَ فِيمَا طُلِبَ مِنْكَ دَلِيلٌ عَلَى انْطِمَاسِ الْبَصِيرَةِ مِنْكَ.",
            frenchText = "T’épuiser pour ce qui t’est garanti tout en négligeant ce qui t’est demandé est un signe d’obscurcissement de la clairvoyance.",
            author = "Ibn ‘Atâ’ Allâh al-Iskandarî",
            book = "Al-Hikam al-‘Atâ’iyya",
            reference = "Hikma 5 (numérotation courante)",
            authenticity = null,
            tags = setOf("gestion du temps", "discipline personnelle", "priorités", "bonnes habitudes"),
            sourceProvider = "Al-Hikam al-‘Atâ’iyya",
            sourceId = "hikma-5",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        ),
        DailyReminder(
            id = "hikma_10",
            type = ReminderType.HIKAM,
            theme = "sincérité",
            arabicText = "الأَعْمَالُ صُوَرٌ قَائِمَةٌ، وَأَرْوَاحُهَا وُجُودُ سِرِّ الإِخْلَاصِ فِيهَا.",
            frenchText = "Les œuvres sont des formes dressées ; leur âme est la présence du secret de la sincérité en elles.",
            author = "Ibn ‘Atâ’ Allâh al-Iskandarî",
            book = "Al-Hikam al-‘Atâ’iyya",
            reference = "Hikma 10 (numérotation courante)",
            authenticity = null,
            tags = setOf("sincérité", "intention", "bonnes habitudes", "discipline personnelle"),
            sourceProvider = "Al-Hikam al-‘Atâ’iyya",
            sourceId = "hikma-10",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        ),
        DailyReminder(
            id = "hikma_12",
            type = ReminderType.HIKAM,
            theme = "réflexion",
            arabicText = "مَا نَفَعَ الْقَلْبَ شَيْءٌ مِثْلُ عُزْلَةٍ يَدْخُلُ بِهَا مَيْدَانَ فِكْرَةٍ.",
            frenchText = "Rien n’est plus bénéfique au cœur qu’un moment de retrait qui ouvre un espace à la réflexion.",
            author = "Ibn ‘Atâ’ Allâh al-Iskandarî",
            book = "Al-Hikam al-‘Atâ’iyya",
            reference = "Hikma 12 (numérotation courante)",
            authenticity = null,
            tags = setOf("réflexion", "gestion du temps", "discipline personnelle", "maîtrise de soi"),
            sourceProvider = "Al-Hikam al-‘Atâ’iyya",
            sourceId = "hikma-12",
            sourceVersion = "revue-2026-08-31",
            sourceFetchedAt = "2026-08-31",
            reviewStatus = "MANUALLY_VERIFIED_PRIMARY_TEXT",
            translationStatus = "EDITORIAL_TRANSLATION_REVIEWED",
            sourceUrl = null
        )
    )

    fun all(context: Context): List<DailyReminder> =
        verifiedHadiths(context) + scholarItems

    fun byId(context: Context, id: String): DailyReminder? =
        all(context).firstOrNull { it.id == id }

    fun verifiedHadiths(context: Context): List<DailyReminder> {
        cachedHadiths?.let { return it }
        return synchronized(this) {
            cachedHadiths?.let { return@synchronized it }
            val loaded = loadHadithAsset(context)
            cachedHadiths = loaded
            loaded
        }
    }

    fun hasCompleteVerifiedLibrary(context: Context): Boolean {
        val hadiths = runCatching { verifiedHadiths(context) }.getOrDefault(emptyList())
        return hadiths.size == EXPECTED_HADITHS &&
            scholarItems.size == EXPECTED_SCHOLAR_WISDOM &&
            hadiths.all { item ->
                item.type == ReminderType.HADITH &&
                    item.authenticity?.isNotBlank() == true &&
                    item.reference.isNotBlank() &&
                    item.sourceProvider == "HadeethEnc.com" &&
                    item.sourceId.isNotBlank() &&
                    item.sourceVersion.isNotBlank() &&
                    item.reviewStatus == "VERIFIED_OFFICIAL_SOURCE" &&
                    item.translationStatus == "SOURCE_TRANSLATION_UNMODIFIED"
            } &&
            scholarItems.all { item ->
                item.type != ReminderType.HADITH &&
                    item.author.isNotBlank() &&
                    item.book.isNotBlank() &&
                    item.reference.isNotBlank() &&
                    item.sourceId.isNotBlank() &&
                    item.sourceVersion.isNotBlank() &&
                    item.reviewStatus == "MANUALLY_VERIFIED_PRIMARY_TEXT" &&
                    item.translationStatus == "EDITORIAL_TRANSLATION_REVIEWED"
            }
    }

    private fun loadHadithAsset(context: Context): List<DailyReminder> {
        val raw = context.assets.open(HADITH_ASSET)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        check(root.getInt("count") == EXPECTED_HADITHS) {
            "Verified hadith asset must contain exactly $EXPECTED_HADITHS entries."
        }
        val array = root.getJSONArray("items")
        val items = buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val tagsJson = item.getJSONArray("tags")
                val tags = buildSet {
                    for (tagIndex in 0 until tagsJson.length()) {
                        add(tagsJson.getString(tagIndex))
                    }
                }
                add(
                    DailyReminder(
                        id = item.getString("id"),
                        type = ReminderType.HADITH,
                        theme = item.getString("theme"),
                        arabicText = item.getString("arabicText"),
                        frenchText = item.getString("frenchText"),
                        author = item.getString("author"),
                        book = item.getString("book"),
                        reference = item.getString("reference"),
                        authenticity = item.getString("authenticity"),
                        tags = tags,
                        sourceProvider = item.getString("sourceProvider"),
                        sourceId = item.getString("sourceId"),
                        sourceVersion = item.getString("sourceVersion"),
                        sourceFetchedAt = item.getString("sourceFetchedAt"),
                        reviewStatus = item.getString("reviewStatus"),
                        translationStatus = item.getString("translationStatus"),
                        sourceUrl = item.optString("sourceUrl").ifBlank { null }
                    )
                )
            }
        }
        check(items.map { it.id }.distinct().size == items.size) {
            "Verified hadith IDs must be unique."
        }
        return items
    }
}

object DailyReminderManager {
    private const val FILE = "daily_reminders"
    private const val DAY_KEY = "selected_epoch_day"
    private const val ID_KEY = "selected_id"
    private const val RECENT_KEY = "recent_ids"
    private const val NOTIFICATION_ENABLED = "notification_enabled"
    private const val LAST_NOTIFICATION_DAY = "last_notification_epoch_day"
    private const val MAX_RECENT = 60

    const val NOTIFICATION_HOUR = 20

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
        "maîtrise de soi",
        "entraide",
        "bonnes mœurs"
    )

    private val dailyTypeCycle = listOf(
        ReminderType.HADITH,
        ReminderType.HADITH,
        ReminderType.HADITH,
        ReminderType.GHAZALI,
        ReminderType.HADITH,
        ReminderType.HIKAM,
        ReminderType.HADITH
    )

    fun notificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(NOTIFICATION_ENABLED, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(NOTIFICATION_ENABLED, enabled)
            .apply()
    }

    fun wasNotificationShownToday(
        context: Context,
        date: LocalDate = LocalDate.now()
    ): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getLong(LAST_NOTIFICATION_DAY, Long.MIN_VALUE) == date.toEpochDay()

    fun markNotificationShown(
        context: Context,
        date: LocalDate = LocalDate.now()
    ) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_NOTIFICATION_DAY, date.toEpochDay())
            .apply()
    }

    @Synchronized
    fun today(context: Context, date: LocalDate = LocalDate.now()): DailyReminder {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val epochDay = date.toEpochDay()
        val storedDay = prefs.getLong(DAY_KEY, Long.MIN_VALUE)
        val storedId = prefs.getString(ID_KEY, null)

        if (storedDay == epochDay && storedId != null) {
            ReminderLibrary.byId(context, storedId)?.let { return it }
        }

        val all = ReminderLibrary.all(context)
        check(all.isNotEmpty()) { "Daily reminder library is empty." }

        val recent = prefs.getString(RECENT_KEY, "")
            .orEmpty()
            .split('|')
            .filter { it.isNotBlank() }

        val theme = weeklyThemes[Math.floorMod((epochDay / 7L).toInt(), weeklyThemes.size)]
        val desiredType = dailyTypeCycle[Math.floorMod(date.dayOfWeek.value - 1, dailyTypeCycle.size)]

        val selected = chooseReminder(
            all = all,
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

    private fun chooseReminder(
        all: List<DailyReminder>,
        epochDay: Long,
        theme: String,
        desiredType: ReminderType,
        recentIds: Set<String>
    ): DailyReminder {
        val fresh = all.filterNot { it.id in recentIds }

        // Weekly theme takes precedence over source-type rotation.
        val tiers = listOf(
            fresh.filter { it.type == desiredType && matchesTheme(it, theme) },
            fresh.filter { matchesTheme(it, theme) },
            fresh.filter { it.type == desiredType },
            fresh,
            all.filter { matchesTheme(it, theme) },
            all.filter { it.type == desiredType },
            all
        )

        val candidates = tiers.firstOrNull { it.isNotEmpty() }.orEmpty()
        val seed = (epochDay xor (theme.hashCode().toLong() shl 1)).toInt()
        return candidates[Math.floorMod(seed, candidates.size)]
    }

    private fun matchesTheme(item: DailyReminder, theme: String): Boolean =
        item.theme == theme || theme in item.tags
}

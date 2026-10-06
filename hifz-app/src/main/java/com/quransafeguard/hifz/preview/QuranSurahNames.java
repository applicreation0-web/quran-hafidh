package com.quransafeguard.hifz.preview;

/** The 114 canonical surah names, in Mushaf order, for direct-navigation pickers. */
final class QuranSurahNames {
    private static final String[] NAMES = {
        "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام", "الأعراف", "الأنفال",
        "التوبة", "يونس", "هود", "يوسف", "الرعد", "إبراهيم", "الحجر", "النحل",
        "الإسراء", "الكهف", "مريم", "طه", "الأنبياء", "الحج", "المؤمنون", "النور",
        "الفرقان", "الشعراء", "النمل", "القصص", "العنكبوت", "الروم", "لقمان", "السجدة",
        "الأحزاب", "سبأ", "فاطر", "يس", "الصافات", "ص", "الزمر", "غافر",
        "فصلت", "الشورى", "الزخرف", "الدخان", "الجاثية", "الأحقاف", "محمد", "الفتح",
        "الحجرات", "ق", "الذاريات", "الطور", "النجم", "القمر", "الرحمن", "الواقعة",
        "الحديد", "المجادلة", "الحشر", "الممتحنة", "الصف", "الجمعة", "المنافقون", "التغابن",
        "الطلاق", "التحريم", "الملك", "القلم", "الحاقة", "المعارج", "نوح", "الجن",
        "المزمل", "المدثر", "القيامة", "الإنسان", "المرسلات", "النبأ", "النازعات", "عبس",
        "التكوير", "الانفطار", "المطففين", "الانشقاق", "البروج", "الطارق", "الأعلى", "الغاشية",
        "الفجر", "البلد", "الشمس", "الليل", "الضحى", "الشرح", "التين", "العلق",
        "القدر", "البينة", "الزلزلة", "العاديات", "القارعة", "التكاثر", "العصر", "الهمزة",
        "الفيل", "قريش", "الماعون", "الكوثر", "الكافرون", "النصر", "المسد", "الإخلاص",
        "الفلق", "الناس"
    };

    private QuranSurahNames() {}

    /**
     * The Arabic name wrapped in Unicode bidi isolates (FSI … PDI). Device report: in the
     * French (LTR) labels the verse numbers following an Arabic name were pulled into its
     * right-to-left run ("objectif 81 → 1 البقرة" instead of "البقرة 1 → 81"). Isolated, the
     * name stays right-to-left inside while the numbers around it keep their own order.
     */
    static String name(int surah) {
        return "\u2068" + rawName(surah) + "\u2069";
    }

    static String rawName(int surah) {
        if (surah < 1 || surah > 114) throw new IllegalArgumentException("invalid surah " + surah);
        return NAMES[surah - 1];
    }

    /**
     * User decision ("tout en arabe"): every verse range reads like the Mushaf, start on the
     * right and the arrow toward An-Nās — "88 ← 1 البقرة" inside one surah, and across surahs
     * each end grouped "name n°" ("الذاريات 37 ← الحجرات 1", option B).
     *
     * <p>Built directly in that visual order inside a left-to-right isolate (LRI … PDI), with
     * no-break spaces: on the device a right-to-left isolate was not honoured once a Parcours
     * cell wrapped ("2 ← 1 الفاتحة" came out reversed), whereas a left-to-right run never
     * reorders. Only the surah names themselves stay right-to-left (FSI … PDI).
     */
    static String range(com.quransafeguard.hifz.core.VerseRef start, com.quransafeguard.hifz.core.VerseRef end) {
        if (start.getSurah() == end.getSurah()) {
            if (start.getAyah() == end.getAyah()) return verse(start);
            return "\u2066" + end.getAyah() + NBSP + "←" + NBSP + start.getAyah() + NBSP + name(start.getSurah()) + "\u2069";
        }
        return "\u2066" + name(end.getSurah()) + NBSP + end.getAyah() + NBSP + "←" + NBSP
            + name(start.getSurah()) + NBSP + start.getAyah() + "\u2069";
    }

    /** One verse, Mushaf style: "16 البقرة". */
    static String verse(com.quransafeguard.hifz.core.VerseRef ref) {
        return "\u2066" + ref.getAyah() + NBSP + name(ref.getSurah()) + "\u2069";
    }

    private static final String NBSP = "\u00A0";

    static String labelFor(int surah) {
        return surah + " · " + name(surah);
    }

    /** Shared "jump to a surah" dialog for every screen that navigates the Mushaf by page. */
    static void showPicker(android.app.Activity activity, GeometryRepository geometry, java.util.function.IntConsumer onPageChosen) {
        String[] items = new String[114];
        for (int surah = 1; surah <= 114; surah++) items[surah - 1] = labelFor(surah);
        new android.app.AlertDialog.Builder(activity)
            .setTitle("Aller à une sourate")
            .setItems(items, (dialog, which) -> onPageChosen.accept(geometry.pageForVerse(new com.quransafeguard.hifz.core.VerseRef(which + 1, 1))))
            .show();
    }
}

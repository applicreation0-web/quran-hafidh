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

    static String name(int surah) {
        if (surah < 1 || surah > 114) throw new IllegalArgumentException("invalid surah " + surah);
        return NAMES[surah - 1];
    }

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

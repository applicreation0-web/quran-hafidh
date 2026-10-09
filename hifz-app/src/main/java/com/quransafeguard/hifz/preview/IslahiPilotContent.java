package com.quransafeguard.hifz.preview;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Source-checked ENGLISH editorial digests of Tadabbur-i Qur'an, volume 9,
 * Sūrah al-Nās (114:1–6). Not a transcript, a translation of the Urdu original,
 * or the full copyrighted English commentary. Every range is attached to a
 * specific cited section; no inferred verse/paragraph mappings are fabricated.
 * The entirety of this pilot stays available without network permission.
 */
public final class IslahiPilotContent {
    private IslahiPilotContent() {}

    public static final String NAS_TITLE =
        "Seeking Refuge from Satan's Deceptive Suggestions";
    public static final String NAS_SUMMARY =
        "Paired with al-Falaq, al-Nas is a prayer for God's protection. "
        + "Whereas al-Falaq seeks refuge from several evils, this surah focuses "
        + "on Satan, whose deceptive suggestions threaten people. It names God "
        + "as mankind's Lord, Sovereign and God, and exposes the enemy's tactics "
        + "and his accomplices among human beings and jinn.";
    public static final String NAS_SOURCE =
        "Amīn Aḥsan Iṣlāḥī, Tadabbur-i Qur'an, vol. 9; "
        + "English translation by Dr Shehzad Saleem. "
        + "Central Theme: printed p. 1; commentary: printed pp. 2–6.";

    public static final class Section {
        public final int firstAyah;
        public final int lastAyah;
        public final String title;
        public final String readingNote;
        public final String printedPages;
        public Section(int first, int last, String title, String note, String pages) {
            if (first < 1 || last < first || last > 6) throw new IllegalArgumentException("range");
            firstAyah = first;
            lastAyah = last;
            this.title = title;
            readingNote = note;
            printedPages = pages;
        }
        public String reference() {
            return "114:" + firstAyah + (firstAyah == lastAyah ? "" : "–" + lastAyah);
        }
    }

    private static final List<Section> NAS_SECTIONS =
        Collections.unmodifiableList(Arrays.asList(
            new Section(1, 3,
                "The One in whom refuge is sought",
                "The three divine titles form a connected argument: the One who cares "
                + "for humanity is its rightful Sovereign, and He alone deserves "
                + "worship and reliance.",
                "Printed p. 2"),
            new Section(4, 4,
                "How the deceiver withdraws",
                "Iṣlāḥī explains al-khannās as the deceiver who retreats after "
                + "misleading others and abandons them to the consequences. "
                + "Satan influences through persuasion and false promises, "
                + "not irresistible power.",
                "Printed pp. 3–5"),
            new Section(5, 6,
                "The targets and agents of suggestion",
                "The suggestions target the human heart. Iṣlāḥī identifies those "
                + "who entice others towards evil among both human beings and jinn.",
                "Printed p. 5")
        ));

    public static boolean hasEnglishReadingNotes(int surah, int first, int last) {
        return surah == 114 && first == 1 && last == 6;
    }

    public static boolean isMultiPageNavigationPilot(int surah, int first, int last) {
        return surah == 9 && first == 38 && last == 42;
    }

    public static List<Section> nasSections() {
        return NAS_SECTIONS;
    }
}

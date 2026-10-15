package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only Al-Baqarah navigation. Grand themes are a SHORT EDITORIAL
 * representation of Mawdudi's Topics and their Interconnection, not quotations
 * of titles invented for him. Fine ayah-theme titles live in the PRIVATE pack.
 * No Hifz engine, earned progress or Ibn Kathir boundary is mutated here.
 */
final class BaqaraPilotSpec {
    static final int FIRST_AYAH = 1;
    static final int LAST_AYAH = 286;
    static final String MAWDUDI_INTRO = "Towards Understanding the Qur'an, Al-Baqarah, Topics and their Interconnection";
    static final String THEMES_SOURCE = "Ayah Themes (editorial index, not Mawdudi's own headings)";

    static final class Theme {
        final int first;
        final int last;
        final String displayTitle;
        Theme(int first, int last, String displayTitle) {
            this.first = first;
            this.last = last;
            this.displayTitle = displayTitle;
        }
        boolean contains(int ayah) { return ayah >= first && ayah <= last; }
        String range() { return "2:" + first + "–" + last; }
    }

    // EXACT 10 ranges from Mawdudi's introduction; SHORT LABELS ARE EDITORIAL.
    private static final Theme[] THEMES = {
        new Theme(1, 20, "Guidance and Responses to Revelation"),
        new Theme(21, 29, "Invitation to Divine Guidance"),
        new Theme(30, 39, "Adam and the Origin of Guidance"),
        new Theme(40, 120, "The Israelites and Divine Guidance"),
        new Theme(121, 141, "Abraham and the Inheritance of Faith"),
        new Theme(142, 152, "The Change of Qiblah"),
        new Theme(153, 251, "Building and Guiding the Community"),
        new Theme(252, 260, "Accountability and Resurrection"),
        new Theme(261, 283, "Charity, Interest and Transactions"),
        new Theme(284, 286, "Faith and the Closing Supplication")
    };

    // EXACT 40 verse-section intervals of the English Tafhim web edition.
    private static final int[][] COMMENTARY_SECTIONS = {
        {1,7},{8,20},{21,29},{30,39},{40,46},{47,59},{60,61},{62,71},
        {72,82},{83,86},{87,96},{97,103},{104,112},{113,121},{122,129},
        {130,141},{142,147},{148,151},{152,163},{164,167},{168,176},
        {177,182},{183,188},{189,196},{197,210},{211,216},{217,221},
        {222,228},{229,231},{232,235},{236,242},{243,248},{249,253},
        {254,257},{258,260},{261,266},{267,273},{274,281},{282,283},{284,286}
    };

    private BaqaraPilotSpec() {}

    static List<Theme> themes() {
        ArrayList<Theme> items = new ArrayList<>();
        Collections.addAll(items, THEMES);
        return Collections.unmodifiableList(items);
    }

    static int themeIndexForAyah(int ayah) {
        int valid = Math.max(FIRST_AYAH, Math.min(LAST_AYAH, ayah));
        for (int i = 0; i < THEMES.length; i++) if (THEMES[i].contains(valid)) return i;
        throw new IllegalStateException("Mawdudi macro structure has a gap");
    }

    static int sectionIndexForAyah(int ayah) {
        for (int i = 0; i < COMMENTARY_SECTIONS.length; i++)
            if (ayah >= COMMENTARY_SECTIONS[i][0] && ayah <= COMMENTARY_SECTIONS[i][1])
                return i;
        return -1;
    }

    static int[] sectionRange(int index) {
        return COMMENTARY_SECTIONS[index].clone();
    }

    static int sectionCount() { return COMMENTARY_SECTIONS.length; }
}

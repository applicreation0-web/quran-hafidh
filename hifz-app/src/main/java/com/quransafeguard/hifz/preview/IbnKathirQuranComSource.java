package com.quransafeguard.hifz.preview;

/**
 * Quran.com (English, abridged Ibn Kathir): verified public-source references.
 *
 * Opens the ACTUAL commentary for the first verse in each of the 1,903 pinned
 * digital groups. Never guesses a boundary or embeds the third-party commentary.
 *
 * Only the source's explicitly checked Qaf headings are included as a tiny
 * read-only, reference-labelled sample. They do not create mnemonic cues.
 */
final class IbnKathirQuranComSource {
    private IbnKathirQuranComSource() {}

    static String urlForGroup(IbnKathirGroupIndex.Group group) {
        if(group==null || group.surah<1 || group.surah>114
             || group.startAyah<1 || group.endAyah<group.startAyah
             || group.endAyah>286)
            throw new IllegalArgumentException("Unverified Ibn Kathir source range");
        // Exact verified Quran.com tafsir route. No user controlled URL or host.
        return "https://quran.com/en/"+group.surah+":"+group.startAyah
             +"/tafsirs/en-tafisr-ibn-kathir";
    }

    /** Original source headings; no translations, Quran text or generated themes. */
    static String[] verifiedQafHeadings(IbnKathirGroupIndex.Group group) {
        if(group==null || group.surah!=50) return new String[0];
        switch(group.startAyah) {
            case 1:
                if(group.endAyah!=5) break;
                return new String[] {
                    "The Disbelievers wonder at the Message and Resurrection"
                };
            case 6:
                if(group.endAyah!=11) break;
                return new String[] {
                    "Allah's Power and Ability over what is Greater than Resurrection"
                };
            case 12:
                if(group.endAyah!=15) break;
                return new String[] {
                    "Reminding the Quraysh of the Destruction of earlier Disbelieving Nations",
                    "Repeating the Creation is Easier than originating It"
                };
            case 16:
                if(group.endAyah!=22) break;
                return new String[] {
                    "Allah encompasses and watches all of Man's Activity",
                    "Reminding Mankind of the Stupor of Death, the Blast of the Trumpet and the Day of Gathering"
                };
            case 23:
                if(group.endAyah!=29) break;
                return new String[] {
                    "The Angel will bear Witness; Allah commands that the Disbeliever be thrown into the Fire",
                    "Man and Devil dispute before Allah"
                };
            case 30:
                if(group.endAyah!=35) break;
                return new String[] {
                    "Jahannam and Paradise and their Dwellers"
                };
            case 36:
                if(group.endAyah!=40) break;
                return new String[] {
                    "Warning the Disbelievers of the imminent Torment; commanding the Prophet to pray and have Patience"
                };
            case 41:
                if(group.endAyah!=45) break;
                return new String[] {
                    "Admonition from Some Scenes of the Day of Resurrection",
                    "Comforting the Prophet"
                };
        }
        return new String[0];
    }
}

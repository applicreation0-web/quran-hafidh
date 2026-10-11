package com.quransafeguard.hifz.preview;

/**
 * Local, source-attested English Ibn Kathir headings. No browser, URL,
 * machine-generated title or commentary. Missing headings stay absent.
 * The group locator is not itself proof of an approved mnemonic Amorce.
 * Sources: Qaf editorial checkpoint + original digital 53.json at
 * spa5k/tafsir_api commit eb82bb6294efe30ad5c135c03b1864afaa70e855.
 */
final class IbnKathirOfflineTitles {
    private IbnKathirOfflineTitles() {}
    static String[] verifiedHeadings(IbnKathirGroupIndex.Group group) {
        if(group==null)return new String[0];
        if(group.surah==50)return verifiedQafHeadings(group);
        if(group.surah==53)return verifiedNajmHeadings(group);
        return new String[0];
    }
    private static String[] verifiedNajmHeadings(IbnKathirGroupIndex.Group group) {
        switch(group.startAyah) {
            case 1:
                if(group.endAyah==4) return new String[]{
                    "The First Surah in which a Prostration is revealed",
                    "Allah swears the Messenger is True and His Words are a Revelation from Him"};
                break;
            case 5:
                if(group.endAyah==18) return new String[]{
                    "The Trustworthy Angel brought Allah's Revelation to the Trustworthy Messenger (ﷺ)"};
                break;
            case 19:
                if(group.endAyah==26) return new String[]{"Refuting Idolatry, Al-Lat and Al-'Uzza"};
                break;
            case 27:
                if(group.endAyah==30) return new String[]{
                    "Refuting the Claim of the Idolators that the Angels are Allah's Daughters"};
                break;
            case 31:
                if(group.endAyah==32) return new String[]{
                    "Allah knows Every Matter, whether Small or Big, and He rewards Each according to His or Her Deeds"};
                break;
            case 33:
                if(group.endAyah==41) return new String[]{
                    "Chastising Those Who disobey Allah and stop giving Charity"};
                break;
            case 42:
                if(group.endAyah==55) return new String[]{
                    "Some Attributes of the Lord, that He returns Man as He originated Him, and some of what He does with His Servants"};
                break;
            case 56:
                if(group.endAyah==62) return new String[]{
                    "A Warning and Exhortation, the Order to prostrate and to be humble"};
                break;
        }
        return new String[0];
    }
    /** Checked Qaf group headings: faithful exact English quotations only. */
    private static String[] verifiedQafHeadings(IbnKathirGroupIndex.Group group) {
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

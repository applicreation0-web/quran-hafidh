package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Al-Baqarah thematic index adapted from https://quran.com/al-baqarah/info
 * (Surah Overview, adapted from Ibn Ashur). English navigation labels are
 * shortened editorial renderings, not verbatim quotes. Verse references are
 * overlapping thematic links, NEVER a partition or new Ibn Kathir borders.
 */
final class IbnAshurBaqaraThemes {
    static final String SOURCE_URL = "https://quran.com/al-baqarah/info";

    static final class Theme {
        final String id, titleEn;
        private final int[] bounds;
        Theme(String id, String titleEn, int... bounds) {
            if (bounds.length == 0 || bounds.length % 2 != 0)
                throw new IllegalArgumentException("Pairs of boundaries required");
            for (int i = 0; i < bounds.length; i += 2)
                if (bounds[i] < 1 || bounds[i+1] > 286 || bounds[i] > bounds[i+1])
                    throw new IllegalArgumentException("Bad Al-Baqarah reference");
            this.id = id;
            this.titleEn = titleEn;
            this.bounds = bounds.clone();
        }
        boolean contains(int ayah) {
            for (int i = 0; i < bounds.length; i+=2)
                if (ayah >= bounds[i] && ayah <= bounds[i+1]) return true;
            return false;
        }
        boolean intersects(IbnKathirGroupIndex.Group group) {
            if (group.surah != 2) return false;
            for (int i = 0; i < bounds.length; i+=2)
                if (group.startAyah <= bounds[i+1] && group.endAyah >= bounds[i])
                    return true;
            return false;
        }
        String references() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < bounds.length; i+=2) {
                if (i != 0) b.append(", ");
                b.append("2:").append(bounds[i]);
                if (bounds[i] != bounds[i+1]) b.append("–").append(bounds[i+1]);
            }
            return b.toString();
        }
    }

    private static final List<Theme> ALL;
    static {
        List<Theme> t = new ArrayList<>();
        t.add(new Theme("opening", "The Opening Challenge", 1,1,23,23));
        t.add(new Theme("humanity", "Believers, Disbelievers and Hypocrites", 2,20));
        t.add(new Theme("adam", "Adam and Satan's Enmity", 30,39));
        t.add(new Theme("israelites", "History of the Israelites and the Covenant", 40,123));
        t.add(new Theme("disputes", "Disputes involving Jews and Christians", 105,121));
        t.add(new Theme("abraham", "Abraham and the Inheritance of His Faith", 124,141));
        t.add(new Theme("qiblah", "The Changing of the Qiblah", 142,150));
        t.add(new Theme("righteousness", "Defining True Righteousness", 177,177));
        t.add(new Theme("abrogation", "The Principle of Abrogation", 106,106));
        t.add(new Theme("creation", "Signs in Creation and Refutation of Shirk", 164,173));
        t.add(new Theme("legislation", "Legislation for the Muslim Community", 177,283));
        t.add(new Theme("reminders", "Parables and Spiritual Reminders",
            18,19,74,74,153,153,189,189,217,217,243,243,255,255));
        t.add(new Theme("supplication", "Concluding Supplication", 284,286));
        ALL = Collections.unmodifiableList(t);
    }
    private IbnAshurBaqaraThemes() {}
    static List<Theme> all() { return ALL; }

    static List<Integer> matchingIndices(Theme theme, List<IbnKathirGroupIndex.Group> groups) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++)
            if (theme.intersects(groups.get(i))) positions.add(i);
        return Collections.unmodifiableList(positions);
    }
}

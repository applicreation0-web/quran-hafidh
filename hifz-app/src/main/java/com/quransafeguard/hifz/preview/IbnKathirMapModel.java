package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Pure selection/page policy shared by the graphical map and its JVM integration tests. */
final class IbnKathirMapModel {
    static final class PageSpan {
        final int first;
        final int last;
        PageSpan(int first, int last) {
            if (first < 1 || last < first || last > 604) throw new IllegalArgumentException("page span");
            this.first = first;
            this.last = last;
        }
    }

    /** Minimal state required to restore the exact map position after recreation. */
    static final class SavedState {
        final int surah;
        final int groupIndex;
        final int previewPage;
        final int scrollY;
        SavedState(int surah, int groupIndex, int previewPage, int scrollY) {
            this.surah = surah;
            this.groupIndex = groupIndex;
            this.previewPage = previewPage;
            this.scrollY = Math.max(0, scrollY);
        }
    }

    private IbnKathirMapModel() {}

    static IbnKathirGroupIndex.Group groupAt(int surah, int groupIndex, IbnKathirGroupIndex index) {
        if (index == null) throw new IllegalArgumentException("index required");
        List<IbnKathirGroupIndex.Group> groups = index.groupsForSurah(surah);
        if (groups.isEmpty()) throw new IllegalStateException("surah has no documentary group");
        return groups.get(Math.max(0, Math.min(groups.size() - 1, groupIndex)));
    }

    static int groupIndexForAyah(int surah, int ayah, IbnKathirGroupIndex index) {
        List<IbnKathirGroupIndex.Group> groups = index.groupsForSurah(surah);
        for (int i = 0; i < groups.size(); i++) {
            IbnKathirGroupIndex.Group group = groups.get(i);
            if (ayah >= group.startAyah && ayah <= group.endAyah) return i;
        }
        return 0;
    }

    static PageSpan pageSpan(IbnKathirGroupIndex.Group group, GeometryRepository geometry) {
        if (group == null || geometry == null) throw new IllegalArgumentException("group geometry required");
        int first = geometry.pageForVerse(new VerseRef(group.surah, group.startAyah));
        int last = geometry.pageForVerse(new VerseRef(group.surah, group.endAyah));
        return new PageSpan(first, last);
    }

    static int clampPage(IbnKathirGroupIndex.Group group, int preferred,
                         GeometryRepository geometry) {
        PageSpan span = pageSpan(group, geometry);
        return Math.max(span.first, Math.min(span.last, preferred));
    }

    /** Exact canonical verses of this documentary group which are printed on one QCF page. */
    static List<VerseRef> highlightedVerses(IbnKathirGroupIndex.Group group, int page,
                                            GeometryRepository geometry) {
        PageSpan span = pageSpan(group, geometry);
        if (page < span.first || page > span.last) return java.util.Collections.emptyList();
        LinkedHashSet<VerseRef> exact = new LinkedHashSet<>();
        for (VerseRef verse : geometry.versesOnLines(geometry.lineIdsOnPage(page))) {
            if (verse.getSurah() == group.surah
                    && verse.getAyah() >= group.startAyah
                    && verse.getAyah() <= group.endAyah) {
                exact.add(verse);
            }
        }
        return new ArrayList<>(exact);
    }
}

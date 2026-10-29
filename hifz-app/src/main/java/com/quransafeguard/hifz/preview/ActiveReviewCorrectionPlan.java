package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Read-only selection of the pages actually revealed AND containing flagged verses in the
 * portion of Révision active that was really covered. Nothing here grants Hifz credits.
 * Order follows the actual corpus traversal (not numerical Mushaf page order).
 */
final class ActiveReviewCorrectionPlan {
    static final class Page {
        final int page;
        final List<VerseRef> flagged;
        Page(int page, List<VerseRef> flagged) {
            this.page = page;
            this.flagged = Collections.unmodifiableList(new ArrayList<>(flagged));
        }
    }

    private ActiveReviewCorrectionPlan() {}

    static List<Page> select(List<VerseRef> traversed, Set<Integer> revealedPages,
                             List<VerseRef> flagged, ToIntFunction<VerseRef> pageForVerse) {
        if (traversed == null || revealedPages == null || flagged == null
                || pageForVerse == null || revealedPages.isEmpty() || flagged.isEmpty())
            return Collections.emptyList();
        Set<VerseRef> weak = new HashSet<>(flagged);
        LinkedHashMap<Integer, LinkedHashSet<VerseRef>> selected = new LinkedHashMap<>();
        for (VerseRef verse : traversed) {
            if (verse == null || !weak.contains(verse)) continue;
            int page = pageForVerse.applyAsInt(verse);
            if (page < 1 || page > 604 || !revealedPages.contains(page)) continue;
            if (!selected.containsKey(page)) selected.put(page, new LinkedHashSet<>());
            selected.get(page).add(verse);
        }
        List<Page> result = new ArrayList<>();
        for (Map.Entry<Integer, LinkedHashSet<VerseRef>> entry : selected.entrySet())
            result.add(new Page(entry.getKey(), new ArrayList<>(entry.getValue())));
        return Collections.unmodifiableList(result);
    }
}

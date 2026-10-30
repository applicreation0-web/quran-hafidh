package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Every revealed QCF page is replayed once; manual verse markers are not consulted. */
final class ActiveReviewCorrectionPlan {
    private ActiveReviewCorrectionPlan() {}

    static List<Integer> select(Set<Integer> revealedPages) {
        if (revealedPages == null || revealedPages.isEmpty()) return Collections.emptyList();
        LinkedHashSet<Integer> ordered = new LinkedHashSet<>();
        for (Integer page : revealedPages) {
            if (page == null || page < 1 || page > 604)
                throw new IllegalArgumentException("Invalid revealed Mushaf page");
            ordered.add(page);
        }
        return Collections.unmodifiableList(new ArrayList<>(ordered));
    }
}

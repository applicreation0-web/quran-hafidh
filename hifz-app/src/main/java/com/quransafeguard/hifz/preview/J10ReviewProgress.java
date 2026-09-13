package com.quransafeguard.hifz.preview;

import java.util.HashSet;
import java.util.Set;

/** Tracks actual Mushaf page callbacks before a J10 group can be validated. */
final class J10ReviewProgress {
    private final int firstPage;
    private final int lastPage;
    private final Set<Integer> shown = new HashSet<>();

    J10ReviewProgress(int firstPage, int lastPage) {
        this.firstPage = Math.max(1, firstPage);
        this.lastPage = Math.max(this.firstPage, lastPage);
    }

    void markShown(int page) {
        if (page >= firstPage && page <= lastPage) shown.add(page);
    }

    boolean canValidate() {
        for (int page = firstPage; page <= lastPage; page++) {
            if (!shown.contains(page)) return false;
        }
        return true;
    }
}

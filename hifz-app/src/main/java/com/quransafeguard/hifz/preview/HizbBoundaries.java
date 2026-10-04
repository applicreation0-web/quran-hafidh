package com.quransafeguard.hifz.preview;

/**
 * The 60 Hizb boundaries, derived from {@link QuranRubBoundaries}' already-verified 240-row
 * rub' table (each Hizb's own start is the row at position 0 of its 4 rub'). Kept as a thin
 * view rather than a second hardcoded table so the two can never drift apart.
 *
 * Deliberately Hizb-level only, never rub' (quarter-Hizb): the bundled Mushaf SVGs carry no
 * distinctive marker for a rub' boundary that falls mid-page, so any future Quran-subdivision
 * feature (e.g. weekly scheduling) must key off Hizb, which always lands on a real page start.
 */
final class HizbBoundaries {
    private HizbBoundaries() {}

    /** {hizb, surah, ayah, page} for hizb 1..60. */
    static int[] rowForHizb(int hizb) {
        if (hizb < 1 || hizb > 60) throw new IllegalArgumentException("invalid hizb " + hizb);
        int[] rubRow = QuranRubBoundaries.rowForIndex((hizb - 1) * 4 + 1); // that hizb's own start (position 0)
        return new int[] { hizb, rubRow[1], rubRow[2], rubRow[3] };
    }

    /** Which Hizb the given page currently falls within — the last Hizb whose start page is not after it. */
    static int currentAt(int page) {
        int current = 1;
        for (int h = 1; h <= 60; h++) {
            if (rowForHizb(h)[3] > page) break;
            current = h;
        }
        return current;
    }

    /** The Hizb boundary starting on this exact page, or -1 if none does. */
    static int boundaryOnPage(int page) {
        for (int h = 1; h <= 60; h++) {
            if (rowForHizb(h)[3] == page) return h;
        }
        return -1;
    }
}

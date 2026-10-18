package com.quransafeguard.hifz.preview;

/**
 * A canonical Ayah Themes passage may cross Atlas unit boundaries.
 * The local context is display-only; it never creates a new source passage,
 * changes its ID, or attributes the complete source heading to a partial range.
 */
final class AtlasPassageContext {
    private AtlasPassageContext() {}

    static int intersectionStart(int sourceStart, int unitStart) {
        return Math.max(sourceStart, unitStart);
    }

    static int intersectionEnd(int sourceEnd, int unitEnd) {
        return Math.min(sourceEnd, unitEnd);
    }

    static String displayTitle(int surah, int sourceStart, int sourceEnd,
                               int unitStart, int unitEnd, String original) {
        if (surah < 1 || surah > 114 || sourceStart < 1 || unitStart < 1 ||
                sourceEnd < sourceStart || unitEnd < unitStart)
            throw new IllegalArgumentException("Invalid canonical or Atlas boundary");
        int start = intersectionStart(sourceStart, unitStart);
        int end = intersectionEnd(sourceEnd, unitEnd);
        if (start > end)
            throw new IllegalArgumentException("No passage/Atlas intersection");
        // Full passage: the untouched source title remains the correct caption.
        if (start == sourceStart && end == sourceEnd)
            return original == null ? "" : original;
        // Partial passage: NEVER attribute the full source heading to this unit.
        // Use a grounded reference until a local title is reviewed and approved.
        String portion = surah + ":" + start + (start == end ? "" : "–" + end);
        String source = surah + ":" + sourceStart
                + (sourceStart == sourceEnd ? "" : "–" + sourceEnd);
        return portion + " · partie du passage " + source;
    }
}

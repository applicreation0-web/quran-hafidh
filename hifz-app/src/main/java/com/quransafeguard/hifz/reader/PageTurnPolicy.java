package com.quransafeguard.hifz.reader;

/** Pure page-direction policy for a right-to-left Arabic Mushaf. */
public final class PageTurnPolicy {
    private PageTurnPolicy() { }

    /** Positive/rightward swipe advances; negative/leftward swipe goes back. */
    public static int deltaForHorizontalSwipe(float dx) {
        if (dx > 0f) return 1;
        if (dx < 0f) return -1;
        return 0;
    }
}

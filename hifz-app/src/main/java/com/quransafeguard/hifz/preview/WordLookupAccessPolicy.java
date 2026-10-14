package com.quransafeguard.hifz.preview;

/** View-only English word lookup gate. Does not modify Hifz state, masks or timers. */
final class WordLookupAccessPolicy {
    private WordLookupAccessPolicy() {}

    /** A temporarily revealed or fail-open displayed passage is still a MASKED repetition. */
    static boolean allowed(boolean verifiedLocalPack,
                           boolean eligibleScreen,
                           boolean userEnabled,
                           int scheduledMask,
                           int displayedMask,
                           boolean temporaryReveal,
                           boolean sessionFinished,
                           boolean annotationMode) {
        return verifiedLocalPack && eligibleScreen && userEnabled
            && scheduledMask == 0 && displayedMask == 0
            && !temporaryReveal && !sessionFinished && !annotationMode;
    }

    /** Never trust a callback arriving after a page, render or state transition. */
    static boolean acceptTap(boolean allowedNow,
                             int callbackPage,
                             int visiblePage,
                             long callbackEpoch,
                             long activeEpoch,
                             String key) {
        if (!allowedNow || callbackPage < 1 || callbackPage > 604
            || callbackPage != visiblePage || callbackEpoch != activeEpoch
            || key == null || !key.matches("[1-9][0-9]{0,2}:[1-9][0-9]{0,2}:[1-9][0-9]{0,2}"))
            return false;
        String[] parts=key.split(":", -1);
        int surah=Integer.parseInt(parts[0]);
        int ayah=Integer.parseInt(parts[1]);
        int word=Integer.parseInt(parts[2]);
        return surah>=1 && surah<=114 && ayah>=1 && ayah<=286 && word>=1;
    }
}

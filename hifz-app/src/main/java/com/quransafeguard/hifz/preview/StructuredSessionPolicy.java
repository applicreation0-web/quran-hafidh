package com.quransafeguard.hifz.preview;

/** Small pure policy shared by structured-session UI and tests. */
final class StructuredSessionPolicy {
    static final int MAX_TOTAL_REVEALS_FOR_NORMAL_CREDIT = 2;

    private StructuredSessionPolicy() {}

    static boolean assistancePasses(int totalReveals) {
        return totalReveals >= 0 && totalReveals <= MAX_TOTAL_REVEALS_FOR_NORMAL_CREDIT;
    }

    static boolean murajaahCanValidate(boolean hasActualEnd) {
        return hasActualEnd;
    }

    static boolean shouldInitialReaderShow(boolean hasShown, boolean sessionCompleted) {
        return !hasShown && !sessionCompleted;
    }
}

package com.quransafeguard.hifz.preview;

/** Fixed active-review time bands. Passive reading retains its separate legacy policy. */
final class ActiveReviewDurationPolicy {
    private ActiveReviewDurationPolicy() {}

    static int activeMinutes(int corpusLines, int totalLines) {
        if (totalLines <= 0 || corpusLines < 0 || corpusLines > totalLines)
            throw new IllegalArgumentException("Invalid active corpus size");
        long scaled = 30L * corpusLines;
        if (scaled <= 15L * totalLines) return 40;
        if (scaled <= 25L * totalLines) return 60;
        return 70;
    }

    static int fragilityMinutes(int activeMinutes) {
        if (activeMinutes <= 40) return 5;
        if (activeMinutes <= 60) return 10;
        return 15;
    }

    /** Old in-flight checkpoints must still decode without rewriting their frozen target. */
    static boolean validPersistedMinutes(int minutes) {
        return minutes == 40 || minutes == 70 || (minutes >= 30 && minutes % 15 == 0);
    }

    static boolean validProfileMinutes(int minutes) {
        return minutes == 0 || minutes == 30 || minutes == 40 || minutes == 45 || minutes == 60 || minutes == 70;
    }
}

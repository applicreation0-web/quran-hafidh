package com.quransafeguard.hifz.preview;

import java.util.Locale;

final class SessionTimerPolicy {
    private SessionTimerPolicy() {}

    static String label(String mode, long elapsedMs, int targetMinutes) {
        String elapsed = SessionClock.format(elapsedMs);
        if ("SABQI".equals(mode) || "ITQAN".equals(mode)) return elapsed;
        return elapsed + " / " + String.format(Locale.ROOT, "%02d:00", Math.max(0, targetMinutes));
    }
}

package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

/** Tiny local counters used only for raw session instrumentation; no network or external storage. */
final class HifzSessionMetricsStore {
    private static final String PREFS = "quran_hifz_preview_v1";
    private static final String RECENT_LINES = "recentSessionReviewedLines";
    private static final String LAST_ANCHORING = "lastAnchoringMetrics";
    private final SharedPreferences p;

    HifzSessionMetricsStore(Context context) {
        p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    int consolidationLines() { return Math.max(0, p.getInt(RECENT_LINES, 0)); }

    void addConsolidationLines(int lines) {
        if (lines <= 0) return;
        p.edit().putInt(RECENT_LINES, consolidationLines() + lines).apply();
    }

    void clearConsolidation() { p.edit().remove(RECENT_LINES).apply(); }

    void recordAnchoring(String metrics) {
        p.edit().putString(LAST_ANCHORING, metrics == null ? "" : metrics).apply();
    }

    String lastAnchoring() { return p.getString(LAST_ANCHORING, ""); }
}

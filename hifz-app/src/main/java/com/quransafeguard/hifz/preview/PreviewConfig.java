package com.quransafeguard.hifz.preview;

/** Centralized working parameters. Values marked WORKING are not product-frozen. */
public final class PreviewConfig {
    private PreviewConfig() {}

    public static final int SCHEMA_VERSION = 1;
    public static final int SABQI_LINES = 5; // frozen
    public static final int SABQI_MINUTES_WORKING = 90;
    public static final int ITQAN_MINUTES_WORKING = 60;
    public static final int MURAJAAH_MINUTES_WORKING = 45;
    public static final int FREE_MEM_MINUTES_WORKING = 45;

    // Working Itqan mask split only. Total x30 is frozen; the split is not.
    public static final int ITQAN_VISIBLE_REPS_WORKING = 10;
    public static final int ITQAN_25_REPS_WORKING = 5;
    public static final int ITQAN_50_REPS_WORKING = 5;
    public static final int ITQAN_75_REPS_WORKING = 5;
    public static final int ITQAN_100_REPS_WORKING = 5;
    public static final int ITQAN_TOTAL_REPS = 30;

    public static final double INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING = 9.0;
    public static final int MURAJAAH_RECENT_SABQI_MINUTES_WORKING = 15;
    public static final int MURAJAAH_ITQAN_MINUTES_WORKING = 30;
    public static final int EINK_FULL_CLEAN_PAGE_INTERVAL_WORKING = 8;

    public static int itqanMaskForNextRep(int completed) {
        if (completed < 0 || completed >= ITQAN_TOTAL_REPS) return 0;
        int next = completed + 1;
        int a = ITQAN_VISIBLE_REPS_WORKING;
        int b = a + ITQAN_25_REPS_WORKING;
        int c = b + ITQAN_50_REPS_WORKING;
        int d = c + ITQAN_75_REPS_WORKING;
        if (next <= a) return 0;
        if (next <= b) return 25;
        if (next <= c) return 50;
        if (next <= d) return 75;
        return 100;
    }
}

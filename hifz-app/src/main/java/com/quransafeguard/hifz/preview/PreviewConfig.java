package com.quransafeguard.hifz.preview;

/** Centralized working parameters. Values marked WORKING are not product-frozen. */
public final class PreviewConfig {
    private PreviewConfig() {}

    public static final int SCHEMA_VERSION = 6;
    public static final int SABQI_LINES = 5; // frozen
    public static final int FREE_MEM_MINUTES_WORKING = 45;

    /** Stabilisation's weekly unit target: three sessions (Tue/Thu/Sat) at 8/7/7 lines = 1.5 pages/week. */
    public static final int STABILIZATION_WEEKLY_LINES = 22;

    public static final int SABQI_VISIBLE_REPS = 15;
    public static final int SABQI_25_REPS = 5;
    public static final int SABQI_50_REPS = 5;
    public static final int SABQI_75_REPS = 5;
    public static final int SABQI_100_REPS = 7;
    public static final int SABQI_TOTAL_REPS = 37;

    // Itqan ×40: strengthen both initial exposure and final unaided recall.
    public static final int ITQAN_VISIBLE_REPS_WORKING = 15;
    public static final int ITQAN_25_REPS_WORKING = 5;
    public static final int ITQAN_50_REPS_WORKING = 5;
    public static final int ITQAN_75_REPS_WORKING = 5;
    public static final int ITQAN_100_REPS_WORKING = 10;
    public static final int ITQAN_TOTAL_REPS = 40;

    // Reconstruction: stronger visible re-exposure, no 25% stage, then progressive recall.
    public static final int ITQAN_LIGHT_VISIBLE_REPS = 20;
    public static final int ITQAN_LIGHT_25_REPS = 0;
    public static final int ITQAN_LIGHT_50_REPS = 5;
    public static final int ITQAN_LIGHT_75_REPS = 5;
    public static final int ITQAN_LIGHT_100_REPS = 5;
    public static final int ITQAN_LIGHT_TOTAL_REPS = 35;

    public static final double INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING = 8.0;
    public static final double INITIAL_RECENT_SECONDS_PER_LINE_WORKING = 9.0;
    public static final int SPEED_MIN_LINES = 20;
    public static final int SPEED_MIN_SECONDS = 60;
    public static final double SPEED_MAX_CHANGE_RATIO = 0.10;
    public static final int SPEED_BOOTSTRAP_SAMPLES_WORKING = 3;
    public static final double SPEED_ACCEPT_MIN_SECONDS_PER_LINE_WORKING = 3.5;
    public static final double SPEED_ATYPICAL_SECONDS_PER_LINE_WORKING = 20.0;
    public static final double SPEED_ACCEPT_MAX_SECONDS_PER_LINE_WORKING = 40.0;
    public static final double SPEED_REJECT_DEVIATION_RATIO_WORKING = 0.50;

    // BOOX refresh policy. Page changes get a full GC cleanup; small changes use REGAL/GU
    // and periodically trigger GC so counters, masks and audio outlines do not accumulate ghosting.
    public static final int EINK_FULL_CLEAN_PAGE_INTERVAL_WORKING = 1;
    public static final int EINK_LOCAL_CHANGES_BEFORE_FULL_CLEAN_WORKING = 12;
    public static final int EINK_MASK_CHANGES_BEFORE_FULL_CLEAN_WORKING = 4;
    // Audio moves verse-by-verse and can run for long periods: clean more often than generic local UI.
    public static final int EINK_AUDIO_CHANGES_BEFORE_FULL_CLEAN_WORKING = 6;

    /** Stable cyclic traversal of recent Sabqi, including the one-block case. */
    public static int nextRecentReviewIndex(int currentIndex, int blockCount) {
        if (blockCount <= 0) return -1;
        return Math.floorMod(currentIndex + 1, blockCount);
    }

    /** Completion is governed only by the fixed foreground-session duration. */
    public static boolean timedSessionComplete(long elapsedMs, int durationMinutes) {
        if (durationMinutes <= 0) return true;
        return elapsedMs >= durationMinutes * 60_000L;
    }

    public static int sabqiMaskForNextRep(int completed) {
        if (completed < 0 || completed >= SABQI_TOTAL_REPS) return 0;
        int next = completed + 1;
        int a = SABQI_VISIBLE_REPS;
        int b = a + SABQI_25_REPS;
        int c = b + SABQI_50_REPS;
        int d = c + SABQI_75_REPS;
        if (next <= a) return 0;
        if (next <= b) return 25;
        if (next <= c) return 50;
        if (next <= d) return 75;
        return 100;
    }

    public static int itqanMaskForNextRep(int completed) {
        return itqanMaskForNextRep(completed, AnchoringQueue.ItqanProtocol.FULL);
    }

    public static int itqanTotalReps(AnchoringQueue.ItqanProtocol protocol) {
        return protocol == AnchoringQueue.ItqanProtocol.LIGHT ? ITQAN_LIGHT_TOTAL_REPS : ITQAN_TOTAL_REPS;
    }

    public static int itqanMaskForNextRep(int completed, AnchoringQueue.ItqanProtocol protocol) {
        int total = itqanTotalReps(protocol);
        if (completed < 0 || completed >= total) return 0;
        int next = completed + 1;
        boolean light = protocol == AnchoringQueue.ItqanProtocol.LIGHT;
        int a = light ? ITQAN_LIGHT_VISIBLE_REPS : ITQAN_VISIBLE_REPS_WORKING;
        int b = a + (light ? ITQAN_LIGHT_25_REPS : ITQAN_25_REPS_WORKING);
        int c = b + (light ? ITQAN_LIGHT_50_REPS : ITQAN_50_REPS_WORKING);
        int d = c + (light ? ITQAN_LIGHT_75_REPS : ITQAN_75_REPS_WORKING);
        if (next <= a) return 0;
        if (next <= b) return 25;
        if (next <= c) return 50;
        if (next <= d) return 75;
        return 100;
    }

    public static boolean isItqanValidationRep(int completedBefore, AnchoringQueue.ItqanProtocol protocol) {
        int next = completedBefore + 1;
        int total = itqanTotalReps(protocol);
        return next >= total - 1 && next <= total
            && itqanMaskForNextRep(completedBefore, protocol) == 100;
    }

    public static boolean itqanValidationPassed(int finalStageReveals) {
        return finalStageReveals < 2;
    }

    /** Balanced physical-line split for user-declared difficult Ancrage units. */
    public static int[] fractionatedBlockSizes(int lineCount) {
        if (lineCount <= 0) return new int[]{0};
        int blockCount = (lineCount + SABQI_LINES - 1) / SABQI_LINES;
        int base = lineCount / blockCount;
        int longer = lineCount % blockCount;
        int[] sizes = new int[blockCount];
        for (int i = 0; i < blockCount; i++) sizes[i] = base + (i < longer ? 1 : 0);
        return sizes;
    }

    /** Balanced physical-line split applied independently to each canonical surah segment. */
    public static int[] fractionatedBlockSizes(int[] surahSegmentLineCounts) {
        if (surahSegmentLineCounts == null || surahSegmentLineCounts.length == 0) return new int[]{0};
        int totalBlocks = 0;
        for (int lineCount : surahSegmentLineCounts) {
            if (lineCount <= 0) throw new IllegalArgumentException("surah segment must contain physical lines");
            totalBlocks += fractionatedBlockSizes(lineCount).length;
        }
        int[] sizes = new int[totalBlocks];
        int at = 0;
        for (int lineCount : surahSegmentLineCounts) {
            int[] segment = fractionatedBlockSizes(lineCount);
            System.arraycopy(segment, 0, sizes, at, segment.length);
            at += segment.length;
        }
        return sizes;
    }

    public static int fractionatedBlockCount(int[] surahSegmentLineCounts) {
        return fractionatedBlockSizes(surahSegmentLineCounts).length;
    }

    public static int fractionatedBlockStart(int[] surahSegmentLineCounts, int blockIndex) {
        int[] sizes = fractionatedBlockSizes(surahSegmentLineCounts);
        int at = Math.max(0, Math.min(blockIndex, sizes.length - 1));
        int start = 0;
        for (int i = 0; i < at; i++) start += sizes[i];
        return start;
    }

    public static int fractionatedBlockLength(int[] surahSegmentLineCounts, int blockIndex) {
        int[] sizes = fractionatedBlockSizes(surahSegmentLineCounts);
        int at = Math.max(0, Math.min(blockIndex, sizes.length - 1));
        return sizes[at];
    }

    public static int fractionatedBlockCount(int lineCount) {
        return fractionatedBlockSizes(lineCount).length;
    }

    public static int fractionatedBlockStart(int lineCount, int blockIndex) {
        int[] sizes = fractionatedBlockSizes(lineCount);
        int at = Math.max(0, Math.min(blockIndex, sizes.length - 1));
        int start = 0;
        for (int i = 0; i < at; i++) start += sizes[i];
        return start;
    }

    public static int fractionatedBlockLength(int lineCount, int blockIndex) {
        int[] sizes = fractionatedBlockSizes(lineCount);
        int at = Math.max(0, Math.min(blockIndex, sizes.length - 1));
        return sizes[at];
    }
}

package com.quransafeguard.hifz.preview;

/** Pure cadence math shared by Entretien sizing, Consolidation capacity and advisory ranges. */
final class HifzCadence {
    private HifzCadence() {}

    static double sanitizedSecondsPerLine(double value) {
        if (value > 0.0 && Double.isFinite(value)) return value;
        return PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING;
    }

    static int targetLines(int minutes, double secondsPerLine) {
        if (minutes <= 0) return 0;
        double cadence = sanitizedSecondsPerLine(secondsPerLine);
        return Math.max(1, (int) Math.floor(minutes * 60.0 / cadence));
    }

    static int targetFiveLineCapacity(int minutes, double secondsPerLine) {
        int raw = targetLines(minutes, secondsPerLine);
        int block = PreviewConfig.SABQI_LINES;
        if (raw < block) return 0;
        return (raw / block) * block;
    }

    static double recalibrate(double previousSecondsPerLine, int reviewedLines, long activeElapsedMs) {
        double previous = sanitizedSecondsPerLine(previousSecondsPerLine);
        if (reviewedLines < PreviewConfig.SPEED_MIN_LINES
                || activeElapsedMs < PreviewConfig.SPEED_MIN_SECONDS * 1000L) return previous;
        double measured = (activeElapsedMs / 1000.0) / reviewedLines;
        if (!(measured > 0.0) || !Double.isFinite(measured)) return previous;
        double blended = 0.7 * previous + 0.3 * measured;
        double low = previous * (1.0 - PreviewConfig.SPEED_MAX_CHANGE_RATIO);
        double high = previous * (1.0 + PreviewConfig.SPEED_MAX_CHANGE_RATIO);
        return Math.max(low, Math.min(high, blended));
    }

    static int[] advisoryFiveLineRange(double secondsPerLine) {
        return new int[] {
            targetFiveLineCapacity(5, secondsPerLine),
            targetFiveLineCapacity(10, secondsPerLine)
        };
    }
}

package com.quransafeguard.hifz.preview;

/**
 * P4's J-15 dynamic Entretien (passive Murajaah) session length. Below the equivalent of 15 juz
 * memorized, the existing flat 30-minute session already covers everything in reasonable time, so
 * it stays untouched. Past that point, the session must grow with the real amount of material —
 * sized so the whole ACQUIRED corpus can still be cycled through roughly every 15 minutes of
 * daily practice, rounded up to a clean 15-minute step, never shrinking below the historical
 * 45-minute floor once it starts growing at all.
 */
final class MaintenanceCoveragePolicy {
    private static final int EQUIVALENT_JUZ_THRESHOLD = 15;
    private static final int TOTAL_JUZ = 30;
    private static final int BELOW_THRESHOLD_MINUTES = 30;
    private static final int MINIMUM_GROWN_MINUTES = 45;
    private static final int ROUNDING_STEP_MINUTES = 15;
    private static final int TARGET_COVERAGE_DAYS = 15;

    private MaintenanceCoveragePolicy() {}

    static int minutes(int acquiredPhysicalLines, int totalQuranPhysicalLines, double secondsPerLine) {
        if (acquiredPhysicalLines < 0 || totalQuranPhysicalLines <= 0 || secondsPerLine < 0)
            throw new IllegalArgumentException("Invalid maintenance coverage input");
        double equivalentJuz = (double) acquiredPhysicalLines / totalQuranPhysicalLines * TOTAL_JUZ;
        if (equivalentJuz < EQUIVALENT_JUZ_THRESHOLD) return BELOW_THRESHOLD_MINUTES;
        double requiredMinutes = acquiredPhysicalLines * secondsPerLine / 60.0 / TARGET_COVERAGE_DAYS;
        int rounded = ceilToStep(requiredMinutes, ROUNDING_STEP_MINUTES);
        return Math.max(MINIMUM_GROWN_MINUTES, rounded);
    }

    private static int ceilToStep(double value, int step) {
        return (int) (Math.ceil(value / step) * step);
    }
}

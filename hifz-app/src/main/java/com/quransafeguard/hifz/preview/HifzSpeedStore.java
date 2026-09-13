package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

/** Schema-v4 speed state. Entretien and Consolidation never update each other's calibration. */
final class HifzSpeedStore {
    private static final String PREFS = "quran_hifz_preview_v1";
    private final SharedPreferences p;

    HifzSpeedStore(Context context) {
        p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    double maintenanceSecondsPerLine() {
        return p.getFloat("murajaahSecPerLine", (float) PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING);
    }

    double consolidationSecondsPerLine() {
        return p.getFloat("recentSecPerLine", (float) PreviewConfig.INITIAL_RECENT_SECONDS_PER_LINE_WORKING);
    }

    boolean maintenanceCalibrated() { return p.getBoolean("murajaahSpeedCalibrated", false); }
    int maintenanceSamples() { return Math.max(0, p.getInt("murajaahSpeedSamples", 0)); }
    boolean consolidationCalibrated() { return p.getBoolean("recentSpeedCalibrated", false); }
    int consolidationSamples() { return Math.max(0, p.getInt("recentSpeedSamples", 0)); }

    SpeedCalibrationPolicy.Result calibrateMaintenance(int lines, long elapsedMs) {
        SpeedCalibrationPolicy.Result result = SpeedCalibrationPolicy.evaluate(
            maintenanceSecondsPerLine(), maintenanceCalibrated(), maintenanceSamples(), lines, elapsedMs);
        if (result.status != SpeedCalibrationPolicy.Status.REJECTED) {
            p.edit()
                .putFloat("murajaahSecPerLine", (float) result.secondsPerLine)
                .putBoolean("murajaahSpeedCalibrated", result.calibrated)
                .putInt("murajaahSpeedSamples", result.samples)
                .apply();
        }
        return result;
    }

    SpeedCalibrationPolicy.Result calibrateConsolidation(int lines, long elapsedMs) {
        SpeedCalibrationPolicy.Result result = SpeedCalibrationPolicy.evaluate(
            consolidationSecondsPerLine(), consolidationCalibrated(), consolidationSamples(), lines, elapsedMs);
        if (result.status != SpeedCalibrationPolicy.Status.REJECTED) {
            p.edit()
                .putFloat("recentSecPerLine", (float) result.secondsPerLine)
                .putBoolean("recentSpeedCalibrated", result.calibrated)
                .putInt("recentSpeedSamples", result.samples)
                .apply();
        }
        return result;
    }

    static String instrumentationLabel(int lines, long elapsedMs, SpeedCalibrationPolicy.Result result) {
        long seconds = Math.max(0L, elapsedMs / 1000L);
        String suffix = result == null || result.status != SpeedCalibrationPolicy.Status.REJECTED ? "" : "·skip";
        return Math.max(0, lines) + "L/" + seconds + "s" + suffix;
    }
}

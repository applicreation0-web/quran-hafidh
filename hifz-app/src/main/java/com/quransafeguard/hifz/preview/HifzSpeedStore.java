package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

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

    SpeedCalibration.Result calibrateMaintenance(int lines, long elapsedMs) {
        SpeedCalibration.Result result = SpeedCalibration.evaluate(
            maintenanceSecondsPerLine(), maintenanceCalibrated(), maintenanceSamples(), lines, elapsedMs);
        if (result.status != SpeedCalibration.Status.REJECTED) {
            p.edit()
                .putFloat("murajaahSecPerLine", (float) result.secondsPerLine)
                .putBoolean("murajaahSpeedCalibrated", result.calibrated)
                .putInt("murajaahSpeedSamples", result.samples)
                .apply();
        }
        return result;
    }

    SpeedCalibration.Result calibrateConsolidation(int lines, long elapsedMs) {
        SpeedCalibration.Result result = SpeedCalibration.evaluate(
            consolidationSecondsPerLine(), consolidationCalibrated(), consolidationSamples(), lines, elapsedMs);
        if (result.status != SpeedCalibration.Status.REJECTED) {
            p.edit()
                .putFloat("recentSecPerLine", (float) result.secondsPerLine)
                .putBoolean("recentSpeedCalibrated", result.calibrated)
                .putInt("recentSpeedSamples", result.samples)
                .apply();
        }
        return result;
    }

    String maintenanceSummary() {
        return speedSummary(maintenanceSecondsPerLine(), maintenanceCalibrated(), maintenanceSamples());
    }

    String consolidationSummary() {
        return speedSummary(consolidationSecondsPerLine(), consolidationCalibrated(), consolidationSamples());
    }

    private static String speedSummary(double secondsPerLine, boolean calibrated, int samples) {
        String state = calibrated
            ? Math.max(1, samples) + (samples == 1 ? " mesure" : " mesures")
            : "estimation";
        return String.format(Locale.ROOT, "%.1f s/ligne · %s", Math.max(0.0, secondsPerLine), state);
    }

    static String instrumentationLabel(int lines, long elapsedMs, SpeedCalibration.Result result) {
        int safeLines = Math.max(0, lines);
        if (result == null) return safeLines + " lignes";
        if (result.status == SpeedCalibration.Status.REJECTED) {
            return safeLines + " lignes · mesure non retenue";
        }
        return safeLines + " lignes · "
            + String.format(Locale.ROOT, "%.1f s/ligne", Math.max(0.0, result.secondsPerLine));
    }
}

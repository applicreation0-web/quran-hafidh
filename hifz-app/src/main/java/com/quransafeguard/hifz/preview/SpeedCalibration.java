package com.quransafeguard.hifz.preview;

/** Pure, provisional speed calibration policy. Thresholds remain WORKING until BOOX field data. */
final class SpeedCalibration {
    enum Status { ACCEPTED, ATYPICAL, REJECTED }

    static final class Result {
        final Status status;
        final double secondsPerLine;
        final int samples;
        final boolean calibrated;
        final String reason;

        Result(Status status, double secondsPerLine, int samples, boolean calibrated, String reason) {
            this.status = status;
            this.secondsPerLine = secondsPerLine;
            this.samples = samples;
            this.calibrated = calibrated;
            this.reason = reason;
        }
    }

    private SpeedCalibration() {}

    static Result evaluate(double currentSecondsPerLine, boolean calibrated, int samples,
                           int reviewedLines, long activeElapsedMs) {
        double current = HifzCadence.sanitizedSecondsPerLine(currentSecondsPerLine);
        int safeSamples = Math.max(0, samples);
        if (reviewedLines < PreviewConfig.SPEED_MIN_LINES) {
            return new Result(Status.REJECTED, current, safeSamples, calibrated, "moins de 20 lignes");
        }
        if (activeElapsedMs < PreviewConfig.SPEED_MIN_SECONDS * 1000L) {
            return new Result(Status.REJECTED, current, safeSamples, calibrated, "moins de " + PreviewConfig.SPEED_MIN_SECONDS + " secondes");
        }

        double measured = (activeElapsedMs / 1000.0) / reviewedLines;
        if (!Double.isFinite(measured) || measured < PreviewConfig.SPEED_ACCEPT_MIN_SECONDS_PER_LINE_WORKING) {
            return new Result(Status.REJECTED, current, safeSamples, calibrated, "vitesse trop rapide / mesure invalide");
        }
        if (measured > PreviewConfig.SPEED_ACCEPT_MAX_SECONDS_PER_LINE_WORKING) {
            return new Result(Status.REJECTED, current, safeSamples, calibrated, "vitesse trop lente / mesure invalide");
        }

        Status acceptedStatus = measured > PreviewConfig.SPEED_ATYPICAL_SECONDS_PER_LINE_WORKING
            ? Status.ATYPICAL : Status.ACCEPTED;

        if (safeSamples < PreviewConfig.SPEED_BOOTSTRAP_SAMPLES_WORKING) {
            return new Result(acceptedStatus, measured, safeSamples + 1, true,
                acceptedStatus == Status.ATYPICAL ? "amorçage atypique accepté" : "amorçage accepté");
        }

        double deviation = Math.abs(measured - current) / current;
        if (deviation > PreviewConfig.SPEED_REJECT_DEVIATION_RATIO_WORKING) {
            return new Result(Status.REJECTED, current, safeSamples, calibrated, "écart supérieur à 50 %");
        }

        double blended = 0.7 * current + 0.3 * measured;
        double low = current * (1.0 - PreviewConfig.SPEED_MAX_CHANGE_RATIO);
        double high = current * (1.0 + PreviewConfig.SPEED_MAX_CHANGE_RATIO);
        double next = Math.max(low, Math.min(high, blended));
        return new Result(acceptedStatus, next, safeSamples + 1, true,
            acceptedStatus == Status.ATYPICAL ? "valeur lente atypique acceptée" : "mesure acceptée");
    }
}

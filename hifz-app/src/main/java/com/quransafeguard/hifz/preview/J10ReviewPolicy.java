package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

/** Pure calendar/deadline policy for the 10-day acquired-material review guarantee. */
final class J10ReviewPolicy {
    static final int MAX_REVIEW_GAP_DAYS = 10;
    static final int FORECAST_DAYS = 10;
    static final double TENSION_RATIO = 0.80;

    enum Sustainability { NORMAL, TENSION, NON_TENABLE }

    static final class Forecast {
        final int requiredLines;
        final int requiredMinutes;
        final int availableMinutes;
        final int deficitMinutes;
        final Sustainability sustainability;

        Forecast(int requiredLines, int requiredMinutes, int availableMinutes,
                 int deficitMinutes, Sustainability sustainability) {
            this.requiredLines = Math.max(0, requiredLines);
            this.requiredMinutes = Math.max(0, requiredMinutes);
            this.availableMinutes = Math.max(0, availableMinutes);
            this.deficitMinutes = Math.max(0, deficitMinutes);
            this.sustainability = sustainability == null ? Sustainability.NORMAL : sustainability;
        }
    }

    private J10ReviewPolicy() {}

    static LocalDate deadline(LocalDate lastReviewed) {
        if (lastReviewed == null) throw new IllegalArgumentException("lastReviewed required");
        return lastReviewed.plusDays(MAX_REVIEW_GAP_DAYS);
    }

    static int ageDays(LocalDate lastReviewed, LocalDate today) {
        if (lastReviewed == null || today == null) return 0;
        return (int) Math.max(0L, ChronoUnit.DAYS.between(lastReviewed, today));
    }

    static Forecast forecast(Collection<LocalDate> lastReviews, LocalDate today,
                             double secondsPerLine, int availableMinutes) {
        if (today == null) throw new IllegalArgumentException("today required");
        LocalDate horizon = today.plusDays(FORECAST_DAYS - 1L);
        int requiredLines = 0;
        if (lastReviews != null) {
            for (LocalDate reviewed : lastReviews) {
                if (reviewed != null && !deadline(reviewed).isAfter(horizon)) requiredLines++;
            }
        }
        double safeSeconds = secondsPerLine > 0.0 && Double.isFinite(secondsPerLine)
            ? secondsPerLine : PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING;
        int requiredMinutes = requiredLines <= 0 ? 0
            : (int) Math.ceil(requiredLines * safeSeconds / 60.0);
        int available = Math.max(0, availableMinutes);
        int deficit = Math.max(0, requiredMinutes - available);
        Sustainability status;
        if (requiredMinutes > available) {
            status = Sustainability.NON_TENABLE;
        } else if (available > 0 && requiredMinutes >= available * TENSION_RATIO) {
            status = Sustainability.TENSION;
        } else if (available == 0 && requiredMinutes > 0) {
            status = Sustainability.NON_TENABLE;
        } else {
            status = Sustainability.NORMAL;
        }
        return new Forecast(requiredLines, requiredMinutes, available, deficit, status);
    }

    static boolean shouldPreempt(int ageDays, Sustainability sustainability) {
        int age = Math.max(0, ageDays);
        Sustainability status = sustainability == null ? Sustainability.NORMAL : sustainability;
        if (age >= 9) return true;
        if (age >= 8) return status != Sustainability.NORMAL;
        return age >= 7 && status == Sustainability.NON_TENABLE;
    }
}

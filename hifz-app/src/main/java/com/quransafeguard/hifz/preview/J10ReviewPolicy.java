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

    static LocalDate unknownHistoricalSeed(LocalDate today) {
        if (today == null) throw new IllegalArgumentException("today required");
        return today.minusDays(MAX_REVIEW_GAP_DAYS);
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
        double safeSeconds = safeSeconds(secondsPerLine);
        int requiredMinutes = minutesForLines(requiredLines, safeSeconds);
        int available = Math.max(0, availableMinutes);
        int deficit = Math.max(0, requiredMinutes - available);
        Sustainability status = status(requiredMinutes, available);
        return new Forecast(requiredLines, requiredMinutes, available, deficit, status);
    }

    /**
     * Deadline-aware forecast. Every day is a prefix constraint: work due by D+n must fit in
     * capacity available through D+n. This prevents later capacity from hiding an earlier miss.
     */
    static Forecast forecastByDay(Collection<LocalDate> lastReviews, LocalDate today,
                                  double secondsPerLine, int[] dailyCapacityMinutes) {
        if (today == null) throw new IllegalArgumentException("today required");
        double safeSeconds = safeSeconds(secondsPerLine);
        int horizonDays = dailyCapacityMinutes == null ? 0
            : Math.min(FORECAST_DAYS, dailyCapacityMinutes.length);
        if (horizonDays <= 0) {
            int dueToday = countDueBy(lastReviews, today);
            int required = minutesForLines(dueToday, safeSeconds);
            return new Forecast(dueToday, required, 0, required,
                required > 0 ? Sustainability.NON_TENABLE : Sustainability.NORMAL);
        }

        int cumulativeCapacity = 0;
        Forecast worstTension = null;
        for (int offset = 0; offset < horizonDays; offset++) {
            cumulativeCapacity += Math.max(0, dailyCapacityMinutes[offset]);
            LocalDate cutoff = today.plusDays(offset);
            int dueLines = countDueBy(lastReviews, cutoff);
            int required = minutesForLines(dueLines, safeSeconds);
            int deficit = Math.max(0, required - cumulativeCapacity);
            if (deficit > 0) {
                return new Forecast(dueLines, required, cumulativeCapacity, deficit,
                    Sustainability.NON_TENABLE);
            }
            if (cumulativeCapacity > 0 && required >= cumulativeCapacity * TENSION_RATIO) {
                worstTension = new Forecast(dueLines, required, cumulativeCapacity, 0,
                    Sustainability.TENSION);
            }
        }

        LocalDate horizon = today.plusDays(horizonDays - 1L);
        int dueLines = countDueBy(lastReviews, horizon);
        int required = minutesForLines(dueLines, safeSeconds);
        if (worstTension != null) return worstTension;
        return new Forecast(dueLines, required, cumulativeCapacity, 0, Sustainability.NORMAL);
    }

    private static int countDueBy(Collection<LocalDate> lastReviews, LocalDate cutoff) {
        int count = 0;
        if (lastReviews == null) return count;
        for (LocalDate reviewed : lastReviews) {
            if (reviewed != null && !deadline(reviewed).isAfter(cutoff)) count++;
        }
        return count;
    }

    private static double safeSeconds(double secondsPerLine) {
        return secondsPerLine > 0.0 && Double.isFinite(secondsPerLine)
            ? secondsPerLine : PreviewConfig.INITIAL_MURAJAAH_SECONDS_PER_LINE_WORKING;
    }

    private static int minutesForLines(int lines, double secondsPerLine) {
        return lines <= 0 ? 0 : (int) Math.ceil(lines * secondsPerLine / 60.0);
    }

    private static Sustainability status(int requiredMinutes, int availableMinutes) {
        if (requiredMinutes > availableMinutes) return Sustainability.NON_TENABLE;
        if (availableMinutes > 0 && requiredMinutes >= availableMinutes * TENSION_RATIO) {
            return Sustainability.TENSION;
        }
        if (availableMinutes == 0 && requiredMinutes > 0) return Sustainability.NON_TENABLE;
        return Sustainability.NORMAL;
    }

    static boolean shouldPreempt(int ageDays, Sustainability sustainability) {
        int age = Math.max(0, ageDays);
        Sustainability status = sustainability == null ? Sustainability.NORMAL : sustainability;
        if (age >= 9) return true;
        if (age >= 8) return status != Sustainability.NORMAL;
        return age >= 7 && status == Sustainability.NON_TENABLE;
    }
}

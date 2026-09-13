package com.quransafeguard.hifz.preview;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure calendar/attendance rule for promoting recent five-line blocks. */
public final class RecentPromotionPolicy {
    private RecentPromotionPolicy() {}

    public static final int MIN_AGE_DAYS = 90;
    public static final int MAX_RECENT_BLOCKS = 60;
    public static final int ORIGINAL_REQUIRED_SESSIONS = 20;
    public static final int ORIGINAL_PLANNED_SESSIONS = 26;

    public static final class Decision {
        public final boolean promote;
        public final boolean forced;
        public final int plannedSessions;
        public final int requiredSessions;
        public final int completedSessions;
        public final long ageDays;

        Decision(boolean promote, boolean forced, int plannedSessions,
                 int requiredSessions, int completedSessions, long ageDays) {
            this.promote = promote;
            this.forced = forced;
            this.plannedSessions = plannedSessions;
            this.requiredSessions = requiredSessions;
            this.completedSessions = completedSessions;
            this.ageDays = ageDays;
        }
    }

    /**
     * Evaluate one recent block. Age is measured from addedOn. Attendance starts only when
     * Sunday Consolidation is genuinely available, but it keeps a full 90-day reference window
     * so late activation can never reduce the expected ~77% attendance to one or two sessions.
     */
    public static Decision evaluate(LocalDate addedOn, LocalDate through,
                                    LocalDate consolidationStart,
                                    List<LocalDate> completedConsolidations,
                                    int recentBlockCount, boolean oldest) {
        if (addedOn == null || through == null) {
            throw new IllegalArgumentException("promotion dates required");
        }
        LocalDate cadenceStart = consolidationStart == null ? addedOn
            : (consolidationStart.isAfter(addedOn) ? consolidationStart : addedOn);
        long ageDays = Math.max(0L, ChronoUnit.DAYS.between(addedOn, through));
        boolean forced = recentBlockCount > MAX_RECENT_BLOCKS && oldest;

        LocalDate attendanceWindowEnd = cadenceStart.plusDays(MIN_AGE_DAYS);
        int planned = plannedSundays(cadenceStart, attendanceWindowEnd);
        int required = requiredSessions(planned);
        LocalDate completedThrough = through.isBefore(attendanceWindowEnd) ? through : attendanceWindowEnd;
        int completed = completedSundays(cadenceStart, completedThrough, completedConsolidations);
        boolean normal = ageDays >= MIN_AGE_DAYS && completed >= required;
        return new Decision(forced || normal, forced, planned, required, completed, ageDays);
    }

    static int requiredSessions(int plannedSessions) {
        if (plannedSessions <= 0) return 0;
        return (plannedSessions * ORIGINAL_REQUIRED_SESSIONS
            + ORIGINAL_PLANNED_SESSIONS - 1) / ORIGINAL_PLANNED_SESSIONS;
    }

    static int plannedSundays(LocalDate start, LocalDate endInclusive) {
        if (start == null || endInclusive == null || endInclusive.isBefore(start)) return 0;
        LocalDate first = start;
        while (first.getDayOfWeek() != DayOfWeek.SUNDAY) first = first.plusDays(1);
        if (first.isAfter(endInclusive)) return 0;
        return (int) (ChronoUnit.WEEKS.between(first, endInclusive) + 1L);
    }

    private static int completedSundays(LocalDate start, LocalDate endInclusive,
                                        List<LocalDate> completed) {
        if (completed == null || completed.isEmpty() || endInclusive.isBefore(start)) return 0;
        Set<LocalDate> unique = new HashSet<>();
        for (LocalDate date : completed) {
            if (date == null || date.isBefore(start) || date.isAfter(endInclusive)) continue;
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) unique.add(date);
        }
        return unique.size();
    }
}

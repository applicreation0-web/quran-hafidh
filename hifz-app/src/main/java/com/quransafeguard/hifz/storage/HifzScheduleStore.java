package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Default weekly schedule plus explicit overdue completion tracking.
 *
 * Monday/Wednesday/Friday keep two distinct slots: the 90-minute morning Sabqi acquisition and
 * the short evening Sabqi review. They are stored independently so completing one can never
 * silently complete, advance or erase the other. Missed work remains pending without penalty or
 * automatic doubling.
 */
public final class HifzScheduleStore {
    public static final String SABQI = "SABQI";
    public static final String SABQI_REVIEW = "SABQI_REVIEW";
    public static final String ITQAN = "ITQAN";
    public static final String MURAJAAH = "MURAJAAH";

    public static final class Pending {
        public final LocalDate scheduledDate;
        public final String mode;
        public final boolean overdue;
        Pending(LocalDate scheduledDate, String mode, boolean overdue) {
            this.scheduledDate = scheduledDate;
            this.mode = mode;
            this.overdue = overdue;
        }
    }

    private final SharedPreferences p;
    private final HifzProgressStore progress;

    public HifzScheduleStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences("quran_hifz_schedule_v2", Context.MODE_PRIVATE);
        progress = new HifzProgressStore(context);
    }

    /** Primary learning/revision mode for the day. */
    public static String modeFor(LocalDate date) {
        DayOfWeek d = date.getDayOfWeek();
        switch (d) {
            case MONDAY:
            case WEDNESDAY:
            case FRIDAY: return SABQI;
            case TUESDAY:
            case THURSDAY: return ITQAN;
            case SATURDAY:
            case SUNDAY: return MURAJAAH;
            default: throw new IllegalStateException("Unknown weekday");
        }
    }

    /** Ordered slots for one day. Morning always precedes the evening consolidation slot. */
    public static List<String> slotsFor(LocalDate date) {
        if (SABQI.equals(modeFor(date))) {
            return Arrays.asList(SABQI, SABQI_REVIEW);
        }
        return Collections.singletonList(modeFor(date));
    }

    public boolean isCompleted(LocalDate date, String mode) {
        return p.getBoolean(key(date, mode), false);
    }

    public void markCompleted(LocalDate date, String mode) {
        if (!slotsFor(date).contains(mode)) {
            throw new IllegalArgumentException("Mode does not match scheduled slot");
        }
        p.edit().putBoolean(key(date, mode), true).apply();
    }

    /**
     * Returns the oldest unfinished scheduled slot. This is the soft-reschedule rule: no missed
     * slot becomes a failure and no later slot is doubled to compensate for it.
     */
    public Pending nextPending(LocalDate today) {
        if (!progress.isConfigured()) return null;
        LocalDate start = progress.programStartDate();
        if (start.isAfter(today)) start = today;
        LocalDate cursor = start;
        while (!cursor.isAfter(today)) {
            for (String mode : slotsFor(cursor)) {
                if (!isCompleted(cursor, mode)) {
                    return new Pending(cursor, mode, cursor.isBefore(today));
                }
            }
            cursor = cursor.plusDays(1);
        }
        return null;
    }

    private static String key(LocalDate date, String mode) { return "done:" + date + ":" + mode; }
}

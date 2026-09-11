package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Default weekly schedule plus explicit overdue completion tracking. No missed session is treated as failure. */
public final class HifzScheduleStore {
    public static final String SABQI = "SABQI";
    public static final String ITQAN = "ITQAN";
    public static final String MURAJAAH = "MURAJAAH";

    public static final class Pending {
        public final LocalDate scheduledDate;
        public final String mode;
        public final boolean overdue;
        Pending(LocalDate scheduledDate, String mode, boolean overdue) {
            this.scheduledDate = scheduledDate; this.mode = mode; this.overdue = overdue;
        }
    }

    private final SharedPreferences p;
    private final HifzProgressStore progress;

    public HifzScheduleStore(Context context) {
        p = context.getApplicationContext().getSharedPreferences("quran_hifz_schedule_v1", Context.MODE_PRIVATE);
        progress = new HifzProgressStore(context);
    }

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

    public boolean isCompleted(LocalDate date, String mode) {
        return p.getBoolean(key(date, mode), false);
    }

    public void markCompleted(LocalDate date, String mode) {
        if (!modeFor(date).equals(mode)) throw new IllegalArgumentException("Mode does not match scheduled day");
        p.edit().putBoolean(key(date, mode), true).apply();
    }

    /** Returns the oldest unfinished scheduled session. This is the soft-reschedule rule. */
    public Pending nextPending(LocalDate today) {
        if (!progress.isConfigured()) return null;
        LocalDate start = progress.programStartDate();
        if (start.isAfter(today)) start = today;
        LocalDate cursor = start;
        while (!cursor.isAfter(today)) {
            String mode = modeFor(cursor);
            if (!isCompleted(cursor, mode)) return new Pending(cursor, mode, cursor.isBefore(today));
            cursor = cursor.plusDays(1);
        }
        return null;
    }

    private static String key(LocalDate date, String mode) { return "done:" + date + ":" + mode; }
}

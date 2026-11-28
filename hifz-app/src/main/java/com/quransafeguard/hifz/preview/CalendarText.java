package com.quransafeguard.hifz.preview;

import android.content.Context;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Calendar labels belong to the activity locale. Scheduling and receipts use ISO dates. */
final class CalendarText {
    private CalendarText() {}

    static String weekday(Context context, DayOfWeek day, boolean abbreviated) {
        int labels = abbreviated ? R.array.calendar_weekdays_short : R.array.calendar_weekdays;
        return context.getResources().getStringArray(labels)[day.getValue()-1];
    }

    private static String month(Context context, LocalDate date) {
        return context.getResources().getStringArray(R.array.calendar_months)[date.getMonthValue()-1];
    }

    static String date(Context context, LocalDate date) {
        return context.getString(R.string.calendar_date, date.getDayOfMonth(), month(context,date), date.getYear());
    }

    static String dayMonth(Context context, LocalDate date) {
        return context.getString(R.string.calendar_day_month, date.getDayOfMonth(), month(context,date));
    }

    static String dateWithWeekday(Context context, LocalDate date, boolean year) {
        return context.getString(R.string.calendar_date_weekday, weekday(context,date.getDayOfWeek(),false),
            year ? date(context,date) : dayMonth(context,date));
    }

    static String monthYear(Context context, LocalDate date) {
        return context.getString(R.string.calendar_month_year, month(context,date), date.getYear());
    }
}

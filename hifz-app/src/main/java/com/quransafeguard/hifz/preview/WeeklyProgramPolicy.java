package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledCadence;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Dated cadence facade. Classic defers byte-for-byte to core 1.17.1;
 * Recommended = Mon/Wed/Fri Sabqi; Tue/Thu/Sat Itqan.
 * Sunday has only the eligible final reviews, never a seventh ordinary lesson.
 * Prior dates retain their original program when the user switches.
 */
final class WeeklyProgramPolicy {
    private WeeklyProgramPolicy() {}

    static CadenceAction actionFor(DayOfWeek day,
            HifzPrefs.WeeklyProgram mode,int classicLearningDays) {
        if(day==null || mode==null)throw new IllegalArgumentException("day and programme required");
        if(mode==HifzPrefs.WeeklyProgram.CLASSIC)
            return HifzSchedule.INSTANCE.actionFor(day,classicLearningDays); // history only
        if(day==DayOfWeek.SUNDAY)return CadenceAction.REVISION;
        if(mode==HifzPrefs.WeeklyProgram.MANUAL_SEVEN)
            return HifzSchedule.INSTANCE.actionFor(day,classicLearningDays);
        return day==DayOfWeek.MONDAY || day==DayOfWeek.WEDNESDAY
            || day==DayOfWeek.FRIDAY
            ? CadenceAction.LEARNING : CadenceAction.STABILIZATION;
    }

    static CadenceAction actionFor(HifzPrefs prefs, LocalDate date, int learningDays) {
        if (prefs.universalProfile()) {
            int mask = prefs.universalLearningWeekMask(date);
            if (mask >= 0) return universalAction(date.getDayOfWeek(), mask);
            learningDays = prefs.defaultLearningDaysPerWeek();
        }
        return actionFor(date.getDayOfWeek(), prefs.weeklyProgramFor(date), learningDays);
    }

    static CadenceAction universalAction(DayOfWeek day, int mask) {
        if (day == DayOfWeek.SUNDAY) return CadenceAction.REVISION;
        int bit = 1 << (day.getValue() - 1);
        return (mask & bit) == 0 ? CadenceAction.STABILIZATION : CadenceAction.LEARNING;
    }

    /** Lesson eligibility for receipt counts. Generic calendar slots may exist with no enabled lesson. */
    static CadenceAction lessonActionFor(HifzPrefs prefs, LocalDate date, int learningDays) {
        CadenceAction action = actionFor(prefs, date, learningDays);
        if (action == CadenceAction.STABILIZATION && !prefs.itqanDisciplineScheduled(date)) return null;
        return action;
    }

    static TreeMap<LocalDate,Integer> decodeDatedMasks(String raw, String flag, int maximum,
            boolean mondayRequired) throws JSONException {
        TreeMap<LocalDate,Integer> weeks = new TreeMap<>();
        JSONArray records = new JSONArray(raw);
        for (int index = 0; index < records.length(); index++) {
            var entry = records.getJSONObject(index);
            LocalDate date = LocalDate.parse(entry.getString("from"));
            Object value = entry.get(flag);
            if ((mondayRequired && date.getDayOfWeek() != DayOfWeek.MONDAY) || !(value instanceof Integer)
                    || (Integer)value < 0 || (Integer)value > maximum || weeks.put(date,(Integer)value) != null)
                throw new JSONException("Invalid dated schedule configuration");
        }
        return weeks;
    }

    static ScheduledCadence nextDue(HifzPrefs prefs,
            LocalDate from,LocalDate today,Set<LocalDate> completed,int classicLearningDays) {
        if(prefs==null || from==null || today==null || completed==null)
            throw new IllegalArgumentException("dated cadence required");
        if(today.isBefore(from))return null;
        for(LocalDate day=from;!day.isAfter(today);day=day.plusDays(1)) {
            if(completed.contains(day))continue;
            return new ScheduledCadence(day,actionFor(prefs,day,classicLearningDays),day.isBefore(today));
        }
        return null;
    }

    static int recommendedItqanTargetLines(DayOfWeek day) {
        if(day==DayOfWeek.TUESDAY || day==DayOfWeek.THURSDAY || day==DayOfWeek.SATURDAY)
            return ItqanFourSessionPolicy.WEEKLY_PAGE_LINES_PER_SESSION;
        return 0;
    }
}

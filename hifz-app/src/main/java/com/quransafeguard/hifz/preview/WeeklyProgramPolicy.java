package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.HifzSchedule;
import com.quransafeguard.hifz.core.ScheduledCadence;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Dated cadence facade. Classic defers byte-for-byte to core 1.17.1;
 * Recommended = Mon/Wed/Fri Sabqi; Tue/Thu/Sat/Sun Itqan.
 * Sunday's specific finals are separate tasks, not revisions.
 * Prior dates retain their original program when the user switches.
 */
final class WeeklyProgramPolicy {
    private WeeklyProgramPolicy() {}

    static CadenceAction actionFor(DayOfWeek day,
            HifzPrefs.WeeklyProgram mode,int classicLearningDays) {
        if(day==null || mode==null)throw new IllegalArgumentException("day and programme required");
        if(mode==HifzPrefs.WeeklyProgram.CLASSIC)
            return HifzSchedule.INSTANCE.actionFor(day,classicLearningDays);
        return day==DayOfWeek.MONDAY || day==DayOfWeek.WEDNESDAY
            || day==DayOfWeek.FRIDAY
            ? CadenceAction.LEARNING : CadenceAction.STABILIZATION;
    }

    static ScheduledCadence nextDue(HifzPrefs prefs,
            LocalDate from,LocalDate today,Set<LocalDate> completed,int classicLearningDays) {
        if(prefs==null || from==null || today==null || completed==null)
            throw new IllegalArgumentException("dated cadence required");
        if(today.isBefore(from))return null;
        for(LocalDate day=from;!day.isAfter(today);day=day.plusDays(1)) {
            if(completed.contains(day))continue;
            return new ScheduledCadence(day,actionFor(day.getDayOfWeek(),
                prefs.weeklyProgramFor(day),classicLearningDays),day.isBefore(today));
        }
        return null;
    }

    static int recommendedItqanTargetLines(DayOfWeek day) {
        if(day==DayOfWeek.TUESDAY || day==DayOfWeek.SUNDAY)return 8;
        if(day==DayOfWeek.THURSDAY || day==DayOfWeek.SATURDAY)return 7;
        return 0;
    }
}

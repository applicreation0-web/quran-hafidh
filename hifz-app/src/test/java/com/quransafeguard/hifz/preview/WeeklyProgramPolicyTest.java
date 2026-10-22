package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.ScheduledCadence;
import org.junit.Test;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public final class WeeklyProgramPolicyTest {
    @Test public void recommendedItqanIsExactlyFourDaysAndThirtyPhysicalLineTargets() {
        int itqanCount=0,sabqiCount=0,lines=0;
        for(DayOfWeek day:DayOfWeek.values()) {
            CadenceAction action=WeeklyProgramPolicy.actionFor(day,
                HifzPrefs.WeeklyProgram.RECOMMENDED,6);
            if(action==CadenceAction.STABILIZATION) {
                itqanCount++;
                lines+=WeeklyProgramPolicy.recommendedItqanTargetLines(day);
            } else if(action==CadenceAction.LEARNING) {
                sabqiCount++;
                assertEquals(0,WeeklyProgramPolicy.recommendedItqanTargetLines(day));
            } else fail("Recommended Sunday cannot become general revision");
        }
        assertEquals(4,itqanCount);
        assertEquals(3,sabqiCount);
        assertEquals(30,lines);
        assertEquals(8,WeeklyProgramPolicy.recommendedItqanTargetLines(DayOfWeek.TUESDAY));
        assertEquals(7,WeeklyProgramPolicy.recommendedItqanTargetLines(DayOfWeek.THURSDAY));
        assertEquals(7,WeeklyProgramPolicy.recommendedItqanTargetLines(DayOfWeek.SATURDAY));
        assertEquals(8,WeeklyProgramPolicy.recommendedItqanTargetLines(DayOfWeek.SUNDAY));
    }

    @Test public void recommendedLearningThreeOfThreeIsCalendarOnlyAndRestartsOnMonday(){
        LocalDate monday=LocalDate.of(2026,10,19);
        for(int week=0;week<4;week++){
            LocalDate start=monday.plusWeeks(week);
            assertEquals(" · séance 1/3",WeeklyDashboardPlanner.learningSessionCue(
                start.getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED));
            assertEquals(" · séance 2/3",WeeklyDashboardPlanner.learningSessionCue(
                start.plusDays(2).getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED));
            assertEquals(" · séance 3/3",WeeklyDashboardPlanner.learningSessionCue(
                start.plusDays(4).getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED));
            assertEquals("",WeeklyDashboardPlanner.learningSessionCue(
                start.plusDays(1).getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED));
            assertEquals("",WeeklyDashboardPlanner.learningSessionCue(
                start.plusDays(6).getDayOfWeek(),HifzPrefs.WeeklyProgram.RECOMMENDED));
        }
        // Historic/other programme semantics remain unchanged.
        assertEquals("",WeeklyDashboardPlanner.learningSessionCue(DayOfWeek.MONDAY,
            HifzPrefs.WeeklyProgram.CLASSIC));
        assertEquals("",WeeklyDashboardPlanner.learningSessionCue(DayOfWeek.MONDAY,
            HifzPrefs.WeeklyProgram.MANUAL_SEVEN));
    }

    @Test public void weeklyScreenActuallyDisplaysLearningProgressNotOnlyPlannerRawText() throws Exception {
        java.lang.reflect.Method compact=MainActivity.class.getDeclaredMethod("compactScheduleLine",String.class);
        compact.setAccessible(true);
        assertEquals("Révision active + Apprentissage · 1/3",
            compact.invoke(null,"Révision active 30 min + Apprentissage · séance 1/3 · 5 lignes"));
        assertEquals("Révision active + Apprentissage · 2/3",
            compact.invoke(null,"Révision active 30 min + Apprentissage · séance 2/3 · 5 lignes"));
        assertEquals("Révision active + Apprentissage · 3/3",
            compact.invoke(null,"Révision active 30 min + Apprentissage · séance 3/3 · 5 lignes"));
        assertEquals("Révision active + Stabilisation · 2/4",
            compact.invoke(null,"Révision active 30 min + Stabilisation · séance 2/4 · 7 lignes"));
        assertEquals("✓ Révision active + Apprentissage · 3/3",
            compact.invoke(null,"✓ Révision active 30 min + ✓ Apprentissage · séance 3/3"));
    }

    @Test public void classicDelegatesOriginalCanonicalCadenceWithoutChange() {
        for(int ratio=0;ratio<=6;ratio++)
            for(DayOfWeek day:DayOfWeek.values())
                assertEquals(com.quransafeguard.hifz.core.HifzSchedule.INSTANCE.actionFor(day,ratio),
                    WeeklyProgramPolicy.actionFor(day,HifzPrefs.WeeklyProgram.CLASSIC,ratio));
    }

    @Test public void switchingProgramDoesNotChangeAnyProgressionOrHistoricalDate() {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("v6LearnedLineIds","[\"p520-l07\"]");
        disk.disk.put("itqanUnitStart","50:1");
        disk.disk.put("itqanRep",18);
        disk.disk.put("v6AcquiredCreditLineIds","[\"p518-l01\"]");
        disk.disk.put("lastRecentSabqiReviewDate","2026-10-08");
        disk.disk.put("murajaahCursor","50:1");
        Map<String,Object> before=new LinkedHashMap<>(disk.disk);
        HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate today=HifzClock.today();
        assertEquals(HifzPrefs.WeeklyProgram.CLASSIC,prefs.weeklyProgramMode());
        assertTrue(prefs.setWeeklyProgramMode(HifzPrefs.WeeklyProgram.RECOMMENDED));
        assertEquals(HifzPrefs.WeeklyProgram.CLASSIC,prefs.weeklyProgramFor(today.minusDays(1)));
        assertEquals(HifzPrefs.WeeklyProgram.RECOMMENDED,prefs.weeklyProgramFor(today));
        assertEquals(4,prefs.itqanDaysPerWeek());
        assertEquals(3,prefs.learningDaysPerWeek());
        HifzPrefs afterRestart=new HifzPrefs(disk);
        assertEquals(HifzPrefs.WeeklyProgram.RECOMMENDED,afterRestart.weeklyProgramMode());
        for(Map.Entry<String,Object> state:before.entrySet())
            assertEquals("No cursor/repetition/acquired/date mutation: "+state.getKey(),
                state.getValue(),disk.disk.get(state.getKey()));
        assertTrue(afterRestart.setWeeklyProgramMode(HifzPrefs.WeeklyProgram.CLASSIC));
        assertEquals(HifzPrefs.WeeklyProgram.CLASSIC,afterRestart.weeklyProgramMode());
        assertEquals(HifzPrefs.WeeklyProgram.CLASSIC,afterRestart.weeklyProgramFor(today.minusDays(1)));
    }

    @Test public void overdueDatesKeepOriginalModeAndTodayUsesCurrentMode() {
        InMemoryPrefs disk=new InMemoryPrefs();
        HifzPrefs prefs=new HifzPrefs(disk);
        LocalDate today=HifzClock.today();
        disk.disk.put("weeklyProgramTransitionsV1",
            "[{\"from\":\""+today.toString()+"\",\"mode\":\"RECOMMENDED\"}]");
        LocalDate yesterday=today.minusDays(1);
        assertEquals(com.quransafeguard.hifz.core.HifzSchedule.INSTANCE.actionFor(yesterday.getDayOfWeek(),3),
            WeeklyProgramPolicy.actionFor(yesterday.getDayOfWeek(),prefs.weeklyProgramFor(yesterday),3));
        HashSet<LocalDate> done=new HashSet<>();
        done.add(yesterday);
        ScheduledCadence due=WeeklyProgramPolicy.nextDue(prefs,yesterday,today,done,3);
        assertNotNull(due);
        assertEquals(today,due.getScheduledDate());
        assertEquals(WeeklyProgramPolicy.actionFor(today.getDayOfWeek(),
            HifzPrefs.WeeklyProgram.RECOMMENDED,3),due.getAction());
        assertFalse(due.getOverdue());
    }
}

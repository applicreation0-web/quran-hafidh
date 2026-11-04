package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/** P1 parcours audit: cross-midnight OPEN Itqan evening must be visible in "Aujourd'hui".
 * Uses an isolated emulator only; no user backup or credits are written. */
public final class ItqanCarryoverNavigation11711InstrumentedTest {
    private Context context;
    private SharedPreferences disk;

    @Before public void before() {
        context=ApplicationProvider.getApplicationContext();
        HifzClock.setClockForTests(Clock.fixed(
            Instant.parse("2026-11-04T08:00:00Z"), ZoneId.of("UTC")));
        disk=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(disk.edit().clear().commit());
    }

    @After public void after() {
        assertTrue(disk.edit().clear().commit());
        HifzClock.resetClockForTests();
    }

    @Test public void tuesdayUnfinishedItqanMustBeShownOnWednesdayTodayAndKeepFrozenDate() throws Exception {
        HifzPrefs prefs=new HifzPrefs(context);
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        String originalId=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Arrays.asList("525:8","525:9","525:10","525:11","525:12","525:13","525:14"));
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(
            "stabilization-2026-11-03",ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit(originalId,
                ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN));
        ConsolidationCycleEngine.Session open=engine.openSession(cycle,"carryover-2026-11-03");
        assertTrue(prefs.persistConsolidationSession(open));
        assertEquals("2026-11-03",prefs.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-"));
        final String frozenBefore=disk.getString("v6ConsolidationStabilizationState",null);
        assertNotNull(frozenBefore);

        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                try {
                    Method list=MainActivity.class.getDeclaredMethod(
                        "unfinishedWeekendSessions",LocalDate.class);
                    list.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    List<MainActivity.TodaySession> sessions=(List<MainActivity.TodaySession>)
                        list.invoke(activity,LocalDate.of(2026,11,4));
                    MainActivity.TodaySession recovered=null;
                    for(MainActivity.TodaySession row:sessions)
                        if(HifzSessionActivity.RECENT_SABQI_REVIEW.equals(row.mode))recovered=row;
                    assertNotNull("Today must show frozen previous-day Itqan evening",recovered);
                    assertEquals(LocalDate.of(2026,11,3),recovered.scheduled);
                    assertFalse("Never mark an OPEN evening as done",recovered.done);
                    assertTrue(recovered.label.contains("Consolidation Itqān"));
                    assertTrue(recovered.label.contains("2026-11-03"));
                } catch(Exception reflection) {throw new AssertionError(reflection);}
            });
        }
        assertEquals("Reading today's navigation must not mutate the frozen repetition checkpoint",
            frozenBefore,disk.getString("v6ConsolidationStabilizationState",null));
        assertEquals("2026-11-03",new HifzPrefs(context).unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.STABILIZATION,"stabilization-"));
    }
}

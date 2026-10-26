package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Regression for the user's 25 October 2026 backup situation, without embedding
 * private backup bytes or fabricating repetitions and acquired credits.
 * A Saturday OPEN Renforcement and Sunday OPEN Itqān finale must both remain
 * visible on Monday and preserve their exact original session identities.
 */
public final class SundayMondayResumeInstrumentedTest {
    private SharedPreferences raw;
    private final ConsolidationCycleEngine engine=new ConsolidationCycleEngine();

    @Before public void setUp() {
        Context context=ApplicationProvider.getApplicationContext();
        raw=context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
    }
    @After public void tearDown() { raw.edit().clear().commit(); }

    private ConsolidationCycleEngine.Session frozen(String id,
            ConsolidationCycleEngine.Family family,
            ConsolidationCycleEngine.Protocol protocol) {
        String physical=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Arrays.asList("p523-l01","p523-l02"));
        return engine.openSession(engine.startCycle(id,family,
            new ConsolidationCycleEngine.Unit(physical,protocol)),"session-"+id);
    }

    @SuppressWarnings("unchecked")
    @Test public void mondayShowsBothOriginalOpenSessionsWithoutMutation() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        HifzPrefs prefs=new HifzPrefs(context);
        assertTrue(prefs.persistConsolidationSession(frozen(
            "learning-2026-10-24",ConsolidationCycleEngine.Family.LEARNING,
            ConsolidationCycleEngine.Protocol.SNOWBALL)));
        assertTrue(prefs.persistConsolidationSession(frozen(
            "stabilization-final-2026-10-25",ConsolidationCycleEngine.Family.STABILIZATION,
            ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED)));
        Map<String,?> before=raw.getAll();
        // Activity creation requires Android's main Looper, even for a
        // reflection-only UI navigation test. Do not bypass that invariant.
        final java.util.concurrent.atomic.AtomicReference<List<MainActivity.TodaySession>>
            restored=new java.util.concurrent.atomic.AtomicReference<>();
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
            .runOnMainSync(() -> {
                try {
                    MainActivity shell=new MainActivity();
                    Field field=MainActivity.class.getDeclaredField("prefs");
                    field.setAccessible(true);field.set(shell,prefs);
                    Method resume=MainActivity.class.getDeclaredMethod(
                        "unfinishedWeekendSessions",LocalDate.class);
                    resume.setAccessible(true);
                    restored.set((List<MainActivity.TodaySession>)resume.invoke(
                        shell,LocalDate.of(2026,10,26)));
                } catch(ReflectiveOperationException error) {
                    throw new AssertionError("Cannot inspect original-date OPEN recovery",error);
                }
            });
        List<MainActivity.TodaySession> monday=restored.get();
        assertNotNull("Monday navigation result must be visible",monday);
        boolean sabqi=false,itqan=false;
        for(MainActivity.TodaySession row:monday) {
            if(row.mode.equals(HifzSessionActivity.LEARNING_CONSOLIDATION)) {
                assertEquals(LocalDate.of(2026,10,24),row.scheduled);
                assertFalse(row.done);sabqi=true;
            }
            if(row.mode.equals(HifzSessionActivity.CONSOLIDATION_FINAL)) {
                assertEquals(LocalDate.of(2026,10,25),row.scheduled);
                assertFalse(row.done);itqan=true;
            }
        }
        assertTrue("Saturday OPEN Sabqi reinforcement must remain accessible",sabqi);
        assertTrue("Sunday OPEN Itqān finale must remain accessible",itqan);
        assertEquals("Preview cannot write credits, snapshots or dates",before,raw.getAll());
    }
}

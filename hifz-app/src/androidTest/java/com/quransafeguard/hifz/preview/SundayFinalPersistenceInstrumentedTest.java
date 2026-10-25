package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.time.LocalDate;
import java.util.Arrays;
import static org.junit.Assert.*;

/**
 * Instrumented regression: a Sunday finale must not consume an open weekday
 * snapshot, nor lose its own dated progress after process recreation.
 */
public final class SundayFinalPersistenceInstrumentedTest {
    private static final String STORE="quran_hifz_preview_v1";
    private Context context;
    private SharedPreferences raw;
    private final ConsolidationCycleEngine engine=new ConsolidationCycleEngine();

    @Before public void setUp() {
        context=ApplicationProvider.getApplicationContext();
        raw=context.getSharedPreferences(STORE,Context.MODE_PRIVATE);
        raw.edit().clear().commit();
    }
    @After public void tearDown() { raw.edit().clear().commit(); }

    private ConsolidationCycleEngine.Session open(
            String cycleId,ConsolidationCycleEngine.Family family,
            ConsolidationCycleEngine.Protocol protocol) {
        String lineIds=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList(
            "p1-l01","p1-l02","p1-l03"));
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(cycleId,family,
            new ConsolidationCycleEngine.Unit(lineIds,protocol));
        return engine.openSession(cycle,"frozen-"+cycleId);
    }

    @Test public void bothFamiliesKeepSundayAndWeekdayStatesAfterReopen() {
        HifzPrefs prefs=new HifzPrefs(context);
        for(ConsolidationCycleEngine.Family family:ConsolidationCycleEngine.Family.values()){
            String stem=family==ConsolidationCycleEngine.Family.LEARNING?"learning":"stabilization";
            String ordinaryId=stem+"-2026-10-24";
            String finalId=stem+"-final-2026-10-25";
            ConsolidationCycleEngine.Session weekday=engine.recordRepetition(
                open(ordinaryId,family,ConsolidationCycleEngine.Protocol.SNOWBALL));
            assertTrue(prefs.persistConsolidationSession(weekday));
            ConsolidationCycleEngine.Session sunday=engine.recordRepetition(
                engine.recordRepetition(open(finalId,family,ConsolidationCycleEngine.Protocol.SUNDAY_PAGE)));
            assertTrue(prefs.persistConsolidationSession(sunday));

            HifzPrefs afterReopen=new HifzPrefs(context);
            assertEquals("2026-10-24",afterReopen.unfinishedConsolidationSessionDate(family,stem+"-"));
            assertEquals("2026-10-25",afterReopen.unfinishedConsolidationSessionDate(family,stem+"-final-"));
            ConsolidationCycleEngine.Session weekdayRestored=afterReopen.restoreConsolidationSession(
                engine,family,ordinaryId);
            ConsolidationCycleEngine.Session sundayRestored=afterReopen.restoreConsolidationSession(
                engine,family,finalId);
            assertNotNull(weekdayRestored);assertNotNull(sundayRestored);
            assertEquals(1,weekdayRestored.donePerStage());
            assertEquals(2,sundayRestored.donePerStage());
            assertEquals(ConsolidationCycleEngine.Protocol.SUNDAY_PAGE,
                sundayRestored.cycle().units().get(0).protocol());
            assertTrue(afterReopen.discardConsolidationSession(family,finalId));
            assertNotNull(afterReopen.restoreConsolidationSession(engine,family,ordinaryId));
            assertNull(afterReopen.restoreConsolidationSession(engine,family,finalId));
        }
    }

    @Test public void legacySundaySlotMigratesWithoutChangingAnyRepetitions() throws Exception {
        HifzPrefs prefs=new HifzPrefs(context);
        String id="learning-final-2026-10-18";
        ConsolidationCycleEngine.Session sunday=engine.recordRepetition(
            open(id,ConsolidationCycleEngine.Family.LEARNING,
                ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED));
        assertTrue(prefs.persistConsolidationSession(sunday));
        String snapshot=raw.getString("v6ConsolidationLearningStateFinalV1",null);
        assertNotNull(snapshot);
        assertTrue(raw.edit().remove("v6ConsolidationLearningStateFinalV1")
            .putString("v6ConsolidationLearningState",snapshot).commit());

        HifzPrefs upgraded=new HifzPrefs(context);
        assertEquals("2026-10-18",upgraded.unfinishedConsolidationSessionDate(
            ConsolidationCycleEngine.Family.LEARNING,"learning-final-"));
        assertNull(raw.getString("v6ConsolidationLearningState",null));
        assertEquals(snapshot,raw.getString("v6ConsolidationLearningStateFinalV1",null));
        ConsolidationCycleEngine.Session restored=upgraded.restoreConsolidationSession(
            engine,ConsolidationCycleEngine.Family.LEARNING,id);
        assertNotNull(restored);
        assertEquals(1,restored.donePerStage());
        assertEquals(ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED,
            restored.cycle().units().get(0).protocol());
    }
}

package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Runtime persistence contract for grouped Consolidation sessions. */
public final class ConsolidationPersistenceInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private static final String LEARNING_KEY = "v6ConsolidationLearningState";
    private static final String STABILIZATION_KEY = "v6ConsolidationStabilizationState";

    private Context context;
    private SharedPreferences main;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        assertTrue(main.edit().clear().commit());
    }

    @After public void tearDown() {
        assertTrue(main.edit().clear().commit());
    }

    @Test public void openMixedSessionSurvivesPrefsRecreationExactly() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.setSabqiProgress(11, 3));
        assertTrue(prefs.setItqanProgress(17, 2, 4, new VerseRef(49, 1), new VerseRef(49, 5)));
        int[] individualBefore = individualCounters(prefs);

        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "stabilization-cycle-1",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("light-unit", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle,
            new ConsolidationCycleEngine.Unit("full-unit", ConsolidationCycleEngine.Protocol.FULL));
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "session-open-1");

        // LIGHT/FULL size-2: 10 + 7 repetitions complete stage 0; one more gives stage 1/unit 1/done 1.
        for (int i = 0; i < 18; i++) session = engine.recordRepetition(session);
        assertEquals(1, session.stage());
        assertEquals(1, session.nextUnitIndex());
        assertEquals(1, session.donePerStage());

        assertTrue(prefs.persistConsolidationSession(session));
        assertArrayEquals(individualBefore, individualCounters(prefs));
        assertTrue(main.contains(STABILIZATION_KEY));
        assertFalse(main.contains(LEARNING_KEY));

        // New objects model process recreation; no in-memory Session/Cycle object is reused.
        HifzPrefs reopened = new HifzPrefs(context);
        ConsolidationCycleEngine restoredEngine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Session restored = reopened.restoreConsolidationSession(
            restoredEngine, ConsolidationCycleEngine.Family.STABILIZATION);

        assertNotNull(restored);
        assertTrue(restored.open());
        assertEquals("session-open-1", restored.sessionId());
        assertEquals("stabilization-cycle-1", restored.cycle().cycleId());
        assertEquals(ConsolidationCycleEngine.Family.STABILIZATION, restored.cycle().family());
        assertEquals(2, restored.sessionGroupSize());
        assertEquals(2, restored.cycle().units().size());
        assertEquals("light-unit", restored.cycle().units().get(0).id());
        assertEquals(ConsolidationCycleEngine.Protocol.LIGHT, restored.cycle().units().get(0).protocol());
        assertEquals("full-unit", restored.cycle().units().get(1).id());
        assertEquals(ConsolidationCycleEngine.Protocol.FULL, restored.cycle().units().get(1).protocol());
        assertEquals(1, restored.stage());
        assertEquals(1, restored.nextUnitIndex());
        assertEquals(1, restored.donePerStage());
        assertArrayEquals(new int[]{10, 0, 2, 3, 3}, restored.stageVectorAt(0));
        assertArrayEquals(new int[]{7, 2, 3, 3, 5}, restored.stageVectorAt(1));
        assertArrayEquals(individualBefore, individualCounters(reopened));
    }

    @Test public void learningAndStabilizationPersistenceArePhysicallySeparated() {
        HifzPrefs prefs = new HifzPrefs(context);
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();

        ConsolidationCycleEngine.Cycle stabilization = engine.startCycle(
            "stabilization-cycle",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("stable-1", ConsolidationCycleEngine.Protocol.LIGHT));
        ConsolidationCycleEngine.Session stabilizationSession = engine.openSession(stabilization, "stable-session");
        assertTrue(prefs.persistConsolidationSession(stabilizationSession));

        ConsolidationCycleEngine.Cycle learning = engine.startCycle(
            "learning-cycle",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("learn-1", ConsolidationCycleEngine.Protocol.LEARNING37));
        ConsolidationCycleEngine.Session learningSession = engine.openSession(learning, "learn-session");
        assertTrue(prefs.persistConsolidationSession(learningSession));

        assertTrue(main.contains(LEARNING_KEY));
        assertTrue(main.contains(STABILIZATION_KEY));

        HifzPrefs reopened = new HifzPrefs(context);
        ConsolidationCycleEngine.Session restoredLearning = reopened.restoreConsolidationSession(
            new ConsolidationCycleEngine(), ConsolidationCycleEngine.Family.LEARNING);
        ConsolidationCycleEngine.Session restoredStabilization = reopened.restoreConsolidationSession(
            new ConsolidationCycleEngine(), ConsolidationCycleEngine.Family.STABILIZATION);
        assertEquals("learning-cycle", restoredLearning.cycle().cycleId());
        assertEquals("learn-session", restoredLearning.sessionId());
        assertEquals("stabilization-cycle", restoredStabilization.cycle().cycleId());
        assertEquals("stable-session", restoredStabilization.sessionId());
    }

    @Test public void corruptFrozenGroupSizeFailsClosedInsteadOfRepairingSnapshot() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "cycle-corrupt",
            ConsolidationCycleEngine.Family.STABILIZATION,
            new ConsolidationCycleEngine.Unit("light", ConsolidationCycleEngine.Protocol.LIGHT));
        cycle = engine.addUnit(cycle,
            new ConsolidationCycleEngine.Unit("full", ConsolidationCycleEngine.Protocol.FULL));
        assertTrue(prefs.persistConsolidationSession(engine.openSession(cycle, "session-corrupt")));

        String raw = main.getString(STABILIZATION_KEY, null);
        assertNotNull(raw);
        JSONObject corrupt = new JSONObject(raw);
        corrupt.put("sessionGroupSize", 3);
        assertTrue(main.edit().putString(STABILIZATION_KEY, corrupt.toString()).commit());

        try {
            new HifzPrefs(context).restoreConsolidationSession(
                new ConsolidationCycleEngine(), ConsolidationCycleEngine.Family.STABILIZATION);
            fail("corrupt frozen group size must fail closed");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage() != null && !expected.getMessage().trim().isEmpty());
        }
    }

    @Test public void persistingEveryGroupedRepetitionNeverMutatesIndividualCounters() {
        HifzPrefs prefs = new HifzPrefs(context);
        assertTrue(prefs.setSabqiProgress(29, 6));
        assertTrue(prefs.setItqanProgress(31, 7, 5, new VerseRef(49, 1), new VerseRef(49, 5)));
        int[] individualBefore = individualCounters(prefs);

        ConsolidationCycleEngine engine = new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle = engine.startCycle(
            "learning-cycle-full-pass",
            ConsolidationCycleEngine.Family.LEARNING,
            new ConsolidationCycleEngine.Unit("learn-a", ConsolidationCycleEngine.Protocol.LEARNING37));
        ConsolidationCycleEngine.Session session = engine.openSession(cycle, "learning-session-full-pass");

        while (!session.readyToClose()) {
            session = engine.recordRepetition(session);
            assertTrue(prefs.persistConsolidationSession(session));
            assertArrayEquals(individualBefore, individualCounters(prefs));
        }
        assertEquals(5, session.stage());
        assertArrayEquals(individualBefore, individualCounters(new HifzPrefs(context)));
    }

    private static int[] individualCounters(HifzPrefs prefs) {
        return new int[]{
            prefs.sabqiRep(),
            prefs.sabqiAssisted(),
            prefs.itqanRep(),
            prefs.itqanAssisted(),
            prefs.itqanFinalReveals()
        };
    }
}

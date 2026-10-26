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
        assertTrue(raw.edit().putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,true).commit());
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

    @Test public void unstartedSabqiFinalRetainsItsSundayAfterAnotherWeek() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        HifzPrefs prefs=new HifzPrefs(context);
        raw.edit().putString("v6AcquiredCreditLineIds","[]").commit();
        // Use the production physical-unit codec, not a hand-written encoding.
        String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("16:0","16:1"));
        raw.edit().putString("learningSnowball8WeekHistory",new org.json.JSONArray().put(
            new org.json.JSONObject().put("week","2026-10-19").put("units",new org.json.JSONArray().put(unit))).toString()).commit();
        Map<String,?> before=raw.getAll();
        for(String day:new String[]{"2026-10-26","2026-11-02","2026-11-09"})
            assertEquals(LocalDate.of(2026,10,25),new HifzPrefs(context)
                .pendingLearningFinalDate(LocalDate.parse(day)));
        assertEquals(before,raw.getAll());
    }

    @Test public void ordinaryItqanUsesExactV1PhysicalCursorAfterPreferencesReopen() {
        Context context=ApplicationProvider.getApplicationContext();
        raw.edit().putInt("schema",6).putString("v6LearnedLineIds","[]")
            .putString("v6StabilizedLineIds","[]").putString("v6AcquiredCreditLineIds","[]")
            .putString("v6QuarantineLineIds","[]").putString("v6LegacyPartialAcquiredLineIds","[]")
            .putString("sabqiStart","2:1").putString("sabqiEnd","48:29")
            .putString("itqanFourNextPhysicalLineIdV1","522:0")
            .putString(ItqanRegimeStore.LEG,ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS.name())
            .putString(ItqanRegimeStore.CURSOR,"51:31")
            .putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,false).commit();
        GeometryRepository geometry=GeometryRepository.get(ApplicationProvider.getApplicationContext());
        for(int reopen=0;reopen<2;reopen++) {
            HifzPrefs prefs=new HifzPrefs(context);
            AnchoringQueue.Entry entry=prefs.currentAnchoringEntry(geometry);
            assertEquals("522:0",prefs.ordinaryItqanLineIds(GeometryRepository.parseVerse(entry.start),
                GeometryRepository.parseVerse(entry.end),geometry).get(0));
            assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
            assertNull(prefs.frozenItqanSnapshot(geometry));
            assertEquals("51:31",raw.getString(ItqanRegimeStore.CURSOR,""));
        }
    }

    @Test public void directIntentCannotCreateNewPostNasFinalDespiteEligibleReceipts() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        List<String> ids=GeometryRepository.get(context).lineIdsOnPage(523).subList(0,2);
        String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids);
        assertTrue(raw.edit().putInt("schema",6)
            .putString("v6LearnedLineIds","[]").putString("v6AcquiredCreditLineIds","[]")
            .putString("v6StabilizedLineIds",new org.json.JSONArray(ids).toString())
            .putString("v6QuarantineLineIds","[]").putString("v6LegacyPartialAcquiredLineIds","[]")
            .putBoolean(ItqanRegimeStore.INITIAL_TAIL_COMPLETED,true)
            .putString("itqanProgrammeChoiceV1","CONSOLIDATION")
            .putString("itqanProgrammeStartsOnV1","2026-01-01")
            .putString("v6StabilizationEarnedEveningUnitsV1",new org.json.JSONArray().put(unit).toString())
            .putString("stabilizationSnowball8WeekHistory",new org.json.JSONArray().put(
                new org.json.JSONObject().put("week","2026-10-19").put("units",new org.json.JSONArray().put(unit))).toString())
            .commit());
        assertFalse(new HifzPrefs(context).stabilizationSnowballFinalUnits(LocalDate.of(2026,10,25)).isEmpty());
        android.content.Intent intent=new android.content.Intent(context,HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.CONSOLIDATION_FINAL)
            .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,"2026-10-25");
        try(androidx.test.core.app.ActivityScenario<HifzSessionActivity> scenario=
                androidx.test.core.app.ActivityScenario.launch(intent)) {
            scenario.onActivity(activity->{
                assertFalse(raw.contains("v6ConsolidationStabilizationStateFinalV1"));
                assertTrue(new HifzPrefs(context).progressionSnapshotV6().acquired.isEmpty());
                assertEquals(new java.util.LinkedHashSet<>(ids),new HifzPrefs(context).progressionSnapshotV6().stabilized);
            });
        }
    }
}

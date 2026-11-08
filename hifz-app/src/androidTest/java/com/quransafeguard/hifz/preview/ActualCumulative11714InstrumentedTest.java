package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** ONLY disposable BOOX-resolution emulator captures of genuine 1.17.14 UI.
 * Fixture states are synthetic; no production progress, release APK or keystore. */
public final class ActualCumulative11714InstrumentedTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    private SharedPreferences disk() {
        return context.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
    }

    @After public void clearFixture() {
        disk().edit().clear().commit();
    }

    private static String ui(HifzSessionActivity a, String name) throws Exception {
        Field f=HifzSessionActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return ((TextView)f.get(a)).getText().toString();
    }

    private static void repeatCurrentUnit(HifzSessionActivity a) {
        try {
            Method m=HifzSessionActivity.class.getDeclaredMethod("completeGroupedCycleRep");
            m.setAccessible(true);
            m.invoke(a);
        } catch(Exception error) {
            throw new AssertionError("Cannot record real grouped-cycle repetition",error);
        }
    }

    private void runCase(boolean stabilization) throws Exception {
        disk().edit().clear().commit();
        GeometryRepository geometry=GeometryRepository.get(context);
        List<String> folio=geometry.lineIdsOnPage(525);
        assertTrue("Authentic page must contain 10 consecutive QCF lines",folio.size()>=10);
        ArrayList<String> all=new ArrayList<>(folio.subList(0,10));
        List<String> a=folio.subList(0,5), b=folio.subList(5,10);
        String first=ConsolidationPhysicalUnitPolicy.encodeLineUnit(a);
        String next=ConsolidationPhysicalUnitPolicy.encodeLineUnit(b);
        LocalDate day=HifzClock.today(),mon=day.minusDays(day.getDayOfWeek().getValue()-1);
        String family=stabilization?"STABILIZATION":"LEARNING";
        String key=stabilization?"stabilization":"learning";
        JSONObject authenticDailyReceipt=new JSONObject()
            .put("family",family).put("date",day.toString()).put("lines",new JSONArray(b));
        assertTrue(disk().edit()
            .putString(key+"SnowballWeekAnchor",mon.toString())
            .putString(key+"SnowballUnitIds",new JSONArray().put(first).put(next).toString())
            .putString(stabilization?"v6StabilizedLineIds":"v6LearnedLineIds",new JSONArray(all).toString())
            .putString("v6DailyLessonReceiptsV1",new JSONArray().put(authenticDailyReceipt).toString())
            .commit());
        HifzPrefs prefs=new HifzPrefs(context);
        ConsolidationCycleEngine.Family f=stabilization
           ? ConsolidationCycleEngine.Family.STABILIZATION
           : ConsolidationCycleEngine.Family.LEARNING;
        List<ConsolidationCycleEngine.Unit> plan=stabilization
           ?prefs.stabilizedConsolidationUnits(day)
           :prefs.learningConsolidationUnits(day);
        assertEquals("Two authentic QCF blocks + one physical continuous pass",3,plan.size());
        assertEquals(ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(List.of(first,next)),
            plan.get(2).id());
        assertEquals(stabilization
            ?ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS_FIVE
            :ConsolidationCycleEngine.Protocol.SNOWBALL,
            plan.get(2).protocol());
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(key+"-"+day,f,plan.get(0));
        cycle=engine.addUnit(cycle,plan.get(1));
        cycle=engine.addUnit(cycle,plan.get(2));
        ConsolidationCycleEngine.Session session=engine.openSession(cycle,"actual-screen-fixture");
        int previousPasses=stabilization?14:20;
        for(int i=0;i<previousPasses;i++) session=engine.recordRepetition(session);
        assertEquals(2,session.nextUnitIndex());
        assertEquals(stabilization?5:10,session.quotaAt(2));
        assertEquals(0,session.donePerStage());
        assertTrue("Frozen OPEN must be persisted before UI",prefs.persistConsolidationSession(session));
        final String mode=stabilization
            ?HifzSessionActivity.RECENT_SABQI_REVIEW
            :HifzSessionActivity.LEARNING_CONSOLIDATION;
        Intent intent=new Intent(context,HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE,mode);
        try(ActivityScenario<HifzSessionActivity> screen=ActivityScenario.launch(intent)) {
            screen.onActivity(activity->{
                try{
                    assertTrue("Active full cumul screen must be present",
                        ui(activity,"program").contains(stabilization?"passage final ×5":"unité 3/3"));
                    assertTrue("Real engine must display frozen repetitions",
                        ui(activity,"progress").contains(stabilization?"0/5":"0/10"));
                }catch(Exception ex){throw new AssertionError(ex);}
            });
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(
                screen,"mushaf","real "+family+" cumulative QCF");
            Independent11710ScreensInstrumentedTest.capture(stabilization
               ?"itqan-new-AB-x5-real-QCF-0of5":"sabqi-AB-x10-real-QCF-0of10");
            screen.onActivity(ActualCumulative11714InstrumentedTest::repeatCurrentUnit);
            screen.onActivity(activity->{
                try{
                    assertTrue("First real repetition must be visible",
                        ui(activity,"progress").contains(stabilization?"1/5":"1/10"));
                }catch(Exception ex){throw new AssertionError(ex);}
            });
            Independent11710ScreensInstrumentedTest.capture(stabilization
               ?"itqan-new-AB-x5-real-QCF-1of5":"sabqi-AB-x10-real-QCF-1of10");
        }
    }

    @Test public void realItqanCumulativeFiveScreen()throws Exception{
        runCase(true);
    }

    @Test public void realSabqiCumulativeTenScreen()throws Exception{
        runCase(false);
    }
}

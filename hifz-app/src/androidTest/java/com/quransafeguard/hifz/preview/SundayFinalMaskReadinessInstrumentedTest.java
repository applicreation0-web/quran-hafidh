package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.lang.reflect.*;
import java.util.*;
import static org.junit.Assert.*;

/** The actual repetition click handler must reject loading or unavailable masks in both finales. */
public final class SundayFinalMaskReadinessInstrumentedTest {
    @Test public void sabqiCannotCountMaskedRecallBeforePreparedPaint() throws Exception {
        verify(ConsolidationCycleEngine.Family.LEARNING,HifzSessionActivity.LEARNING_FINAL,"learning-final-");
    }
    @Test public void itqanCannotCountMaskedRecallBeforePreparedPaint() throws Exception {
        verify(ConsolidationCycleEngine.Family.STABILIZATION,HifzSessionActivity.CONSOLIDATION_FINAL,"stabilization-final-");
    }
    private void verify(ConsolidationCycleEngine.Family family,String mode,String prefix) throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",0);
        assertTrue(raw.edit().clear().commit());
        HifzPrefs prefs=new HifzPrefs(c);
        GeometryRepository g=GeometryRepository.get(c);
        String date="2026-10-25";
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(g.lineIdsOnPage(1));
        ConsolidationCycleEngine.Session session=engine.recordRepetition(engine.openSession(
            engine.startCycle(prefix+date,family,new ConsolidationCycleEngine.Unit(unit,
                ConsolidationCycleEngine.Protocol.SUNDAY_PAGE)),"mask-readiness"));
        assertTrue(prefs.persistConsolidationSession(session));
        Intent intent=new Intent(c,HifzSessionActivity.class).putExtra(HifzSessionActivity.EXTRA_MODE,mode)
            .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,date);
        try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(intent)) {
            scenario.onActivity(activity->{
                try {
                    set(activity,"consolidationSession",session);
                    set(activity,"currentMask",100);
                    Method click=HifzSessionActivity.class.getDeclaredMethod("completeGroupedCycleRep");
                    click.setAccessible(true);
                    for(boolean unavailable:new boolean[]{false,true}) {
                        set(activity,"sundayFinalAnchorUnavailable",unavailable);
                        set(activity,"sundayFinalMaskedPageReady",false);
                        Map<String,?> before=new HashMap<>(raw.getAll());
                        click.invoke(activity);
                        assertEquals("No repetition or credit before mask is ready",before,raw.getAll());
                    }
                }catch(ReflectiveOperationException error){throw new AssertionError(error);}
            });
        } finally {assertTrue(raw.edit().clear().commit());}
    }
    private static void set(Object target,String name,Object value)throws ReflectiveOperationException {
        Field f=HifzSessionActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
}

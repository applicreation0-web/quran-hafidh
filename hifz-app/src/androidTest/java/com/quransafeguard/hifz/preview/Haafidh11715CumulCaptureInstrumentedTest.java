package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Android screen captures of disposable frozen QCF cumulative states, not mockups.
 * This class is copied UNMODIFIED into 1.17.14's exact worktree for BEFORE captures.
 * It never validates an artificial cycle, mutates the authentic app's data, or signs an APK. */
public final class Haafidh11715CumulCaptureInstrumentedTest {
    private static Object field(Object target,String name) {
        try {
            Field f=target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch(Exception e) { throw new AssertionError(name,e); }
    }

    private static List<List<String>> contiguousRealBlocks(GeometryRepository geometry) {
        for(int page=2;page<=604;page++) {
            List<String> lines=geometry.lineIdsOnPage(page);
            if(lines.size()<10) continue;
            List<String> first=new ArrayList<>(lines.subList(0,5));
            List<String> second=new ArrayList<>(lines.subList(5,10));
            List<String> ids=Arrays.asList(
                ConsolidationPhysicalUnitPolicy.encodeLineUnit(first),
                ConsolidationPhysicalUnitPolicy.encodeLineUnit(second));
            if(ConsolidationPhysicalUnitPolicy.physicallyContinuous(ids,geometry))
                return Arrays.asList(first,second);
        }
        throw new AssertionError("No contiguous source QCF fixture available");
    }

    private static void capture(ConsolidationCycleEngine.Family family,
                                String mode,String prefix,String title,String screenshot)throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(context);
        List<List<String>> blocks=contiguousRealBlocks(geometry);
        String a=ConsolidationPhysicalUnitPolicy.encodeLineUnit(blocks.get(0));
        String b=ConsolidationPhysicalUnitPolicy.encodeLineUnit(blocks.get(1));
        String all=ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(Arrays.asList(a,b));
        LocalDate today=HifzClock.today();
        String id=prefix+today;
        ConsolidationCycleEngine engine=new ConsolidationCycleEngine();
        ConsolidationCycleEngine.Protocol perBlock=family==ConsolidationCycleEngine.Family.LEARNING
            ?ConsolidationCycleEngine.Protocol.SNOWBALL
            :ConsolidationCycleEngine.Protocol.SNOWBALL_STABILIZATION_SEVEN;
        ConsolidationCycleEngine.Protocol composite=family==ConsolidationCycleEngine.Family.LEARNING
            ?ConsolidationCycleEngine.Protocol.SNOWBALL
            :ConsolidationCycleEngine.Protocol.SNOWBALL_ALL_PARTS_FIVE;
        List<ConsolidationCycleEngine.Unit> units=Arrays.asList(
            new ConsolidationCycleEngine.Unit(a,perBlock),
            new ConsolidationCycleEngine.Unit(b,perBlock),
            new ConsolidationCycleEngine.Unit(all,composite));
        ConsolidationCycleEngine.Cycle cycle=engine.startCycle(id,family,units.get(0));
        cycle=engine.addUnit(cycle,units.get(1));
        cycle=engine.addUnit(cycle,units.get(2));
        ConsolidationCycleEngine.Session opened=engine.openSession(cycle,"session-"+today);
        int repetitions=family==ConsolidationCycleEngine.Family.LEARNING?20:14;
        for(int i=0;i<repetitions;i++)opened=engine.recordRepetition(opened);
        assertEquals(2,opened.nextUnitIndex());
        assertEquals(0,opened.donePerStage());
        int expected=family==ConsolidationCycleEngine.Family.LEARNING?10:5;
        assertEquals(expected,opened.stageVectorAt(2)[opened.stage()]);
        HifzPrefs prefs=new HifzPrefs(context);
        assertTrue("Disposable authentic QCF OPEN fixture must be durable",prefs.persistConsolidationSession(opened));
        Intent intent=new Intent(context,HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE,mode)
            .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,today.toString());
        try(ActivityScenario<HifzSessionActivity> scene=ActivityScenario.launch(intent)){
            scene.onActivity(activity->{
                try{
                    Method render=activity.getClass().getDeclaredMethod("renderGroupedCycle",
                        List.class,ConsolidationCycleEngine.Family.class,String.class,String.class,
                        String.class,Runnable.class);
                    render.setAccessible(true);
                    render.invoke(activity,units,family,prefix,title,"", (Runnable)()->{});
                }catch(Exception e){throw new AssertionError("Cannot render exact frozen OPEN UI",e);}
                TextView program=(TextView)field(activity,"program");
                TextView progress=(TextView)field(activity,"progress");
                assertTrue(program.getText().toString().startsWith(title));
                assertTrue("Actual frozen quota visible",progress.getText().toString().contains("/"+expected));
                assertEquals(all,
                    ((ConsolidationCycleEngine.Session)field(activity,"consolidationSession"))
                        .unitIds().get(2));
            });
            HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scene,"mushaf",screenshot);
            Independent11710ScreensInstrumentedTest.capture(screenshot);
        }finally {
            // Never let this intentionally incomplete OPEN proof become a receipt.
            assertTrue(prefs.discardConsolidationSession(family,id));
        }
    }

    @Test public void authenticItqanFivefoldCumulFolioAndCounter()throws Exception{
        capture(ConsolidationCycleEngine.Family.STABILIZATION,
            HifzSessionActivity.RECENT_SABQI_REVIEW,"stabilization-",
            "Consolidation","11715-cumul-itqan-x5");
    }
    @Test public void authenticSabqiTenfoldCumulFolioAndCounter()throws Exception{
        capture(ConsolidationCycleEngine.Family.LEARNING,
            HifzSessionActivity.LEARNING_CONSOLIDATION,"learning-",
            "Renforcement","11715-cumul-sabqi-x10");
    }
}

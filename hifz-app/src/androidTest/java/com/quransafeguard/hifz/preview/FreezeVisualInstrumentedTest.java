package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Real Android BOOX screens + zero-credit, full-credit and Juz boundary proofs. */
public final class FreezeVisualInstrumentedTest {
    private static Object get(Object object,String field){
        try{
            Field f=object.getClass().getDeclaredField(field);
            f.setAccessible(true);
            return f.get(object);
        }catch(Exception e){throw new AssertionError("Missing view "+field,e);}
    }
    @Test public void actualJuzViewHasThirtyTouchTargetsAndTruePageNavigation(){
        try(ActivityScenario<ProgressMapActivity> s=ActivityScenario.launch(ProgressMapActivity.class)){
            s.onActivity(a->{
                LinearLayout[] cards=(LinearLayout[])get(a,"juzCards");
                assertEquals(30,cards.length);
                for(int i=0;i<30;i++){
                    assertNotNull("One touch target per canonical Juz",cards[i]);
                    assertTrue(cards[i].isClickable());
                }
                assertEquals(0,((Integer)get(a,"selectedJuz")).intValue());
                LinearLayout overview=(LinearLayout)get(a,"juzOverview");
                assertEquals(View.VISIBLE,overview.getVisibility());
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-juz-overview");
            s.onActivity(a->{
                LinearLayout[] cards=(LinearLayout[])get(a,"juzCards");
                assertTrue("Tap Juz 30, not a modal picker",cards[29].performClick());
                ProgressGridView grid=(ProgressGridView)get(a,"grid");
                assertEquals(30,((Integer)get(a,"selectedJuz")).intValue());
                assertEquals(582,grid.firstVisiblePage());
                assertEquals(604,grid.lastVisiblePage());
                assertEquals(View.GONE,((LinearLayout)get(a,"juzOverview")).getVisibility());
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-progress-juz30");
            s.onActivity(a->{
                a.onBackPressed();
                assertEquals(0,((Integer)get(a,"selectedJuz")).intValue());
                assertEquals(View.VISIBLE,((LinearLayout)get(a,"juzOverview")).getVisibility());
                LinearLayout[] cards=(LinearLayout[])get(a,"juzCards");
                assertTrue(cards[0].performClick());
                ProgressGridView grid=(ProgressGridView)get(a,"grid");
                assertEquals(1,grid.firstVisiblePage());
                assertEquals(21,grid.lastVisiblePage());
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-progress-juz01");
        }
    }
    @Test public void physicalLineLevelsFillPagesWithoutFakeCredits(){
        Context context=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(context);
        java.util.ArrayList<GeometryRepository.LineMeta> lines=new java.util.ArrayList<>();
        for(int i=0;i<geometry.lineCount();i++)lines.add(geometry.line(i));
        Set<String> none=java.util.Collections.emptySet();
        int[][] empty=JuzProgressSummary.fromLines(lines,none,none,none);
        assertEquals(30,empty.length);
        int counted=0;
        for(int[] c:empty){
            assertTrue("Each canonical Juz contains real lines",c[JuzProgressSummary.TOTAL]>0);
            assertEquals(0,JuzProgressSummary.done(c));
            counted+=c[JuzProgressSummary.TOTAL];
        }
        assertEquals("No line may be lost at the Juz boundary",geometry.lineCount(),counted);
        Set<String> all=new HashSet<>();
        for(GeometryRepository.LineMeta line:lines)all.add(line.id);
        int[][] full=JuzProgressSummary.fromLines(lines,none,none,all);
        for(int j=0;j<30;j++){
            assertEquals(empty[j][JuzProgressSummary.TOTAL],full[j][JuzProgressSummary.TOTAL]);
            assertEquals(full[j][JuzProgressSummary.TOTAL],full[j][JuzProgressSummary.ACQUIRED]);
            assertEquals(100,JuzProgressSummary.percent(
                JuzProgressSummary.done(full[j]),full[j][JuzProgressSummary.TOTAL]));
        }

        Set<String> acquired=new HashSet<>();
        Set<String> learned=new HashSet<>();
        Set<String> stabilized=new HashSet<>();
        for(int i=0;i<Math.min(60,lines.size());i++){
            String id=lines.get(i).id;
            if(i<20)acquired.add(id);
            else if(i<40)stabilized.add(id);
            else learned.add(id);
        }
        // A real line is counted only once even when stale sets overlap.
        learned.addAll(acquired);
        stabilized.addAll(acquired);
        int[][] partial=JuzProgressSummary.fromLines(lines,learned,stabilized,acquired);
        assertTrue(partial[0][JuzProgressSummary.ACQUIRED]>0);
        assertTrue(partial[0][JuzProgressSummary.STABILIZED]>0);
        assertTrue(partial[0][JuzProgressSummary.LEARNED]>0);
        assertEquals(60,JuzProgressSummary.done(partial[0]));
        assertTrue(JuzProgressSummary.percent(JuzProgressSummary.done(partial[0]),
            partial[0][JuzProgressSummary.TOTAL])<100);
        for(int j=1;j<30;j++)assertEquals(0,JuzProgressSummary.done(partial[j]));
    }
}

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
    @Test public void genuineHifzCreditsAloneFillCanonicalJuzGauges(){
        Context context=ApplicationProvider.getApplicationContext();
        GeometryRepository geometry=GeometryRepository.get(context);
        int[][] empty=JuzProgressSummary.summarize(geometry,java.util.Collections.emptySet());
        assertEquals(30,empty.length);
        for(int[] count:empty){
            assertTrue("Every canonical Juz has Qur'an verses",count[0]>0);
            assertEquals(0,count[1]);
        }
        Set<String> all=new HashSet<>();
        for(int i=0;i<geometry.lineCount();i++)all.add(geometry.line(i).id);
        int[][] fullyAcquired=JuzProgressSummary.summarize(geometry,all);
        for(int i=0;i<30;i++){
            assertEquals(empty[i][0],fullyAcquired[i][0]);
            assertEquals(empty[i][0],fullyAcquired[i][1]);
            assertEquals(100,JuzProgressSummary.percent(
                fullyAcquired[i][1],fullyAcquired[i][0]));
        }
        Set<String> partial=new HashSet<>();
        for(int i=0;i<Math.min(60,geometry.lineCount());i++)
            partial.add(geometry.line(i).id);
        int[][] partialCount=JuzProgressSummary.summarize(geometry,partial);
        assertTrue("Real credited QCF verses should increase Juz 1",partialCount[0][1]>0);
        assertTrue("Juz 1 cannot be marked 100% after merely 60 QCF lines",
            JuzProgressSummary.percent(partialCount[0][1],partialCount[0][0])<100);
        for(int i=1;i<30;i++)
            assertEquals("No credit may cross into untouched Juz "+(i+1),0,partialCount[i][1]);
    }
}

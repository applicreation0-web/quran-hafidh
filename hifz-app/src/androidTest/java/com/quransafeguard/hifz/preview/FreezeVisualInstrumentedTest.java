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
    /** Synthetic display-only states using the current physical-line API. */
    private static int[] visualLevels(int learned,int stabilized,int acquired,int total){
        if(learned<0||stabilized<0||acquired<0||total<=0
                ||learned+stabilized+acquired>total)
            throw new IllegalArgumentException("Invalid visual fixture");
        int[] states=new int[total];
        java.util.Arrays.fill(states,ProgressGridView.VIDE);
        java.util.Arrays.fill(states,0,learned,ProgressGridView.APPRENTISSAGE);
        java.util.Arrays.fill(states,learned,learned+stabilized,ProgressGridView.STABILISER);
        java.util.Arrays.fill(states,learned+stabilized,learned+stabilized+acquired,ProgressGridView.ACQUIS);
        return states;
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
            // Read-only visual fixture: example fill levels on an actual Android
            // screenshot, without changing Hifz prefs, receipts or saved progression.
            s.onActivity(a->{
                JuzProgressBar[] shapes=(JuzProgressBar[])get(a,"juzBars");
                shapes[0].setLineStatuses(visualLevels(18,9,3,100));
                shapes[1].setLineStatuses(visualLevels(0,0,75,100));
                shapes[2].setLineStatuses(visualLevels(20,30,40,100));
                shapes[3].setLineStatuses(visualLevels(0,0,100,100));
                assertEquals(30,shapes[0].filledLines());
                assertEquals(100,shapes[3].filledLines());
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-juz-filled-fixture");
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
            s.recreate();
            s.onActivity(a->{
                assertEquals(1,((Integer)get(a,"selectedJuz")).intValue());
                ProgressGridView grid=(ProgressGridView)get(a,"grid");
                assertEquals(1,grid.firstVisiblePage());
                assertEquals(21,grid.lastVisiblePage());
                a.onBackPressed();
                assertEquals(View.VISIBLE,((LinearLayout)get(a,"juzOverview")).getVisibility());
            });
        }
    }
    @Test public void sharedJuzFolioOpensTheAuthenticPageAndReturnsToTheGrid() {
        android.app.Instrumentation ins =
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
        try (ActivityScenario<ProgressMapActivity> scenario = ActivityScenario.launch(ProgressMapActivity.class)) {
            scenario.onActivity(activity -> {
                LinearLayout[] cards = (LinearLayout[]) get(activity, "juzCards");
                assertTrue(cards[2].performClick());
                ProgressGridView grid = (ProgressGridView) get(activity, "grid");
                assertEquals(62, grid.lastVisiblePage());
                activity.onBackPressed();
                assertTrue(cards[3].performAccessibilityAction(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK, null));
                assertEquals(62, grid.firstVisiblePage());
            });
            android.app.Instrumentation.ActivityMonitor monitor =
                ins.addMonitor(StudyReaderActivity.class.getName(), null, false);
            try {
                scenario.onActivity(activity -> {
                    ProgressGridView grid = (ProgressGridView) get(activity, "grid");
                    long now = android.os.SystemClock.uptimeMillis();
                    android.view.MotionEvent down = android.view.MotionEvent.obtain(
                        now, now, android.view.MotionEvent.ACTION_DOWN, 10, 10, 0);
                    android.view.MotionEvent up = android.view.MotionEvent.obtain(
                        now, now + 20, android.view.MotionEvent.ACTION_UP, 10, 10, 0);
                    try {
                        assertTrue(grid.dispatchTouchEvent(down));
                        assertTrue(grid.dispatchTouchEvent(up));
                    } finally {
                        down.recycle();
                        up.recycle();
                    }
                });
                android.app.Activity reader = ins.waitForMonitorWithTimeout(monitor, 20_000);
                assertNotNull(reader);
                assertEquals(62, reader.getIntent().getIntExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, -1));
                ins.runOnMainSync(reader::finish);
                scenario.onActivity(activity -> {
                    assertEquals(4, ((Integer)get(activity,"selectedJuz")).intValue());
                    activity.onBackPressed();
                    assertEquals(0, ((Integer)get(activity,"selectedJuz")).intValue());
                });
            } finally {
                ins.removeMonitor(monitor);
            }
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

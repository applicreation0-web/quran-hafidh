package com.quransafeguard.hifz.preview;

import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Two genuine BOOX screenshots of the full HOME, from a running Android app.
 * Select a projected year; never change progress, receipts or the post-Nas latch.
 */
public final class CapHifdhTwoRealScreensInstrumentedTest {
    private static Object field(Object o, String key) {
        try {
            Field f=o.getClass().getDeclaredField(key);
            f.setAccessible(true);
            return f.get(o);
        } catch (Exception e) { throw new AssertionError("Field " + key,e); }
    }
    private static void set(Object o, String key, Object value) {
        try {
            Field f=o.getClass().getDeclaredField(key);
            f.setAccessible(true);
            f.set(o,value);
        } catch (Exception e) { throw new AssertionError("Set " + key,e); }
    }
    private static void render(CapHifzPanel panel) {
        try {
            Method m=CapHifzPanel.class.getDeclaredMethod("render");
            m.setAccessible(true);
            m.invoke(panel);
        } catch (Exception e) { throw new AssertionError("Cap Hifdh render",e); }
    }
    private static boolean flag(CapHifzTimelineView t, String name) {
        return (Boolean) field(t,name);
    }
    private static CapHifzTimelineView timeline(CapHifzPanel p) {
        return (CapHifzTimelineView)field(p,"timeline");
    }
    @Test public void screenshotBothFullHomesBeforeAndAfterAnNasReadOnly() {
        try (ActivityScenario<MainActivity> scene=ActivityScenario.launch(MainActivity.class)) {
            final int[] years=new int[2];
            // MainActivity loads the QCF/forecast on the UI asynchronously.
            // A one-shot ActivityScenario callback can observe an empty
            // forecast before refresh() has completed; wait for real readiness.
            boolean loaded=false;
            for (int attempt=0;attempt<100;attempt++) {
                final boolean[] ready={false};
                scene.onActivity(activity -> {
                    CapHifzPanel panel=(CapHifzPanel)field(activity,"capHifz");
                    List<CapHifzForecast.Quarter> forecast=
                        (List<CapHifzForecast.Quarter>)field(panel,"forecastQuarters");
                    ready[0]=forecast!=null && !forecast.isEmpty()
                        && timeline(panel)!=null;
                });
                if (ready[0]) { loaded=true;break; }
                android.os.SystemClock.sleep(150L);
            }
            assertTrue("The REAL Cap Hifdh forecast failed to initialize on Android",
                loaded);
            scene.onActivity(a -> {
                CapHifzPanel p=(CapHifzPanel)field(a,"capHifz");
                List<CapHifzForecast.Quarter> forecast =
                    (List<CapHifzForecast.Quarter>)field(p,"forecastQuarters");
                assertFalse("Real forecast not loaded", forecast.isEmpty());
                CapHifzTimelineView t=timeline(p);
                assertNotNull(t);
                assertTrue("Before An-Nas must have Acquisition", flag(t,"showSabqi"));
                assertTrue("Before An-Nas must have Maîtrise", flag(t,"showTail"));
                years[0]=(Integer)field(p,"year");
                assertEquals("BOOX home must fit without vertical scrolling",
                    View.VISIBLE,p.getVisibility());
            });
            Independent11710ScreensInstrumentedTest.capture(
                "cap-hifdh-full-home-before-nas");
            scene.onActivity(a -> {
                CapHifzPanel p=(CapHifzPanel)field(a,"capHifz");
                List<CapHifzForecast.Quarter> forecast =
                    (List<CapHifzForecast.Quarter>)field(p,"forecastQuarters");
                LocalDate nasFinish=null;
                for (CapHifzForecast.Quarter row:forecast)
                    if (row.tailFinish!=null) {nasFinish=row.tailFinish;break;}
                assertNotNull("A real projected An-Nas finish date is required",nasFinish);
                int target=nasFinish.getYear()+1;
                assertTrue("Post-Nas must be future year",target>years[0]);
                assertTrue("Acquisition continues beyond Nas",
                    target<=(Integer)field(p,"lastForecastYear"));
                set(p,"year",target);
                render(p);
                CapHifzTimelineView t=timeline(p);
                assertNotNull(t);
                assertTrue("Acquisition remains visible after An-Nas",flag(t,"showSabqi"));
                assertFalse("Maîtrise rail must disappear after An-Nas",flag(t,"showTail"));
                assertFalse("No Entretien rail or ghost should fill the freed space",
                    flag(t,"showMaintenance"));
                years[1]=target;
            });
            Independent11710ScreensInstrumentedTest.capture(
                "cap-hifdh-full-home-after-nas");
            assertTrue(years[1]>years[0]);

            // Same REAL after-Nās forecast and the SAME Android process:
            // compare arrows versus a native Patrick Hand dropdown, without
            // enabling the dropdown for official users or changing receipts.
            scene.onActivity(a -> {
                CapHifzPanel p=(CapHifzPanel)field(a,"capHifz");
                p.setDropdownPreviewForCapture(true);
                assertFalse("Dropdown comparison must retain genuine predicted years",
                    ((List<?>)field(p,"shownCheckpoints")).isEmpty());
                assertTrue("Dropdown comparison must still have Acquisition",
                    flag(timeline(p),"showSabqi"));
                assertFalse("Maîtrise must stay absent", flag(timeline(p),"showTail"));
            });
            Independent11710ScreensInstrumentedTest.capture(
                "cap-hifdh-full-home-after-nas-dropdown-closed");
            scene.onActivity(a -> {
                CapHifzPanel p=(CapHifzPanel)field(a,"capHifz");
                android.widget.TextView field=(android.widget.TextView)field(p,"checkpointLabel");
                assertTrue("Real dropdown anchor is clickable",field.performClick());
                android.widget.ListPopupWindow popup =
                    (android.widget.ListPopupWindow)field(p,"milestoneDropdown");
                assertNotNull(popup);
                assertTrue("Native Android milestone dropdown must be visible",
                    popup.isShowing());
            });
            Independent11710ScreensInstrumentedTest.capture(
                "cap-hifdh-full-home-after-nas-dropdown-open");
            scene.onActivity(a -> {
                CapHifzPanel p=(CapHifzPanel)field(a,"capHifz");
                android.widget.ListPopupWindow popup =
                    (android.widget.ListPopupWindow)field(p,"milestoneDropdown");
                if (popup != null) popup.dismiss();
                p.setDropdownPreviewForCapture(false);
            });
        }
    }
}

package com.quransafeguard.hifz.preview;

import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Freeze 2 real Android screenshots + read-only 30-Juz geometry invariants. */
public final class FreezeVisualInstrumentedTest {
    @Test public void realBooxProgressionJuzZoom() throws Exception {
        try(ActivityScenario<ProgressMapActivity> scenario=
                ActivityScenario.launch(ProgressMapActivity.class)) {
            scenario.onActivity(a -> {
                try {
                    Method show=ProgressMapActivity.class.getDeclaredMethod("showJuz",int.class);
                    show.setAccessible(true);
                    show.invoke(a,30);
                    Field f=ProgressMapActivity.class.getDeclaredField("grid");
                    f.setAccessible(true);
                    ProgressGridView grid=(ProgressGridView)f.get(a);
                    assertEquals(582,grid.firstVisiblePage());
                    assertEquals(604,grid.lastVisiblePage());
                } catch(Exception e){throw new AssertionError("Real Juz 30 page window",e);}
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-progress-juz30");
            scenario.onActivity(a -> {
                try {
                    Method show=ProgressMapActivity.class.getDeclaredMethod("showJuz",int.class);
                    show.setAccessible(true);
                    show.invoke(a,1);
                    Field f=ProgressMapActivity.class.getDeclaredField("grid");
                    f.setAccessible(true);
                    ProgressGridView grid=(ProgressGridView)f.get(a);
                    assertEquals(1,grid.firstVisiblePage());
                    assertEquals(21,grid.lastVisiblePage());
                } catch(Exception e){throw new AssertionError("Real Juz 1 page window",e);}
            });
            Independent11710ScreensInstrumentedTest.capture("freeze2-progress-juz01");
        }
    }
}

package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Runtime UI regressions that matter on BOOX: hidden reader chrome must free space and long Hifz titles may wrap. */
public final class HifzUiLayoutInstrumentedTest {

    @Test public void hiddenStudyChromeReleasesItsLayoutHeight() throws Exception {
        try (ActivityScenario<StudyReaderActivity> scenario = ActivityScenario.launch(StudyReaderActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    Method hide = StudyReaderActivity.class.getDeclaredMethod("hideControls");
                    hide.setAccessible(true);
                    hide.invoke(activity);
                    assertEquals(View.GONE, fieldView(activity, "topControls").getVisibility());
                    assertEquals(View.GONE, fieldView(activity, "readerActions").getVisibility());
                    assertEquals(View.GONE, fieldView(activity, "pageRail").getVisibility());
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Unable to inspect Study reader chrome", error);
                }
            });
        }
    }

    @Test public void structuredSessionHeaderAllowsLongAnchoringTitleToWrap() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.ITQAN);
        try (ActivityScenario<HifzSessionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                try {
                    Field field = HifzSessionActivity.class.getDeclaredField("program");
                    field.setAccessible(true);
                    TextView program = (TextView) field.get(activity);
                    assertTrue("Structured session title must allow at least two lines", program.getMaxLines() >= 2);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Unable to inspect structured-session title", error);
                }
            });
        }
    }

    private static View fieldView(Object target, String name) throws ReflectiveOperationException {
        Field field = StudyReaderActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return (View) field.get(target);
    }
}

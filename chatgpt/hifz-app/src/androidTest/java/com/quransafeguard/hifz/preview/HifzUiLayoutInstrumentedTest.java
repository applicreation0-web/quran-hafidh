package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Runtime UI regressions that matter on BOOX: hidden chrome frees space and long Hifz titles fit. */
public final class HifzUiLayoutInstrumentedTest {

    @Test public void hiddenStudyChromeReleasesItsLayoutHeight() throws Exception {
        AtomicInteger before = new AtomicInteger();
        try (ActivityScenario<StudyReaderActivity> scenario = ActivityScenario.launch(StudyReaderActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    View mushaf = fieldView(activity, "mushaf");
                    before.set(mushaf.getHeight());
                    assertTrue("Mushaf must be laid out before the regression check", before.get() > 0);
                    Method hide = StudyReaderActivity.class.getDeclaredMethod("hideControls");
                    hide.setAccessible(true);
                    hide.invoke(activity);
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Unable to inspect Study reader chrome", error);
                }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                try {
                    assertEquals(View.GONE, fieldView(activity, "topControls").getVisibility());
                    assertEquals(View.GONE, fieldView(activity, "readerActions").getVisibility());
                    assertEquals(View.GONE, fieldView(activity, "pageRail").getVisibility());
                    int after = fieldView(activity, "mushaf").getHeight();
                    assertTrue("Hiding chrome must give the Mushaf more vertical layout height: before="
                        + before.get() + " after=" + after, after > before.get());
                } catch (ReflectiveOperationException error) {
                    throw new AssertionError("Unable to inspect Study reader chrome", error);
                }
            });
        }
    }

    @Test public void structuredSessionHeaderAllowsLongAnchoringTitleToWrapWithoutEllipsis() {
        Context context = ApplicationProvider.getApplicationContext();
        Intent intent = new Intent(context, HifzSessionActivity.class)
            .putExtra(HifzSessionActivity.EXTRA_MODE, HifzSessionActivity.ITQAN);
        String longRealTitle = "Ancrage fractionné · 55:1 → 56:16 · bloc 4/4 · 40 répétitions";
        try (ActivityScenario<HifzSessionActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                TextView program = program(activity);
                program.setText(longRealTitle);
                program.requestLayout();
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(activity -> {
                TextView program = program(activity);
                assertTrue("Structured session title must allow at least two lines", program.getMaxLines() >= 2);
                assertTrue("Long anchoring title must stay within its declared line budget",
                    program.getLineCount() <= program.getMaxLines());
                Layout layout = program.getLayout();
                assertNotNull("Program title must be laid out", layout);
                int lastLine = Math.max(0, layout.getLineCount() - 1);
                assertEquals("Long anchoring title must not be ellipsized", 0, layout.getEllipsisCount(lastLine));
            });
        }
    }

    private static TextView program(HifzSessionActivity activity) {
        try {
            Field field = HifzSessionActivity.class.getDeclaredField("program");
            field.setAccessible(true);
            return (TextView) field.get(activity);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Unable to inspect structured-session title", error);
        }
    }

    private static View fieldView(Object target, String name) throws ReflectiveOperationException {
        Field field = StudyReaderActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return (View) field.get(target);
    }
}

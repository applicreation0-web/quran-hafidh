package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Actual emulator views of both Itqan titles; no durable Hifz state is touched. */
public final class BooxPostNasVisualInstrumentedTest {
    private static Object field(Object owner, String name) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(owner);
        } catch (Exception e) {
            throw new AssertionError("Missing field " + name, e);
        }
    }
    private static void set(Object owner, String name, Object value) {
        try {
            Field f = owner.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(owner, value);
        } catch (Exception e) {
            throw new AssertionError("Cannot set " + name, e);
        }
    }
    private static void call(Object owner, String name) {
        try {
            Method m = owner.getClass().getDeclaredMethod(name);
            m.setAccessible(true);
            m.invoke(owner);
        } catch (Exception e) {
            throw new AssertionError("Cannot render " + name, e);
        }
    }

    /** Preview-only SharedPreferences overlay: reads are delegated, writes never happen. */
    private static HifzPrefs overlay(HifzPrefs original, boolean postNas) {
        SharedPreferences delegate = (SharedPreferences) field(original, "p");
        SharedPreferences preview = (SharedPreferences) Proxy.newProxyInstance(
            SharedPreferences.class.getClassLoader(), new Class[]{SharedPreferences.class},
            (proxy, method, args) -> {
                if ("getBoolean".equals(method.getName()) && args != null && args.length > 0 &&
                    (ItqanRegimeStore.INITIAL_TAIL_COMPLETED.equals(args[0]) ||
                     ItqanRegimeStore.LEGACY_INITIAL_TAIL_COMPLETED.equals(args[0]))) {
                    return postNas;
                }
                if ("edit".equals(method.getName())) {
                    throw new AssertionError("The BOOX visual fixture may not edit Hifz prefs");
                }
                try {
                    return method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            }
        );
        return new HifzPrefs(preview);
    }
    /** Full seven-day visual fixture; deliberately unsaved and uncredited. */
    private static void showFullWeek(MainActivity activity) {
        LocalDate today = HifzClock.today();
        List<WeeklyDashboardPlanner.Row> days = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = today.plusDays(day);
            days.add(new WeeklyDashboardPlanner.Row(date,
                WeeklyDashboardPlanner.day(date, today), "Matin", "Soir", WeeklyDashboardPlanner.State.DUE));
        }
        set(activity, "weekRows", days);
        set(activity, "selectedWeekDate", today);
        call(activity, "renderSelectedWeekDay");
        LinearLayout week = (LinearLayout) field(activity, "weekStrip");
        assertEquals("Seven-day timeline must exist before and after An-Nās", 1, week.getChildCount());
        android.widget.FrameLayout frame = (android.widget.FrameLayout) week.getChildAt(0);
        boolean sevenFound = false;
        for (int i = 0; i < frame.getChildCount(); i++)
            if (frame.getChildAt(i) instanceof LinearLayout
                    && ((LinearLayout)frame.getChildAt(i)).getChildCount() == 7)
                sevenFound = true;
        assertTrue("All seven stations must be available in the visual state", sevenFound);
    }

    private static void check(LinearLayout card, boolean postNas) {
        assertNotNull(card);
        assertTrue(card.getChildAt(0) instanceof TextView);
        assertTrue(card.getChildAt(1) instanceof TextView);
        TextView title = (TextView) card.getChildAt(0);
        TextView future = (TextView) card.getChildAt(1);
        assertEquals(postNas ? "Maintien" : "Stabiliser", title.getText().toString());
        assertEquals("(Maintien)", future.getText().toString());
        assertEquals(postNas ? View.GONE : View.VISIBLE, future.getVisibility());
        assertEquals("Identical typography before and after transition",
            title.getTypeface(), future.getTypeface());
        assertEquals("Identical title size",
            title.getTextSize(), future.getTextSize(), 0.01f);
        assertTrue("The original Itqan icon remains between title and hidden cue",
            card.getChildAt(2) instanceof android.widget.ImageView);
    }

    @Test public void sameRealBooxLayoutBeforeAndAfterNasWithoutFakeCredits() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            final HifzPrefs[] original = new HifzPrefs[1];
            scenario.onActivity(a -> {
                call(a, "showParcours");
                original[0] = (HifzPrefs) field(a, "prefs");
                set(a, "prefs", overlay(original[0], false));
                call(a, "refreshItqanCardVocabulary");
                call(a, "refreshStabilizationPosition");
                check((LinearLayout) field(a, "itqanCard"), false);
                showFullWeek(a);
            });
            Independent11710ScreensInstrumentedTest.capture("itqan-before-nas");
            scenario.onActivity(a -> {
                set(a, "prefs", overlay(original[0], true));
                call(a, "refreshItqanCardVocabulary");
                call(a, "refreshStabilizationPosition");
                check((LinearLayout) field(a, "itqanCard"), true);
                showFullWeek(a);
            });
            Independent11710ScreensInstrumentedTest.capture("itqan-after-nas");
            scenario.onActivity(a -> {
                set(a, "prefs", original[0]);
                call(a, "refreshItqanCardVocabulary");
            });
        }
    }
}

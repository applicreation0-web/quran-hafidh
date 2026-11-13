package com.quransafeguard.hifz.preview;

import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import org.junit.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Actual BOOX-size Android visual contract. Synthetic rows are only displayed
 * in memory; no SharedPreferences, receipts or progression are ever modified.
 */
public final class BooxTodaySelectedDayInstrumentedTest {
    private static Object field(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new AssertionError("Missing view field " + name, e);
        }
    }
    private static void set(Object target, String name, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new AssertionError("Unable to set visual-only field " + name, e);
        }
    }
    private static void call(MainActivity activity, String method) {
        try {
            Method m = MainActivity.class.getDeclaredMethod(method);
            m.setAccessible(true);
            m.invoke(activity);
        } catch (Exception e) {
            throw new AssertionError("BOOX visual action failed: " + method, e);
        }
    }
    private static LinearLayout station(MainActivity activity, int index) {
        LinearLayout rail = (LinearLayout) field(activity, "weekStrip");
        assertEquals(1, rail.getChildCount());
        FrameLayout timeline = (FrameLayout) rail.getChildAt(0);
        LinearLayout stations = null;
        for (int i = 0; i < timeline.getChildCount(); i++)
            if (timeline.getChildAt(i) instanceof LinearLayout)
                stations = (LinearLayout) timeline.getChildAt(i);
        assertNotNull("The seven real day touch targets must remain", stations);
        assertEquals(7, stations.getChildCount());
        return (LinearLayout) stations.getChildAt(index);
    }
    private static void check(LinearLayout station, boolean today, boolean selected, boolean validated) {
        FrameLayout marker = (FrameLayout) station.getChildAt(0);
        assertEquals("Today ring must be independent of the selection",
            today, marker.getBackground() != null);
        TextView node = (TextView) marker.getChildAt(0);
        assertEquals("No symbols inside any circle", "", node.getText().toString());
        GradientDrawable disk = (GradientDrawable) node.getBackground();
        assertNotNull(disk.getColor());
        assertEquals("Only validated dates receive solid ink fill",
            validated ? Ui.INK : Ui.PAPER, disk.getColor().getDefaultColor());
        TextView date = (TextView) station.getChildAt(2);
        assertEquals("Only the consulted date is underlined", selected,
            (date.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) != 0);
        String spoken = station.getContentDescription().toString();
        assertEquals("Today must be spoken separately", today, spoken.contains("aujourd’hui"));
        assertEquals("Selected status must be spoken separately", selected, spoken.contains("sélectionné"));
        assertTrue("The seven stations remain clickable", station.isClickable());
    }
    @Test public void todayRingPersistsWhenAnotherValidatedDayIsConsulted() {
        final LocalDate today = HifzClock.today();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                call(a, "showParcours");
                List<WeeklyDashboardPlanner.Row> rows = new ArrayList<>();
                for (int i = 0; i < 7; i++) {
                    LocalDate day = today.plusDays(i);
                    rows.add(new WeeklyDashboardPlanner.Row(day,
                        WeeklyDashboardPlanner.day(day, today), "Matin", "Soir",
                        i == 0 || i == 2 ? "Validé" : "À faire"));
                }
                set(a, "weekRows", rows);
                set(a, "selectedWeekDate", today);
                call(a, "renderSelectedWeekDay");
                check(station(a, 0), true, true, true);
                check(station(a, 2), false, false, true);
                check(station(a, 1), false, false, false);
            });
            Independent11710ScreensInstrumentedTest.capture("calendar-today-selected-and-validated");
            scenario.onActivity(a -> {
                set(a, "selectedWeekDate", today.plusDays(2));
                call(a, "renderSelectedWeekDay");
                check(station(a, 0), true, false, true);
                check(station(a, 2), false, true, true);
                check(station(a, 1), false, false, false);
            });
            Independent11710ScreensInstrumentedTest.capture("calendar-other-day-consulted-today-stays-ringed");
        }
    }
}

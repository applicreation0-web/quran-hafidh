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
        TextView weekday = (TextView) station.getChildAt(1);
        assertEquals("The real day marker is independent of the selection",
            today, (weekday.getPaintFlags() & Paint.UNDERLINE_TEXT_FLAG) != 0);
        assertEquals("The consulted day adds exactly two outer rings",
            selected ? 3 : 1, marker.getChildCount());
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
        assertEquals("Today must be spoken separately", today, spoken.contains(station.getContext().getString(R.string.calendar_today)));
        assertEquals("Selected status must be spoken separately", selected, spoken.contains(station.getContext().getString(R.string.calendar_selected)));
        assertTrue("The seven stations remain clickable", station.isClickable());
    }
    @Test public void todayRingPersistsWhenAFutureDayIsConsulted() {
        final LocalDate today = HifzClock.today();
        // Freeze the same in-memory fixture across both screenshots. The
        // legitimate async dashboard loader can refresh actual ledger rows
        // between ActivityScenario callbacks; it must not replace the fake
        // "today validated" case this isolated visual contract is asserting.
        final List<WeeklyDashboardPlanner.Row> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.plusDays(i);
            rows.add(new WeeklyDashboardPlanner.Row(day,
                "Matin", "Soir",
                i == 0 || i == 2 ? WeeklyDashboardPlanner.State.VALIDATED : WeeklyDashboardPlanner.State.DUE)); // Future cannot count as validated.
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> {
                call(a, "showParcours");
                set(a, "weekRows", rows);
                set(a, "selectedWeekDate", today);
                call(a, "renderSelectedWeekDay");
                check(station(a, 0), true, true, true);
                check(station(a, 2), false, false, false); // No fake future completion.
                check(station(a, 1), false, false, false);
            });
            Independent11710ScreensInstrumentedTest.capture("calendar-today-selected-and-validated");
            scenario.onActivity(a -> {
                // Assert the same explicit synthetic rows, not a ledger
                // refresh asynchronously installed since the first capture.
                set(a, "weekRows", rows);
                set(a, "selectedWeekDate", today.plusDays(2));
                call(a, "renderSelectedWeekDay");
                check(station(a, 0), true, false, true);
                check(station(a, 2), false, true, false); // Consulted, but still future.
                check(station(a, 1), false, false, false);
            });
            Independent11710ScreensInstrumentedTest.capture("calendar-other-day-consulted-today-stays-ringed");
        }
    }

    @Test public void localizedRailShowsSevenDistinctDaysAndSpansTheYearBoundary() {
        android.content.Context app = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        int savedLanguage = HaafidhActivity.languageIndex(app);
        String[] tags = {"fr","en","wo"};
        String[] abbreviations = {"Lun Mar Mer Jeu Ven Sam Dim","Mon Tue Wed Thu Fri Sat Sun","Alt Tal Àll Alx Àjj Gaa Dib"};
        LocalDate monday = LocalDate.of(2026,12,28);
        List<WeeklyDashboardPlanner.Row> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++) rows.add(new WeeklyDashboardPlanner.Row(monday.plusDays(i),
            "—", "—", WeeklyDashboardPlanner.State.FORECAST));
        try {
            for (int language=0; language<tags.length; language++) {
                assertTrue(HaafidhActivity.setLanguage(app,language));
                String tag = tags[language];
                String[] expected = abbreviations[language].split(" ");
                try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                    scenario.onActivity(activity -> {
                        assertEquals(tag,activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
                        call(activity,"showParcours");
                        set(activity,"weekRows",rows);
                        set(activity,"selectedWeekDate",monday.plusDays(6));
                        call(activity,"renderSelectedWeekDay");
                        for (int i = 0; i < 7; i++) {
                            LinearLayout cell=station(activity,i);
                            assertEquals(expected[i],((TextView)cell.getChildAt(1)).getText().toString());
                            assertEquals(Integer.toString(monday.plusDays(i).getDayOfMonth()),
                                ((TextView)cell.getChildAt(2)).getText().toString());
                            assertTrue(cell.getContentDescription().toString().startsWith(
                                CalendarText.dateWithWeekday(activity,monday.plusDays(i),true)));
                            assertTrue(cell.isClickable());
                        }
                    });
                    HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("calendar-"+tag+".png");
                }
            }
        } finally {
            assertTrue(HaafidhActivity.setLanguage(app,savedLanguage));
        }
    }
}

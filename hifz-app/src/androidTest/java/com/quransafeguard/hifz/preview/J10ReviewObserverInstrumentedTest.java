package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class J10ReviewObserverInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";

    private Context context;
    private HifzPrefs prefs;
    private GeometryRepository geometry;
    private J10ReviewPlanner planner;
    private J10ReviewObserver observer;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        detachApplicationObserver();
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        prefs = new HifzPrefs(context);
        geometry = GeometryRepository.get(context);
        planner = new J10ReviewPlanner(context);
        observer = new J10ReviewObserver(planner);
    }

    @After public void tearDown() {
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        resetApplicationObserverState();
        restoreApplicationObserver();
    }

    @Test public void validatedNewLessonAndEveningReviewCreditSameFiveLines() {
        LocalDate learned = LocalDate.of(2026, 9, 13);
        LocalDate reviewed = learned.plusDays(4);
        int start = geometry.firstLineIndex(prefs.sabqiStart());
        assertTrue(prefs.completeSabqiBlock(start, start + 4, start + 5, learned.toString(), "lesson"));
        observer.onPreferenceChanged("lastSabqiDate", learned);

        Map<String, LocalDate> afterLesson = new J10ReviewStore(context).snapshot();
        for (int i = start; i <= start + 4; i++) assertEquals(learned, afterLesson.get(geometry.line(i).id));

        assertTrue(prefs.completeSabqiTodayReview(reviewed.toString(), "review"));
        observer.onPreferenceChanged("lastSabqiTodayReviewDate", reviewed);
        Map<String, LocalDate> afterReview = new J10ReviewStore(context).snapshot();
        for (int i = start; i <= start + 4; i++) assertEquals(reviewed, afterReview.get(geometry.line(i).id));
    }

    @Test public void displayOnlyDoesNotResetJ10Date() {
        LocalDate learned = LocalDate.of(2026, 9, 13);
        int start = geometry.firstLineIndex(prefs.sabqiStart());
        assertTrue(prefs.completeSabqiBlock(start, start + 4, start + 5, learned.toString(), "lesson"));
        observer.onPreferenceChanged("lastSabqiDate", learned);
        planner.syncAcquired(learned.plusDays(6));

        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        for (int i = start; i <= start + 4; i++) assertEquals(learned, snapshot.get(geometry.line(i).id));
    }

    /**
     * Historical-date tests must not race the process-global SharedPreferences listener, whose
     * production contract intentionally reconciles with the device's real LocalDate.
     */
    private void detachApplicationObserver() {
        if (!(context instanceof QuranHifzApp)) return;
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener((QuranHifzApp) context);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        resetApplicationObserverState();
    }

    private void restoreApplicationObserver() {
        if (!(context instanceof QuranHifzApp)) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener((QuranHifzApp) context);
    }

    private void resetApplicationObserverState() {
        if (!(context instanceof QuranHifzApp)) return;
        try {
            setField(context, "observer", null);
            setField(context, "planner", null);
            setField(context, "pendingReconcile", false);
            setField(context, "openingPriority", false);
        } catch (ReflectiveOperationException error) {
            throw new AssertionError("Unable to isolate QuranHifzApp observer", error);
        }
    }

    private static void setField(Object target, String name, Object value)
            throws ReflectiveOperationException {
        Field field = QuranHifzApp.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}

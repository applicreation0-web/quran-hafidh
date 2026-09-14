package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Map;
import java.util.Arrays;

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

    @Test public void structuredFractionatedCreditIgnoresVisibleLabelAndMatchesDisplayedBlockLayout() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        VerseRef start = new VerseRef(55, 70);
        VerseRef end = new VerseRef(56, 16);
        String range = "[{\"start\":\"55:70\",\"end\":\"56:16\"}]";
        String queue = "[{\"start\":\"55:70\",\"end\":\"56:16\",\"origin\":\"PROMOTED\",\"protocol\":\"FULL\",\"failures\":0}]";
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit()
            .putString("promotedRanges", range)
            .putString("unconsolidatedPromotedRanges", range)
            .putString("anchoringQueue", queue)
            .putBoolean("anchoringQueueInitialized", true)
            .putInt("anchoringQueueIndex", 0)
            .putString("hardAnchoringSurahs", "[55]")
            .commit();

        int[] segments = geometry.surahSegmentLineCounts(start, end);
        assertTrue(Arrays.equals(new int[]{6, 7}, segments));
        assertTrue(prefs.setItqanProgress(40, 0, 0, start, end));
        assertTrue(prefs.advanceItqanBlock(1, start, end, date.toString(),
            "Texte UI arbitraire sans flèche, sans plage et sans mot Ancrage"));

        observer.onPreferenceChanged("lastItqanDate", date);

        assertEquals(start, prefs.lastItqanCreditStart());
        assertEquals(end, prefs.lastItqanCreditEnd());
        assertEquals(0, prefs.lastItqanCreditBlockIndex());
        java.util.List<String> unit = geometry.lineIdsForVerseRange(start, end);
        int expected = PreviewConfig.fractionatedBlockLength(segments, 0);
        Map<String, LocalDate> snapshot = planner.snapshot();
        for (int i = 0; i < unit.size(); i++) {
            if (i < expected) assertEquals(date, snapshot.get(unit.get(i)));
            else assertTrue(!date.equals(snapshot.get(unit.get(i))));
        }
    }

    @Test public void structuredMurajaahCreditIgnoresVisibleLabel() {
        LocalDate date = LocalDate.of(2026, 9, 14);
        VerseRef start = prefs.murajaahCursor();
        EligibleRangeForTest range = eligibleMurajaahRange(start);
        assertTrue(prefs.completeMurajaah(range.next, start, range.end, date.toString(),
            "Libellé Entretien libre sans plage"));

        observer.onPreferenceChanged("lastMurajaahDate", date);

        assertEquals(start, prefs.lastMurajaahCreditStart());
        assertEquals(range.end, prefs.lastMurajaahCreditEnd());
        java.util.List<String> ids = planner.traversalLineIds(start, range.end);
        Map<String, LocalDate> snapshot = planner.snapshot();
        for (String id : ids) assertEquals(date, snapshot.get(id));
    }

    @Test public void preferenceKeyFilterDistinguishesJ10DeltasFromUnrelatedSettings() {
        assertTrue(J10ReviewObserver.handlesPreferenceKey("recentSabqi"));
        assertTrue(J10ReviewObserver.handlesPreferenceKey("lastSabqiDate"));
        assertTrue(J10ReviewObserver.handlesPreferenceKey("lastSabqiTodayReviewDate"));
        assertTrue(J10ReviewObserver.handlesPreferenceKey("lastItqanDate"));
        assertTrue(J10ReviewObserver.handlesPreferenceKey("lastMurajaahDate"));
        assertTrue(!J10ReviewObserver.handlesPreferenceKey("forceEink"));
        assertTrue(!J10ReviewObserver.handlesPreferenceKey("lastItqanCreditStart"));
        assertTrue(J10ReviewObserver.requiresFullReconcileKey("itqanRanges"));
        assertTrue(J10ReviewObserver.requiresFullReconcileKey("promotedRanges"));
        assertTrue(!J10ReviewObserver.requiresFullReconcileKey("forceEink"));
    }

    private EligibleRangeForTest eligibleMurajaahRange(VerseRef start) {
        com.quransafeguard.hifz.core.EligibleCorpus corpus = prefs.murajaahCorpus();
        VerseRef end = corpus.next(start);
        return new EligibleRangeForTest(end, corpus.next(end));
    }

    private static final class EligibleRangeForTest {
        final VerseRef end;
        final VerseRef next;
        EligibleRangeForTest(VerseRef end, VerseRef next) { this.end = end; this.next = next; }
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

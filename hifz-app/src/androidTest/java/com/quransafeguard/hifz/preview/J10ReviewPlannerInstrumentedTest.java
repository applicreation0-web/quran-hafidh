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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10ReviewPlannerInstrumentedTest {
    private static final String HIFZ_PREFS = "quran_hifz_preview_v1";

    private Context context;
    private GeometryRepository geometry;
    private HifzPrefs prefs;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        detachApplicationObserver();
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        prefs = new HifzPrefs(context);
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        context.getSharedPreferences(HIFZ_PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        resetApplicationObserverState();
        restoreApplicationObserver();
    }

    @Test public void defaultSyncProtectsAcquiredBaseButNotPendingReconstruction() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        assertTrue(planner.syncAcquired(today));

        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        String acquired = geometry.line(geometry.firstLineIndex(new VerseRef(2, 1))).id;
        String pending = geometry.line(geometry.firstLineIndex(new VerseRef(49, 1))).id;
        assertEquals(J10ReviewPolicy.unknownHistoricalSeed(today), snapshot.get(acquired));
        assertEquals(today.minusDays(10), snapshot.get(acquired));
        assertFalse(snapshot.containsKey(pending));
    }

    @Test public void newlyValidatedLessonIsImmediatelyProtected() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        int start = geometry.firstLineIndex(new VerseRef(2, 75));
        assertTrue(prefs.completeSabqiBlock(start, start + 4, start + 5, today.toString(), "test"));

        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        assertTrue(planner.syncAcquired(today));
        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        for (int i = start; i <= start + 4; i++) assertEquals(today, snapshot.get(geometry.line(i).id));
    }

    @Test public void completedFractionatedSubBlockIsProtectedWithoutExposingUnfinishedRemainder() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        assertTrue(prefs.setHardAnchoringSurahs(Collections.singletonList(49)));
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        List<AnchoringQueue.Entry> queue = prefs.anchoringQueue();
        AnchoringQueue.Entry current = queue.get(prefs.anchoringQueueIndex(queue.size()));
        VerseRef start = GeometryRepository.parseVerse(current.start);
        VerseRef end = GeometryRepository.parseVerse(current.end);
        List<VerseRef> verses = geometry.versesForRange(start, end);
        List<String> lineIds = geometry.lineIdsForVerseRange(start, end);
        assertTrue(prefs.isFractionatedUnit(verses));
        int blockCount = PreviewConfig.fractionatedBlockCount(lineIds.size());
        assertTrue(blockCount > 1);
        int firstLength = PreviewConfig.fractionatedBlockLength(lineIds.size(), 0);

        String label = "Ancrage fractionné · bloc 1/" + blockCount + " validé · révélations 0";
        assertTrue(prefs.advanceItqanBlock(1, start, end, today.toString(), label));
        int queueIndexBefore = prefs.anchoringQueueIndex();
        VerseRef murajaahBefore = prefs.murajaahCursor();

        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        new J10ReviewObserver(planner).onPreferenceChanged("lastItqanDate", today);

        assertEquals(queueIndexBefore, prefs.anchoringQueueIndex());
        assertEquals(murajaahBefore, prefs.murajaahCursor());
        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        for (int i = 0; i < firstLength; i++) assertEquals(today, snapshot.get(lineIds.get(i)));
        assertFalse(snapshot.containsKey(lineIds.get(firstLength)));
    }

    @Test public void syncAloneSeedsCompletedSubBlockAsDueNowInsteadOfFreshlyReviewed() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        assertTrue(prefs.setHardAnchoringSurahs(Collections.singletonList(49)));
        assertTrue(prefs.reconcileAnchoringQueue(geometry));
        AnchoringQueue.Entry current = prefs.currentAnchoringEntry(geometry);
        VerseRef start = GeometryRepository.parseVerse(current.start);
        VerseRef end = GeometryRepository.parseVerse(current.end);
        List<String> lineIds = geometry.lineIdsForVerseRange(start, end);
        int firstLength = PreviewConfig.fractionatedBlockLength(lineIds.size(), 0);

        assertTrue(prefs.advanceItqanBlock(1, start, end, today.toString(),
            "Ancrage fractionné · bloc 1 validé"));
        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        assertTrue(planner.syncAcquired(today));

        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        for (int i = 0; i < firstLength; i++) {
            assertEquals(today.minusDays(10), snapshot.get(lineIds.get(i)));
        }
        assertFalse(snapshot.containsKey(lineIds.get(firstLength)));
    }

    /** Keep deterministic historical-date tests isolated from the process-global observer,
     * which correctly uses the device's real LocalDate in production. */
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

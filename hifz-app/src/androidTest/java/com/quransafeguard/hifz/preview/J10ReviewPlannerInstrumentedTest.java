package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10ReviewPlannerInstrumentedTest {
    private Context context;
    private GeometryRepository geometry;
    private HifzPrefs prefs;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("quran_hifz_preview_v1", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
        prefs = new HifzPrefs(context);
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        context.getSharedPreferences("quran_hifz_preview_v1", Context.MODE_PRIVATE).edit().clear().commit();
        context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void defaultSyncProtectsAcquiredBaseButNotPendingReconstruction() {
        LocalDate today = LocalDate.of(2026, 9, 13);
        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        assertTrue(planner.syncAcquired(today));

        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        String acquired = geometry.line(geometry.firstLineIndex(new VerseRef(2, 1))).id;
        String pending = geometry.line(geometry.firstLineIndex(new VerseRef(49, 1))).id;
        assertEquals(today, snapshot.get(acquired));
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

        assertTrue(prefs.advanceItqanBlock(1, today.toString(), "fraction 1"));
        int queueIndexBefore = prefs.anchoringQueueIndex();
        VerseRef murajaahBefore = prefs.murajaahCursor();

        J10ReviewPlanner planner = new J10ReviewPlanner(context);
        assertTrue(planner.syncAcquired(today));

        assertEquals(queueIndexBefore, prefs.anchoringQueueIndex());
        assertEquals(murajaahBefore, prefs.murajaahCursor());
        Map<String, LocalDate> snapshot = new J10ReviewStore(context).snapshot();
        for (int i = 0; i < firstLength; i++) assertEquals(today, snapshot.get(lineIds.get(i)));
        assertFalse(snapshot.containsKey(lineIds.get(firstLength)));
    }
}

package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Canonical schema-6 progression contract.
 *
 * UI chain: Apprentissage -> Appris -> Stabilisation -> Stabilise ->
 * Consolidation -> Acquis -> Revision.
 * Persisted state sets are exact/exclusive, never overlapping cumulative credits.
 */
public final class HifzV6StateTransitionsInstrumentedTest {
    private static final String MAIN = "quran_hifz_preview_v1";
    private static final String LEARNED = "v6LearnedLineIds";
    private static final String STABILIZED = "v6StabilizedLineIds";
    private static final String ACQUIRED = "v6AcquiredCreditLineIds";
    private static final String ACTIVE_J10 = "v6ActiveJ10LastReviewed";
    private static final String UNKNOWN_DUE = "v6UnknownDueLineIds";

    private Context context;
    private SharedPreferences main;
    private SharedPreferences legacyJ10;
    private GeometryRepository geometry;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        main = context.getSharedPreferences(MAIN, Context.MODE_PRIVATE);
        legacyJ10 = context.getSharedPreferences(J10ReviewStore.NAME, Context.MODE_PRIVATE);
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
        geometry = GeometryRepository.get(context);
    }

    @After public void tearDown() {
        main.edit().clear().commit();
        legacyJ10.edit().clear().commit();
    }

    @Test public void canonicalChainUsesExclusiveDurableStatesAndSurvivesReopen() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String line = geometry.line(0).id;

        assertEquals(HifzPrefs.ProgressState.NONE, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.LEARNING, prefs.nextActionV6(line));
        assertExclusive(line, false, false, false);

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        assertEquals(HifzPrefs.ProgressState.LEARNED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.STABILIZATION, prefs.nextActionV6(line));
        assertExclusive(line, true, false, false);

        prefs = new HifzPrefs(context);
        assertEquals(HifzPrefs.ProgressState.LEARNED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.STABILIZATION, prefs.nextActionV6(line));

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
        assertEquals(HifzPrefs.ProgressState.STABILIZED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.CONSOLIDATION, prefs.nextActionV6(line));
        assertExclusive(line, false, true, false);

        prefs = new HifzPrefs(context);
        assertEquals(HifzPrefs.ProgressState.STABILIZED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.CONSOLIDATION, prefs.nextActionV6(line));

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
        assertEquals(HifzPrefs.ProgressState.ACQUIRED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.REVIEW, prefs.nextActionV6(line));
        assertExclusive(line, false, false, true);

        prefs = new HifzPrefs(context);
        assertEquals(HifzPrefs.ProgressState.ACQUIRED, prefs.progressStateV6(line));
        assertEquals(HifzPrefs.ProgressAction.REVIEW, prefs.nextActionV6(line));
        assertExclusive(line, false, false, true);
    }

    @Test public void skippingAStageFailsClosedAndBatchFailureIsAtomic() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String first = geometry.line(0).id;
        String second = geometry.line(1).id;

        expectIllegalState(() -> prefs.transitionV6Lines(
            Arrays.asList(first), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED));
        expectIllegalState(() -> prefs.transitionV6Lines(
            Arrays.asList(first), HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED));
        assertExclusive(first, false, false, false);

        prefs.transitionV6Lines(Arrays.asList(first), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        Map<String, ?> beforeFailedBatch = snapshot(main);

        expectIllegalState(() -> prefs.transitionV6Lines(
            Arrays.asList(first, second), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED));

        assertEquals("failed mixed batch must not partially advance", beforeFailedBatch, snapshot(main));
        assertExclusive(first, true, false, false);
        assertExclusive(second, false, false, false);
    }

    @Test public void duplicateOrStaleCompletionNeverRegressesAnAdvancedLine() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String line = geometry.line(2).id;

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        assertExclusive(line, true, false, false);

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        assertExclusive(line, false, true, false);

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);
        assertExclusive(line, false, false, true);
        assertEquals(HifzPrefs.ProgressAction.REVIEW, prefs.nextActionV6(line));
    }

    @Test public void transitionsTouchOnlyCanonicalStateSetsNotJ10CursorsOrHistoricalCounters() throws Exception {
        HifzPrefs prefs = new HifzPrefs(context);
        String line = geometry.line(3).id;
        String sentinel = geometry.line(4).id;
        long exactJ10 = LocalDate.of(2026, 9, 9).toEpochDay();

        assertTrue(main.edit()
            .putString(ACTIVE_J10, "{\"" + sentinel + "\":" + exactJ10 + "}")
            .putString(UNKNOWN_DUE, new JSONArray(Arrays.asList(sentinel)).toString())
            .putInt("sabqiLineCursor", 4321)
            .putString("itqanCursor", "49:7")
            .putString("murajaahCursor", "2:44")
            .putInt("sabqiRep", 17)
            .putInt("sabqiAssisted", 2)
            .putInt("itqanRep", 9)
            .putInt("itqanAssisted", 1)
            .putInt("itqanFinalReveals", 2)
            .commit());
        assertTrue(legacyJ10.edit().putLong("line:" + sentinel, exactJ10).commit());

        Map<String, ?> legacyBefore = snapshot(legacyJ10);
        Map<String, ?> before = snapshot(main);

        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.LEARNING_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.STABILIZATION_COMPLETED);
        prefs.transitionV6Lines(Arrays.asList(line), HifzPrefs.ProgressEvent.CONSOLIDATION_COMPLETED);

        Map<String, ?> after = snapshot(main);
        assertAllKeysUnchangedExcept(before, after, LEARNED, STABILIZED, ACQUIRED);
        assertEquals(legacyBefore, snapshot(legacyJ10));
        assertExclusive(line, false, false, true);
    }

    private void assertExclusive(String line, boolean learned, boolean stabilized, boolean acquired) throws Exception {
        assertEquals(learned, stringSet(LEARNED).contains(line));
        assertEquals(stabilized, stringSet(STABILIZED).contains(line));
        assertEquals(acquired, stringSet(ACQUIRED).contains(line));
        int memberships = (learned ? 1 : 0) + (stabilized ? 1 : 0) + (acquired ? 1 : 0);
        assertTrue("line must have at most one canonical persisted state", memberships <= 1);
    }

    private java.util.LinkedHashSet<String> stringSet(String key) throws Exception {
        JSONArray array = new JSONArray(main.getString(key, "[]"));
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }

    private static Map<String, ?> snapshot(SharedPreferences prefs) {
        return new LinkedHashMap<>(prefs.getAll());
    }

    private static void assertAllKeysUnchangedExcept(
            Map<String, ?> before, Map<String, ?> after, String... allowedChangedKeys) {
        java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>();
        java.util.Collections.addAll(allowed, allowedChangedKeys);
        java.util.LinkedHashSet<String> all = new java.util.LinkedHashSet<>();
        all.addAll(before.keySet());
        all.addAll(after.keySet());
        for (String key : all) {
            if (allowed.contains(key)) continue;
            assertTrue("unexpected key removal: " + key, before.containsKey(key));
            assertTrue("unexpected key creation: " + key, after.containsKey(key));
            assertEquals("unexpected mutation outside canonical state sets: " + key, before.get(key), after.get(key));
        }
    }

    private static void expectIllegalState(ThrowingRunnable body) throws Exception {
        try {
            body.run();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // Expected fail-closed behavior.
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}

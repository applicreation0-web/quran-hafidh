package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10ReviewStoreInstrumentedTest {
    private static final String NAME = "quran_hifz_j10_v1";
    private Context context;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After public void tearDown() {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void syncSeedsMissingLinesPersistsAndRemovesStaleLines() {
        LocalDate seed = LocalDate.of(2026, 9, 13);
        J10ReviewStore store = new J10ReviewStore(context);
        assertTrue(store.syncAcquired(Arrays.asList("l10", "l11", "l12"), seed));

        Map<String, LocalDate> first = store.snapshot();
        assertEquals(3, first.size());
        assertEquals(seed, first.get("l10"));
        assertEquals(seed, first.get("l11"));

        J10ReviewStore reopened = new J10ReviewStore(context);
        assertEquals(first, reopened.snapshot());

        assertTrue(reopened.syncAcquired(Arrays.asList("l11", "l12", "l13"), seed.plusDays(1)));
        Map<String, LocalDate> second = reopened.snapshot();
        assertFalse(second.containsKey("l10"));
        assertEquals(seed, second.get("l11"));
        assertEquals(seed, second.get("l12"));
        assertEquals(seed.plusDays(1), second.get("l13"));
    }

    @Test public void markReviewedUpdatesOnlyAlreadyAcquiredLines() {
        LocalDate seed = LocalDate.of(2026, 9, 13);
        LocalDate reviewed = seed.plusDays(4);
        J10ReviewStore store = new J10ReviewStore(context);
        assertTrue(store.syncAcquired(Arrays.asList("l20", "l21"), seed));

        assertTrue(store.markReviewed(Arrays.asList("l20", "not-acquired"), reviewed));
        Map<String, LocalDate> snapshot = store.snapshot();
        assertEquals(reviewed, snapshot.get("l20"));
        assertEquals(seed, snapshot.get("l21"));
        assertFalse(snapshot.containsKey("not-acquired"));
    }

    @Test public void emptySyncClearsTrackedAcquiredSet() {
        J10ReviewStore store = new J10ReviewStore(context);
        assertTrue(store.syncAcquired(Collections.singletonList("l30"), LocalDate.of(2026, 9, 13)));
        assertTrue(store.syncAcquired(Collections.emptyList(), LocalDate.of(2026, 9, 14)));
        assertTrue(store.snapshot().isEmpty());
    }
}

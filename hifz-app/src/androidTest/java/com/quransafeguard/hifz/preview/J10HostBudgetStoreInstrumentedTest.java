package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class J10HostBudgetStoreInstrumentedTest {
    private static final String STORE = "quran_hifz_j10_host_v1";
    private Context context;

    @Before public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @After public void tearDown() {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void anchoringCanHostJ10WithoutCreditingNormalAnchoringCompletion() {
        J10HostBudgetStore store = new J10HostBudgetStore(context);
        LocalDate day = LocalDate.of(2026, 9, 15);

        assertTrue(store.addConsumed(HifzSessionActivity.ITQAN, day, 90_000L));
        assertEquals(90_000L, store.consumedMs(HifzSessionActivity.ITQAN, day));
        assertTrue(store.markSlotConsumed(HifzSessionActivity.ITQAN, day));
        assertTrue(store.isSlotConsumed(HifzSessionActivity.ITQAN, day));
        assertFalse(store.isSlotConsumed(HifzSessionActivity.ITQAN, day.plusDays(1)));
    }
}

package com.quransafeguard.hifz.storage;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ReaderStateStoreInstrumentedTest {
    @Test
    public void pageRoundTripIsIndependentAndBounded() {
        ReaderStateStore store = new ReaderStateStore(ApplicationProvider.getApplicationContext());
        store.clear();
        try {
            assertEquals(1, store.loadPage());
            store.savePage(604);
            assertEquals(604, store.loadPage());
        } finally {
            store.clear();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfRangePage() {
        ReaderStateStore store = new ReaderStateStore(ApplicationProvider.getApplicationContext());
        store.savePage(605);
    }
}

package com.quransafeguard.hifz.storage;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.HifzState;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** Locks the persisted VerseRef format to the domain contract (surah:ayah). */
public final class HifzStateStoreInstrumentedTest {
    @Test
    public void stateRoundTripPreservesAllCursors() {
        HifzStateStore store = new HifzStateStore(ApplicationProvider.getApplicationContext());
        store.clear();
        try {
            HifzState expected = new HifzState(
                new VerseRef(2, 1),
                new VerseRef(2, 5),
                new VerseRef(114, 1),
                new VerseRef(2, 3),
                new VerseRef(114, 2)
            );

            store.save(expected);
            HifzState actual = store.loadOrNull();

            assertNotNull(actual);
            assertEquals(expected, actual);
        } finally {
            store.clear();
        }
    }
}

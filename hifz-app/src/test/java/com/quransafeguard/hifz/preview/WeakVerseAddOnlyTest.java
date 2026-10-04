package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/** Quiz bilan (option 6C): adding "À revoir" verses to Repères faibles is add-only and idempotent. */
public final class WeakVerseAddOnlyTest {
    @Test public void addsOnlyMissingVersesAndNeverUnflags() {
        HifzPrefs prefs = new HifzPrefs(new InMemoryPrefs());
        VerseRef a = new VerseRef(2, 5), b = new VerseRef(2, 6);
        prefs.toggleMurajaahWeakVerse(a);
        assertEquals(1, prefs.addMurajaahWeakVerses(Arrays.asList(a, b)));
        assertEquals(Arrays.asList(a, b), prefs.murajaahWeakVerses());
        assertEquals(0, prefs.addMurajaahWeakVerses(Arrays.asList(a, b)));
        assertEquals(Arrays.asList(a, b), prefs.murajaahWeakVerses());
    }
}

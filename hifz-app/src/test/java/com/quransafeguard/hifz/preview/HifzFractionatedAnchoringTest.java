package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class HifzFractionatedAnchoringTest {
    @Test public void anyMarkedSurahMakesAMixedUnitFractionated() {
        assertTrue(HifzPrefs.containsHardAnchoringSurah(
            Collections.singletonList(55),
            Arrays.asList(new VerseRef(55, 70), new VerseRef(56, 1), new VerseRef(56, 16))));
        assertFalse(HifzPrefs.containsHardAnchoringSurah(
            Collections.singletonList(54),
            Arrays.asList(new VerseRef(55, 70), new VerseRef(56, 1), new VerseRef(56, 16))));
        assertFalse(HifzPrefs.containsHardAnchoringSurah(
            Collections.emptyList(), Collections.singletonList(new VerseRef(55, 70))));
    }
}

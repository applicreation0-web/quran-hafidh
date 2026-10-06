package com.quransafeguard.hifz.preview;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Device report: "objectif 81 → 1 البقرة" — Arabic names must be bidi-isolated in French labels. */
public final class QuranSurahNamesBidiTest {
    @Test public void namesAreIsolatedSoFollowingNumbersKeepTheirOrder() {
        String name = QuranSurahNames.name(2);
        assertTrue(name.startsWith("⁨") && name.endsWith("⁩"));
        assertEquals("البقرة", QuranSurahNames.rawName(2));
        assertEquals("⁨البقرة⁩", name);
    }
}

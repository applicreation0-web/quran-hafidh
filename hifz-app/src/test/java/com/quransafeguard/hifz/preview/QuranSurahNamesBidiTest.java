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

    /** User decision "tout en arabe": every range reads like the Mushaf, arrow toward An-Nās. */
    @Test public void rangesReadRightToLeftLikeTheMushaf() {
        com.quransafeguard.hifz.core.VerseRef a = new com.quransafeguard.hifz.core.VerseRef(2, 1);
        assertEquals("\u2067⁨البقرة⁩ 1 ← 88\u2069",
            QuranSurahNames.range(a, new com.quransafeguard.hifz.core.VerseRef(2, 88)));
        assertEquals("\u2067\u2066⁨الحجرات⁩ 1\u2069 ← \u2066⁨الذاريات⁩ 37\u2069\u2069",
            QuranSurahNames.range(new com.quransafeguard.hifz.core.VerseRef(49, 1), new com.quransafeguard.hifz.core.VerseRef(51, 37)));
        assertEquals("a one-verse range is just the verse", QuranSurahNames.verse(a), QuranSurahNames.range(a, a));
    }
}

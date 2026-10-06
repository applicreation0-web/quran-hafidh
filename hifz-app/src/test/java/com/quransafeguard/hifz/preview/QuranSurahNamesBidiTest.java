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

    /**
     * User decision "tout en arabe": every range reads like the Mushaf, start on the right, arrow
     * toward An-Nās. Written in visual order inside a left-to-right isolate (device report: a
     * right-to-left isolate was not honoured in a wrapped Parcours cell).
     */
    @Test public void rangesAreWrittenInMushafVisualOrder() {
        com.quransafeguard.hifz.core.VerseRef a = new com.quransafeguard.hifz.core.VerseRef(2, 1);
        assertEquals("\u206688\u00A0←\u00A01\u00A0⁨البقرة⁩\u2069",
            QuranSurahNames.range(a, new com.quransafeguard.hifz.core.VerseRef(2, 88)));
        assertEquals("\u2066⁨الذاريات⁩\u00A037\u00A0←\u00A0⁨الحجرات⁩\u00A01\u2069",
            QuranSurahNames.range(new com.quransafeguard.hifz.core.VerseRef(49, 1), new com.quransafeguard.hifz.core.VerseRef(51, 37)));
        assertEquals("a one-verse range is just the verse", QuranSurahNames.verse(a), QuranSurahNames.range(a, a));
        assertTrue("no right-to-left isolate the device could drop", !QuranSurahNames.range(a, new com.quransafeguard.hifz.core.VerseRef(3, 5)).contains("\u2067"));
    }
}

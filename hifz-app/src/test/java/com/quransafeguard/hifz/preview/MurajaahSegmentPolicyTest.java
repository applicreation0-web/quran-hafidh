package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Device report: Entretien split into tiny passages (2:1→81 · 83→84 · 86→88) for no visible reason. */
public final class MurajaahSegmentPolicyTest {
    @Test public void contiguousAndSmallSameSurahGapsStayOnePassage() {
        assertTrue(MurajaahSegmentPolicy.continues(new VerseRef(2, 80), new VerseRef(2, 81)));
        assertTrue(MurajaahSegmentPolicy.continues(new VerseRef(2, 81), new VerseRef(2, 83)));
        assertTrue(MurajaahSegmentPolicy.continues(new VerseRef(2, 84), new VerseRef(2, 86)));
        assertTrue(MurajaahSegmentPolicy.continues(new VerseRef(2, 81), new VerseRef(2, 85)));
    }

    @Test public void realJumpsStaySeparatePassages() {
        assertFalse("more than three missing verses", MurajaahSegmentPolicy.continues(new VerseRef(2, 81), new VerseRef(2, 86)));
        assertFalse("another surah", MurajaahSegmentPolicy.continues(new VerseRef(2, 286), new VerseRef(49, 1)));
        assertFalse("wrap-around", MurajaahSegmentPolicy.continues(new VerseRef(51, 37), new VerseRef(2, 1)));
    }
}

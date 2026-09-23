package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * WritingExerciseActivity.verseLabel is a static, Android-free string builder (same pattern as
 * MushafView's timeoutAfterObservedRender/updateObservedRender) so it can be exercised directly
 * here — this project has no Robolectric, so the Activity itself can't be instantiated.
 *
 * Confirmed on a real device: without the directional isolates this wraps the surah name and ayah
 * range in, Android's bidi algorithm sweeps the plain Latin ayah range into the same reordered
 * island as the adjacent Arabic word, rendering "5 → 6" as "6 → 5" (and moving the surah name to
 * the end) — a plain setTextDirection(LTR) does not stop this on its own.
 */
public final class WritingExerciseLabelTest {
    private static final String LRI = "⁦";
    private static final String RLI = "⁧";
    private static final String PDI = "⁩";

    @Test public void singleVerseLineShowsJustThatAyah() {
        String label = WritingExerciseActivity.verseLabel(1, Collections.singletonList(new VerseRef(1, 1)));
        assertEquals("Page 1 — " + RLI + "الفاتحة" + PDI + " " + LRI + "1" + PDI, label);
    }

    @Test public void multiVerseLineIsolatesTheAyahRangeSeparatelyFromTheSurahName() {
        String label = WritingExerciseActivity.verseLabel(1, Arrays.asList(new VerseRef(1, 5), new VerseRef(1, 6)));
        assertEquals("Page 1 — " + RLI + "الفاتحة" + PDI + " " + LRI + "5 → 6" + PDI, label);
    }

    @Test public void isolatesKeepTheAyahRangeInLogicalLeftToRightOrder() {
        String label = WritingExerciseActivity.verseLabel(1, Arrays.asList(new VerseRef(1, 5), new VerseRef(1, 6)));
        int rangeStart = label.indexOf(LRI);
        int rangeEnd = label.indexOf(PDI, rangeStart);
        String isolatedRange = label.substring(rangeStart + LRI.length(), rangeEnd);
        assertEquals("5 → 6", isolatedRange);
    }

    @Test public void emptyVersesFallsBackToJustThePage() {
        assertEquals("Page 42", WritingExerciseActivity.verseLabel(42, Collections.emptyList()));
    }

    @Test public void alwaysWrapsTheSurahNameInARightToLeftIsolate() {
        String label = WritingExerciseActivity.verseLabel(1, Collections.singletonList(new VerseRef(1, 1)));
        assertTrue(label.contains(RLI + "الفاتحة" + PDI));
    }
}

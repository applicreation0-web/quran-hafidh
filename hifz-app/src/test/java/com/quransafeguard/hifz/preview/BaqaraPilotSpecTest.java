package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** New Baqarah map is strictly read-only; this test verifies both continuous navigation scales. */
public final class BaqaraPilotSpecTest {
    @Test public void mawdudiMacroStagesCoverWholeBaqarahInOrder() {
        int expected = 1;
        for (BaqaraPilotSpec.Theme t : BaqaraPilotSpec.themes()) {
            assertEquals(expected, t.first);
            assertTrue(t.last >= t.first);
            assertNotNull(t.displayTitle);
            assertFalse(t.displayTitle.trim().isEmpty());
            expected = t.last + 1;
        }
        assertEquals(287, expected);
        assertEquals(10, BaqaraPilotSpec.themes().size());
        for (int ayah = 1; ayah <= 286; ayah++)
            assertTrue(BaqaraPilotSpec.themes().get(
                BaqaraPilotSpec.themeIndexForAyah(ayah)).contains(ayah));
    }

    @Test public void originalEnglishCommentarySectionsAreContinuous() {
        int expected = 1;
        assertEquals(40, BaqaraPilotSpec.sectionCount());
        for (int i = 0; i < BaqaraPilotSpec.sectionCount(); i++) {
            int[] range = BaqaraPilotSpec.sectionRange(i);
            assertEquals(expected, range[0]);
            assertTrue(range[1] >= range[0]);
            expected = range[1] + 1;
        }
        assertEquals(287, expected);
        for (int ayah = 1; ayah <= 286; ayah++) {
            int i = BaqaraPilotSpec.sectionIndexForAyah(ayah);
            assertTrue(i >= 0 && i < 40);
            int[] span = BaqaraPilotSpec.sectionRange(i);
            assertTrue(span[0] <= ayah && ayah <= span[1]);
        }
    }

    @Test public void knownDifferentBordersRemainIndependent() {
        assertEquals(0, BaqaraPilotSpec.themeIndexForAyah(20));
        assertEquals(1, BaqaraPilotSpec.themeIndexForAyah(21));
        assertEquals(6, BaqaraPilotSpec.themeIndexForAyah(153));
        assertEquals(9, BaqaraPilotSpec.themeIndexForAyah(286));
        assertEquals(13, BaqaraPilotSpec.sectionIndexForAyah(120));
        assertEquals(13, BaqaraPilotSpec.sectionIndexForAyah(121));
        assertEquals(14, BaqaraPilotSpec.sectionIndexForAyah(122));
    }
}

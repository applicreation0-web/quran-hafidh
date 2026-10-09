package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

public class IbnKathirGroupIndexTest {
    @Test public void everyVerseHasExactlyOneDocumentaryGroup() {
        IbnKathirGroupIndex idx = IbnKathirGroupIndex.shared();
        assertEquals(1903, idx.count());
        int total = 0;
        for (int surah = 1; surah <= 114; surah++) {
            int expectedStart = 1;
            for (IbnKathirGroupIndex.Group g : idx.groupsForSurah(surah)) {
                assertEquals(surah, g.surah);
                assertEquals(expectedStart, g.startAyah);
                assertEquals(g, idx.containing(surah, expectedStart));
                assertEquals(g, idx.containing(surah, g.endAyah));
                total += g.endAyah - expectedStart + 1;
                expectedStart = g.endAyah + 1;
            }
            assertNull(idx.containing(surah, expectedStart));
        }
        assertEquals(6236, total);
    }

    @Test public void work05QafPrintedSampleRetainsExactBoundaries() {
        IbnKathirGroupIndex idx = IbnKathirGroupIndex.shared();
        int[][] qaf = {{1,5},{6,11},{12,15},{16,22},{23,29},{30,35},{36,40},{41,45}};
        assertEquals(qaf.length, idx.groupsForSurah(50).size());
        for(int i=0;i<qaf.length;i++) {
            IbnKathirGroupIndex.Group g=idx.groupsForSurah(50).get(i);
            assertEquals(qaf[i][0],g.startAyah);
            assertEquals(qaf[i][1],g.endAyah);
        }
        assertEquals("IKEN050_030_035",idx.containing(50,30).id);
    }

    @Test public void noInjectedEditorialTitleOrCommentary() {
        String decoded=IbnKathirGroupIndex.decodeEndpoints();
        assertTrue(decoded.startsWith("1:"));
        assertEquals(114,decoded.trim().split("\\n").length);
        assertFalse(decoded.contains("Allah"));
        assertFalse(decoded.toLowerCase(java.util.Locale.ROOT).contains("munir"));
    }

    @Test(expected=IllegalStateException.class)
    public void brokenBoundariesFailClosed() {
        IbnKathirGroupIndex.parseForTest("50:5,11");
    }
}

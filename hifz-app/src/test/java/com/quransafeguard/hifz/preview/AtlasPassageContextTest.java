package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Canonical passages are never split into new Ayah Themes IDs for Atlas display. */
public final class AtlasPassageContextTest {
    @Test public void fullPassageKeepsUnmodifiedOriginalTitle() {
        assertEquals("Canonical source heading",
                AtlasPassageContext.displayTitle(4,101,104,101,104,"Canonical source heading"));
    }

    @Test public void partialPassageDoesNotRepeatAnUnrelatedOriginalTitle() {
        String label=AtlasPassageContext.displayTitle(4,101,104,104,104,"Prayer in danger");
        assertEquals("4:104 · partie du passage 4:101–104",label);
        assertFalse(label.contains("Prayer"));
    }

    @Test public void oneCanonicalPassageCanSpanThreeUnits() {
        assertEquals("51:1–14 · partie du passage 51:1–23",
            AtlasPassageContext.displayTitle(51,1,23,1,14,"Full source"));
        assertEquals("51:15–19 · partie du passage 51:1–23",
            AtlasPassageContext.displayTitle(51,1,23,15,19,"Full source"));
        assertEquals("51:20–23 · partie du passage 51:1–23",
            AtlasPassageContext.displayTitle(51,1,23,20,23,"Full source"));
    }

    @Test public void lutAndAlAykahCannotBorrowAnEntireHeading() {
        assertEquals("15:78–79 · partie du passage 15:61–79",
            AtlasPassageContext.displayTitle(15,61,79,78,79,"Lūṭ"));
    }

    @Test public void noOverlapFailsClosed() {
        try {
            AtlasPassageContext.displayTitle(4,101,104,105,125,"Wrong");
            fail("Unrelated passage accepted");
        } catch(IllegalArgumentException expected) {}
    }
}

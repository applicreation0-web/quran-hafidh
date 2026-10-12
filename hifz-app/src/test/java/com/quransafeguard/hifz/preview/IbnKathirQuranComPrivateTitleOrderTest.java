package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Verbatim source title order is preserved; only initial display offset varies. */
public final class IbnKathirQuranComPrivateTitleOrderTest {
    @Test public void genericPrefacesAreSkippedWithoutChangingSourceOrder() {
        String[] headings = {
            "Which was revealed in Placeholder",
            "The Beginning of the Placeholder Section",
            "The Virtues of Placeholder",
            "Synthetic thematic heading"
        };
        int initial=IbnKathirQuranComPrivateTitles.firstThematicIndex(headings);
        assertEquals(3,initial);
        assertEquals("Synthetic thematic heading",headings[initial]);
        assertEquals("Which was revealed in Placeholder",headings[(initial+1)%headings.length]);
    }

    @Test public void reviewedGroupsUseOnlyTheirSourceSafeOffsets() {
        String[] headings = {"synthetic-0", "synthetic-1", "synthetic-2"};
        assertEquals(2,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN011_001_004", headings));
        assertEquals(2,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN018_001_005", headings));
        assertEquals(1,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN020_001_008", headings));
        assertEquals(1,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN079_001_014", headings));
    }

    @Test public void reviewedOffsetFailsClosedWhenSourceArrayIsShorter() {
        assertEquals(0,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN020_001_008", new String[]{"synthetic-only"}));
        assertEquals(0,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN011_001_004", null));
    }

    @Test public void unreviewedGroupKeepsGenericFallback() {
        String[] headings = {"Which was revealed in Placeholder", "Synthetic theme"};
        assertEquals(1,IbnKathirQuranComPrivateTitles.preferredFirstTitleIndex(
                "IKEN999_999_999", headings));
    }

    @Test public void oneSyntheticIntroductionRemainsAvailable() {
        String[] headings = {"Which was revealed in Placeholder"};
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(headings));
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(new String[0]));
        assertEquals(0,IbnKathirQuranComPrivateTitles.firstThematicIndex(null));
    }
}

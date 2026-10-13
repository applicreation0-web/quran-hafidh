package com.quransafeguard.hifz.preview;

import org.junit.Test;
import org.json.JSONArray;
import static org.junit.Assert.*;

/** M9: no multi-heading carousel, fabricated fallback or unreviewed title. */
public final class IbnKathirQuranComPrivateTitleOrderTest {
    @Test public void exactlyOneApprovedHeadingIsAcceptedVerbatim(){
        String heading="Approved synthetic heading";
        assertEquals(heading,IbnKathirQuranComPrivateTitles.validateApprovedTitle(heading));
    }
    @Test public void legacyArraysCanNeverCycle(){
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle(
            new JSONArray().put("old heading").put("another heading")));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle(
            new JSONArray().put("even one legacy item")));
    }
    @Test public void noFallbackOrSilentlyTrimmedTitle(){
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle(null));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle(""));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle(" title"));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle("title "));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle("title\\nline".replace("\\n","\n")));
        assertEquals(null,IbnKathirQuranComPrivateTitles.validateApprovedTitle("X".repeat(146)));
    }
}

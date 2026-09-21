package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly from a physical-device test: tapping any verse in Révision active (to record
 * "Fin réelle" or to jump between segments) revealed the whole rest of the page, leaving only the
 * tapped verse's own line still maskable. Root cause: mushaf.setSelection(selection, lines) feeds
 * its second argument straight into reader.js's shared `lineIds`, the very variable render()'s
 * masking-candidate filter also consults — so highlighting a single verse's line inadvertently
 * collapsed the whole page's masking scope down to that one line. The three call sites must pass
 * currentLineIds (the full, already-correct, mode-appropriate masking scope kept in sync by
 * applyActiveLandmarks/onPageShown/goPage) instead of a verse-narrowed lookup, so that highlighting
 * a verse for progress tracking never touches what the rest of the page is allowed to mask.
 */
public final class RevisionActiveMaskScopeSurvivesVerseTapSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length();
        }
    }

    @Test public void everyMurajaahSetSelectionCallPreservesTheFullMaskingScope() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");

        assertTrue("the unvalidated segment-end highlight must keep the page's own masking scope",
            session.contains("mushaf.setSelection(Collections.singletonList(current.end), currentLineIds);"));
        assertTrue("restoring the saved endpoint highlight on page entry must keep the page's own masking scope",
            session.contains("mushaf.setSelection(Collections.singletonList(murajaahActualEnd), currentLineIds);"));
        assertTrue("tapping a verse to record progress must keep the page's own masking scope",
            session.contains("mushaf.setSelection(Collections.singletonList(verse),currentLineIds);"));

        assertFalse("no mushaf.setSelection call may narrow the masking scope to just the highlighted "
                + "verse's own line (that collapses the rest of the page's masking to nothing in "
                + "Révision active, since render() reuses this same lineIds for its mask candidates)",
            session.contains("geometry.lineIdsForVerseRange(current.end, current.end)")
                || session.contains("geometry.lineIdsForVerseRange(murajaahActualEnd, murajaahActualEnd)")
                || session.contains("geometry.lineIdsForVerseRange(verse,verse)")
                || session.contains("geometry.lineIdsForVerseRange(verse, verse)"));
        assertTrue("exactly the three known call sites must use setSelection this way",
            countOccurrences(session, "mushaf.setSelection(") == 3);
    }
}

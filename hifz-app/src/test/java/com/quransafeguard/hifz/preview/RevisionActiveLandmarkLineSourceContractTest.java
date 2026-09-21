package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: a fully masked page in Révision active gives a learner no way to confirm
 * they're reciting from the right point after a page swipe, unless they have every page's exact
 * start/end verse memorized. The page's first physical line is now excluded from the maskable set
 * entirely, so it always stays visible as a synchronization landmark — every other line still
 * masks normally. This is a source-contract check because HifzSessionActivity/GeometryRepository
 * require an Android Context this JVM test suite cannot construct.
 */
public final class RevisionActiveLandmarkLineSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
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

    @Test public void firstLineOfEveryActivePageStaysUnmasked() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String helper = method(session,
            "private List<String> maskableLineIdsForActivePage(int page) {", "private void updateMurajaahActions()");
        assertTrue("must read the full page's lines first", helper.contains("geometry.lineIdsOnPage(page)"));
        assertTrue("must drop exactly the first line, keeping the rest maskable",
            helper.contains("all.subList(1, all.size())"));
        assertTrue("a single-line page must fall back to leaving it fully visible",
            helper.contains("Collections.emptyList()"));
        assertTrue("every page-entry/page-swipe path in active mode must route through the landmark helper, not the raw page lookup",
            countOccurrences(session, "maskableLineIdsForActivePage(") >= 5);
        assertTrue("no active-mode call site may bypass the helper with the raw page lookup",
            !session.contains("currentLineIds=geometry.lineIdsOnPage(")
                && !session.contains("currentLineIds = geometry.lineIdsOnPage("));
    }
}

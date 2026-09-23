package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported directly: a fully masked page in Révision active gives a learner no way to confirm
 * they're reciting from the right point after a page swipe, or where to stop before turning the
 * page, unless they have every page's exact start/end verse memorized. Half of the page's first
 * physical line (its reading-first words) and half of its last physical line (its reading-last
 * words) now stay permanently visible as synchronization landmarks; the other half of each of
 * those two lines still masks normally. Cells are stored in ascending x order (left to right)
 * while Arabic reads right to left, so "reading-first" means the cell array's *tail* and
 * "reading-last" means its *head* — landmarkCellIndices in reader.js must get this the right way
 * round, or the visible half would be the wrong end of the line.
 *
 * These are source-contract checks because HifzSessionActivity/MushafView require an Android
 * Context this JVM test suite cannot construct, and the reader is a WebView/JS component with no
 * Robolectric in this project.
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

    @Test public void sessionAppliesLandmarksOnEveryActivePageEntryAndSwipe() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String helper = method(session,
            "private List<String> applyActiveLandmarks(int page) {", "private void updateMurajaahActions()");
        assertTrue("must read the full page's lines, masking is now handled at cell granularity, not by dropping a whole line",
            helper.contains("geometry.lineIdsOnPage(page)"));
        assertTrue("must push the page's first line as the start landmark", helper.contains("all.get(0)"));
        assertTrue("must push the page's last line as the end landmark", helper.contains("all.get(all.size() - 1)"));
        assertTrue("must actually tell the reader about both landmarks",
            helper.contains("mushaf.setLandmarkLines(first, last);"));
        assertTrue("every page-entry/page-swipe path in active mode must route through this one helper",
            countOccurrences(session, "applyActiveLandmarks(") >= 5);
        assertTrue("no active-mode call site may bypass the helper with the raw page lookup",
            !session.contains("currentLineIds=geometry.lineIdsOnPage(")
                && !session.contains("currentLineIds = geometry.lineIdsOnPage("));
    }

    @Test public void mushafForwardsLandmarksToTheReaderAndIntoTheBootPayload() throws Exception {
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue("must expose a setter mirroring setHighlightVerses/setAudioVerse",
            mushaf.contains("public void setLandmarkLines(String startLineId, String endLineId) {"));
        assertTrue("a fresh page load must carry the current start landmark",
            mushaf.contains(".put(\"landmarkStart\", landmarkStartLineId)"));
        assertTrue("a fresh page load must carry the current end landmark",
            mushaf.contains(".put(\"landmarkEnd\", landmarkEndLineId)"));
        assertTrue("live updates (without a full reload) must call the reader's own setter",
            mushaf.contains("window.HifzReader.setLandmarks("));
    }

    @Test public void readerRevealsTheReadingOrderCorrectHalfOfEachLandmarkLine() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("reader must expose the live setter the native side calls",
            reader.contains("setLandmarks(startId,endId){landmarkStart=startId?String(startId):null;landmarkEnd=endId?String(endId):null;render()}"));
        String split = method(reader,
            "function landmarkCellIndices(cellCount,role){", "function maskCandidates(lines,polys){");
        assertTrue("a single-cell line can't be meaningfully split, so it must stay fully maskable",
            split.contains("if(cellCount<=1)return null;"));
        assertTrue("the reveal count must round up so an odd cell count favors revealing, not hiding",
            split.contains("Math.ceil(cellCount/2)"));
        assertTrue("'start' must mask the head (low-index/leftmost) half — since cells run low-to-high in x "
                + "while Arabic reads right to left, the leftmost cells are read *last*, so they're safe to mask",
            split.contains("{from:0,to:cellCount-reveal}"));
        assertTrue("'end' must mask the tail (high-index/rightmost) half — the rightmost cells are read *first*, "
                + "so masking them (not the landmark's leftmost reading-last words) is what makes it an end landmark",
            split.contains("{from:reveal,to:cellCount}"));
        assertTrue("maskCandidates must actually honor that range when building today's mask pool",
            reader.contains("if(range&&(ci<range.from||ci>=range.to))return;"));
    }
}

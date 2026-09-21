package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Phase 3 of the weak-spot highlight (task #34): a flag auto-clears after 3 clean Révision active
 * recalls, never from passive viewing. Révéler is a whole-page reveal (not per-verse), so "clean"
 * is tracked at page granularity: a flagged verse whose page was never revealed during today's
 * active pass counts one clean pass toward auto-clearing; a verse whose page WAS revealed resets
 * its streak to 0 instead. These are source-contract checks (see RevisionWeakSpotSourceContractTest
 * for why): HifzPrefs needs an Android Context this JVM suite cannot construct, and the reader is a
 * WebView/JS component with no Robolectric in this project.
 */
public final class RevisionWeakSpotDecaySourceContractTest {
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

    @Test public void prefsClearsAfterThreeCleanPassesAndResetsOnAnyReveal() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the clean-streak threshold must be exactly 3", prefs.contains("WEAK_VERSE_CLEAN_STREAK_TO_CLEAR = 3"));
        assertTrue("advanceWeakVerseStreaks must exist and take both a clean and a revealed set",
            prefs.contains("public List<VerseRef> advanceWeakVerseStreaks(java.util.Collection<VerseRef> cleanThisSession,"));
        String advance = method(prefs,
            "public List<VerseRef> advanceWeakVerseStreaks(java.util.Collection<VerseRef> cleanThisSession,",
            "public int sabqiLineCursor()");
        assertTrue("a clean pass must increment before comparing to the threshold",
            advance.contains("int next = streaks.getOrDefault(verse, 0) + 1;"));
        assertTrue("reaching the threshold must remove the flag itself, not just reset its counter",
            advance.contains("weak.remove(verse);"));
        assertTrue("any reveal on a flagged verse's page must drop its streak back to 0",
            advance.contains("streaks.remove(verse)"));
        String toggle = method(prefs,
            "public boolean toggleMurajaahWeakVerse(VerseRef verse) {", "public static final int WEAK_VERSE_CLEAN_STREAK_TO_CLEAR");
        assertTrue("manually un-flagging a verse must also drop its stored streak, so a later re-flag starts clean",
            toggle.contains("streaks.remove(verse)"));
    }

    @Test public void sessionTracksRevealsPerPageAndOnlyDecaysDuringActive() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("must track which pages were revealed this active session",
            session.contains("private final java.util.Set<Integer> activeRevealedPages"));
        assertTrue("pressing Révéler must record the current page only in active mode",
            session.contains("if(MURAJAAH_ACTIVE.equals(mode)){\n                    activeRevealedPages.add(currentPage);"));
        assertTrue("completing an active session must advance the weak-verse streaks",
            session.contains("advanceWeakVerseStreaksForActiveSession(corpus);"));
        String completeValidation = method(session,
            "private void completeMurajaahValidation(){", "private void advanceWeakVerseStreaksForActiveSession");
        assertTrue("the streak advance must happen inside the active branch, never the passive one",
            completeValidation.contains("if (active) {\n            advanceWeakVerseStreaksForActiveSession(corpus);"));
    }
}

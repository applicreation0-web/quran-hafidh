package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly: testing recall from memory on material still mid-Stabilisation ("à
 * stabiliser", i.e. unconsolidatedPromotedRanges) is discouraging rather than constructive, since
 * it hasn't had its own repetitions yet. Révision active must therefore read a restricted corpus
 * — the declared-acquired base, historical migration ranges, and only the promoted ranges that
 * have already cleared Consolidation — while passive Entretien keeps reading everything. Active
 * and passive consequently read different corpora and can no longer share one cursor: each now
 * advances its own independent traversal position.
 */
public final class RevisionActiveSettledCorpusSourceContractTest {
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

    @Test public void activeCorpusExcludesUnconsolidatedMaterial() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String activeCorpus = method(prefs,
            "public EligibleCorpus activeMurajaahCorpus() {", "public VerseRef activeMurajaahCursor()");
        assertTrue("active must still read the declared-acquired base", activeCorpus.contains("itqanRanges()"));
        assertTrue("active must still read historical migration ranges", activeCorpus.contains("legacyMurajaahPromotedRanges()"));
        assertTrue("active must only read promoted material that already cleared Consolidation",
            activeCorpus.contains("consolidatedPromotedRanges()"));
        assertFalse("active must never read the still-pending \"à stabiliser\" subset directly",
            activeCorpus.contains("unconsolidatedPromotedRanges()"));
    }

    @Test public void activeAndPassiveHaveFullyIndependentCursorsAndProgressMarkers() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("active needs its own cursor getter", prefs.contains("public VerseRef activeMurajaahCursor()"));
        assertTrue("active needs its own cursor setter", prefs.contains("public void setActiveMurajaahCursor(VerseRef value)"));
        assertTrue("active needs its own validity check", prefs.contains("public boolean isActiveMurajaahCursorValid()"));
        assertTrue("active needs its own transient endpoint marker", prefs.contains("public VerseRef activeMurajaahActualEnd()"));
        assertTrue("active needs its own transient page marker", prefs.contains("public int activeMurajaahPage()"));
        assertTrue("closing active must advance its own cursor, not the passive one",
            prefs.contains("public boolean completeActiveMurajaah(VerseRef nextCursor, String date, String label) {"));
        String complete = method(prefs,
            "public boolean completeActiveMurajaah(VerseRef nextCursor, String date, String label) {", "private static String consolidationStateKey");
        assertTrue("must persist the advanced cursor under its own key", complete.contains("\"activeMurajaahCursor\""));
        assertFalse("must never touch the passive cursor key", complete.contains("\"murajaahCursor\""));
    }

    @Test public void sessionRoutesActiveModeToItsOwnCorpusAndCursorEverywhere() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String renderActive = method(session,
            "private void renderMurajaahActive(){", "private void updateMurajaahActions()");
        assertTrue("renderMurajaahActive must validate against its own cursor", renderActive.contains("prefs.isActiveMurajaahCursorValid()"));
        assertTrue("renderMurajaahActive must plan from its own corpus", renderActive.contains("prefs.activeMurajaahCorpus()"));
        assertTrue("renderMurajaahActive must start from its own cursor", renderActive.contains("prefs.activeMurajaahCursor()"));
        assertFalse("renderMurajaahActive must no longer reuse the passive cursor/corpus",
            renderActive.contains("prefs.murajaahCursor()") || renderActive.contains("prefs.murajaahCorpus()"));

        String completeValidation = method(session,
            "private void completeMurajaahValidation(){", "private int countMurajaahLinesThrough");
        assertTrue("validation must pick the corpus per mode before advancing either cursor",
            completeValidation.contains("EligibleCorpus corpus = active ? prefs.activeMurajaahCorpus() : prefs.murajaahCorpus();"));
    }
}

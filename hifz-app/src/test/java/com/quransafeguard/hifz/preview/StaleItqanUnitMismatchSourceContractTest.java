package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * The actual root cause of the "49:1 → 49:4 keeps reappearing" bug, found only after the learner
 * sent a live export at the user's own insistence ("Arrêté avec tes putains de patch et regarde
 * l'origine de l'erreur"): the export showed p4ItqanRotationCursor correctly advanced to "50:1" by
 * the earlier HifzPrefs-level fixes — the rotation itself was fine — yet itqanUnitStart/End were
 * still "49:1"/"49:11" with itqanRep=20. renderItqan() in HifzSessionActivity has its OWN,
 * completely separate in-progress check that trusts prefs.itqanUnitStart()/itqanUnitEnd()
 * verbatim whenever rep or blockIndex is non-zero, entirely independent of what
 * currentAnchoringEntry() (already fixed, already correct) says the current unit actually is. No
 * fix to HifzPrefs's rotation logic could ever matter once local progress existed on a unit from
 * before that fix shipped. Fixed by only trusting the saved unit when it matches
 * anchoringEntry's own bounds; a mismatch means stale progress from an earlier, since-corrected
 * position, discarded via HifzPrefs.discardStaleItqanProgress and re-rendered from scratch.
 */
public final class StaleItqanUnitMismatchSourceContractTest {
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

    @Test public void renderItqanOnlyResumesASavedUnitThatMatchesTheCurrentEntry() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String m = method(session, "private void renderItqan(int autoChainDepth) {", "itqanSessionProtocol = anchoringEntry.protocol;");
        assertTrue("must compare the saved unit against anchoringEntry's own bounds, not just check rep/blockIndex",
            m.contains("savedStart.toString().equals(anchoringEntry.start) && savedEnd.toString().equals(anchoringEntry.end)"));
        assertTrue("a mismatched saved unit must be discarded through HifzPrefs, not silently trusted",
            m.contains("prefs.discardStaleItqanProgress()"));
        assertTrue("after discarding, the render must restart clean rather than patch stale locals (rep, itqanBonusDecision…)",
            m.contains("renderItqan(autoChainDepth);"));
        assertTrue("the resume branch itself must gate on the match, not the old bare rep/blockIndex check",
            m.contains("if(savedMatchesCurrentEntry){"));
    }
}

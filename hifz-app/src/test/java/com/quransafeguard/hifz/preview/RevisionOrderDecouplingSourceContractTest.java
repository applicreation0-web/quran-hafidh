package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * P2's compact Révision selector (Révision active / Entretien) needs free
 * ordering: Entretien (passive Murajaah) must be openable before Révision active is done that
 * day. Only the UI-ordering gate is removed here — the two cursors stay fully independent, and
 * MainActivity's daily cadenceComplete(REVISION) still requires both to be done that day, just
 * not in any particular order (see ClaudeNoGoRegressionSourceContractTest for that invariant).
 */
public final class RevisionOrderDecouplingSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void renderMurajaahNoLongerBlocksOnActiveMurajaahBeingDoneFirst() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse("the passive Entretien screen must no longer refuse to open just because "
                + "Révision active hasn't run yet today",
            source.contains("Révision · active requise"));
        assertFalse(source.contains("Terminez d’abord la Révision active du jour."));
        assertFalse("the removed gate's own condition must be gone too, not just its message",
            source.contains("if (!today.equals(prefs.lastActiveMurajaahDate())) {"));
        assertTrue("the cursor-validity guard right after the removed gate must still be intact",
            source.contains("if (!prefs.isMurajaahCursorValid()) {\n"
                + "            sessionCompleted = true;\n"
                + "            program.setText(\"Révision · curseur à vérifier\");"));
    }
}

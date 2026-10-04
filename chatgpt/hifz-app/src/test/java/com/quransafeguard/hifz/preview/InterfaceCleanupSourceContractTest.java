package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** UI-only cleanup contract: compact controls without rewriting Hifz engines. */
public final class InterfaceCleanupSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void sessionUiKeepsEngineEntryPointsAndUsesContextualSecondaryControls() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("renderSabqi()"));
        assertTrue(session.contains("renderItqan()"));
        assertTrue(session.contains("renderConsolidationCycle()"));
        assertTrue(session.contains("renderLearningConsolidationCycle()"));
        assertTrue(session.contains("annotationUndoButton.setVisibility(View.GONE)"));
        assertTrue(session.contains("new HifzAudioGate(this).available()"));
        assertFalse(session.contains("Ui.roundAction(this,\"↺\",\"Annuler la note\""));
    }

    @Test public void freeMemMaskChoicesAreCompactInsteadOfWeightedPavés() throws Exception {
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue(free.contains("new LinearLayout.LayoutParams(Ui.dp(this,52),Ui.dp(this,48))"));
        assertFalse(free.contains("Ui.weight(b,1);masks.addView(b)"));
    }

    @Test public void progressionEtaHasNoBorderedPanel() throws Exception {
        String progress = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ProgressMapActivity.java");
        assertFalse(progress.contains("Ui.panel(box)"));
        assertTrue(progress.contains("Interface Progression"));
    }
}

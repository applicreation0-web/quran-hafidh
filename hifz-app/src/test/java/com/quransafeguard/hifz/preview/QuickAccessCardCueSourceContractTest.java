package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Home quick-access cues use the same units for both weekly objectives:
 * Apprentissage 1 page/semaine; Stabilisation 2 pages/semaine.
 * Per-session details and repetitions remain inside the actual sessions.
 */
public final class QuickAccessCardCueSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void stabilisationCueStatesAConcreteQuantityLikeApprentissageDoes() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("Apprentissage states its weekly objective, not per-session lines",
            main.contains("pathTile(coreModes, \"Apprentissage\", \"1 page/semaine\""));
        assertTrue("Stabilisation uses the identical page/week unit",
            main.contains("2 pages/semaine"));
        assertTrue("Stabilisation's cue must state its own fixed weekly line target, not a vague word",
            ui.contains(": PreviewConfig.STABILIZATION_WEEKLY_LINES + \" lignes/semaine\""));
        assertFalse("the old unquantified label must not remain", ui.contains("? \"Répétitions\""));
    }
}

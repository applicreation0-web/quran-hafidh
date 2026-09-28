package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly: the home screen's "Stabilisation" quick-access card showed the bare word
 * "Répétitions" underneath, with no quantity — unlike "Apprentissage", whose "5 lignes" cue (a
 * true frozen constant, SABQI_LINES) states a concrete amount. Stabilisation's own per-block
 * repetition count isn't a fixed constant (35 or 40, depending on protocol — see
 * PreviewConfig.itqanTotalReps), but its weekly line target is (STABILIZATION_WEEKLY_LINES = 22),
 * so the cue now states that instead, matching Apprentissage's concrete-quantity style.
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
        assertTrue("Apprentissage's cue must keep stating its frozen constant",
            ui.contains("? \"5 lignes\""));
        assertTrue("Stabilisation's cue must state its own fixed weekly line target, not a vague word",
            ui.contains("? PreviewConfig.STABILIZATION_WEEKLY_LINES + \" lignes/semaine\" : \"\";"));
        assertFalse("the old unquantified label must not remain", ui.contains("? \"Répétitions\""));
    }
}

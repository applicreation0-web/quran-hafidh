package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Apprentissage's own live completion estimate, alongside Stabilisation's: remaining physical
 * lines from the real Sabqi cursor to sabqiEnd, bounded below by sabqiStart so nothing printed
 * before it (Al-Fatiha, under the default 2:75 start) is ever counted as work still to do — it was
 * never part of the Apprentissage walk in the first place, regardless of how far sabqiEnd reaches.
 */
public final class ApprentissageEtaSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void remainingLinesAreBoundedBySabqiStartAndSabqiEnd() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must walk from the real Sabqi cursor, not sabqiStart unconditionally, once "
                + "memorization has actually begun",
            prefs.contains("int from = cursor >= 0 ? Math.max(cursor, first) : first;"));
        assertTrue("must never count lines before sabqiStart as remaining — that excludes "
                + "Al-Fatiha under the default 2:75 start",
            prefs.contains("int first = geometry.firstLineIndex(sabqiStart());"));
        assertTrue("must stop at sabqiEnd, the currently configured target",
            prefs.contains("int last = geometry.lastLineIndex(sabqiEnd());"));
    }

    @Test public void diagnosticShowsALiveApprentissageEtaNextToTheRawPosition() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("the estimate must sit right next to the raw Apprentissage position it explains",
            settings.contains("+\"\\nEstimation fin Apprentissage : \"+apprentissageEtaSummary()"));
        assertTrue("Apprentissage's pace is its own — SABQI_LINES per session times "
                + "learningDaysPerWeek — never Stabilisation's fixed weekly constant",
            settings.contains("int weeklyPace=PreviewConfig.SABQI_LINES*prefs.learningDaysPerWeek();"));
        assertTrue("a cleared target must say so plainly",
            settings.contains("if(remaining==0)return \"à jour\";\n        int weeklyPace="));
    }
}

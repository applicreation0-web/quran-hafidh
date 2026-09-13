package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class Lot2SourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void oneMurajaahCadenceDrivesAllRuntimeSizing() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(session.contains("HifzCadence.targetLines"));
        assertTrue(session.contains("HifzCadence.targetFiveLineCapacity"));
        assertTrue(session.contains("HifzCadence.recalibrate"));
        assertFalse(session.contains("recentSecondsPerLine()"));
        assertFalse(prefs.contains("public double recentSecondsPerLine()"));
        assertFalse(prefs.contains("public void setRecentSecondsPerLine"));
    }

    @Test public void advisoryIsWedFriOnlyAndNeverBecomesScheduledSession() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(main.contains("recentSabqiAdvisory"));
        assertTrue(main.contains("DayOfWeek.WEDNESDAY") && main.contains("DayOfWeek.FRIDAY"));
        assertTrue(main.contains("HifzCadence.advisoryFiveLineRange"));
        assertTrue(main.contains("prefs.recentSabqi().isEmpty()"));
        assertTrue(core.contains("EVENING_REVIEW_MINUTES = 30"));
        assertTrue(core.contains("ANCHORING_ENVELOPE_MINUTES = 60"));
        assertTrue(core.contains("CONSOLIDATION_MINUTES = 30"));
        assertTrue(core.contains("MAINTENANCE_MINUTES = 45"));
        assertFalse(core.contains("MICRO_REVIEW"));
    }

    @Test public void effectiveItqanCorpusIsVisibleButNotEditable() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(prefs.contains("effectiveItqanRanges()"));
        assertTrue(settings.contains("Corpus Itqān réel"));
        assertTrue(settings.contains("À consolider ×40"));
        assertTrue(settings.contains("prefs.effectiveItqanRanges()"));
        assertTrue(settings.contains("prefs.unconsolidatedPromotedRanges()"));
        assertFalse(settings.contains("Modifier le corpus réel"));
        assertFalse(settings.contains("Supprimer du corpus réel"));
    }

    @Test public void fixedRepetitionsAndFixedScheduleRemainUntouched() throws Exception {
        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        assertTrue(config.contains("SABQI_TOTAL_REPS = 37"));
        assertTrue(config.contains("ITQAN_TOTAL_REPS = 40"));
    }
}

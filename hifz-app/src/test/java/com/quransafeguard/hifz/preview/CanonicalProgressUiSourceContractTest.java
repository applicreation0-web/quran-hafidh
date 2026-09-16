package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Phone feedback contract: structured Hifz follows the schema-6 vocabulary and cadence. */
public final class CanonicalProgressUiSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void settingsExposeCanonicalWeeklyCadenceAndNotLegacySchedule() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("Lun/Mer/Ven · Apprentissage"));
        assertTrue(settings.contains("Mar/Jeu · Stabilisation"));
        assertTrue(settings.contains("Sam/Dim · Révision"));
        assertFalse(settings.contains("Mar/Jeu/Sam · Ancrage"));
        assertFalse(settings.contains("Dim · Ancrage puis Consolidation"));
    }

    @Test public void settingsExposeIndependentEditableMultiRangesForStabilizationAndAcquiredCorpus() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(settings.contains("section(root,\"Plages à stabiliser\")"));
        assertTrue(settings.contains("section(root,\"Plages acquises\")"));
        assertTrue(settings.contains("chooseStabilizationRange"));
        assertTrue(settings.contains("chooseAcquiredRange"));
        assertTrue(settings.contains("removeStabilizationRange"));
        assertTrue(settings.contains("removeAcquiredRange"));
        assertTrue(prefs.contains("setV6StabilizationRanges"));
        assertTrue(prefs.contains("setV6AcquiredRanges"));
        assertTrue(prefs.contains("validateV6ManualRanges"));
        assertTrue(prefs.contains("Une plage ne peut pas être à la fois Acquise et À stabiliser."));
    }

    @Test public void repereLexiconMatchesFourActionsAndThreeStates() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        for (String name : new String[]{"Apprentissage","Appris","Stabilisation","Stabilisé","Consolidation","Acquis","Révision","J10"}) {
            assertTrue(name, settings.contains("addRepere(root,\"" + name + "\""));
        }
        assertFalse(settings.contains("addRepere(root,\"Leçon neuve\""));
        assertFalse(settings.contains("addRepere(root,\"Reprise du soir\""));
        assertFalse(settings.contains("addRepere(root,\"Ancrage\""));
        assertFalse(settings.contains("addRepere(root,\"Ancrage fractionné\""));
        assertFalse(settings.contains("addRepere(root,\"Entretien\""));
        assertFalse(settings.contains("addRepere(root,\"En attente\""));
    }

    @Test public void quickActionsAndTodayResolverUseCanonicalActionsAndCarryoverDate() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(main.contains("\"Apprentissage\""));
        assertTrue(main.contains("\"Stabilisation\""));
        assertTrue(main.contains("\"Révision\""));
        assertTrue(main.contains("HifzSchedule.INSTANCE.nextDue"));
        assertTrue(main.contains("HifzSessionActivity.EXTRA_SCHEDULED_DATE"));
        assertTrue(session.contains("if (SABQI.equals(mode)) return \"Apprentissage\""));
        assertTrue(session.contains("if (SABQI_TODAY_REVIEW.equals(mode)) return \"Apprentissage\""));
        assertTrue(session.contains("if (ITQAN.equals(mode)) return \"Stabilisation\""));
        assertTrue(session.contains("return \"Révision\""));
    }

    @Test public void weeklyProjectionUsesCanonicalCadenceAndNames() throws Exception {
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(week.contains("HifzSchedule.INSTANCE.actionFor"));
        assertTrue(week.contains("Apprentissage"));
        assertTrue(week.contains("Stabilisation"));
        assertTrue(week.contains("Révision"));
        assertFalse(week.contains("\"Leçon neuve ·"));
        assertFalse(week.contains("\"Reprise du soir ·"));
        assertFalse(week.contains("\"Ancrage ·"));
        assertFalse(week.contains("\"Ancrage fractionné ·"));
        assertFalse(week.contains("\"Entretien ·"));
    }

    @Test public void localHusaryImportContractRemainsUntouched() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String pack = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzAudioPack.java");
        assertTrue(settings.contains("Al-Husary Muʿallim"));
        assertTrue(settings.contains("Choisir le pack"));
        assertTrue(pack.contains("Quran-Hifz-Husary-Muallim.zip"));
        assertTrue(pack.contains("sha256.txt"));
    }
}

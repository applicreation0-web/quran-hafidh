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

    /**
     * The retention redesign needs three Ancrage mornings (Tue/Thu/Sat) to pair with the
     * three Leçon-neuve mornings (Mon/Wed/Fri), forming weekly groups of three (S1/S2/S3 and
     * A1/A2/A3). Sunday is the sole reserved Révision day; the old two-day Tue/Thu
     * Stabilisation plus Sat/Sun Révision split is the legacy schedule being replaced here.
     * Settings now builds this summary dynamically (weeklyCadenceSummary), since Apprentissage's
     * share of the six non-Sunday days is configurable — the exact default Mon/Wed/Fri split is
     * verified against HifzSchedule.actionFor itself, not this static text, in HifzWeeklyPlanningTest.
     */
    @Test public void settingsExposeCanonicalWeeklyCadenceAndNotLegacySchedule() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("private String weeklyCadenceSummary(){"));
        assertTrue(settings.contains("· Apprentissage   ·   \""));
        assertTrue(settings.contains("· Stabilisation   ·   \""));
        assertTrue(settings.contains("\"Dim · Révision\""));
        assertFalse(settings.contains("Mar/Jeu · Stabilisation"));
        assertFalse(settings.contains("Sam/Dim · Révision"));
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

    @Test public void structuredLearningRangeUsesCanonicalVocabulary() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("Début de la plage d’Apprentissage"));
        assertTrue(settings.contains("Fin de la plage d’Apprentissage"));
        assertFalse(settings.contains("plage à mémoriser"));
        assertFalse(settings.contains("Position de la leçon hors de la plage"));
    }

    @Test public void settingsShowCanonicalSchemaInsteadOfLexiconDefinitions() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("section(root,\"Schéma\")"));
        assertTrue(settings.contains("Apprentissage → Appris → Stabilisation → Stabilisé → Consolidation → Acquis → Révision"));
        assertTrue(settings.contains("consolidationSchemaNote.setText(\"Consolidation · soir \"+stabilizationDays);"));
        assertFalse(settings.contains("section(root,\"Repères\")"));
        assertFalse(settings.contains("addRepere(root"));
    }

    @Test public void sessionMessagesUseCanonicalFrenchGrammar() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(session.contains("de la Apprentissage"));
        assertFalse(session.contains("de Apprentissage"));
        assertFalse(session.contains("d’Stabilisation"));
        assertTrue(session.contains("de l’Apprentissage"));
        assertTrue(session.contains("de la Stabilisation"));
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
        // A bare "Entretien ·" is legitimate now for Sunday evening (no Renforcement/Consolidation
        // component that day, since Sunday morning is the ×5 snowball finale instead); the weekday
        // LEARNING/STABILIZATION evenings must still never go bare — they stay compound.
        assertTrue(week.contains("evening=\"Renforcement + Entretien"));
        assertTrue(week.contains("evening=\"Consolidation + Entretien"));
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

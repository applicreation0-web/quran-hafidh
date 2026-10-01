package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * P4's Roadmap wiring, scoped exactly to what's been validated so far: HifzPrefs.
 * currentRoadmapDecision only ever returns a real Decision for Phase.CURRENT (Al-Baqara Sabqi not
 * yet complete) — BRIDGE onward needs the perpetual Itqān rotation's real lag/lap data, not yet
 * wired, so every reader of the weekly cadence falls back to the manual learningDaysPerWeek
 * setting for any other phase, per the user's own explicit choice of scope. MainActivity and
 * SettingsActivity both read the same effectiveLearningDaysPerWeek()-style helper rather than
 * duplicating the AUTO/MANUAL fallback logic.
 */
public final class RoadmapCadenceWiringSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void roadmapModeDefaultsToAutoWithManualAsTheEscapeHatch() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("enum RoadmapMode { AUTO, MANUAL }"));
        assertTrue(prefs.contains(
            "return \"MANUAL\".equals(p.getString(\"roadmapMode\", \"AUTO\")) ? RoadmapMode.MANUAL : RoadmapMode.AUTO;"));
    }

    @Test public void currentRoadmapDecisionNeverAppliesPastPhaseCurrent() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("MANUAL mode must never consult the Roadmap at all",
            prefs.contains("if (roadmapMode() == RoadmapMode.MANUAL) return null;"));
        assertTrue("BRIDGE/CATCHUP/CRUISE are not yet trustworthy (stubbed lag, no Itqan rotation "
                + "data) — only Phase.CURRENT's decision is ever handed back",
            prefs.contains("return decision.phase == RoadmapPolicy.Phase.CURRENT ? decision : null;"));
    }

    @Test public void mainActivityAllThreeCadenceReadersUseTheSharedEffectiveHelper() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue(main.contains("private int effectiveLearningDaysPerWeek() {"));
        assertTrue("refreshQuickAccessCadenceGating must use the shared helper",
            main.contains("HifzSchedule.INSTANCE.actionFor(HifzClock.today().getDayOfWeek(), effectiveLearningDaysPerWeek());"));
        assertTrue("cadenceComplete must use the shared helper",
            main.contains("HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), effectiveLearningDaysPerWeek());"));
        assertTrue("nextDueCadence must use the shared helper",
            main.contains("HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed,effectiveLearningDaysPerWeek());"));
    }

    @Test public void settingsActivityShowsTheEffectiveRatioAndAnAutoManualToggle() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("private int effectiveLearningDaysPerWeek(){"));
        assertTrue("the manual day-count row must be disabled while AUTO is driving the ratio",
            settings.contains("learningDaysSetting.setEnabled(prefs.roadmapMode()==HifzPrefs.RoadmapMode.MANUAL);"));
        assertTrue(settings.contains(
            "prefs.setRoadmapMode(checked?HifzPrefs.RoadmapMode.AUTO:HifzPrefs.RoadmapMode.MANUAL);"));
    }
}

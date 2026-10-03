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

    @Test public void maintenanceAndConsolidationUseIndependentCadences() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String speeds = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSpeedStore.java");
        assertTrue(session.contains("HifzCadence.targetLines"));
        assertTrue(session.contains("speedStore.maintenanceSecondsPerLine()"));
        assertTrue(session.contains("speedStore.calibrateMaintenance"));
        assertTrue(session.contains("speedStore.calibrateConsolidation"));
        assertTrue(speeds.contains("murajaahSecPerLine"));
        assertTrue(speeds.contains("recentSecPerLine"));
        assertFalse(session.contains("calibrateOldSpeed"));
    }

    @Test public void consolidationIsProgressionDrivenAndNeverAWeekdayCadence() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue(core.contains("enum class CadenceAction"));
        assertTrue(core.contains("LEARNING") && core.contains("STABILIZATION") && core.contains("REVISION"));
        assertFalse(core.contains("CadenceAction.CONSOLIDATION"));
        assertTrue(main.contains("nextDueCadence"));
        assertTrue(main.contains("HifzSchedule.INSTANCE.actionFor"));
        assertTrue(core.contains("EVENING_REVIEW_MINUTES = 30"));
        assertTrue(core.contains("MAINTENANCE_MINUTES = 45"));
    }

    @Test public void quickAccessDurationsDoNotDependOnTodaysPlan() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("HifzSchedule.INSTANCE.targetMinutesFor"));
        assertFalse(session.contains("scheduledTargetMinutes("));
        assertFalse(session.contains("Mode \" + kind + \" absent du planning"));
    }

    @Test public void schema6ExposesTwoIndependentEditableRangeLists() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue(settings.contains("section(root,\"Plages à stabiliser\")"));
        assertTrue(settings.contains("section(root,\"Plages acquises\")"));
        assertTrue(settings.contains("chooseStabilizationRange"));
        assertTrue(settings.contains("chooseAcquiredRange"));
        assertTrue(settings.contains("removeStabilizationRange"));
        assertTrue(settings.contains("removeAcquiredRange"));
        assertTrue(prefs.contains("setV6StabilizationRanges"));
        assertTrue(prefs.contains("setV6AcquiredRanges"));
        assertTrue(prefs.contains("validateV6ManualRanges"));
        assertTrue(prefs.contains("Une plage ne peut pas être à la fois Acquise et À stabiliser"));
    }

    @Test public void anchoringKeepsFullAndReconstructionProtocolsDistinct() throws Exception {
        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        assertTrue(config.contains("SABQI_TOTAL_REPS = 37"));
        assertTrue(config.contains("ITQAN_TOTAL_REPS = 40"));
        assertTrue(config.contains("ITQAN_LIGHT_TOTAL_REPS = 35"));
        assertTrue(config.contains("ITQAN_LIGHT_VISIBLE_REPS = 20"));
        assertTrue(config.contains("ITQAN_LIGHT_25_REPS = 0"));
        assertTrue(config.contains("ITQAN_LIGHT_50_REPS = 5"));
        assertTrue(config.contains("ITQAN_LIGHT_75_REPS = 5"));
        assertTrue(config.contains("ITQAN_LIGHT_100_REPS = 5"));
    }

    @Test public void failedAnchoringStartsTheDeferredPageWithAFreshRunningClock() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(prefs.contains(".putLong(\"itqanElapsedMs\", 0L)"));
        assertTrue(session.contains("restartAnchoringClockAfterDeferral()"));
        assertTrue(session.contains("clock.reset()"));
        assertTrue(session.contains("clock.resume()"));
    }

    @Test public void maintenanceEndpointMayExtendBeyondPlanButMustStayInsideAcquiredCorpus() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("EligibleCorpus corpus = prefs.murajaahCorpus()"));
        assertTrue(session.contains("if (!corpus.contains(verse))"));
        assertTrue(session.contains("if (!corpus.contains(through))"));
        assertTrue(session.contains("countMurajaahLinesThrough(murajaahActualEnd)"));
        assertFalse(session.contains("murajaahPlan.traversalVerses.contains(verse)"));
        assertFalse(session.contains("geometry.lineCountForVerseRange(murajaahPlan.start, murajaahActualEnd)"));
        int tapStart = session.indexOf("@Override public void onVerseTap");
        int tapEnd = session.indexOf("@Override public void onPageSwipe", tapStart);
        assertTrue(tapStart >= 0 && tapEnd > tapStart);
        assertFalse(session.substring(tapStart, tapEnd).contains("renderMode()"));
    }

    @Test public void obsoleteRecentQualityMachineryIsRemoved() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertFalse(prefs.contains("markFirstRecentStable"));
        assertFalse(prefs.contains("deferFirstRecentSabqi"));
        assertFalse(prefs.contains("stableRecentLines()"));
        assertFalse(prefs.contains("LineInterval"));
        assertFalse(prefs.contains("mergeIntervals("));
        assertFalse(prefs.contains("intervalsJson("));
        assertFalse(prefs.contains(".putString(\"stableRecentLines\""));
        assertFalse(prefs.contains("p.getString(\"stableRecentLines\""));
        assertTrue(prefs.contains(".remove(\"stableRecentLines\")"));
    }
}

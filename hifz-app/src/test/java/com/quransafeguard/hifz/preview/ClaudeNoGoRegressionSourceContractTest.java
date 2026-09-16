package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression contracts for the independent Claude NO-GO findings on Quran Hifz 0.7.5. */
public final class ClaudeNoGoRegressionSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void homeRoutesProgressionTriggeredConsolidation() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("MainActivity must expose the existing Consolidation mode",
            main.contains("HifzSessionActivity.RECENT_SABQI_REVIEW"));
        assertTrue("Home must inspect ready Stabilisation units before declaring the program up to date",
            main.contains("stabilizedConsolidationUnits"));
        assertTrue("Home must resume an already-frozen Consolidation session",
            main.contains("restoreConsolidationSession"));
    }

    @Test public void stabilizationProjectionUsesTheSamePhysicalPlannerAsRuntime() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("StabilizationHalfPagePolicy.planPage"));
        assertTrue(week.contains("StabilizationHalfPagePolicy.planPage"));
        assertFalse("Old five-line difficult-surah projection must not survive",
            week.contains("firstTen"));
    }

    @Test public void stabilizationAndConsolidationUseOwnedPhysicalLinesForEntryBoundaries() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String ready = method(prefs, "List<ConsolidationCycleEngine.Unit> stabilizedConsolidationUnits", "/** Fail and defer");
        String complete = method(prefs, "boolean entryIsFullyStabilizedOrAcquired", "public AnchoringQueue.Entry currentAnchoringEntry");
        String render = method(session, "private void renderItqan()", "private String itqanProgramLabel()");
        assertTrue(ready.contains("CorpusLinePolicy.ownedLineIdsForRangeOnPage"));
        assertTrue(complete.contains("CorpusLinePolicy.ownedLineIdsForRangeOnPage"));
        assertTrue(render.contains("CorpusLinePolicy.ownedLineIdsForRangeOnPage"));
    }

    @Test public void manualRangeEditInvalidatesFrozenConsolidationSession() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String manual = method(prefs, "private boolean setV6ManualRanges", "private void validateV6ManualRanges");
        assertTrue(manual.contains(".remove(\"v6ConsolidationStabilizationState\")"));
    }

    @Test public void legacyEveningLabelIsCanonicalizedAtDisplayOnly() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String evening = method(session, "private void renderSabqiTodayReview()", "private void renderConsolidationCycle()");
        assertTrue(evening.contains("HifzDisplayVocabulary.canonicalize(prefs.lastSabqiTodayReviewLabel())"));
    }

    @Test public void validationListenersContainProgressionExceptionsInsteadOfCrashingTheApp() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("private void runValidationSafely(Runnable validation)"));
        assertTrue(session.contains("runValidationSafely(this::validateSabqi)"));
        assertTrue(session.contains("runValidationSafely(this::validateItqan)"));
        assertTrue(session.contains("runValidationSafely(this::validateConsolidationCycle)"));
    }
}

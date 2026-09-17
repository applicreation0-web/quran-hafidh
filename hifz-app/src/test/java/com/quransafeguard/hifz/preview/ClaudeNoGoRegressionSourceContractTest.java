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

    @Test public void homeRoutesTheWeekdayPinnedConsolidationSnowball() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("MainActivity must expose the existing Consolidation mode",
            main.contains("HifzSessionActivity.RECENT_SABQI_REVIEW"));
        assertTrue("Home must inspect this week's accumulated Stabilisation units before offering the evening snowball",
            main.contains("stabilizedConsolidationUnits"));
        assertTrue("Home must not re-offer tonight's snowball once it was already validated",
            main.contains("lastStabilizationSnowballEveningDate"));
    }

    @Test public void stabilizationProjectionUsesTheSamePhysicalPlannerAsRuntime() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String week = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue(main.contains("StabilizationHalfPagePolicy.planPage"));
        assertTrue(week.contains("StabilizationHalfPagePolicy.planPage"));
        assertFalse("Old five-line difficult-surah projection must not survive",
            week.contains("firstTen"));
    }

    /**
     * The physical unit fed into the weekly snowball is computed once, in renderItqan(), from
     * owned physical lines; stabilizedConsolidationUnits() now just reads that same unit back
     * from this week's accumulator, so it no longer needs its own ownedLineIdsForRangeOnPage call.
     */
    @Test public void stabilizationAndConsolidationUseOwnedPhysicalLinesForEntryBoundaries() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String complete = method(prefs, "boolean entryIsFullyStabilizedOrAcquired", "public AnchoringQueue.Entry currentAnchoringEntry");
        String render = method(session, "private void renderItqan()", "private String itqanProgramLabel()");
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

    /**
     * The four grouped-cycle validations (evening snowball ×2, Sunday final review ×2) share
     * renderGroupedCycle()'s single "runValidationSafely(onValidate)" call site, so each is
     * verified by confirming it is passed in as that Runnable rather than by its own literal
     * "runValidationSafely(this::validateX)" call.
     */
    @Test public void validationListenersContainProgressionExceptionsInsteadOfCrashingTheApp() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("private void runValidationSafely(Runnable validation)"));
        assertTrue(session.contains("runValidationSafely(this::validateSabqi)"));
        assertTrue(session.contains("runValidationSafely(this::validateItqan)"));
        assertTrue(session.contains("v -> runValidationSafely(onValidate)"));
        assertTrue(session.contains("this::validateConsolidationCycle"));
        assertTrue(session.contains("this::validateLearningConsolidationCycle"));
        assertTrue(session.contains("this::validateConsolidationFinalReview"));
        assertTrue(session.contains("this::validateLearningFinalReview"));
    }

    /**
     * The Consolidation gate is now evaluated fresh every day (evening snowball once the
     * morning's Stabilisation is done, Sunday's ×5 final review otherwise) rather than
     * progression-triggered; nextMode() wraps the whole computation so a corrupt state never
     * crashes the app, only routes to Diagnostic.
     */
    @Test public void homeConsolidationGateIsDailyAndNeverThrows() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        String gate = method(main, "private String nextMode(ScheduledCadence due)", "private String computeNextMode");
        assertTrue(gate.contains("catch(RuntimeException"));
        assertTrue(gate.contains("consolidationNeedsAttention=true"));
        assertTrue(gate.contains("learningConsolidationNeedsAttention=true"));
        String evening = method(main, "private String eveningStabilizationMode", "private String nextMode(ScheduledCadence due)");
        assertTrue(evening.contains("lastStabilizationSnowballEveningDate"));
        assertTrue(evening.contains("stabilizedConsolidationUnits"));
    }

    @Test public void rangeNormalizationNeverSplitsPerSurah() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String sort = method(prefs, "private static List<VerseRange> sortRangesPreservingBoundaries", "private static List<VerseRange> rangesFromVerses");
        assertTrue(sort.contains("sameSurahAdjacent"));
        assertFalse(sort.contains("fromOrdinal(segmentStart)"));
    }
}

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

    /**
     * Live-confirmed: after a Stabilisation range edit leaves itqanRotationStart outside the new
     * corpus, EligibleCorpus.nextAnchored's require() threw on every subsequent validation
     * (surfaced only as the generic "État de progression à vérifier" pink banner, 35/35 done,
     * with no actionable recovery). validateItqan() must self-heal via repairedItqanRotationStart()
     * instead of reading the raw, possibly-invalid cursor.
     */
    @Test public void stabilizationValidationSelfHealsAnInvalidRotationStartInsteadOfThrowing() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(prefs.contains("public VerseRef repairedItqanRotationStart()"));
        String repaired = method(prefs, "public VerseRef repairedItqanRotationStart()", "\n\n");
        assertTrue(repaired.contains("isRotationStartValid()"));
        assertTrue(repaired.contains("setItqanRotationStart(repaired)"));
        String validate = method(session, "private void validateItqan()", "private void restartItqanAfterAssistance()");
        assertTrue(validate.contains("corpus.nextAnchored(itqanUnit.end, prefs.repairedItqanRotationStart())"));
        assertFalse(validate.contains("corpus.nextAnchored(itqanUnit.end, prefs.itqanRotationStart())"));
    }

    /**
     * Live-confirmed: a fractionated Stabilisation block's boundary verse can straddle the split
     * (CorpusLinePolicy assigns a line to its earliest verse, so a verse can start in block 1's
     * lines and continue into lines actually owned by block 2). The default whole-verse shading
     * then greys out more physical lines than the block's real 6-8 line working set (confirmed:
     * 10 lines shaded for a block the split policy caps at 8). showCurrent() must request strict
     * per-line shading, like J10 already does, whenever the current unit is fractionated.
     */
    @Test public void fractionatedStabilizationUsesStrictLineFocusNotWholeVerseShading() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue(session.contains("mushaf.show(currentPage,currentSelection,currentLineIds,currentMask,fractionatedItqan)"));
        assertTrue(mushaf.contains("public void show(int page, List<VerseRef> selection, List<String> lineIds, int maskPercent, boolean strictLineFocus)"));
        assertTrue(mushaf.contains("lastStrictLineFocus = strictLineFocus;"));
    }

    /**
     * A Mushaf line always belongs to exactly one surah (StabilizationHalfPagePolicy.singleSurah
     * already relies on this), but the five-line Sabqi block never checked it: a block could
     * silently span the last line(s) of one surah plus the first line(s) of the next. Fix mirrors
     * StabilizationHalfPagePolicy's own never-cross-a-surah discipline.
     */
    @Test public void sabqiFiveLineBlockNeverCrossesASurahBoundary() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        String block = method(geometry, "public FiveLineBlock fiveLineBlock(int startLineIndex)", "ArrayList<String> ids = new ArrayList<>();");
        assertTrue(block.contains("int startSurah = lines.get(startLineIndex).verses.get(0).getSurah();"));
        assertTrue(block.contains("getSurah() == startSurah"));
        assertFalse("must not force exactly SABQI_LINES regardless of surah boundary",
            block.contains("!= PreviewConfig.SABQI_LINES"));
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("validateSabqi must advance from the block's real end, not a fixed +5",
            session.contains("sabqiBlock.endLineIndex+1"));
    }

    /** Lecture's page slider is replaced by a direct surah picker (all 114, canonical order). */
    @Test public void studyReaderNavigatesByDirectSurahPickerNotAPageSlider() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String names = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranSurahNames.java");
        assertFalse("page slider must be gone from Lecture", study.contains("new SeekBar(this)"));
        assertTrue("Lecture must open a dialog listing all 114 surahs", study.contains("String[] items = new String[114];"));
        assertTrue("selecting a surah must jump to its first verse's page",
            study.contains("setPage(geometry.pageForVerse(new VerseRef(which + 1, 1)))"));
        assertTrue(names.contains("static String name(int surah)"));
        assertTrue(names.contains("static String labelFor(int surah)"));
    }
}

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
     * per-line shading whenever the current unit is fractionated.
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

    /**
     * Reported directly: a Sabqi block "didn't respect the 5 lines," extending to a 6th line to
     * finish a verse. Root cause: fiveLineBlock used to call a preferCleanVerseBoundary helper that
     * could search up to two lines *past* the natural 5-line target to land on a clean verse
     * boundary — unlike Stabilisation's own preferCleanVerseBoundary (StabilizationHalfPagePolicy),
     * which only ever shifts the split point *between* two blocks whose combined total is fixed,
     * Sabqi's version had no sibling block to shrink in compensation: it just unconditionally grew
     * the standalone block past SABQI_LINES, contradicting fiveLineBlock's own doc comment ("clips
     * to at most SABQI_LINES"). The hard rule (5 lines/session, only a surah boundary makes a
     * shorter week) does not admit a verse-boundary exception at all: a mid-verse cut is accepted
     * and already fully modeled by FiveLineBlock's startsInsideVerse/endsInsideVerse fields.
     */
    @Test public void sabqiFiveLineBlockNeverExceedsFiveLinesForAVerseBoundary() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        String block = method(geometry, "public FiveLineBlock fiveLineBlock(int startLineIndex)", "ArrayList<String> ids = new ArrayList<>();");
        assertTrue("endIndex must be capped at maxIndex (startLineIndex + SABQI_LINES - 1), never past it",
            block.contains("int endIndex = startLineIndex;"));
        assertTrue("the only early stop must be a surah change, never a verse-boundary search",
            block.contains("while (endIndex < maxIndex && lines.get(endIndex + 1).verses.get(0).getSurah() == startSurah) endIndex++;"));
        assertFalse("must no longer call a helper that can push the boundary past the 5-line target",
            geometry.contains("preferCleanVerseBoundary(startLineIndex"));
        assertFalse("the forward-searching clean-boundary helper itself must be gone, not just unused",
            geometry.contains("private int preferCleanVerseBoundary("));
        assertFalse("its isCleanVerseBoundary helper must be gone too",
            geometry.contains("private boolean isCleanVerseBoundary("));
    }

    /** Lecture's page slider is replaced by a direct surah picker (all 114, canonical order). */
    @Test public void weeklySnowballAlsoReviewsTheAccumulatedLinesAsOneContinuousPass() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("HifzPrefs must add a combined continuous unit once at least two blocks accumulated",
            prefs.contains("ConsolidationPhysicalUnitPolicy.encodeContinuousUnit(encodedBlocks)"));
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationPhysicalUnitPolicy.java");
        assertTrue("encodeContinuousUnit must concatenate every accumulated block's lines",
            policy.contains("static String encodeContinuousUnit(List<String> encodedUnits)"));
        String engine = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationCycleEngine.java");
        assertTrue("SNOWBALL/SNOWBALL_FINAL cycles must tolerate up to six configurable weekly days plus the continuous pass",
            engine.contains("protocol == Protocol.SNOWBALL_FINAL) ? 7 : 3"));
    }

    /**
     * Sunday's final review graduates the week's physical blocks to Acquis; the synthetic continuous
     * unit repeats the very same lines and must be excluded from that graduation, or its lines and
     * verses get double-processed (re-promoted, re-subtracted from the recent-Sabqi queue) the
     * moment the weekly snowball grows past its first block.
     */
    @Test public void sundayFinalReviewGraduationSkipsTheSyntheticContinuousUnit() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("both completion paths must read only the real physical blocks",
            prefs.contains("for (String unitId : physicalUnitIds(session)) {"));
        assertFalse("neither completion path may graduate the raw, un-trimmed unit list",
            prefs.contains("for (String unitId : session.unitIds()) {"));
    }

    @Test public void sundaysFinalReviewUsesTheExtendedEightWeekSnowballNotJustThisWeek() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String stabilizationFinal = method(prefs,
            "List<ConsolidationCycleEngine.Unit> stabilizationSnowballFinalUnits(LocalDate today) {", "}");
        assertTrue("Sunday's Stabilisation final review must delegate to the extended 8-week snowball",
            stabilizationFinal.contains("extendedSnowballUnits(ConsolidationCycleEngine.Family.STABILIZATION, today)"));
        String learningFinal = method(prefs,
            "List<ConsolidationCycleEngine.Unit> learningSnowballFinalUnits(LocalDate today) {", "}");
        assertTrue("Sunday's Apprentissage final review must delegate to the extended 8-week snowball",
            learningFinal.contains("extendedSnowballUnits(ConsolidationCycleEngine.Family.LEARNING, today)"));
        String extended = method(prefs,
            "private List<ConsolidationCycleEngine.Unit> extendedSnowballUnits(", "\n    }");
        assertTrue("The extended Sunday pass must read each accumulated block at ×3, not ×10",
            extended.contains("ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED"));
        assertTrue("The extended Sunday pass must span an 8-week cumulative window",
            prefs.contains("SNOWBALL_EXTENDED_WEEKS = 8"));
    }

    /**
     * The whole point of the 8-week extension is that a block promoted in week N keeps resurfacing
     * in Sunday's extended review through week N+7, not just once. Wiping the rolling history when
     * that Sunday's session graduates its blocks to Acquis would collapse the window back down to
     * "this week only" the very next Sunday — the history must only ever shrink by the date-based
     * cutoff in appendToSnowballHistory, never by an unconditional reset on completion.
     */
    @Test public void completingSundaysExtendedReviewNeverWipesTheRollingEightWeekHistory() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String stabilizationComplete = method(prefs,
            "boolean completeConsolidationSessionV6(", "boolean completeLearningConsolidationSessionV6(");
        assertFalse("Sunday's Stabilisation completion must not reset the snowball history to an empty array",
            stabilizationComplete.contains("snowballHistoryKey(ConsolidationCycleEngine.Family.STABILIZATION), \"[]\""));
        String learningComplete = method(prefs,
            "boolean completeLearningConsolidationSessionV6(", "public String lastLearningConsolidationDate()");
        assertFalse("Sunday's Apprentissage completion must not reset the snowball history to an empty array",
            learningComplete.contains("snowballHistoryKey(ConsolidationCycleEngine.Family.LEARNING), \"[]\""));
        assertTrue("Only the weekly evening accumulator (not the 8-week history) resets on Sunday completion",
            stabilizationComplete.contains("snowballUnitsKey(ConsolidationCycleEngine.Family.STABILIZATION), \"[]\""));
        assertTrue("Only the weekly evening accumulator (not the 8-week history) resets on Sunday completion",
            learningComplete.contains("snowballUnitsKey(ConsolidationCycleEngine.Family.LEARNING), \"[]\""));
    }

    @Test public void appendingToSnowballHistoryPrunesByCalendarCutoffNotAHardReset() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String append = method(prefs,
            "private JSONArray appendToSnowballHistory(", "private List<ConsolidationCycleEngine.Unit> weeklySnowballUnits(");
        assertTrue("History pruning must be based on an 8-week-back calendar cutoff",
            append.contains("mondayOf(today).minusWeeks(SNOWBALL_EXTENDED_WEEKS - 1L)"));
        assertTrue("Weeks at or after the cutoff must be kept, not discarded",
            append.contains("!weekStart.isBefore(cutoff)"));
    }

    /**
     * A long absence spanning an unrun Sunday must never let a block quietly age out of the 8-week
     * history before it was ever actually promoted to Acquis — otherwise it would be stuck at
     * Stabilisé/Appris forever, never re-offered for graduation. Both the pruning in
     * appendToSnowballHistory and the window filter in extendedSnowballUnits must consult
     * weekFullyAcquired before dropping/excluding a week past the normal 8-week cutoff.
     */
    @Test public void anUngraduatedWeekIsNeverDroppedPastTheEightWeekCutoff() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String append = method(prefs,
            "private JSONArray appendToSnowballHistory(", "private boolean weekFullyAcquired(");
        assertTrue("Pruning must keep a week past the cutoff when it isn't fully acquired yet",
            append.contains("!weekStart.isBefore(cutoff) || !weekFullyAcquired(week, acquired)"));
        String weekFullyAcquired = method(prefs,
            "private boolean weekFullyAcquired(", "private List<ConsolidationCycleEngine.Unit> weeklySnowballUnits(");
        assertTrue("A block only counts as acquired once every one of its line ids is in v6AcquiredCreditLineIds",
            weekFullyAcquired.contains("acquired.containsAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(encoded))"));
        String extended = method(prefs,
            "private List<ConsolidationCycleEngine.Unit> extendedSnowballUnits(",
            "List<ConsolidationCycleEngine.Unit> stabilizationSnowballFinalUnits(");
        assertTrue("extendedSnowballUnits must still surface an out-of-window week that isn't fully acquired yet",
            extended.contains("if (!withinWindow && weekFullyAcquired(week, acquired)) continue;"));
    }

    /**
     * Retaining an ungraduated week indefinitely (see above) means the 8-week cutoff alone no longer
     * bounds how many blocks extendedSnowballUnits can hand to one Consolidation cycle — a long
     * enough absence from Sunday specifically, combined with continued weekday practice, could in
     * principle pile up more blocks than ConsolidationCycleEngine.maxUnitsFor(SNOWBALL_EXTENDED)
     * allows, which would throw "group size must be 1..30" the moment the session finally opens.
     */
    @Test public void extendedSnowballUnitsClampsToTheEngineCycleCapEvenWithUnboundedHistory() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String extended = method(prefs,
            "private List<ConsolidationCycleEngine.Unit> extendedSnowballUnits(",
            "List<ConsolidationCycleEngine.Unit> stabilizationSnowballFinalUnits(");
        assertTrue("Must clamp to the engine's own cap for SNOWBALL_EXTENDED, minus one slot for the combined pass",
            extended.contains("ConsolidationCycleEngine.maxUnitsFor(ConsolidationCycleEngine.Protocol.SNOWBALL_EXTENDED) - 1"));
        assertTrue("Overflow must be trimmed from the list actually fed into the cycle",
            extended.contains("if (encodedBlocks.size() > cap) encodedBlocks = new ArrayList<>(encodedBlocks.subList(0, cap));"));
    }

    /**
     * SNOWBALL_EXTENDED was added alongside SNOWBALL/SNOWBALL_FINAL, but Protocol enum membership
     * alone is not enough: without an explicit stageVector case it throws
     * IllegalArgumentException("unsupported protocol"), and without a validateUnitForFamily
     * early-return every extended Sunday cycle would fail as neither LEARNING37 nor LIGHT/FULL.
     */
    @Test public void snowballExtendedProtocolIsFullyWiredIntoTheConsolidationEngine() throws Exception {
        String engine = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationCycleEngine.java");
        assertTrue("Protocol enum must declare SNOWBALL_EXTENDED",
            engine.contains("enum Protocol { LEARNING37, LIGHT, FULL, SNOWBALL, SNOWBALL_FINAL, SNOWBALL_EXTENDED }"));
        assertTrue("maxUnitsFor must give SNOWBALL_EXTENDED enough headroom for up to 8 weeks of blocks",
            engine.contains("if (protocol == Protocol.SNOWBALL_EXTENDED) return 30;"));
        String stageVector = method(engine,
            "static int[] stageVector(Protocol protocol, int groupSize, int position) {",
            "private static int[] normalizeProgress(");
        assertTrue("stageVector must handle SNOWBALL_EXTENDED explicitly, not fall through to the unsupported-protocol default",
            stageVector.contains("case SNOWBALL_EXTENDED:\n                return copy(3, 0, 0, 0, 0);"));
        String validate = method(engine,
            "private static void validateUnitForFamily(Family family, Unit unit) {",
            "private static void requireCycle(");
        assertTrue("validateUnitForFamily must exempt SNOWBALL_EXTENDED like SNOWBALL/SNOWBALL_FINAL",
            validate.contains("unit.protocol() == Protocol.SNOWBALL_EXTENDED"));
    }

    /**
     * A block or the new continuous pass can legitimately span two Mushaf pages (a 5-line block near
     * a page break, or the week's 10/15-line continuous reread). renderGroupedCycle must track the
     * FIRST and LAST physical line's page separately so page-swipe navigation (clamped between
     * unitFirstPage/unitLastPage in goPage()) can reach every page the unit's lines are actually on,
     * not just the page its first line happens to sit on.
     */
    @Test public void groupedCycleTracksBothEndsOfAMultiPageUnitNotJustItsFirstPage() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String renderGroupedCycle = method(session,
            "private void renderGroupedCycle(", "private void completeGroupedCycleRep() {");
        assertTrue("unitFirstPage must come from the unit's first physical line",
            renderGroupedCycle.contains("unitFirstPage = physicalLines.get(0).page;"));
        assertTrue("unitLastPage must come from the unit's LAST physical line, not the first",
            renderGroupedCycle.contains("unitLastPage = physicalLines.get(physicalLines.size() - 1).page;"));
        assertFalse("must not collapse the unit to a single page again",
            renderGroupedCycle.contains("unitFirstPage = unitLastPage = currentPage;"));
    }

    /**
     * roundAction's caption used to be a single line clipped to the width of its 48dp icon button
     * ("Passage suivant du corpus" rendered as "Passage s…"), and adjacent actions had only 2dp of
     * side padding, reading as visually stuck together once three actions shared one row. The
     * caption must be allowed to wrap onto a second line instead of truncating, and actions need
     * more breathing room between them.
     */
    @Test public void roundActionCaptionWrapsInsteadOfClippingAndActionsHaveBreathingRoom() throws Exception {
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        String roundAction = method(ui,
            "static LinearLayout roundAction(", "static LinearLayout cardAction(");
        assertFalse("caption must no longer be forced onto a single clipped line",
            roundAction.contains("caption.setSingleLine(true)"));
        assertTrue("caption must wrap onto up to two lines instead",
            roundAction.contains("caption.setMaxLines(2)"));
        assertTrue("caption width must no longer be squeezed to the icon button's own narrow width",
            roundAction.contains("ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));"));
        assertTrue("side padding between adjacent actions must be wider than the original 2dp",
            roundAction.contains("box.setPadding(dp(context,6),0,dp(context,6),0);"));
    }

    /**
     * Jumping to the next disjoint segment of the day's Révision objective (e.g. 2:1→2:74 then
     * 49:1→51:26) must be earned by actually touching this segment's own last verse first — tapping
     * "Passage suivant du corpus" before reaching it silently skipped unread material. The check
     * must use segment position (murajaahSegmentIndexForVerse), never raw ordinal/page magnitude,
     * since a later segment's ordinal can be *lower* than an earlier one's after wraparound.
     */
    @Test public void murajaahBlockJumpRequiresValidatingTheCurrentSegmentsLastVerseFirst() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String updateActions = method(session,
            "private void updateMurajaahActions() {", "private int murajaahSegmentIndexForPage(");
        assertTrue("the jump handler must re-check validation before actually navigating",
            updateActions.contains("if (current != null && !murajaahValidatedThroughSegment(segments, currentIndex)) {"));
        assertTrue("an unvalidated jump attempt must explain what to touch instead of silently doing nothing",
            updateActions.contains("Touchez d’abord le dernier verset de ce passage ("));
        assertTrue("the segment's own end verse must be proactively highlighted while unvalidated",
            updateActions.contains("mushaf.setSelection(Collections.singletonList(current.end),"));
        String validated = method(session,
            "private boolean murajaahValidatedThroughSegment(", "\n    }");
        assertTrue("must compare by segment position, not raw ordinal magnitude across segments",
            validated.contains("int actualIndex = murajaahSegmentIndexForVerse(segments, murajaahActualEnd);"));
        assertTrue("already having read into a later segment must count as validated for this one",
            validated.contains("if (actualIndex > segmentIndex) return true;"));
        assertTrue("onVerseTap must refresh the gating immediately, not only after the next page swipe",
            session.contains("checkpointMurajaah(clock.elapsedMs());\n        updateMurajaahActions();"));
    }

    /**
     * The Révision objective header showed raw surah numbers (e.g. "2:1 → 2:74 · puis 49:1 →
     * 51:26"), which the user has to mentally map to a surah name. murajaahRangeLabel must use
     * QuranSurahNames instead, naming the surah once for a same-surah range and naming both ends
     * when a segment spans more than one surah.
     */
    @Test public void murajaahObjectiveShowsSurahNamesInsteadOfBareNumbers() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String rangeLabel = method(session,
            "private String murajaahRangeLabel(", "private String murajaahVerseLabel(");
        assertTrue("a same-surah range must name the surah once, not repeat it for both ends",
            rangeLabel.contains("QuranSurahNames.name(start.getSurah()) + \" \" + start.getAyah() + \" → \" + end.getAyah();"));
        String objective = method(session,
            "private String murajaahObjectiveLabel() {", "/** \"2:1 → 2:74\"");
        assertFalse("must no longer build the label from bare VerseRef.toString() (\"2:74\")",
            objective.contains("label.append(segmentStart).append(\" → \").append(previous);"));
        assertTrue("must delegate every segment's range to the surah-name-aware formatter",
            objective.contains("label.append(murajaahRangeLabel(segmentStart, previous));"));
    }

    /**
     * The Mushaf reader shades a verse by verse identity wherever it appears on the page (reader.js
     * selectedPolygons/.ayahPolygon.selected), not by physical line. A grouped-cycle unit (e.g. a
     * 5-line Renforcement block) is a fixed line window, not a verse boundary, so a verse that
     * starts inside the unit but continues onto lines outside it would get shaded in full — making
     * the highlighted region visibly span more physical lines than the unit's own declared count.
     * fractionatedItqan is what showCurrent() forwards to MushafView as strictLineFocus, which
     * swaps verse-based shading for a highlight confined to exactly the given line ids
     * (lineFocusLayer) — renderGroupedCycle must set it, the same fix already used for a
     * fractionated Itqan block, or it silently falls back to the stale value left by whichever mode
     * last set it (false by default, reproducing the bug).
     */
    @Test public void groupedCycleUsesStrictLineFocusSoHighlightNeverSpillsPastTheUnit() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String renderGroupedCycle = method(session,
            "private void renderGroupedCycle(", "private void completeGroupedCycleRep() {");
        assertTrue("renderGroupedCycle must force strict line focus so a long verse can't over-shade past the unit's lines",
            renderGroupedCycle.contains("fractionatedItqan = true;"));
    }

    /**
     * Reported directly: Renforcement's Friday evening session showed bloc1, bloc2 and bloc1+2 but
     * never bloc3, even though Friday's own Sabqi block had already been learned that morning. Root
     * cause: renderGroupedCycle only builds a fresh cycle when NO session is persisted — a session
     * opened Wednesday with 2 units (bloc1, bloc2, combined) that the user hadn't finished all ×10
     * reps for by Friday was reused as-is, its unit set frozen at cycle-open time and never
     * re-checked against the week's now-larger accumulator (Cycle/addUnit explicitly forbids adding
     * a unit to an OPEN session, so the engine has no way to grow it in place). Bloc3 sat in the
     * weekly accumulator, invisible to Renforcement, until that stale session finally closed on its
     * own. The fix: when the freshly computed unit list has grown past the persisted session's own
     * size, discard the stale session (losing its in-progress reps, but never silently skipping the
     * new block) and fall through to rebuild fresh from the current, complete unit list.
     */
    @Test public void groupedCycleRebuildsWhenTheWeekAddsABlockToAStaleUnfinishedSession() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String renderGroupedCycle = method(session,
            "private void renderGroupedCycle(", "private void completeGroupedCycleRep() {");
        assertTrue("must detect a grown accumulator by comparing the persisted session's own size against the fresh unit list",
            renderGroupedCycle.contains("consolidationSession != null && consolidationSession.sessionGroupSize() < units.size()"));
        assertTrue("a stale session must be discarded (not silently continued) once the week has grown a new block",
            renderGroupedCycle.contains("prefs.discardConsolidationSession(family);"));
        assertTrue("discarding must fall through to the existing fresh-build path, not skip it",
            renderGroupedCycle.contains("prefs.discardConsolidationSession(family);\n            consolidationSession = null;\n        }\n        if (consolidationSession == null) {"));
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("discardConsolidationSession must exist and must NOT mark the evening as validated",
            prefs.contains("boolean discardConsolidationSession(ConsolidationCycleEngine.Family family) {\n        return p.edit().remove(consolidationStateKey(family)).commit();\n    }"));
    }

    /**
     * A multi-page grouped-cycle unit let every repetition count from page one alone, since
     * completeGroupedCycleRep() never checked which page was on screen — the second page of a
     * continuous pass could go entirely unread. The Répétition action must only appear once the
     * unit's last page is showing, and goPage() must re-check this on every swipe (not just once
     * at render time), since swiping never re-runs renderGroupedCycle itself.
     */
    @Test public void groupedCycleRepetitionActionOnlyAppearsOnTheUnitsLastPage() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("renderGroupedCycle must delegate the Répétition action to the page-gated helper",
            session.contains("showCurrent();\n        updateGroupedCycleRepAction();"));
        String helper = method(session,
            "private void updateGroupedCycleRepAction() {", "private void completeGroupedCycleRep() {");
        assertTrue("the action must be withheld while a page other than the last is showing",
            helper.contains("if (currentPage != unitLastPage) {"));
        assertTrue("the Répétition action itself must only be added once on the last page",
            helper.contains("addRoundAction(\"↻\", \"Répétition\", v -> completeGroupedCycleRep());"));
        String goPage = method(session, "private void goPage(int delta) {", "private void addRoundAction(");
        assertTrue("goPage must refresh the gated action after every swipe, not just at render time",
            goPage.contains("updateGroupedCycleRepAction();"));
    }

    /**
     * StabilizationHalfPagePolicy's 7.5/7.5 line-count split could land mid-verse, splitting a
     * single verse's lines across both physical Consolidation units (confirmed on a real page:
     * Al-Hujurat 49:9 straddling the raw 7/8 boundary on Mushaf page 516). It must now prefer a
     * genuine verse boundary within two lines of that target before accepting a mid-verse cut.
     */
    @Test public void stabilizationHalfPageSplitPrefersACleanVerseBoundaryNearTheTarget() throws Exception {
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        assertTrue("the split must consult a clean-verse-boundary preference before committing to the raw target",
            policy.contains("int split = preferCleanVerseBoundary(lines, start, count, bestLeft);"));
        assertTrue("the tolerance must stay at two lines either side of the target",
            policy.contains("Math.max(5, bestLeft - 2)") && policy.contains("Math.min(count - 5, bestLeft + 2)"));
    }

    @Test public void freeMemActivityAlsoOffersTheDirectSurahPicker() throws Exception {
        String free = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertTrue("Mémorisation libre must expose a Sourate nav button",
            free.contains("Ui.iconButton(this,\"\",\"Sourate\",v->showSurahPicker())"));
        assertTrue("selecting a surah must jump the free-memorization page too",
            free.contains("QuranSurahNames.showPicker(this,geometry,this::setPage)"));
    }

    @Test public void studyReaderNavigatesByDirectSurahPickerNotAPageSlider() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String names = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranSurahNames.java");
        assertFalse("page slider must be gone from Lecture", study.contains("new SeekBar(this)"));
        assertTrue("Lecture must open the shared 114-surah picker dialog",
            study.contains("QuranSurahNames.showPicker(this, geometry, this::setPage)"));
        assertTrue("Mémorisation libre must also open the shared 114-surah picker dialog",
            read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java")
                .contains("QuranSurahNames.showPicker(this,geometry,this::setPage)"));
        assertTrue("the shared picker must list all 114 surahs", names.contains("String[] items = new String[114];"));
        assertTrue("selecting a surah must jump to its first verse's page",
            names.contains("setPage(geometry.pageForVerse(new VerseRef(which + 1, 1)))")
                || names.contains("onPageChosen.accept(geometry.pageForVerse(new com.quransafeguard.hifz.core.VerseRef(which + 1, 1)))"));
        assertTrue(names.contains("static String name(int surah)"));
        assertTrue(names.contains("static String labelFor(int surah)"));
    }

    /**
     * murajaahNextSegmentAfterPage/After used to compare raw page numbers and verse ordinals
     * across segments ("pageForVerse(start) > page", "ordinal(start) > ordinal(end)"). That broke
     * the instant a disjoint acquired corpus wrapped mid-plan (e.g. finishing Juz 30's tail and
     * continuing from Al-Baqara): the wrapped segment's page/ordinal is numerically *lower*, so
     * the comparison silently returned null — no "next segment" button, and finishMurajaah()
     * validated without warning about the unread wrapped passage. Confirmed against real
     * geometry.json with Juz 1 + Juz 15 + Juz 30 as the acquired corpus (see the Python
     * simulation run this session): starting a session at page 604 (An-Nas) produced no jump
     * target under the old logic, and 1:1 under the fixed logic. The fix must key off each
     * segment's own [start,end] bounds / list position, never cross-segment magnitude.
     */
    @Test public void murajaahNextSegmentLookupSurvivesACorpusWrapAroundNotJustAscendingPages() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse("must no longer compare a segment's page against the current page across segments",
            session.contains("if (geometry.pageForVerse(start) > page) return start;"));
        assertFalse("must no longer compare a segment's ordinal against the target ordinal across segments",
            session.contains("if (GeometryRepository.ordinal(start) > GeometryRepository.ordinal(end)) return start;"));
        String segments = method(session,
            "private List<MurajaahSegment> murajaahSegments() {", "private VerseRef murajaahNextSegmentAfterPage(int page) {");
        assertTrue("each segment must record its own start/end page so membership never needs cross-segment comparison",
            segments.contains("geometry.pageForVerse(segmentStart), geometry.pageForVerse(previous)"));
        String afterPage = method(session,
            "private VerseRef murajaahNextSegmentAfterPage(int page) {", "private VerseRef murajaahNextSegmentAfter(VerseRef end) {");
        assertTrue("the current page's segment must be found by its own bounds, not by a global page comparison",
            afterPage.contains("page >= segment.startPage && page <= segment.endPage"));
        assertTrue("the next segment is simply the following entry in traversal order",
            afterPage.contains("return i + 1 < segments.size() ? segments.get(i + 1).start : null;"));
        String afterEnd = method(session,
            "private VerseRef murajaahNextSegmentAfter(VerseRef end) {", "@Override public void onPageSwipe(int delta){goPage(delta);}");
        assertTrue("the tapped endpoint's segment must be found by its own ordinal bounds",
            afterEnd.contains("endOrdinal >= GeometryRepository.ordinal(segment.start)")
                && afterEnd.contains("endOrdinal <= GeometryRepository.ordinal(segment.end)"));
    }

    @Test public void murajaahActionsRefreshOnEveryPageSwipeNotJustAtSessionStart() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("renderMurajaah must delegate its action bar to a helper reusable after a swipe",
            session.contains("restoreMurajaahEndpointSelectionOnCurrentPage();\n        updateMurajaahActions();"));
        String goPage = method(session, "private void goPage(int delta) {", "private void addRoundAction(");
        assertTrue("goPage must refresh the Révision jump button after every swipe, not just at render time",
            goPage.contains("updateMurajaahActions();"));
    }

    /**
     * murajaahCorpus() used to subtract unconsolidatedPromotedRanges() (freshly learned material
     * still awaiting its Stabilisation pass) out of promotedRanges() before exposing it to
     * Révision, per its own doc comment: "Fresh unconsolidated promotions stay hidden." At the
     * current Stabilisation throughput a page can wait most of a year for that pass, with zero
     * exposure in the daily Entretien or the weekly ×5 Révision finale in the meantime — the
     * exact "I'm afraid of forgetting Al-Insan before it's stabilized" gap raised this session.
     * Freshly promoted material must now stay visible to Révision immediately; only the
     * AnchoringQueue/promotion bookkeeping still cares whether it is "consolidated" yet.
     */
    @Test public void murajaahCorpusNoLongerHidesFreshlyLearnedUnconsolidatedMaterial() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String murajaahCorpus = method(prefs,
            "public EligibleCorpus murajaahCorpus() {", "public boolean isItqanCursorValid()");
        assertFalse("must no longer subtract pending promotions out of the Révision corpus",
            murajaahCorpus.contains("subtractCoverage(consolidated"));
        assertTrue("every promoted verse, consolidated or not, must feed the Révision corpus",
            murajaahCorpus.contains("all.addAll(promotedRanges());"));
    }

    /**
     * Sunday evening used to have no Entretien slot at all: HifzSchedule.planFor(SUNDAY) carried a
     * dummy zero-minute SABQI_NEW placeholder for the evening, and MainActivity.computeNextMode's
     * REVISION case returned null the instant the morning ×5 finales were resolved, with no path
     * to the ordinary nightly Murajaah. Sunday evening must now behave like every other evening:
     * a real 30-minute Entretien, gated behind the morning finales and tracked the same way
     * (lastMurajaahDate) so it can't be silently skipped or double-validated.
     */
    @Test public void sundayEveningNowOffersTheOrdinaryEntretien() throws Exception {
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("computeNextMode's REVISION case must fall through to the evening Entretien once both finales resolve",
            main.contains("if(!consolidationFinalResolved(today))return HifzSessionActivity.CONSOLIDATION_FINAL;\n                return eveningRevisionMode(today);"));
        String eveningRevision = method(main,
            "private String eveningRevisionMode(LocalDate today){", "private static boolean isTodayAnchoredMode(String mode){");
        assertTrue("Sunday evening must offer the ordinary Murajaah mode, gated by lastMurajaahDate like every other evening",
            eveningRevision.contains("if(!todayStr.equals(prefs.lastMurajaahDate()))return HifzSessionActivity.MURAJAAH;"));
        assertTrue("cadenceComplete's REVISION case must also require the evening Entretien, not just the morning finales",
            main.contains("return learningFinalResolved(date)&&consolidationFinalResolved(date)\n                    &&date.toString().equals(prefs.lastActiveMurajaahDate())\n                    &&date.toString().equals(prefs.lastMurajaahDate());"));

        String weekly = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue("the weekly dashboard must project a real evening Entretien for Sunday instead of a dash",
            weekly.contains("evening=\"Entretien · \"+projected.label;"));
        assertFalse("the old evening dash must be gone from the REVISION row",
            method(weekly, "case REVISION:{", "default:throw").contains("evening=\"—\";"));
    }

    /**
     * The J10 guard (a 10-day max-review-gap safety net: an interstitial screen, a capacity
     * forecast Toast, Settings status text, dashboard "créneau utilisé" labels) is retired now
     * that the app has a fixed 30-min morning+evening review schedule every day. Its behavioral/
     * UI surface is gone; the underlying schema-6 persistence it shared with the acquired-credit
     * bookkeeping (J10ReviewStore's legacy migration input, J10V6Store, HifzCorpusState's
     * activeJ10LineIds()/validate() invariants) is deliberately left in place — it is inert once
     * unread, and touching it risks live migration/data-integrity issues for existing installs
     * with no functional benefit.
     */
    @Test public void j10GuardSubsystemIsFullyRemovedButMigrationPlumbingSurvives() throws Exception {
        Path root = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview");
        if (!Files.exists(root)) root = Paths.get("..", root.toString());
        for (String removed : new String[]{
            "J10ReviewPlanner.java", "J10ReviewPolicy.java", "J10ReviewActivity.java",
            "J10ReviewObserver.java", "J10ReviewProgress.java", "J10SessionBudget.java",
            "J10HostBudgetStore.java", "QuranHifzApp.java"}) {
            assertFalse("the J10 guard/UI file must be deleted: " + removed, Files.exists(root.resolve(removed)));
        }
        for (String kept : new String[]{"J10ReviewStore.java", "J10V6Store.java"}) {
            assertTrue("legacy migration plumbing must survive: " + kept, Files.exists(root.resolve(kept)));
        }
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertFalse("the retired interstitial must no longer be declared", manifest.contains("J10ReviewActivity"));
        assertFalse("the app no longer needs a custom Application class", manifest.contains("android:name=\".QuranHifzApp\""));
        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertFalse(main.contains("hostBudgetStore"));
        String weekly = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertFalse("the dashboard must no longer report J10-consumed slots", weekly.contains("J10 · créneau utilisé"));
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertFalse(settings.contains("section(root,\"J10\")"));
        assertFalse(settings.contains("J10 · garantie de fraîcheur des passages Acquis"));
    }

    /**
     * Stabilisation is restructured to a hard weekly cadence: three sessions (Tue/Thu/Sat) at
     * 8/7/7 lines (~1.5 pages/week), the only hard rule being surah separation — mirroring
     * Sabqi's existing guaranteed weekly page output. Previously AnchoringQueue entries were
     * page-sized (~15 lines) and consumed in ≤2 blocks with no week alignment at all. Verified
     * against real geometry.json this session: 448 weekly units across the whole corpus, zero
     * gaps, zero duplicate physical lines, zero units spanning more than one surah.
     */
    @Test public void stabilizationEntriesAreNowWeeklySizedNotPageSized() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        assertTrue("a weekly Stabilisation unit builder must exist alongside the page-based one",
            geometry.contains("public VerseUnit eligibleWeeklyStabilizationUnit(VerseRef cursor, VerseRef rangeEnd, EligibleCorpus corpus) {"));
        String weeklyUnit = method(geometry,
            "public VerseUnit eligibleWeeklyStabilizationUnit(VerseRef cursor, VerseRef rangeEnd, EligibleCorpus corpus) {",
            "public EligibleLinePlan planEligibleLines(");
        assertTrue("it must hard-stop at a surah change, never spanning two surahs in one weekly unit",
            weeklyUnit.contains("if (line.verses.get(0).getSurah() != startSurah) break;"));
        assertTrue("it must respect the caller's own pending-range end, never spilling into an unrelated range",
            weeklyUnit.contains("refOrdinal <= rangeEndOrdinal"));
        assertTrue("it must cap at the weekly line target, not the old single-page target",
            weeklyUnit.contains("ids.size() < PreviewConfig.STABILIZATION_WEEKLY_LINES"));

        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the anchoring queue must chunk pending material into weekly units now, not page units",
            prefs.contains("geometry.eligibleWeeklyStabilizationUnit(\n                        cursor, range.getEndInclusive(), pendingCorpus);"));

        String config = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/PreviewConfig.java");
        assertTrue("the weekly line target must be 22 (8+7+7, three sessions/week)",
            config.contains("STABILIZATION_WEEKLY_LINES = 22"));
    }

    /**
     * StabilizationHalfPagePolicy's split now has three tiers instead of two: ≤11 lines stay
     * whole (unchanged), 12-18 lines split two ways exactly as before (unchanged — every real
     * single page is ≤15 lines, so this tier's behavior for today's entries is untouched), and
     * ≥19 lines (only reachable once a weekly unit is ~1.5 pages) split three ways at 8/7/7,
     * using the same clean-verse-boundary preference as the two-way split.
     */
    @Test public void stabilizationPolicyGainsAThreeWaySplitTierWithoutChangingTheTwoWayOne() throws Exception {
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        assertTrue("segments of 12-18 lines must still use the original two-way split, unchanged",
            policy.contains("if (count <= 18) {\n            appendTwoWaySplit(units, lines, page, surah, start, end, count);"));
        assertTrue("segments of 19+ lines must use the new three-way split",
            policy.contains("appendThreeWaySplit(units, lines, page, surah, start, end, count);"));
        String threeWay = method(policy,
            "private static void appendThreeWaySplit(", "private static int preferCleanVerseBoundary(");
        assertTrue("the three-way split must target 8/7/7, mirroring the two-way split's 7.5/7.5 target",
            threeWay.contains("Math.abs(a - 8), Math.max(Math.abs(b - 7), Math.abs(c - 7))"));
        assertTrue("it must reuse the same clean-verse-boundary preference as the two-way split",
            threeWay.contains("preferCleanVerseBoundary(lines, start, count, bestA)"));
        assertTrue("the single-page/15-line ceiling must be gone now that weekly units can span ~1.5 pages",
            policy.contains("2 * PreviewConfig.STABILIZATION_WEEKLY_LINES"));
        assertFalse("the old page-crossing guard must be gone: a weekly unit legitimately spans pages",
            policy.contains("Stabilisation unit may not cross page boundary"));
    }

    /**
     * CorpusLinePolicy.ownedLineIdsForRangeOnPage used to throw if start/end verses weren't on
     * the same physical page. A Stabilisation weekly unit can now run to ~1.5 pages, so every one
     * of its call sites (all Itqan/Anchoring-related) needs the multi-page-capable version.
     */
    @Test public void ownedLineResolutionNoLongerRequiresASinglePhysicalPage() throws Exception {
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/CorpusLinePolicy.java");
        assertFalse("the same-page assertion must be gone", policy.contains("must stay on one Mushaf page"));
        assertFalse("pageForVerse must no longer gate ownership resolution",
            method(policy, "public static List<String> ownedLineIdsForRangeOnPage(", "public static Set<String> touchedLineIds(")
                .contains("geometry.pageForVerse"));
    }

    /**
     * A multi-page Stabilisation unit must render correctly: the swipe range must cover every
     * page the unit touches (unitFirstPage/unitLastPage, mirroring the grouped-cycle snowball's
     * own multi-page fix earlier this session), and the Répétition button's page-snap-back must
     * return to the CURRENT BLOCK's own page, not always the unit's first page — otherwise a
     * block living on the unit's second page could never be seen while repeating it.
     */
    @Test public void multiPageStabilizationUnitsRenderAndSnapBackCorrectly() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("renderItqan must derive unitFirstPage/unitLastPage from the resolved physical lines",
            session.contains("unitFirstPage = physicalLines.get(0).page;")
                && session.contains("unitLastPage = physicalLines.get(physicalLines.size() - 1).page;"));
        assertTrue("the working block's own page must be tracked separately from the whole unit's span",
            session.contains("itqanBlockPage = geometry.linesForExactIds(currentLineIds).get(0).page;"));
        assertTrue("the per-repetition page snap-back must return to the block's own page, not the unit's first page",
            session.contains("if(currentPage!=itqanBlockPage){currentPage=itqanBlockPage;showCurrent();}"));
        assertFalse("the old page-scoped line lookup must no longer be used for the (now multi-page) Itqan unit",
            session.contains("geometry.linesForIdsOnPage"));
        assertFalse("linesForIdsOnPage must be deleted now that nothing calls it",
            read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java")
                .contains("linesForIdsOnPage"));
    }

    /**
     * completeConsolidationSessionV6/completeLearningConsolidationSessionV6 re-verify each frozen
     * Stabilisation block by re-running planPage on just that block's own lines and requiring
     * exactly one whole result — which only holds while the block is ≤11 lines (the "stays whole"
     * tier). The two-way split's own count was always ≤15, so its largest possible half (count-5)
     * could never exceed 10, keeping this invariant for free; the three-way split's target (8/7/7
     * over ~22-30 lines) has no such automatic ceiling and could legitimately produce a 12+ line
     * block without an explicit cap. Confirmed against the whole corpus this session: without the
     * cap, blocks up to size 12 occur; with it, the observed maximum is exactly 11.
     */
    @Test public void threeWaySplitNeverProducesABlockConsolidationCannotReverify() throws Exception {
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        assertTrue("the independently-verifiable ceiling must be named and set to 11",
            policy.contains("private static final int MAX_INDEPENDENTLY_VERIFIABLE_BLOCK = 11;"));
        String threeWay = method(policy,
            "private static void appendThreeWaySplit(", "private static int preferCleanVerseBoundary(");
        assertTrue("the optimizer must only consider combinations where every block stays within the cap",
            threeWay.contains("if (c < 5 || c > cap) continue;"));
        assertTrue("the clean-verse-boundary shift must be discarded if it would push any block over the cap",
            threeWay.contains("if (split2 - split1 < 5 || split1 > cap || split2 - split1 > cap || count - split2 > cap) {"));
    }

    /**
     * split1 and split2 are each independently pulled up to ±2 lines toward their own nearest
     * clean verse boundary. Nothing stopped both shifts from moving *toward* each other — split1
     * forward, split2 back — which could squeeze the middle block down to as little as 1-4 lines
     * even though bestA/bestB were both comfortably ≥5. Confirmed against the whole corpus: 11
     * real weekly units had a middle block of only 3-4 lines under the old guard; the fix (a
     * middle block below the same 5-line floor every block is otherwise guaranteed falls back to
     * the exact target split) brings the corpus-wide minimum back up to 5 everywhere.
     */
    @Test public void threeWaySplitNeverStarvesTheMiddleBlockBelowFiveLines() throws Exception {
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StabilizationHalfPagePolicy.java");
        String threeWay = method(policy,
            "private static void appendThreeWaySplit(", "private static int preferCleanVerseBoundary(");
        assertTrue("the fallback must check the middle block's own size, not just split1 < split2",
            threeWay.contains("if (split2 - split1 < 5"));
        assertFalse("the old crossing-only check must be gone, not just weakened alongside it",
            threeWay.contains("split1 >= split2"));
    }

    @Test public void murajaahJumpButtonHasItsOwnIconDistinctFromPlainPagination() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("the corpus-jump action must not be labelled like a plain pagination control",
            session.contains("\"Passage suivant du corpus\""));
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue("it must resolve to its own icon before the generic pagination fallback",
            ui.contains("if (s.contains(\"passage suivant du corpus\")) return R.drawable.ic_ui_rotation;"));
    }

    /**
     * The weekday-pinned cadence (Mon/Wed/Fri Apprentissage vs Tue/Thu/Sat Stabilisation) used to
     * be hardcoded. Settings can now set Apprentissage's share of the six non-Sunday days anywhere
     * from 0 to 6 (Stabilisation gets the rest) via a configurable split, spread evenly across the
     * week by the same formula at every call site, with Sunday always staying Révision. The 0/6
     * extremes are deliberately allowed: dedicating the whole week to one family simply means the
     * other never comes due that week.
     */
    @Test public void weeklyCadenceSplitBetweenApprentissageAndStabilisationIsConfigurable() throws Exception {
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        assertTrue("the valid range must be named, not a bare magic number at each call site",
            core.contains("const val MIN_LEARNING_DAYS_PER_WEEK = 0")
                && core.contains("const val MAX_LEARNING_DAYS_PER_WEEK = 6")
                && core.contains("const val DEFAULT_LEARNING_DAYS_PER_WEEK = 3"));
        String actionFor = method(core,
            "fun actionFor(day: DayOfWeek, learningDaysPerWeek: Int = DEFAULT_LEARNING_DAYS_PER_WEEK): CadenceAction {",
            "fun nextDue(");
        assertTrue("Sunday must stay the reserved Révision day regardless of the configured split",
            actionFor.contains("if (day == DayOfWeek.SUNDAY) return CadenceAction.REVISION"));
        assertTrue("an out-of-range value must be clamped, never thrown or silently misused",
            actionFor.contains("learningDaysPerWeek.coerceIn(MIN_LEARNING_DAYS_PER_WEEK, MAX_LEARNING_DAYS_PER_WEEK)"));
        assertTrue("the split must use the standard even-distribution formula, not cluster days at the start",
            actionFor.contains("if ((index * n) % 6 < n) CadenceAction.LEARNING else CadenceAction.STABILIZATION"));
        assertTrue("nextDue must thread the configured split through to actionFor, not silently keep the default",
            core.contains("learningDaysPerWeek: Int = DEFAULT_LEARNING_DAYS_PER_WEEK\n    ): ScheduledCadence?")
                && core.contains("action = actionFor(date.dayOfWeek, learningDaysPerWeek)"));

        String engine = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ConsolidationCycleEngine.java");
        assertTrue("the weekly snowball must have headroom for 6 configurable days plus the continuous pass",
            engine.contains("protocol == Protocol.SNOWBALL_FINAL) ? 7 : 3"));

        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("HifzPrefs must expose a clamped read of the configured split",
            prefs.contains("public int learningDaysPerWeek() {"));
        assertTrue("HifzPrefs must reject an out-of-range write rather than silently clamping it",
            prefs.contains("public boolean setLearningDaysPerWeek(int days) {")
                && method(prefs, "public boolean setLearningDaysPerWeek(int days) {", "return p.edit()")
                    .contains("throw new IllegalArgumentException"));
        assertTrue("the weekly snowball's per-family accumulator cap must follow the configured split, not a hardcoded 3",
            prefs.contains("int weeklyCap = family == ConsolidationCycleEngine.Family.LEARNING")
                && prefs.contains("if (current.length() < weeklyCap)"));

        String mainActivity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("the quick-access gate must use the configured split",
            mainActivity.contains("HifzSchedule.INSTANCE.actionFor(HifzClock.today().getDayOfWeek(), prefs.learningDaysPerWeek())"));
        assertTrue("cadence completion checks must use the configured split",
            mainActivity.contains("HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), prefs.learningDaysPerWeek())"));
        assertTrue("the automatic due-task lookup must use the configured split",
            mainActivity.contains("HifzSchedule.INSTANCE.nextDue(prefs.programStartDate(),todayDate,completed,prefs.learningDaysPerWeek())"));

        String dashboard = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue("the seven-day dashboard projection must use the configured split too",
            dashboard.contains("HifzSchedule.INSTANCE.actionFor(date.getDayOfWeek(), prefs.learningDaysPerWeek())"));

        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("Settings must offer a picker for the weekly split",
            settings.contains("private void chooseLearningDaysPerWeek(){")
                && settings.contains(".setSingleChoiceItems(labels,checkedIndex"));
        assertTrue("Settings must persist the chosen value through HifzPrefs",
            settings.contains("prefs.setLearningDaysPerWeek(days)"));
        assertTrue("the Parcours summary must reflect the actual configured split, not a hardcoded Mon/Wed/Fri string",
            settings.contains("private String weeklyCadenceSummary(){"));
    }

    /**
     * At the 0 or 6 extreme, one family has no day at all that week. weeklyCadenceSummary must
     * drop that family's clause entirely rather than printing an empty day list right before
     * "· Apprentissage"/"· Stabilisation" (e.g. " · Apprentissage   ·   Lun/Mar/.../Sam ·
     * Stabilisation   ·   Dim · Révision" with a stray leading separator).
     */
    @Test public void weeklyCadenceSummaryOmitsAnEmptyFamilyClauseAtTheZeroOrSixExtreme() throws Exception {
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String summary = method(settings, "private String weeklyCadenceSummary(){", "private String learningDaysSummary()");
        assertTrue("each family's clause must only be appended when that family actually has a day this week",
            summary.contains("if(!learningDays.isEmpty())")
                && summary.contains("if(!stabilizationDays.isEmpty())"));
    }
}

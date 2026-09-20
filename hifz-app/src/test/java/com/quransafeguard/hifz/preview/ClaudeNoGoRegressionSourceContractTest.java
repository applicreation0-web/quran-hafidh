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
     * The raw 5-line target can land mid-verse, splitting a verse's lines across two Sabqi days —
     * the same class of defect fixed for the Stabilisation half-page split (verified on a real
     * page: Al-Baqarah 2:276 on page 47). Sabqi must apply the same nearby-clean-boundary
     * preference, but never at the cost of crossing into the next surah.
     */
    @Test public void sabqiFiveLineBlockPrefersACleanVerseBoundaryWithoutCrossingASurah() throws Exception {
        String geometry = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/GeometryRepository.java");
        assertTrue("a surah boundary is already a clean stop and must skip the verse-boundary search",
            geometry.contains("int endIndex = naturalEnd == maxIndex"));
        assertTrue("the verse-boundary search must consult preferCleanVerseBoundary",
            geometry.contains("preferCleanVerseBoundary(startLineIndex, startSurah, naturalEnd)"));
        assertTrue("candidates must never cross into a different surah",
            geometry.contains("lines.get(candidate).verses.get(0).getSurah() != startSurah) continue;"));
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
        assertTrue("SNOWBALL/SNOWBALL_FINAL cycles must tolerate a fourth unit for the continuous pass",
            engine.contains("protocol == Protocol.SNOWBALL_FINAL) ? 4 : 3"));
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

    @Test public void sundaysFinalReviewUsesTheSameTenTimesQuotaAsEveryEveningNotALighterFive() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String stabilizationFinal = method(prefs,
            "List<ConsolidationCycleEngine.Unit> stabilizationSnowballFinalUnits(LocalDate today) {", "}");
        assertTrue("Sunday's Stabilisation final review must use the ×10 SNOWBALL protocol",
            stabilizationFinal.contains("ConsolidationCycleEngine.Protocol.SNOWBALL);"));
        String learningFinal = method(prefs,
            "List<ConsolidationCycleEngine.Unit> learningSnowballFinalUnits(LocalDate today) {", "}");
        assertTrue("Sunday's Apprentissage final review must use the ×10 SNOWBALL protocol",
            learningFinal.contains("ConsolidationCycleEngine.Protocol.SNOWBALL);"));
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
        String core = read("hifz-core/src/main/kotlin/com/quransafeguard/hifz/core/HifzCore.kt");
        String planFor = method(core, "fun planFor(day: DayOfWeek): DailyPlan = when (day) {", "fun scheduled(");
        assertTrue("Sunday's evening slot must be the ordinary maintenance Entretien",
            planFor.contains("DayOfWeek.SUNDAY -> DailyPlan(\n            PlannedSession(SessionKind.RECENT_SABQI_REVIEW, 0),\n            PlannedSession(SessionKind.OLD_ITQAN_MURAJAAH, targetMinutesFor(SessionKind.OLD_ITQAN_MURAJAAH))"));

        String main = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MainActivity.java");
        assertTrue("computeNextMode's REVISION case must fall through to the evening Entretien once both finales resolve",
            main.contains("if(!consolidationFinalResolved(today))return HifzSessionActivity.CONSOLIDATION_FINAL;\n                return eveningRevisionMode(today);"));
        String eveningRevision = method(main,
            "private String eveningRevisionMode(LocalDate today){", "private static boolean isTodayAnchoredMode(String mode){");
        assertTrue("Sunday evening must offer the ordinary Murajaah mode, gated by lastMurajaahDate like every other evening",
            eveningRevision.contains("if(!today.toString().equals(prefs.lastMurajaahDate()))return HifzSessionActivity.MURAJAAH;"));
        assertTrue("cadenceComplete's REVISION case must also require the evening Entretien, not just the morning finales",
            main.contains("return learningFinalResolved(date)&&consolidationFinalResolved(date)\n                    &&date.toString().equals(prefs.lastMurajaahDate());"));

        String weekly = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeeklyDashboardPlanner.java");
        assertTrue("the weekly dashboard must project a real evening Entretien for Sunday instead of a dash",
            weekly.contains("evening=\"Entretien · \"+projected.label;"));
        assertFalse("the old evening dash must be gone from the REVISION row",
            method(weekly, "case REVISION:{", "default:throw").contains("evening=\"—\";"));
    }

    @Test public void murajaahJumpButtonHasItsOwnIconDistinctFromPlainPagination() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("the corpus-jump action must not be labelled like a plain pagination control",
            session.contains("\"Passage suivant du corpus\""));
        String ui = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/Ui.java");
        assertTrue("it must resolve to its own icon before the generic pagination fallback",
            ui.contains("if (s.contains(\"passage suivant du corpus\")) return R.drawable.ic_ui_rotation;"));
    }
}

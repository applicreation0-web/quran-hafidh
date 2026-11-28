package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.CadenceAction;
import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class UniversalLearningRouteTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadGeometry() throws Exception {
        Path file = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.isRegularFile(file)) file = Path.of("..").resolve(file);
        geometry = GeometryRepository.fromJson(Files.readString(file));
    }

    @After public void restoreClock() { HifzClock.resetClockForTests(); }

    @Test public void routeSkipsGapsAndHandlesBothEndsOfTheMushaf() {
        var route = UniversalLearningRoute.of(
            UniversalLearningRoute.parse("114:1-114:6, 1:1-1:7"), geometry);
        var first = route.at(0);
        var last = route.at(1);
        assertEquals(0, last.first);
        assertEquals(geometry.lineCount()-1, first.last);
        assertTrue(first.first > last.last + 1);
        assertEquals(first.last-first.first+1 + last.last-last.first+1, route.remaining(0, -1));
        assertNull(route.at(2));
        assertEquals(0, route.remaining(2, last.last+1));
    }

    @Test public void rejectsOverlappingRangesAndCutsThroughACompositeLine() {
        assertThrows(IllegalArgumentException.class, () -> UniversalLearningRoute.of(
            UniversalLearningRoute.parse("1:1-1:7,1:1-1:7"), geometry));
        GeometryRepository.LineMeta composite = null;
        for (int i = 0; i < geometry.lineCount(); i++) {
            if (geometry.line(i).verses.size() > 1) { composite = geometry.line(i); break; }
        }
        assertNotNull(composite);
        var partial = UniversalLearningRoute.parse(composite.verses.get(0).toString());
        assertThrows(IllegalArgumentException.class, () -> UniversalLearningRoute.of(partial, geometry));
    }

    @Test public void physicalAndCanonicalUnitsResolveToExplicitVerseRanges() {
        var pages = UniversalLearningRoute.parse("p604, P1", geometry);
        var route = UniversalLearningRoute.of(pages, geometry);
        assertEquals(604, geometry.line(route.at(0).first).page);
        assertEquals(geometry.lineCount() - 1, route.at(0).last);
        assertEquals(0, route.at(1).first);
        assertEquals(pages.get(1), UniversalLearningRoute.parse("l1–" + (route.at(1).last + 1), geometry).get(0));
        var juz = UniversalLearningRoute.parse("j1-30", geometry);
        assertEquals(UniversalLearningRoute.parse("1:1-114:6"), juz);
        assertEquals(new VerseRef(114,6), UniversalLearningRoute.parse("H60", geometry).get(0).getEndInclusive());
        assertThrows(IllegalArgumentException.class, () -> UniversalLearningRoute.parse("p605", geometry));
        assertThrows(IllegalArgumentException.class, () -> UniversalLearningRoute.parse("h2-1", geometry));
        for (int line = 0; line < geometry.lineCount()-1; line++) {
            var verses = geometry.line(line).verses;
            if (!geometry.line(line+1).verses.contains(verses.get(verses.size()-1))) continue;
            String split = "l1-" + (line+1);
            assertThrows(IllegalArgumentException.class, () -> UniversalLearningRoute.parse(split, geometry));
            return;
        }
        fail("Fixture must contain at least one verse that spans two physical lines");
    }

    @Test public void explicitFirstRunDeclarationNeverManufacturesEarnedLines() {
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putInt("schema", 6).putString("programStartDate", "2026-11-27")
            .putString("v6AcquiredCreditLineIds", "[]").putString("v6StabilizedLineIds", "[]")
            .putString("v6LearnedLineIds", "[]").putBoolean("universalSetupPendingV1", true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertTrue(prefs.configureNewProfile(UniversalLearningRoute.parse("114:1-114:6"),
            UniversalLearningRoute.parse("1:1-1:7"), Collections.emptyList(), false, false, 3, geometry));
        assertFalse(prefs.universalSetupPending());
        assertEquals("[]", raw.getString("v6AcquiredCreditLineIds", ""));
        assertEquals("[]", raw.getString("v6StabilizedLineIds", ""));
        assertEquals("[]", raw.getString("v6LearnedLineIds", ""));
        assertFalse(raw.contains(ItqanRegimeStore.INITIAL_TAIL_COMPLETED));
        assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(1,1)));
        assertFalse(prefs.activeMurajaahCorpus().contains(new VerseRef(114,1)));
        var saved = raw.getAll();
        assertFalse(prefs.configureNewProfile(Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), false, false, 0, geometry));
        assertEquals(saved, raw.getAll());
    }

    @Test public void realSabqiCommitAdvancesToNextConfiguredRangeWithoutCreditingTheGap() {
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putInt("schema", 6).putString("programStartDate", "2026-11-27")
            .putString("v6AcquiredCreditLineIds", "[]").putString("v6StabilizedLineIds", "[]")
            .putString("v6LearnedLineIds", "[]").putString("v6QuarantineLineIds", "[]")
            .putString("v6LegacyPartialAcquiredLineIds", "[]").putBoolean("universalSetupPendingV1", true)
            .putInt("sabqiLineCursor", -1).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertTrue(prefs.configureNewProfile(UniversalLearningRoute.parse("114:1-114:6, 1:1-1:7"),
            Collections.emptyList(), Collections.emptyList(), false, false, 3, geometry));
        var route = prefs.universalLearningRoute(geometry);
        var first = route.at(0);
        assertTrue(first.first > route.at(1).first);
        int ordinal = 0;
        for (int cursor = first.first; cursor <= first.last;) {
            var block = geometry.fiveLineBlock(cursor, first.last);
            assertTrue(MorningProtocolFixture.completeSabqi(prefs, cursor, block.endLineIndex,
                block.endLineIndex + 1, block.lineIds, LocalDate.of(2026,11,27).plusDays(ordinal++).toString(), "test"));
            cursor = block.endLineIndex+1;
        }
        assertEquals(1, prefs.universalLearningRangeIndex());
        assertEquals(route.at(1).first, prefs.sabqiLineCursor());
        assertEquals(first.last-first.first+1, prefs.progressionSnapshotV6().learned.size());
        assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
    }

    @Test public void shorterLearningVolumeDoesNotResizeTheOpenPhysicalLesson() {
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putString("programStartDate","2026-11-27").putBoolean(HifzPrefs.UNIVERSAL_SETUP,true)
            .putInt("sabqiLineCursor",-1).commit();
        HifzPrefs prefs = new HifzPrefs(raw,geometry);
        assertEquals(5,prefs.learningLinesPerSession());
        assertFalse(prefs.setUniversalLearningLines(1));
        assertTrue(prefs.configureNewProfile(UniversalLearningRoute.parse("2:84-2:88"),
            Collections.emptyList(),Collections.emptyList(),false,false,3,3,geometry));
        var range = prefs.universalLearningRoute(geometry).at(0);
        prefs.setSabqiLineCursor(range.first);
        LocalDate date=LocalDate.of(2026,11,27);
        assertEquals(3,prefs.forecastUniversalSabqiBlock(geometry,range.first,range.last).lineIds.size());
        assertEquals(5,prefs.forecastUniversalSabqiLessons(geometry));
        assertEquals(date,prefs.openSabqiLesson(geometry,date));
        var token = prefs.sabqiLessonToken();
        assertTrue(prefs.setUniversalLearningLines(1));
        assertEquals(token,prefs.sabqiLessonToken());
        assertEquals(3,prefs.forecastUniversalSabqiBlock(geometry,range.first,range.last).lineIds.size());
        assertEquals(13,prefs.forecastUniversalSabqiLessons(geometry));
        var opened=prefs.frozenSabqiBlock(geometry,range.last);
        assertTrue(MorningProtocolFixture.completeSabqi(prefs,opened.startLineIndex,opened.endLineIndex,
            opened.endLineIndex+1,opened.lineIds,date.toString(),"synthetic"));
        assertEquals(3,prefs.progressionSnapshotV6().learned.size());
        assertEquals(1,prefs.forecastUniversalSabqiBlock(geometry,prefs.sabqiLineCursor(),range.last).lineIds.size());
        assertFalse(prefs.setUniversalLearningLines(0));
    }

    @Test public void disjointSingleLinesEachRequireTheirOwnForecastLesson() {
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putString("programStartDate","2026-11-27").putBoolean(HifzPrefs.UNIVERSAL_SETUP,true)
            .putInt("sabqiLineCursor",-1).commit();
        HifzPrefs prefs = new HifzPrefs(raw,geometry);
        assertTrue(prefs.configureNewProfile(UniversalLearningRoute.parse("1:1; 1:3-1:4; 2:45; 2:52"),
            Collections.emptyList(),Collections.emptyList(),false,false,1,5,geometry));
        assertEquals(4,prefs.sabqiLinesRemaining(geometry));
        assertEquals(4,prefs.forecastUniversalSabqiLessons(geometry));
    }

    @Test public void configuredMaintenanceCanOpenAndResumeWithoutAnyAnNasLatch() {
        LocalDate day = LocalDate.of(2026, 11, 28);
        HifzClock.setClockForTests(Clock.fixed(day.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putString(HifzPrefs.UNIVERSAL_LEARNING, "[]")
            .putString("programStartDate", day.toString()).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        EligibleCorpus acquired = EligibleCorpus.Companion.of(UniversalLearningRoute.parse("1:1-114:6"));
        JuzMaintenanceStore store = prefs.juzMaintenance();
        assertNull(store.open(day, geometry, acquired));
        assertTrue(prefs.setUniversalItqanDisciplines(true, false));
        assertNull(store.open(day, geometry, acquired));
        assertTrue(prefs.setUniversalItqanDisciplines(false, true));
        JuzMaintenanceStore.Draft first = store.open(day, geometry, acquired);
        assertNotNull(first);
        assertFalse(ItqanRegimeStore.postNasMaintenance(raw));
        assertTrue(prefs.setUniversalItqanDisciplines(false, false));
        assertNull(store.open(day, geometry, acquired));
        assertTrue(prefs.setUniversalItqanDisciplines(true, true));
        assertEquals(first.raw, store.open(day, geometry, acquired).raw);
        var plan = JuzMaintenanceStore.plan(first.juz, geometry, acquired);
        for (int step = 0; step < plan.pages.size() * 2; step++)
            assertTrue(store.advance(store.read(), plan, day));
        assertNull(store.read());
        assertTrue(prefs.universalMaintenanceReviewDue(day));
        var units = prefs.stabilizedConsolidationUnits(day);
        assertEquals(1, units.size());
        assertEquals(ConsolidationCycleEngine.Protocol.POST_NAS_WEEKLY_MASKED, units.get(0).protocol());
        assertFalse(ItqanRegimeStore.postNasMaintenance(raw));
    }

    @Test public void openMaintenanceReviewRetainsItsEvidenceWhenTheNextWeekStarts() {
        LocalDate weekOne = LocalDate.of(2026,11,28);
        LocalDate weekTwo = weekOne.plusWeeks(1);
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putString("programStartDate", "2026-11-27").putBoolean(HifzPrefs.UNIVERSAL_SETUP,true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertTrue(prefs.configureNewProfile(Collections.emptyList(),
            UniversalLearningRoute.parse("1:1-114:6"), Collections.emptyList(), false, true, 0, geometry));
        var first = raw.edit();
        HifzPrefs.appendPostNasMaintainedUnit(raw, first, weekOne, geometry.lineIdsOnPage(1));
        assertTrue(first.commit());
        var units = prefs.stabilizedConsolidationUnits(weekOne);
        assertEquals(1, units.size());
        var family = ConsolidationCycleEngine.Family.STABILIZATION;
        var engine = new ConsolidationCycleEngine();
        String cycleId = "stabilization-" + weekOne;
        var open = engine.openSession(engine.startCycle(cycleId, family, units.get(0)), "synthetic");
        assertTrue(prefs.persistConsolidationSession(open));
        var second = raw.edit();
        HifzPrefs.appendPostNasMaintainedUnit(raw, second, weekTwo, geometry.lineIdsOnPage(2));
        assertTrue(second.commit());
        assertTrue(prefs.universalMaintenanceReviewDue(weekOne));
        var resumed = prefs.restoreConsolidationSession(engine,family,cycleId);
        assertEquals(open.unitIds(),resumed.unitIds());
        var completed = engine.recordRepetition(resumed);
        assertTrue(prefs.persistConsolidationSession(completed));
        assertTrue(prefs.completeSnowballEvening(family,weekOne.toString(),completed));
        assertFalse(prefs.completeSnowballEvening(family,weekOne.toString(),completed));
        assertTrue(prefs.universalMaintenanceReviewDue(weekTwo));
        assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
        assertFalse(ItqanRegimeStore.postNasMaintenance(raw));
    }

    @Test public void declaredAcquisCanAlsoBeAssignedToDeepConsolidationWithoutEarningCredit() {
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putInt("schema", 6).putString("programStartDate", "2026-11-27")
            .putBoolean("universalSetupPendingV1", true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertTrue(prefs.configureNewProfile(Collections.emptyList(),
            UniversalLearningRoute.parse("1:1-114:6"), UniversalLearningRoute.parse("1:1-1:7"),
            true, true, 0, geometry));
        assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(1,1)));
        assertNotNull(prefs.currentAnchoringEntry(geometry));
        assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
        assertFalse(ItqanRegimeStore.postNasMaintenance(raw));
    }

    @Test public void deepConsolidationOutsideHistoricRangesRequiresPhysicalProof() {
        LocalDate date = LocalDate.of(2026,11,28);
        HifzClock.setClockForTests(Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putString("programStartDate", "2026-11-26").putBoolean(HifzPrefs.UNIVERSAL_SETUP,true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        var selected = UniversalLearningRoute.parse("1:1-1:7");
        assertTrue(prefs.configureNewProfile(Collections.emptyList(), Collections.emptyList(), selected,
            true, false, 0, geometry));
        var entry = prefs.currentAnchoringEntry(geometry);
        assertNotNull(entry);
        var start = GeometryRepository.parseVerse(entry.start);
        var end = GeometryRepository.parseVerse(entry.end);
        assertEquals(new VerseRef(1,1), start);
        var plan = StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(
            prefs.ordinaryItqanLineIds(start,end,geometry)));
        var block = plan.get(0);
        var next = prefs.itqanWorkCorpus().nextAnchored(end, prefs.repairedItqanRotationStart());
        boolean finalBlock = plan.size() == 1;
        assertFalse(prefs.completeStabilizationBlockV6(block.lineIds, 1, finalBlock,
            start, end, next, date.toString(), "synthetic", Collections.emptyList()));
        assertTrue(MorningProtocolFixture.completeItqan(prefs, block.lineIds, 1, finalBlock,
            start, end, next, date.toString(), "synthetic", Collections.emptyList()));
        assertEquals(new java.util.LinkedHashSet<>(block.lineIds), prefs.progressionSnapshotV6().stabilized);
        assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
        assertFalse(ItqanRegimeStore.postNasMaintenance(raw));
    }

    @Test public void legacyRangeEditorsCannotAssignFutureLearningToAnotherDiscipline() {
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putInt("schema", 6).putString("programStartDate", "2026-11-27")
            .putBoolean("universalSetupPendingV1", true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        var future = UniversalLearningRoute.parse("114:1-114:6");
        assertTrue(prefs.configureNewProfile(future, Collections.emptyList(),
            Collections.emptyList(), false, false, 2, geometry));
        var before = raw.getAll();
        assertThrows(IllegalArgumentException.class, () -> prefs.setV6AcquiredRanges(future, geometry));
        assertThrows(IllegalArgumentException.class, () -> prefs.setV6StabilizationRanges(future, geometry));
        assertEquals(before, raw.getAll());
    }

    @Test public void explicitWeekdaysApplyNextMondayAndKeepPastWeeksReadable() {
        LocalDate saturday = LocalDate.of(2026, 11, 28);
        HifzClock.setClockForTests(Clock.fixed(saturday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putInt("schema", 6).putString("programStartDate", "2026-11-23")
            .putBoolean("universalSetupPendingV1", true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        var declared = UniversalLearningRoute.parse("1:1-1:7");
        assertTrue(prefs.configureNewProfile(UniversalLearningRoute.parse("114:1-114:6"),
            declared, declared, true, true, 3, geometry));
        var original = new java.util.HashMap<>(raw.getAll());
        LocalDate monday = saturday.plusDays(2);
        assertEquals(monday, prefs.nextLearningWeek());
        assertFalse(prefs.setUniversalLearningWeekdays(31));
        assertTrue(prefs.setUniversalLearningWeekdays((1 << 1) | (1 << 5)));
        var saved = new java.util.HashMap<>(raw.getAll());
        saved.remove(HifzPrefs.UNIVERSAL_LEARNING_WEEKS);
        assertEquals(original, saved);
        assertEquals(3, prefs.learningDaysPerWeek());
        assertEquals(CadenceAction.STABILIZATION, WeeklyProgramPolicy.actionFor(prefs, saturday, 3));
        assertEquals(CadenceAction.REVISION, WeeklyProgramPolicy.actionFor(prefs, saturday.plusDays(1), 3));
        assertEquals(CadenceAction.STABILIZATION, WeeklyProgramPolicy.actionFor(prefs, monday, 3));
        assertEquals(CadenceAction.LEARNING, WeeklyProgramPolicy.actionFor(prefs, monday.plusDays(1), 3));
        assertTrue(prefs.universalMaintenanceDue(monday.plusDays(4)));
        assertFalse(prefs.universalMaintenanceDue(monday.plusDays(5)));

        HifzClock.setClockForTests(Clock.fixed(monday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        HifzPrefs reopened = new HifzPrefs(raw, geometry);
        assertEquals(2, reopened.learningDaysPerWeek());
        assertTrue(reopened.setUniversalLearningWeekdays(1));
        assertEquals(CadenceAction.LEARNING, WeeklyProgramPolicy.actionFor(reopened, monday.plusDays(1), 2));
        assertEquals(CadenceAction.LEARNING, WeeklyProgramPolicy.actionFor(reopened, monday.plusWeeks(1), 2));
        assertEquals(CadenceAction.STABILIZATION, WeeklyProgramPolicy.actionFor(reopened, saturday, 2));
    }

    @Test public void absentItqanDisciplinesProduceNoDebtAndChangesDoNotRewritePastDates() {
        LocalDate monday = LocalDate.of(2026, 11, 23);
        LocalDate tuesday = monday.plusDays(1);
        HifzClock.setClockForTests(Clock.fixed(tuesday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putInt("schema", 6).putString("programStartDate", monday.toString())
            .putBoolean(HifzPrefs.UNIVERSAL_SETUP, true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertTrue(prefs.configureNewProfile(Collections.emptyList(), UniversalLearningRoute.parse("j1", geometry),
            Collections.emptyList(), false, false, 0, geometry));
        HifzBacklog.ActionForDate calendar = date -> WeeklyProgramPolicy.lessonActionFor(prefs, date, 0);
        assertTrue(HifzBacklog.calculate(monday, monday, tuesday, Collections.emptySet(),
            Collections.emptySet(), calendar).isEmpty());

        assertTrue(prefs.setUniversalItqanDisciplines(false, true));
        LocalDate wednesday = tuesday.plusDays(1);
        HifzClock.setClockForTests(Clock.fixed(wednesday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        assertTrue(prefs.setUniversalItqanDisciplines(false, false));
        var late = HifzBacklog.calculate(monday, monday, wednesday, Collections.emptySet(),
            Collections.emptySet(), calendar);
        assertEquals(1, late.size());
        assertEquals(tuesday, late.get(0).date);
    }

    @Test public void maintenanceReceiptStaysOnItsOriginalSlotWhenFinishedAfterMidnight() {
        LocalDate saturday = LocalDate.of(2026, 11, 28);
        HifzClock.setClockForTests(Clock.fixed(saturday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putInt("schema", 6).putString("programStartDate", saturday.toString())
            .putBoolean(HifzPrefs.UNIVERSAL_SETUP, true).commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        var declared = UniversalLearningRoute.parse("j1", geometry);
        assertTrue(prefs.configureNewProfile(Collections.emptyList(), declared, Collections.emptyList(),
            false, true, 0, geometry));
        EligibleCorpus acquired = EligibleCorpus.Companion.of(declared);
        JuzMaintenanceStore store = prefs.juzMaintenance();
        var opened = store.open(saturday, geometry, acquired);
        assertNotNull(opened);
        var plan = JuzMaintenanceStore.plan(opened.juz, geometry, acquired);
        for (int i = 0; i < 2 * plan.pages.size() - 1; i++)
            assertTrue(store.advance(store.read(), plan, saturday));
        var finalPage = store.read();
        LocalDate sunday = saturday.plusDays(1);
        HifzClock.setClockForTests(Clock.fixed(sunday.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        assertEquals(finalPage.raw, store.open(sunday, geometry, acquired).raw);
        assertTrue(store.advance(finalPage, plan, sunday));
        assertEquals(saturday.toString(), prefs.lastItqanDate());
        assertTrue(raw.contains("itqanJuzMaintenanceReceiptV1|" + saturday));
        assertFalse(raw.contains("itqanJuzMaintenanceReceiptV1|" + sunday));
        assertNull(store.open(sunday, geometry, acquired));
        assertFalse(store.advance(finalPage, plan, sunday));
        assertTrue(prefs.progressionSnapshotV6().acquired.isEmpty());
    }

    @Test public void backupValidatesDatedWeekdaysBeforeApplyingThem() throws Exception {
        var values = new org.json.JSONObject()
            .put(HifzPrefs.UNIVERSAL_LEARNING, new org.json.JSONObject().put("t","s").put("v","[]"));
        var weeks = new org.json.JSONObject().put("t","s");
        values.put(HifzPrefs.UNIVERSAL_LEARNING_WEEKS, weeks);
        for (String invalid : java.util.List.of(
                "[{\"from\":\"2026-12-01\",\"days\":11}]",
                "[{\"from\":\"2026-11-30\",\"days\":64}]",
                "[{\"from\":\"2026-11-30\",\"days\":\"10\"}]",
                "[{\"from\":\"2026-11-30\",\"days\":10},{\"from\":\"2026-11-30\",\"days\":12}]")) {
            weeks.put("v", invalid);
            assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        }
        weeks.put("v", "[{\"from\":\"2026-11-30\",\"days\":47}]");
        HifzBackup.validateUniversalProfile(values, geometry);
        HifzClock.setClockForTests(Clock.fixed(
            LocalDate.of(2026, 11, 28).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        values.put(HifzPrefs.UNIVERSAL_DEEP, new org.json.JSONObject().put("t","b").put("v",true));
        values.put(HifzPrefs.UNIVERSAL_MAINTENANCE, new org.json.JSONObject().put("t","b").put("v",true));
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        weeks.put("v", "[{\"from\":\"2026-11-16\",\"days\":63},"
            + "{\"from\":\"2026-11-23\",\"days\":7},{\"from\":\"2026-11-30\",\"days\":10}]");
        HifzBackup.validateUniversalProfile(values, geometry);
    }

    @Test public void backupRejectsMalformedUniversalRangesAndOutOfBoundsCheckpoint() throws Exception {
        var values = new org.json.JSONObject();
        var route = new org.json.JSONObject().put("t", "s").put("v", "not json");
        values.put(HifzPrefs.UNIVERSAL_LEARNING, route);
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        route.put("v", "[{\"start\":\"114:1\",\"end\":\"114:6\"}]");
        HifzBackup.validateUniversalProfile(values, geometry);
        var index = new org.json.JSONObject().put("t", "i").put("v", 2);
        values.put(HifzPrefs.UNIVERSAL_LEARNING_INDEX, index);
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        index.put("v", "0");
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        index.put("v", 0);
        values.put("sabqiLineCursor", new org.json.JSONObject().put("t", "i").put("v", 0));
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        values.remove(HifzPrefs.UNIVERSAL_LEARNING);
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
    }

    @Test public void backupRejectsBadCorporaAndMaintenanceBeforeApplyingThem() throws Exception {
        var values = new org.json.JSONObject()
            .put(HifzPrefs.UNIVERSAL_LEARNING, new org.json.JSONObject().put("t","s").put("v","[]"));
        for (String key : new String[]{"itqanRanges", "promotedRanges", "unconsolidatedPromotedRanges"}) {
            values.put(key, new org.json.JSONObject().put("t","s").put("v","not json"));
            assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
            values.remove(key);
        }
        var draft = new org.json.JSONObject().put("t","s").put("v","not json");
        values.put(JuzMaintenanceStore.DRAFT, draft);
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        var valid = new org.json.JSONObject().put("juz",1).put("opened","2026-11-27")
            .put("phase","READ").put("index",0).put("revealed",new org.json.JSONArray())
            .put("correctionAssisted",false);
        draft.put("v",valid.toString());
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
        values.put("itqanRanges", new org.json.JSONObject().put("t","s")
            .put("v","[{\"start\":\"1:1\",\"end\":\"114:6\"}]"));
        HifzBackup.validateUniversalProfile(values, geometry);
        draft.put("v",valid.put("index",604).toString());
        assertThrows(org.json.JSONException.class, () -> HifzBackup.validateUniversalProfile(values, geometry));
    }

    @Test public void reviewBudgetChangesOnlyFutureSessionsAndKeepsHistoricDefaults() {
        InMemoryPrefs raw = LongHorizonHifzPersistenceStressTest.freshV6Disk();
        raw.edit().putString("itqanRanges", "[{\"start\":\"1:1\",\"end\":\"1:7\"}]")
            .putString("programStartDate", "2026-11-27").commit();
        HifzPrefs prefs = new HifzPrefs(raw, geometry);
        assertFalse(prefs.setUniversalActiveReviewMinutes(30));
        assertEquals(40, prefs.activeReviewMinutes(geometry));
        raw.edit().putBoolean(HifzPrefs.UNIVERSAL_SETUP, true).commit();
        assertTrue(prefs.configureNewProfile(Collections.emptyList(), UniversalLearningRoute.parse("1:1-1:7"),
            Collections.emptyList(), false, false, 0, geometry));
        assertFalse(prefs.setUniversalActiveReviewMinutes(13));
        assertTrue(prefs.setUniversalActiveReviewMinutes(30));
        var date = LocalDate.of(2026,11,28);
        HifzClock.setClockForTests(Clock.fixed(date.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        var slots = prefs.activeReviewSlots();
        var first = new VerseRef(1,1);
        assertTrue(slots.checkpoint(date, ActiveReviewSlots.Slot.MORNING, first, first, 1, 1_000,
            false, prefs.activeReviewMinutes(geometry)));
        assertTrue(prefs.setUniversalActiveReviewMinutes(45));
        assertEquals(30, slots.targetMinutes(date, ActiveReviewSlots.Slot.MORNING, prefs.activeReviewMinutes(geometry)));
        assertEquals(45, slots.targetMinutes(date, ActiveReviewSlots.Slot.EVENING, prefs.activeReviewMinutes(geometry)));
    }
}

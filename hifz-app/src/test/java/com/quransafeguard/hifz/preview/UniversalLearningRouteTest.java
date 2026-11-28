package com.quransafeguard.hifz.preview;

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

    @Test public void configuredMaintenanceCanOpenAndResumeWithoutAnyAnNasLatch() {
        LocalDate day = LocalDate.of(2026, 11, 28);
        HifzClock.setClockForTests(Clock.fixed(day.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
        InMemoryPrefs raw = new InMemoryPrefs();
        raw.edit().putString(HifzPrefs.UNIVERSAL_LEARNING, "[]").commit();
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
}

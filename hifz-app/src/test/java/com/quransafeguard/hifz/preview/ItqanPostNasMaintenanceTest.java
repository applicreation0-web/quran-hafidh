package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The eight tests the latest user decision (ITQĀN FULL ONLY / POST-AN-NĀS) requires, on the real
 * canonical KFQC Madani line geometry and on a real (in-memory) SharedPreferences store.
 */
public final class ItqanPostNasMaintenanceTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadCanonicalGeometry() throws Exception {
        String repoPath = "app/src/main/assets/reader109/geometry.json";
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    // ---- 1. state transition to post-Nas mode occurs once and survives restart ----

    @Test public void postNasTransitionOccursOnceOnFirstNasArrivalAndSurvivesRestart() {
        InMemoryPrefs store = new InMemoryPrefs();
        HifzPrefs prefs = new HifzPrefs(store);
        assertTrue(prefs.saveItqanRotationState(new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(113, 1), false)));
        assertFalse(prefs.itqanPostNasMaintenance());

        assertTrue(prefs.advanceItqanRotationPast(new VerseRef(113, 5)));
        assertFalse("finishing Al-Falaq is not An-Nās", new HifzPrefs(store).itqanPostNasMaintenance());

        assertTrue(prefs.advanceItqanRotationPast(new VerseRef(114, 6)));
        HifzPrefs restarted = new HifzPrefs(store);
        assertTrue("the first An-Nās arrival is persisted synchronously", restarted.itqanPostNasMaintenance());
        assertEquals(Boolean.TRUE, store.disk.get(ItqanRegimeStore.INITIAL_TAIL_COMPLETED));

        // One-way: no later write (stale state, leg flip, reset to 49:1) can turn it back off.
        assertTrue(restarted.saveItqanRotationState(new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, ItqanRotationPolicy.TAIL_START, false)));
        assertTrue(new HifzPrefs(store).itqanPostNasMaintenance());
        assertTrue(new HifzPrefs(store).itqanRotationState().initialTailCompleted);
    }

    @Test public void installsAlreadyPastTheirFirstTailAreAlreadyInMaintenance() {
        InMemoryPrefs legacy = new InMemoryPrefs();
        legacy.disk.put(ItqanRegimeStore.LEGACY_LEG, "FRONT_BAQARA_HUJURAT");
        legacy.disk.put(ItqanRegimeStore.LEGACY_INITIAL_TAIL_COMPLETED, true);
        assertTrue(new HifzPrefs(legacy).itqanPostNasMaintenance());

        InMemoryPrefs p4 = new InMemoryPrefs();
        p4.disk.put(ItqanRegimeStore.LEG, "FRONT_BAQARA_HUJURAT");
        p4.disk.put(ItqanRegimeStore.CURSOR, "2:1");
        p4.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, true);
        assertTrue(new HifzPrefs(p4).itqanPostNasMaintenance());

        assertFalse(new HifzPrefs(new InMemoryPrefs()).itqanPostNasMaintenance());
    }

    // ---- 2. LIGHT cannot be selected for a new session ----

    @Test public void lightCannotBeSelectedForANewSession() {
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, ItqanMaintenancePolicy.protocolForNewSession());
        assertEquals(AnchoringQueue.ItqanProtocol.FULL,
            ItqanMaintenancePolicy.protocolFor(false, AnchoringQueue.ItqanProtocol.LIGHT));

        for (boolean postNas : new boolean[]{false, true}) {
            InMemoryPrefs store = new InMemoryPrefs();
            store.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, postNas);
            store.disk.put(ItqanRegimeStore.CURSOR, "49:1");
            // Even a stale LIGHT stamp and a legacy LIGHT queue entry for the very same range.
            store.disk.put(ItqanRegimeStore.UNIT_PLAN, "49:1|49:5|DEEP_FIRST_PASS|LIGHT");
            store.disk.put("anchoringQueue",
                "[{\"start\":\"49:1\",\"end\":\"49:5\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]");
            ItqanRegimeStore.UnitPlan plan = new HifzPrefs(store).itqanUnitPlan(new AnchoringQueue.Entry(
                "49:1", "49:5", AnchoringQueue.Origin.RECONSTRUCTION, AnchoringQueue.ItqanProtocol.LIGHT, 0));
            assertEquals(AnchoringQueue.ItqanProtocol.FULL, plan.protocol);
            assertEquals(ItqanMaintenancePolicy.regimeFor(postNas), plan.regime);
        }

        for (ItqanMaintenancePolicy.Regime regime : ItqanMaintenancePolicy.Regime.values()) {
            for (ItqanRotationPolicy.Leg leg : ItqanRotationPolicy.Leg.values()) {
                for (AnchoringQueue.Entry unit : HifzPrefs.physicalUnitsInLeg(leg, new VerseRef(49, 1), geometry, regime)) {
                    assertEquals(regime + " " + leg + " " + unit.start, AnchoringQueue.ItqanProtocol.FULL, unit.protocol);
                }
            }
        }
    }

    // ---- 3. legacy LIGHT state migration cannot lose persisted progress ----

    @Test public void legacyLightUnitMidRepetitionFinishesUnderLightWithoutLosingRepetitions() {
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("itqanRep", 22);
        store.disk.put("itqanAssisted", 1);
        store.disk.put("itqanFinalReveals", 0);
        store.disk.put("itqanBlockIndex", 1);
        store.disk.put("itqanUnitStart", "49:1");
        store.disk.put("itqanUnitEnd", "49:13");
        store.disk.put("anchoringQueue", "[]");
        // Already flagged post-An-Nās by the old leg flip: must still NOT re-scale this open unit.
        store.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, true);
        store.disk.put(ItqanRegimeStore.CURSOR, "49:1");
        Map<String, Object> before = new LinkedHashMap<>(store.disk);

        HifzPrefs prefs = new HifzPrefs(store);
        AnchoringQueue.Entry entry = new AnchoringQueue.Entry("49:1", "49:13",
            AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0);
        ItqanRegimeStore.UnitPlan plan = prefs.itqanUnitPlan(entry);
        assertEquals("pre-upgrade TAIL units were LIGHT: finish it as LIGHT", AnchoringQueue.ItqanProtocol.LIGHT, plan.protocol);
        assertEquals(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, plan.regime);
        assertEquals(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, ItqanRegimeStore.selectionRegime(store));
        assertTrue(prefs.stampItqanUnitPlan(entry, plan));

        for (Map.Entry<String, Object> kept : before.entrySet()) {
            assertEquals("persisted progress untouched: " + kept.getKey(), kept.getValue(), store.disk.get(kept.getKey()));
        }
        assertEquals(35, ItqanMaintenancePolicy.totalReps(plan.regime, plan.protocol));
        assertEquals(PreviewConfig.itqanMaskForNextRep(22, AnchoringQueue.ItqanProtocol.LIGHT),
            ItqanMaintenancePolicy.maskForNextRep(plan.regime, plan.protocol, 22));

        ItqanRegimeStore.UnitPlan afterRestart = new HifzPrefs(store).itqanUnitPlan(entry);
        assertEquals(AnchoringQueue.ItqanProtocol.LIGHT, afterRestart.protocol);
        assertEquals(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, afterRestart.regime);

        // Once that unit is done, the very same range is never LIGHT again.
        store.disk.put("itqanRep", 0);
        store.disk.put("itqanBlockIndex", 0);
        store.disk.put("itqanUnitStart", "");
        store.disk.put("itqanUnitEnd", "");
        ItqanRegimeStore.UnitPlan next = new HifzPrefs(store).itqanUnitPlan(entry);
        assertEquals(AnchoringQueue.ItqanProtocol.FULL, next.protocol);
        assertEquals(ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE, next.regime);
    }

    @Test public void legacyQueuedLightAndQueuedFullAreBothHonouredOnlyForTheOpenUnit() {
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("itqanRep", 3);
        store.disk.put("itqanUnitStart", "2:1");
        store.disk.put("itqanUnitEnd", "2:16");
        store.disk.put("anchoringQueue",
            "[{\"start\":\"2:1\",\"end\":\"2:16\",\"origin\":\"RECONSTRUCTION\",\"protocol\":\"LIGHT\",\"failures\":0}]");
        AnchoringQueue.Entry entry = new AnchoringQueue.Entry("2:1", "2:16",
            AnchoringQueue.Origin.PROMOTED, AnchoringQueue.ItqanProtocol.FULL, 0);
        assertEquals(AnchoringQueue.ItqanProtocol.LIGHT, new HifzPrefs(store).itqanUnitPlan(entry).protocol);

        store.disk.put("anchoringQueue", "[]");
        assertEquals("FRONT units were FULL before the upgrade",
            AnchoringQueue.ItqanProtocol.FULL, new HifzPrefs(store).itqanUnitPlan(entry).protocol);
    }

    // ---- 4. post-Nas unit never exceeds 15 physical lines or the Sabqi frontier ----

    @Test public void postNasTailUnitsAreAtMostFifteenOwnedLinesAndTileTheLegExactly() {
        List<AnchoringQueue.Entry> units = HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,
            new VerseRef(2, 1), geometry, ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE);
        assertEquals("49:1", units.get(0).start);
        assertEquals("114:6", units.get(units.size() - 1).end);
        assertTilesExactly(units, new VerseRef(49, 1), new VerseRef(114, 6));
        int fifteen = 0;
        for (AnchoringQueue.Entry unit : units) {
            List<String> owned = owned(unit);
            assertTrue(unit.start + "→" + unit.end + " has " + owned.size() + " lines",
                owned.size() <= ItqanMaintenancePolicy.MAX_LINES);
            assertSingleSurah(owned);
            if (owned.size() >= 13) fifteen++;
        }
        assertTrue("long surahs must actually use the 15-line target, not stay at half pages", fifteen > 20);
    }

    @Test public void postNasFrontUnitsNeverReachSabqisCurrentBlockOnAnyFrontierLine() {
        int[] probes = {geometry.firstLineIndex(new VerseRef(2, 75)), geometry.firstLineIndex(new VerseRef(2, 255)),
            geometry.firstLineIndex(new VerseRef(3, 1)), geometry.firstLineIndex(new VerseRef(4, 176)),
            geometry.firstLineIndex(new VerseRef(18, 1)), geometry.firstLineIndex(new VerseRef(36, 12))};
        for (int frontierLine : probes) {
            for (int shift = 0; shift < 4; shift++) {
                int line = frontierLine + shift;
                VerseRef sabqiBlockStart = geometry.fiveLineBlock(line).startVerse;
                List<AnchoringQueue.Entry> units = HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT,
                    sabqiBlockStart, geometry, ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE);
                assertFalse(units.isEmpty());
                for (AnchoringQueue.Entry unit : units) {
                    List<String> owned = owned(unit);
                    assertTrue(owned.size() <= ItqanMaintenancePolicy.MAX_LINES);
                    assertTrue(unit.end + " must stay before Sabqi's block " + sabqiBlockStart,
                        GeometryRepository.ordinal(GeometryRepository.parseVerse(unit.end))
                            < GeometryRepository.ordinal(sabqiBlockStart));
                    for (GeometryRepository.LineMeta meta : geometry.linesForExactIds(owned)) {
                        assertTrue("line " + meta.id + " is at/after Sabqi's current line " + line, meta.globalIndex < line);
                    }
                }
                assertTilesExactly(units, ItqanRotationPolicy.FRONT_START, ItqanRotationPolicy.frontLegEnd(sabqiBlockStart));
            }
        }
        assertTrue("nothing behind Sabqi yet: FRONT is empty", HifzPrefs.physicalUnitsInLeg(
            ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, new VerseRef(2, 1), geometry,
            ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE).isEmpty());
        assertEquals(new VerseRef(48, 29), ItqanRotationPolicy.frontLegEnd(new VerseRef(49, 1)));
    }

    @Test public void deepFirstPassSizingIsUnchanged() {
        List<AnchoringQueue.Entry> deep = HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,
            new VerseRef(2, 1), geometry, ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS);
        boolean anyLongerThanFifteen = false;
        for (AnchoringQueue.Entry unit : deep) {
            assertEquals(geometry.eligibleWeeklyStabilizationUnit(GeometryRepository.parseVerse(unit.start),
                ItqanRotationPolicy.TAIL_END, com.quransafeguard.hifz.core.EligibleCorpus.Companion.of(
                    Collections.singletonList(new com.quransafeguard.hifz.core.VerseRange(
                        ItqanRotationPolicy.TAIL_START, ItqanRotationPolicy.TAIL_END)))).end.toString(), unit.end);
            anyLongerThanFifteen |= owned(unit).size() > 15;
        }
        assertTrue("phase 1 keeps its ~22-line weekly units (8/7/7)", anyLongerThanFifteen);
    }

    // ---- 5. reaching the Sabqi frontier flips the next tour to 49:1 without moving Sabqi ----

    @Test public void reachingTheSabqiFrontierRestartsAtHujuratAndNeverTouchesSabqi() {
        int sabqiLine = geometry.firstLineIndex(new VerseRef(2, 120));
        VerseRef sabqiBlockStart = geometry.fiveLineBlock(sabqiLine).startVerse;
        ItqanRotationPolicy.State state = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, ItqanRotationPolicy.FRONT_START, true);
        List<String> visited = new ArrayList<>();
        for (int guard = 0; guard < 100; guard++) {
            ItqanRotationPolicy.Pick pick = ItqanRotationPolicy.pick(state, leg -> HifzPrefs.physicalUnitsInLeg(
                leg, sabqiBlockStart, geometry, ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE));
            assertNotNull(pick.unit);
            if (pick.state.leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS) {
                assertEquals("next tour restarts at Al-Hujurāt", "49:1", pick.unit.start);
                assertEquals(ItqanRotationPolicy.TAIL_START, pick.state.cursor);
                assertTrue(pick.state.initialTailCompleted);
                assertEquals("FRONT walked exactly up to the frontier",
                    ItqanRotationPolicy.frontLegEnd(sabqiBlockStart).toString(), visited.get(visited.size() - 1));
                return;
            }
            visited.add(pick.unit.end);
            state = ItqanRotationPolicy.advancedPast(pick.state, GeometryRepository.parseVerse(pick.unit.end));
        }
        throw new AssertionError("FRONT never flipped back to TAIL");
    }

    @Test public void currentAnchoringEntryFlipsToHujuratAndLeavesEverySabqiKeyByteIdentical() {
        InMemoryPrefs store = new InMemoryPrefs();
        int sabqiLine = geometry.firstLineIndex(new VerseRef(2, 120));
        store.disk.put("sabqiLineCursor", sabqiLine);
        store.disk.put("sabqiStart", "2:75");
        store.disk.put("sabqiEnd", "2:286");
        store.disk.put("sabqiRep", 4);
        store.disk.put("anchoringQueueInitialized", true);
        store.disk.put("anchoringQueue", "[]");
        store.disk.put("itqanRanges", "[]");
        store.disk.put(ItqanRegimeStore.LEG, "FRONT_BAQARA_HUJURAT");
        store.disk.put(ItqanRegimeStore.INITIAL_TAIL_COMPLETED, true);
        // Cursor just past the frontier's own last Itqān unit: FRONT is exhausted.
        VerseRef frontEnd = ItqanRotationPolicy.frontLegEnd(geometry.fiveLineBlock(sabqiLine).startVerse);
        store.disk.put(ItqanRegimeStore.CURSOR, com.quransafeguard.hifz.core.QuranCanon.INSTANCE.next(frontEnd).toString());
        Map<String, Object> sabqiBefore = sabqiKeys(store);

        AnchoringQueue.Entry selected = new HifzPrefs(store).currentAnchoringEntry(geometry);
        assertEquals("49:1", selected.start);
        assertEquals("TAIL_HUJURAT_NAS", store.disk.get(ItqanRegimeStore.LEG));
        assertEquals("49:1", store.disk.get(ItqanRegimeStore.CURSOR));
        assertEquals(Boolean.TRUE, store.disk.get(ItqanRegimeStore.INITIAL_TAIL_COMPLETED));
        assertTrue("a post-Nas TAIL unit is ≤15 lines", owned(selected).size() <= 15);
        assertEquals(sabqiBefore, sabqiKeys(store));

        // Completing that unit and advancing the rotation still never writes a Sabqi key.
        HifzPrefs prefs = new HifzPrefs(store);
        seedSchema6(store, Collections.emptyList(), Collections.emptyList(), owned(selected));
        assertTrue(prefs.completeItqanMaintenanceUnitV6(owned(selected), GeometryRepository.parseVerse(selected.start),
            GeometryRepository.parseVerse(selected.end), null, "2026-10-04", "test", geometry));
        assertTrue(prefs.advanceItqanRotationPast(GeometryRepository.parseVerse(selected.end)));
        assertEquals(sabqiBefore, sabqiKeys(store));
    }

    @Test public void legPlansArePersistedReusedAndRecomputedOnlyWhenTheFrontierMoves() {
        InMemoryPrefs store = new InMemoryPrefs();
        HifzPrefs prefs = new HifzPrefs(store);
        ItqanMaintenancePolicy.Regime m = ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE;
        VerseRef sabqi = geometry.fiveLineBlock(geometry.firstLineIndex(new VerseRef(3, 1))).startVerse;
        List<AnchoringQueue.Entry> fresh = prefs.cachedUnitsInLeg(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT,
            sabqi, geometry, m, java.util.Collections.emptySet());
        String saved = (String) store.disk.get(HifzPrefs.LEG_PLAN_PREFIX + "FRONT_BAQARA_HUJURAT");
        assertNotNull(saved);
        List<AnchoringQueue.Entry> reused = new HifzPrefs(store).cachedUnitsInLeg(
            ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, sabqi, geometry, m, java.util.Collections.emptySet());
        assertEquals(ranges(fresh), ranges(reused));
        assertEquals("reuse never rewrites the plan", saved, store.disk.get(HifzPrefs.LEG_PLAN_PREFIX + "FRONT_BAQARA_HUJURAT"));

        VerseRef moved = geometry.fiveLineBlock(geometry.firstLineIndex(new VerseRef(3, 30))).startVerse;
        List<AnchoringQueue.Entry> longer = prefs.cachedUnitsInLeg(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT,
            moved, geometry, m, java.util.Collections.emptySet());
        assertEquals(ranges(HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, moved, geometry, m)),
            ranges(longer));
        assertTrue(longer.size() > fresh.size());

        store.disk.put(HifzPrefs.LEG_PLAN_PREFIX + "FRONT_BAQARA_HUJURAT",
            ((String) store.disk.get(HifzPrefs.LEG_PLAN_PREFIX + "FRONT_BAQARA_HUJURAT")).replaceFirst("\n.*", "\ngarbage line"));
        assertEquals("a corrupt plan is recomputed, never trusted", ranges(longer), ranges(prefs.cachedUnitsInLeg(
            ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT, moved, geometry, m, java.util.Collections.emptySet())));
    }

    private static List<String> ranges(List<AnchoringQueue.Entry> units) {
        List<String> out = new ArrayList<>();
        for (AnchoringQueue.Entry unit : units) out.add(unit.start + "-" + unit.end);
        return out;
    }

    @Test public void homeStabilisationCardFollowsTheRegime() {
        assertEquals("22 lignes/semaine", Ui.stabilizationCue(false));
        assertEquals("15 lignes · entretien", Ui.stabilizationCue(true));
    }

    // ---- 6. post-Nas FULL target is exactly 20 reps = 10 visible + 10 anchors ----

    @Test public void postNasTargetIsExactlyTwentyRepsTenVisibleThenTenAnchored() {
        ItqanMaintenancePolicy.Regime m = ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE;
        assertEquals("FULL ×40 halved, rounded up", (PreviewConfig.ITQAN_TOTAL_REPS + 1) / 2,
            ItqanMaintenancePolicy.totalReps(m, AnchoringQueue.ItqanProtocol.FULL));
        assertEquals(20, ItqanMaintenancePolicy.TOTAL_REPS);
        assertEquals(10, ItqanMaintenancePolicy.VISIBLE_REPS);
        assertEquals(10, ItqanMaintenancePolicy.ANCHOR_REPS);
        int visible = 0, anchored = 0;
        for (int completed = 0; completed < 20; completed++) {
            int mask = ItqanMaintenancePolicy.maskForNextRep(m, AnchoringQueue.ItqanProtocol.FULL, completed);
            boolean anchor = ItqanMaintenancePolicy.anchoredRecallRep(m, completed);
            if (completed < 10) { assertEquals(0, mask); assertFalse(anchor); visible++; }
            else { assertEquals(100, mask); assertTrue(anchor); anchored++; }
        }
        assertEquals(10, visible);
        assertEquals(10, anchored);
        assertFalse(ItqanMaintenancePolicy.anchoredRecallRep(m, 20));
        assertTrue(ItqanMaintenancePolicy.isValidationRep(m, AnchoringQueue.ItqanProtocol.FULL, 18));
        assertTrue(ItqanMaintenancePolicy.isValidationRep(m, AnchoringQueue.ItqanProtocol.FULL, 19));
        assertFalse(ItqanMaintenancePolicy.isValidationRep(m, AnchoringQueue.ItqanProtocol.FULL, 17));
    }

    // ---- 7. no progressive mask after first Nas ----

    @Test public void noProgressiveEraserAfterTheFirstNasButPhaseOneKeepsIt() {
        LinkedHashSet<Integer> maintenanceMasks = new LinkedHashSet<>();
        LinkedHashSet<Integer> deepMasks = new LinkedHashSet<>();
        for (int completed = -1; completed <= 45; completed++) {
            maintenanceMasks.add(ItqanMaintenancePolicy.maskForNextRep(
                ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE, AnchoringQueue.ItqanProtocol.FULL, completed));
            deepMasks.add(ItqanMaintenancePolicy.maskForNextRep(
                ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, AnchoringQueue.ItqanProtocol.FULL, completed));
        }
        assertEquals(new LinkedHashSet<>(Arrays.asList(0, 100)), maintenanceMasks);
        assertTrue(deepMasks.containsAll(Arrays.asList(0, 25, 50, 75, 100)));
        assertEquals(40, ItqanMaintenancePolicy.totalReps(
            ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS, AnchoringQueue.ItqanProtocol.FULL));
    }

    // ---- 8. reinforcement lap cannot mutate Sabqi, duplicate V6 credit, or duplicate the snowball ----

    @Test public void reinforcementLapChangesNoProgressionNoSnowballNoSabqi() throws Exception {
        AnchoringQueue.Entry unit = firstMaintenanceUnit(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS);
        List<String> lines = owned(unit);
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("sabqiLineCursor", 1234);
        store.disk.put("sabqiStart", "2:75");
        List<String> stabilized = lines.subList(0, lines.size() / 2);
        List<String> acquired = lines.subList(lines.size() / 2, lines.size());
        seedSchema6(store, Collections.emptyList(), stabilized, acquired);
        Map<String, Object> before = new LinkedHashMap<>(store.disk);

        HifzPrefs prefs = new HifzPrefs(store);
        for (int lap = 0; lap < 2; lap++) {
            assertTrue(prefs.completeItqanMaintenanceUnitV6(lines, GeometryRepository.parseVerse(unit.start),
                GeometryRepository.parseVerse(unit.end), null, "2026-10-0" + (4 + lap), "lap", geometry));
        }
        for (String key : new String[]{"v6LearnedLineIds", "v6StabilizedLineIds", "v6AcquiredCreditLineIds"}) {
            assertEquals(key, before.get(key), store.disk.get(key));
        }
        assertNull("no Consolidation snowball entry for reinforcement", store.disk.get("stabilizationSnowballUnitIds"));
        assertNull(store.disk.get("stabilizationSnowball8WeekHistory"));
        assertEquals(sabqiKeys(before), sabqiKeys(store));
    }

    @Test public void newMaterialIsCreditedOnceInVerifiableBlocksAndApprisIsNeverTaken() throws Exception {
        AnchoringQueue.Entry unit = null;
        for (AnchoringQueue.Entry candidate : HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,
                new VerseRef(2, 1), geometry, ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE)) {
            if (owned(candidate).size() >= 14) { unit = candidate; break; }
        }
        assertNotNull(unit);
        List<String> lines = owned(unit);
        List<String> learned = lines.subList(0, 1);
        List<String> acquired = lines.subList(1, 2);
        List<String> fresh = lines.subList(2, lines.size());
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("sabqiLineCursor", 99);
        seedSchema6(store, learned, Collections.emptyList(), acquired);
        Map<String, Object> sabqiBefore = sabqiKeys(store);

        HifzPrefs prefs = new HifzPrefs(store);
        assertTrue(prefs.completeItqanMaintenanceUnitV6(lines, GeometryRepository.parseVerse(unit.start),
            GeometryRepository.parseVerse(unit.end), null, "2026-10-04", "first", geometry));
        assertEquals("only never-credited lines become Stabilisé", fresh, jsonList(store, "v6StabilizedLineIds"));
        assertEquals("Appris stays in its Apprentissage chain", learned, jsonList(store, "v6LearnedLineIds"));
        assertEquals(acquired, jsonList(store, "v6AcquiredCreditLineIds"));

        String history = (String) store.disk.get("stabilizationSnowball8WeekHistory");
        JSONArray weeks = new JSONArray(history);
        JSONArray units = weeks.getJSONObject(0).getJSONArray("units");
        List<String> enrolled = new ArrayList<>();
        for (int i = 0; i < units.length(); i++) {
            List<String> ids = ConsolidationPhysicalUnitPolicy.decodeLineUnit(units.getString(i));
            List<StabilizationHalfPagePolicy.Unit> verified = StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(ids));
            assertEquals("Consolidation must re-verify each enrolled unit as one block", 1, verified.size());
            assertEquals(ids, verified.get(0).lineIds);
            assertTrue(ids.size() <= 11);
            enrolled.addAll(ids);
        }
        assertEquals("exactly the new lines are enrolled, once", fresh, enrolled);
        String unitsAfterFirst = (String) store.disk.get("stabilizationSnowballUnitIds");

        // The next lap over the same unit is pure reinforcement: nothing credited or enrolled again.
        assertTrue(prefs.completeItqanMaintenanceUnitV6(lines, GeometryRepository.parseVerse(unit.start),
            GeometryRepository.parseVerse(unit.end), null, "2026-10-04", "second", geometry));
        assertEquals(fresh, jsonList(store, "v6StabilizedLineIds"));
        assertEquals(history, store.disk.get("stabilizationSnowball8WeekHistory"));
        assertEquals(unitsAfterFirst, store.disk.get("stabilizationSnowballUnitIds"));
        assertEquals(sabqiBefore, sabqiKeys(store));
    }

    @Test public void maintenanceRefusesAnOversizedUnit() {
        InMemoryPrefs store = new InMemoryPrefs();
        seedSchema6(store, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        List<String> sixteen = new ArrayList<>();
        for (int i = 0; i < 16; i++) sixteen.add(geometry.line(i).id);
        try {
            new HifzPrefs(store).completeItqanMaintenanceUnitV6(sixteen, new VerseRef(1, 1), new VerseRef(2, 5),
                null, "2026-10-04", "x", geometry);
            throw new AssertionError("16 lines must be refused");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("15"));
        }
    }

    // ---- helpers ----

    private static AnchoringQueue.Entry firstMaintenanceUnit(ItqanRotationPolicy.Leg leg) {
        return HifzPrefs.physicalUnitsInLeg(leg, new VerseRef(49, 1), geometry,
            ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE).get(0);
    }

    private static List<String> owned(AnchoringQueue.Entry unit) {
        return CorpusLinePolicy.ownedLineIdsForRangeOnPage(GeometryRepository.parseVerse(unit.start),
            GeometryRepository.parseVerse(unit.end), geometry);
    }

    private static void assertSingleSurah(List<String> owned) {
        int surah = -1;
        for (GeometryRepository.LineMeta line : geometry.linesForExactIds(owned)) {
            int s = line.verses.get(0).getSurah();
            if (surah < 0) surah = s;
            assertEquals("a unit never crosses a surah", surah, s);
        }
    }

    /** Units cover [start, end]'s owned lines exactly once each, in order, with no gap. */
    private static void assertTilesExactly(List<AnchoringQueue.Entry> units, VerseRef start, VerseRef end) {
        List<String> all = new ArrayList<>();
        VerseRef expectedStart = start;
        for (AnchoringQueue.Entry unit : units) {
            assertEquals("units are contiguous", expectedStart.toString(), unit.start);
            all.addAll(owned(unit));
            expectedStart = com.quransafeguard.hifz.core.QuranCanon.INSTANCE.next(GeometryRepository.parseVerse(unit.end));
        }
        assertEquals(units.get(units.size() - 1).end, end.toString());
        assertEquals(CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end, geometry), all);
    }

    private static Map<String, Object> sabqiKeys(InMemoryPrefs store) { return sabqiKeys(store.disk); }

    private static Map<String, Object> sabqiKeys(Map<String, Object> disk) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : disk.entrySet()) {
            if (e.getKey().toLowerCase(java.util.Locale.ROOT).contains("sabqi")
                    || e.getKey().startsWith("learningSnowball")) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    private static void seedSchema6(InMemoryPrefs store, List<String> learned, List<String> stabilized, List<String> acquired) {
        store.disk.put("schema", 6);
        store.disk.put("v6LearnedLineIds", new JSONArray(learned).toString());
        store.disk.put("v6StabilizedLineIds", new JSONArray(stabilized).toString());
        store.disk.put("v6AcquiredCreditLineIds", new JSONArray(acquired).toString());
        store.disk.put("v6QuarantineLineIds", "[]");
        store.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
    }

    private static List<String> jsonList(InMemoryPrefs store, String key) throws Exception {
        JSONArray array = new JSONArray((String) store.disk.get(key));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) out.add(array.getString(i));
        return out;
    }
}

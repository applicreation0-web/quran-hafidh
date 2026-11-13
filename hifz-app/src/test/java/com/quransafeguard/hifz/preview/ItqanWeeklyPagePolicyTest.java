package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ItqanWeeklyPagePolicyTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadGeometry() throws Exception {
        Path file = Path.of("app/src/main/assets/reader109/geometry.json");
        if (!Files.exists(file)) file = Path.of("..").resolve(file);
        geometry = GeometryRepository.fromJson(Files.readString(file));
    }

    @Test public void page528StartsAtTheTopAndSplitsNajmFromQamar() {
        List<String> lines = geometry.lineIdsOnPage(528);
        ItqanFourSessionPolicy.Plan plan = plan(lines);
        assertEquals(List.of(4, 5, 4), counts(plan));
        assertEquals(lines.subList(0, 4), plan.sessions.get(0).blocks.get(0).lineIds);
        assertEquals(lines.subList(4, 9), plan.sessions.get(1).blocks.get(0).lineIds);
        assertEquals(lines.subList(9, 13), plan.sessions.get(2).blocks.get(0).lineIds);
        assertEquals(53, plan.sessions.get(0).blocks.get(0).surah);
        assertEquals(53, plan.sessions.get(1).blocks.get(0).surah);
        assertEquals(54, plan.sessions.get(2).blocks.get(0).surah);
        assertEquals(new VerseRef(54, 7), geometry.linesForExactIds(
            List.of("529:0")).get(0).verses.get(0));
    }

    @Test public void boundedRemainderAndShortPageNeverBorrowNextPage() {
        List<String> page527 = geometry.lineIdsOnPage(527);
        assertEquals(List.of(5, 0, 0), counts(plan(page527.subList(10, 15))));
        assertEquals(List.of(5, 5, 5), counts(plan(page527)));
        assertEquals(List.of(5, 5, 2), counts(plan(page527.subList(0, 12))));
        assertEquals(List.of(5, 3, 0), counts(plan(page527.subList(0, 8))));
        assertEquals(List.of(2, 0, 0), counts(plan(page527.subList(0, 2))));
        assertNull(ItqanFourSessionPolicy.weeklyPage(
            geometry, geometry.linesForExactIds(page527).get(0).globalIndex, Set.of()));
        Set<String> alsoNextPage = new LinkedHashSet<>(page527.subList(10, 15));
        alsoNextPage.add("528:0");
        assertEquals(5, ItqanFourSessionPolicy.weeklyPage(
            geometry, geometry.linesForExactIds(page527).get(10).globalIndex, alsoNextPage).lines());
    }

    @Test public void entireCorpusUsesEachRealLineExactlyOnceAcrossSurahLocalBlocks() {
        for (int page = 1; page <= 604; page++) {
            List<String> ids = geometry.lineIdsOnPage(page);
            if (page <= 2 || ids.isEmpty()) continue;
            ItqanFourSessionPolicy.Plan plan = plan(ids);
            ArrayList<String> seen = new ArrayList<>();
            for (ItqanFourSessionPolicy.Session session : plan.sessions) {
                assertTrue(session.lineCount <= 5);
                for (ItqanFourSessionPolicy.Block block : session.blocks) {
                    for (GeometryRepository.LineMeta line : geometry.linesForExactIds(block.lineIds)) {
                        assertTrue(line.verses.stream().allMatch(v -> v.getSurah() == block.surah));
                    }
                    seen.addAll(block.lineIds);
                }
            }
            assertEquals("Page " + page, ids, seen);
            assertEquals(seen.size(), new HashSet<>(seen).size());
        }
    }

    @Test public void specialPagesRequireVerifiedGeometryBeforePlanning() {
        for (int page = 1; page <= 2; page++) {
            List<String> ids = geometry.lineIdsOnPage(page);
            assertThrows(IllegalStateException.class, () -> plan(ids));
        }
    }

    @Test public void weeklySnapshotResumesAt39AndLegacyEncodingRemainsV1() throws Exception {
        List<String> lines = geometry.lineIdsOnPage(528);
        ItqanRotationPolicy.State rotation = new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, new VerseRef(53, 45), false);
        LocalDate monday = LocalDate.of(2026, 11, 16);
        ItqanFourSessionSnapshot weekly = ItqanFourSessionSnapshot.freeze(
            plan(lines), rotation, monday, geometry).withProgress(1, 0, 39, geometry);
        ItqanFourSessionSnapshot resumed = ItqanFourSessionSnapshot.decodeWeeklyPage(weekly.encode(), geometry);
        assertEquals(2, new JSONObject(resumed.encode()).getInt("version"));
        assertEquals(39, resumed.reps);
        assertEquals(1, resumed.sessionIndex);
        assertEquals(List.of(4, 5, 4), counts(resumed.plan));
        assertEquals(weekly.encode(), resumed.encode());
        assertThrows(IllegalStateException.class, () -> ItqanFourSessionSnapshot.decode(
            weekly.encode(), geometry));
        ItqanFourSessionSnapshot legacy = ItqanFourSessionSnapshot.freeze(
            ItqanFourSessionPolicy.plan(geometry.linesForExactIds(lines), false),
            rotation, monday, geometry);
        assertEquals(1, new JSONObject(legacy.encode()).getInt("version"));
        assertEquals(legacy.encode(), ItqanFourSessionSnapshot.decode(legacy.encode(), geometry).encode());
        assertThrows(IllegalStateException.class, () -> ItqanFourSessionSnapshot.decodeWeeklyPage(
            new JSONObject(weekly.encode()).put("version", 1).toString(), geometry));
    }

    private static ItqanFourSessionPolicy.Plan plan(List<String> lines) {
        return ItqanFourSessionPolicy.weeklyPage(geometry,
            geometry.linesForExactIds(lines).get(0).globalIndex, new LinkedHashSet<>(lines));
    }

    private static List<Integer> counts(ItqanFourSessionPolicy.Plan plan) {
        return plan.sessions.stream().map(slot -> slot.lineCount).toList();
    }
}

package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Option 7B: the Parcours › Stabilisation position is read from the real leg plans, exactly. */
public final class ItqanRotationProgressTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadCanonicalGeometry() throws Exception {
        String repoPath = "app/src/main/assets/reader109/geometry.json";
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    @Test public void positionMatchesTodaysUnitAndBothLegPlans() {
        InMemoryPrefs store = new InMemoryPrefs();
        int sabqiLine = geometry.firstLineIndex(new VerseRef(2, 120));
        store.disk.put("sabqiLineCursor", sabqiLine);
        store.disk.put("sabqiStart", "2:75");
        store.disk.put("sabqiEnd", "2:286");
        store.disk.put("anchoringQueueInitialized", true);
        store.disk.put("anchoringQueue", "[]");
        store.disk.put("itqanRanges", "[]");
        store.disk.put(ItqanRegimeStore.LEG, "TAIL_HUJURAT_NAS");
        store.disk.put(ItqanRegimeStore.CURSOR, "50:1");
        store.disk.put("schema", 6);
        store.disk.put("v6LearnedLineIds", "[]");
        store.disk.put("v6StabilizedLineIds", "[]");
        store.disk.put("v6AcquiredCreditLineIds", "[]");
        store.disk.put("v6QuarantineLineIds", "[]");
        store.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
        HifzPrefs prefs = new HifzPrefs(store);
        AnchoringQueue.Entry today = prefs.currentAnchoringEntry(geometry);
        HifzPrefs.ItqanRotationProgress position = prefs.itqanRotationProgress(geometry);
        assertNotNull(today);
        assertNotNull(position);
        assertEquals(today.start, position.start.toString());
        assertTrue(position.index >= 1 && position.index <= position.total);
        assertEquals("reading the position never moves the rotation",
            today.start, prefs.currentAnchoringEntry(geometry).start);
    }

    @Test public void hizbOfUsesCanonicalHizbStarts() {
        assertEquals(1, QuranRubBoundaries.hizbOf(new VerseRef(1, 1)));
        assertEquals(52, QuranRubBoundaries.hizbOf(new VerseRef(49, 1)));
        assertEquals(53, QuranRubBoundaries.hizbOf(new VerseRef(51, 31)));
        assertEquals(60, QuranRubBoundaries.hizbOf(new VerseRef(114, 6)));
    }
}

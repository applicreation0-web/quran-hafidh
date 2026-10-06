package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** User decision (option A): a Stabilisation block shows its printed page/lines and its verses. */
public final class StabilisationBlockLabelTest {
    private static GeometryRepository geometry;
    private static final String NBSP = " ";

    @BeforeClass public static void load() throws Exception {
        String repoPath = "app/src/main/assets/reader109/geometry.json";
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    private static List<StabilizationHalfPagePolicy.Unit> blocksOf(String start, String end) {
        List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            GeometryRepository.parseVerse(start), GeometryRepository.parseVerse(end), geometry);
        return StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(owned));
    }

    @Test public void everyPrintedPageNumbersItsLinesUpToFifteen() {
        int checked = 0;
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            boolean lastOnPage = i + 1 == geometry.lineCount() || geometry.line(i + 1).page != line.page;
            if (line.page >= 3 && lastOnPage) {
                assertEquals("page " + line.page, 15, geometry.printedLineNumber(line));
                checked++;
            }
        }
        assertEquals(602, checked);
        assertEquals("pages 1–2 have their own layout", -1, geometry.printedLineNumber(geometry.line(0)));
    }

    @Test public void alHujuratFirstBlocksShowPrintedLinesAndVerses() {
        List<StabilizationHalfPagePolicy.Unit> blocks = blocksOf("49:1", "49:11");
        assertEquals(3, blocks.size());
        String first = QuranSurahNames.block(geometry, blocks.get(0).lineIds);
        assertTrue(first, first.startsWith("p." + NBSP + "515 l." + NBSP + "9–15 · "));
        assertEquals(QuranSurahNames.range(new VerseRef(49, 1), new VerseRef(49, 4)),
            first.substring(first.indexOf(" · ") + 3));
        assertTrue(QuranSurahNames.block(geometry, blocks.get(1).lineIds).startsWith("p." + NBSP + "516 l." + NBSP + "1–6 · "));
        assertTrue(QuranSurahNames.block(geometry, blocks.get(2).lineIds).startsWith("p." + NBSP + "516 l." + NBSP + "7–15 · "));
    }

    @Test public void aCutInsideAVerseIsMarkedOnBothSides() {
        boolean found = false;
        for (AnchoringQueue.Entry unit : HifzPrefs.physicalUnitsInLeg(ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS,
                new VerseRef(114, 6), geometry, ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS)) {
            List<StabilizationHalfPagePolicy.Unit> blocks = blocksOf(unit.start, unit.end);
            for (int i = 1; i < blocks.size() && !found; i++) {
                String before = QuranSurahNames.block(geometry, blocks.get(i - 1).lineIds);
                String after = QuranSurahNames.block(geometry, blocks.get(i).lineIds);
                if (after.contains("(suite)")) {
                    assertTrue(before + " | " + after, before.contains("(début)"));
                    found = true;
                }
            }
            if (found) break;
        }
        assertTrue("Al-Ḥujurāt → An-Nās holds cuts inside a verse", found);
    }

    @Test public void aMissingLineFallsBackToTheGivenLabel() {
        assertEquals("fallback", QuranSurahNames.block(geometry, java.util.Collections.<String>emptyList(), "fallback"));
    }
}

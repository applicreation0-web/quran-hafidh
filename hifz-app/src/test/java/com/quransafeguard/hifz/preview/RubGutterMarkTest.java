package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Spec UI pass 2, §28: canonical Hizb/Rubʿ écussons on the real KFQC geometry, fail-closed. */
public final class RubGutterMarkTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadCanonicalGeometry() throws Exception {
        String repoPath = "app/src/main/assets/reader109/geometry.json";
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void all240BoundariesProduceAMarkOnTheirOwnLineWithAllFourTypes() throws Exception {
        int[] types = new int[4];
        int marks = 0;
        for (int[] row : QuranRubBoundaries.TABLE) {
            JSONObject mark = RubGutterMark.forPage(geometry, null, row[3]);
            assertNotNull("boundary " + row[0] + " must be located", mark);
            assertEquals(row[5], mark.getInt("position"));
            assertEquals(row[4], mark.getInt("hizb"));
            VerseRef verse = new VerseRef(row[1], row[2]);
            assertEquals(verse.toString(), mark.getString("verse"));
            GeometryRepository.LineMeta line = geometry.line(geometry.firstLineIndex(verse));
            assertEquals("aligned on the verse's real line", line.top, mark.getDouble("top"), 1e-9);
            assertEquals(line.bottom, mark.getDouble("bottom"), 1e-9);
            assertTrue(mark.getDouble("bottom") > mark.getDouble("top"));
            types[row[5]]++;
            marks++;
        }
        assertEquals(240, marks);
        for (int type = 0; type < 4; type++) assertEquals(60, types[type]);
    }

    @Test public void pagesWithoutANewBoundaryGetNoMark() {
        int without = 0;
        for (int page = 1; page <= 604; page++) {
            if (QuranRubBoundaries.boundaryOnPage(page) != null) continue;
            assertNull(RubGutterMark.forPage(geometry, null, page));
            without++;
        }
        assertEquals(604 - 240, without);
    }

    @Test public void exactFirstWordBoxIsCarriedWhenGiven() throws Exception {
        int[] row = QuranRubBoundaries.boundaryOnPage(106);
        JSONArray words = new JSONArray().put(new JSONArray().put(303.11).put(261.38).put(333.96).put(287.6));
        JSONObject mark = RubGutterMark.forPage(geometry, words, 106);
        assertEquals(row[5], mark.getInt("position"));
        assertEquals(4, mark.getJSONArray("wordBox").length());
    }

    @Test public void readerDrawsOnlyFromCanonicalDataAndKeepsBadgeAndNavigation() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(mushaf.contains(".put(\"rubMark\", rubMarkFor(page));"));
        assertTrue("no data = no écusson", reader.contains("if(!rubMark)return null;"));
        assertTrue("lines really interrupted around the écusson",
            reader.contains("place(segs[0],0,Math.min(span,gapTop));place(segs[1],gapBottom,span);"));
        assertTrue("page badge never covered", reader.contains("badgeTop=Math.min(mark.y+mark.size/2+3,Math.max(badgeTop,viewportHeight-d));"));
        assertFalse("boundary shown once: no header badge", study.contains("rubBadge"));
        assertTrue("Hizb navigation kept", study.contains("QuranRubNames.showPicker(this, this::setPage)"));
    }
}

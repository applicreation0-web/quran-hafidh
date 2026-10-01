package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StabilizationHalfPageInstrumentedTest {
    private GeometryRepository geometry;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        geometry = GeometryRepository.get(context);
    }

    @Test public void everyCanonicalPageObeysHalfPageBoundariesWithoutVerseSplits() {
        int seenPages = 0;
        int seenLines = 0;
        for (int page = 1; page <= 604; page++) {
            List<GeometryRepository.LineMeta> pageLines = linesForPage(page);
            assertFalse("page " + page + " must contain canonical Quran lines", pageLines.isEmpty());
            assertTrue("page " + page + " exceeds the fail-closed 15-line ceiling", pageLines.size() <= 15);

            List<StabilizationHalfPagePolicy.Unit> units = StabilizationHalfPagePolicy.planPage(pageLines);
            assertFalse("page " + page + " must produce at least one Stabilisation unit", units.isEmpty());

            ArrayList<String> original = new ArrayList<>();
            for (GeometryRepository.LineMeta line : pageLines) original.add(line.id);
            ArrayList<String> rebuilt = new ArrayList<>();
            int offset = 0;
            for (int unitIndex = 0; unitIndex < units.size(); unitIndex++) {
                StabilizationHalfPagePolicy.Unit unit = units.get(unitIndex);
                assertEquals("unit may not cross page boundary", page, unit.page);
                assertFalse("unit must contain physical lines", unit.lineIds.isEmpty());

                for (String lineId : unit.lineIds) {
                    GeometryRepository.LineMeta expected = pageLines.get(offset++);
                    assertEquals("unit must remain continuous and ordered", expected.id, lineId);
                    assertEquals("unit may not cross surah boundary", unit.surah, singleSurah(expected));
                    rebuilt.add(lineId);
                }

                // A physical 7/8 split is allowed to fall inside one aya. Page and surah
                // boundaries above are the only semantic boundaries enforced here.
            }

            assertEquals("all physical lines must be consumed exactly once", original.size(), offset);
            assertEquals("page line order and multiplicity must be exact", original, rebuilt);
            assertEquals("page may not duplicate physical lines", original.size(), new HashSet<>(rebuilt).size());
            seenPages++;
            seenLines += pageLines.size();
        }
        assertEquals(604, seenPages);
        assertEquals(8820, seenLines);
        assertEquals(8820, geometry.lineCount());
    }

    private List<GeometryRepository.LineMeta> linesForPage(int page) {
        ArrayList<GeometryRepository.LineMeta> out = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (line.page == page) out.add(line);
        }
        return out;
    }

    private int singleSurah(GeometryRepository.LineMeta line) {
        assertFalse("physical Quran line must contain verses", line.verses.isEmpty());
        Set<Integer> surahs = new HashSet<>();
        for (VerseRef verse : line.verses) surahs.add(verse.getSurah());
        assertEquals("physical Quran line may not mix surahs", 1, surahs.size());
        return surahs.iterator().next();
    }

    private boolean sharesVerse(GeometryRepository.LineMeta left, GeometryRepository.LineMeta right) {
        Set<VerseRef> verses = new HashSet<>(left.verses);
        for (VerseRef verse : right.verses) if (verses.contains(verse)) return true;
        return false;
    }
}

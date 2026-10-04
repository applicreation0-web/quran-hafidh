package com.quransafeguard.hifz.preview;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** C23 product contract: Anchoring may split a page, but a block never crosses a surah boundary. */
public final class C23SurahAwareAnchoringInstrumentedTest {
    private GeometryRepository geometry;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        geometry = GeometryRepository.get(context);
    }

    @Test public void namedBoundaryPagesMatchFrozenC23Layouts() {
        assertLayout(new VerseRef(53, 1), new VerseRef(53, 26),
            new int[]{13}, new int[]{5,4,4}); // page 526
        assertLayout(new VerseRef(53,45), new VerseRef(54,6),
            new int[]{9,4}, new int[]{5,4,4}); // page 528
        assertLayout(new VerseRef(54,50), new VerseRef(55,18),
            new int[]{4,9}, new int[]{4,5,4}); // page 531
        assertLayout(new VerseRef(55,70), new VerseRef(56,16),
            new int[]{6,7}, new int[]{3,3,4,3}); // page 534
        assertLayout(new VerseRef(58,22), new VerseRef(59,3),
            new int[]{6,7}, new int[]{3,3,4,3}); // page 545
    }

    @Test public void everyMushafPageRangePreservesLinesOrderAndSingleSurahBlocks() {
        for (int page = 1; page <= 604; page++) {
            List<GeometryRepository.LineMeta> pageLines = linesForPage(page);
            assertFalse("page " + page + " must contain geometry", pageLines.isEmpty());
            VerseRef start = pageLines.get(0).verses.get(0);
            List<VerseRef> lastRefs = pageLines.get(pageLines.size() - 1).verses;
            VerseRef end = lastRefs.get(lastRefs.size() - 1);
            assertRangeInvariants(start, end);
        }
    }

    private void assertLayout(VerseRef start, VerseRef end, int[] expectedSegments, int[] expectedBlocks) {
        assertArrayEquals(expectedSegments, geometry.surahSegmentLineCounts(start, end));
        assertArrayEquals(expectedBlocks,
            PreviewConfig.fractionatedBlockSizes(geometry.surahSegmentLineCounts(start, end)));
        assertRangeInvariants(start, end);
    }

    private void assertRangeInvariants(VerseRef start, VerseRef end) {
        List<String> original = geometry.lineIdsForVerseRange(start, end);
        int[] segments = geometry.surahSegmentLineCounts(start, end);
        int[] blocks = PreviewConfig.fractionatedBlockSizes(segments);
        List<String> rebuilt = new ArrayList<>();
        int offset = 0;
        for (int blockSize : blocks) {
            assertTrue("block size must be 1..5", blockSize > 0 && blockSize <= 5);
            List<String> block = original.subList(offset, offset + blockSize);
            Set<Integer> surahs = new HashSet<>();
            for (String lineId : block) {
                GeometryRepository.LineMeta line = lineById(lineId);
                for (VerseRef verse : line.verses) surahs.add(verse.getSurah());
            }
            assertEquals("a C23 block must contain exactly one surah", 1, surahs.size());
            rebuilt.addAll(block);
            offset += blockSize;
        }
        assertEquals("all original lines must be consumed", original.size(), offset);
        assertEquals("line order and multiplicity must be exact", original, rebuilt);
        assertEquals("no duplicate physical line", original.size(), new HashSet<>(rebuilt).size());
    }

    private List<GeometryRepository.LineMeta> linesForPage(int page) {
        ArrayList<GeometryRepository.LineMeta> out = new ArrayList<>();
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (line.page == page) out.add(line);
        }
        return out;
    }

    private GeometryRepository.LineMeta lineById(String id) {
        for (int i = 0; i < geometry.lineCount(); i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            if (line.id.equals(id)) return line;
        }
        throw new AssertionError("unknown line id " + id);
    }
}

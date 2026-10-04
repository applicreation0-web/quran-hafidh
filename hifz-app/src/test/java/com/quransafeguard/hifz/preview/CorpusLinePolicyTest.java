package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRange;
import com.quransafeguard.hifz.core.VerseRef;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CorpusLinePolicyTest {
    @Test public void boundaryLineMayBeDisplayedButIsNotOwnedByLaterAcquiredRange() {
        GeometryRepository.LineMeta line = new GeometryRepository.LineMeta(
            0,
            "L",
            1,
            Arrays.asList(new VerseRef(2, 20), new VerseRef(2, 21))
        );
        List<GeometryRepository.LineMeta> lines = Collections.singletonList(line);
        List<VerseRange> acquired = Collections.singletonList(
            new VerseRange(new VerseRef(2, 21), new VerseRef(2, 40))
        );

        assertEquals(new VerseRef(2, 20), CorpusLinePolicy.ownerVerse(line));
        Set<String> owned = CorpusLinePolicy.ownedLineIds(acquired, lines);
        Set<String> touched = CorpusLinePolicy.touchedLineIds(acquired, lines);

        assertFalse(owned.contains("L"));
        assertTrue(touched.contains("L"));
    }

    @Test public void adjacentVerseRangesSharingPhysicalLineHaveDisjointOwnership() {
        GeometryRepository.LineMeta shared = new GeometryRepository.LineMeta(
            0,
            "shared",
            1,
            Arrays.asList(new VerseRef(2, 20), new VerseRef(2, 21))
        );
        List<GeometryRepository.LineMeta> lines = Collections.singletonList(shared);
        List<VerseRange> pending = Collections.singletonList(
            new VerseRange(new VerseRef(2, 1), new VerseRef(2, 20))
        );
        List<VerseRange> acquired = Collections.singletonList(
            new VerseRange(new VerseRef(2, 21), new VerseRef(2, 40))
        );

        assertTrue(CorpusLinePolicy.ownedLineIds(pending, lines).contains("shared"));
        assertFalse(CorpusLinePolicy.ownedLineIds(acquired, lines).contains("shared"));
        assertTrue(CorpusLinePolicy.touchedLineIds(acquired, lines).contains("shared"));
    }

    @Test(expected = IllegalStateException.class)
    public void lineWithoutVerseFailsClosed() {
        GeometryRepository.LineMeta invalid = new GeometryRepository.LineMeta(
            0,
            "empty",
            1,
            Collections.emptyList()
        );
        CorpusLinePolicy.ownerVerse(invalid);
    }
}

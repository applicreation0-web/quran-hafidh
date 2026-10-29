package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Editorial pilot must not silently expand with unverifiable generated pairs. */
public final class AtlasDiscriminantCasesTest {
    @Test public void fourVerifiedPairsAreUniqueAndGrounded() {
        assertEquals(4,AtlasDiscriminantCases.PILOT.size());
        Set<String> unordered=new HashSet<>();
        for(AtlasDiscriminantCases.Entry e:AtlasDiscriminantCases.PILOT) {
            assertFalse(e.first.equals(e.second));
            String a=e.first.toString(),b=e.second.toString();
            assertTrue(unordered.add(a.compareTo(b)<0?a+"|"+b:b+"|"+a));
            assertFalse(e.kind.trim().isEmpty());
            assertFalse(e.firstCue.trim().isEmpty());
            assertFalse(e.secondCue.trim().isEmpty());
            assertTrue(e.explanation.length()>25);
            assertTrue(e.sourceUrl.startsWith("https://"));
            assertFalse(e.source.trim().isEmpty());
        }
    }
    @Test public void followsCurrentAtlasVerseOrStartsAtPilotFirst() {
        assertEquals(0,AtlasDiscriminantCases.preferredIndex(30,37));
        assertEquals(0,AtlasDiscriminantCases.preferredIndex(39,52));
        assertEquals(1,AtlasDiscriminantCases.preferredIndex(17,31));
        assertEquals(2,AtlasDiscriminantCases.preferredIndex(7,160));
        assertEquals(3,AtlasDiscriminantCases.preferredIndex(2,58));
        assertEquals(0,AtlasDiscriminantCases.preferredIndex(4,10));
    }
    @Test public void markedWordsAreNarrowAndOneBased() {
        AtlasDiscriminantCases.Entry a=AtlasDiscriminantCases.PILOT.get(0);
        assertArrayEquals(new int[]{2},a.firstWordIndices);
        assertArrayEquals(new int[]{2},a.secondWordIndices);
        for(int i=1;i<AtlasDiscriminantCases.PILOT.size();i++) {
            assertEquals(0,AtlasDiscriminantCases.PILOT.get(i).firstWordIndices.length);
            assertEquals(0,AtlasDiscriminantCases.PILOT.get(i).secondWordIndices.length);
        }
    }
}
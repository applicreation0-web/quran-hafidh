package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Editorial promotion is explicit; shared verses retain every approved relation. */
public final class AtlasDiscriminantCasesTest {
    @Test public void approvedPairsAreUniqueAndHaveExclusiveProvenance() {
        assertFalse(AtlasDiscriminantCases.CATALOGUE.isEmpty());
        Set<String> unordered=new HashSet<>();
        Set<Integer> ids=new HashSet<>();
        for(AtlasDiscriminantCases.Entry e:AtlasDiscriminantCases.CATALOGUE) {
            assertFalse(e.first.equals(e.second));
            String a=e.first.toString(),b=e.second.toString();
            assertTrue(unordered.add(a.compareTo(b)<0?a+"|"+b:b+"|"+a));
            assertTrue(ids.add(e.auditId));
            assertFalse(e.kind.trim().isEmpty());
            assertEquals("No invented religious explanation", "",e.explanation);
            if(e.auditId==292) {
                assertEquals("2:63",a);assertEquals("2:93",b);
                assertTrue(e.source.contains("utilisateur"));
            } else {
                assertTrue(e.source.startsWith("al-Abbad"));
                assertTrue(e.sourceUrl.startsWith("https://quranpedia.app/books/b10901/"));
            }
        }
    }
    @Test public void contextualLookupNeverOffersAnUnrelatedFallback() {
        assertEquals(-1,AtlasDiscriminantCases.indexFor(0,0));
        assertEquals(-1,AtlasDiscriminantCases.indexFor(115,1));
        assertEquals(-1,AtlasDiscriminantCases.indexFor(30,-1));
        for(int i=0;i<AtlasDiscriminantCases.CATALOGUE.size();i++) {
            AtlasDiscriminantCases.Entry e=AtlasDiscriminantCases.CATALOGUE.get(i);
            assertTrue(AtlasDiscriminantCases.indicesForVerse(e.first.getSurah(),e.first.getAyah()).contains(i));
            assertTrue(AtlasDiscriminantCases.indicesForVerse(e.second.getSurah(),e.second.getAyah()).contains(i));
        }
    }
    @Test public void userPriorityKeepsExactEleventhWordOnBothQcfPages() {
        AtlasDiscriminantCases.Entry e=null;
        for(AtlasDiscriminantCases.Entry candidate:AtlasDiscriminantCases.CATALOGUE)
            if(candidate.auditId==292)e=candidate;
        assertNotNull(e);
        assertArrayEquals(new int[]{11},e.firstWordIndices);
        assertArrayEquals(new int[]{11},e.secondWordIndices);
        assertEquals(10,e.firstPage);assertEquals(14,e.secondPage);
    }
}

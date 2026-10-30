package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/** Every approved pair must be reachable; indexing must preserve multi-pair verses. */
public final class MutashCatalogueIndexTest {
    @Test public void everyPairReachableFromBothVersesAndBothPages() {
        for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++) {
            AtlasDiscriminantCases.Entry e=AtlasDiscriminantCases.PILOT.get(i);
            assertTrue(AtlasDiscriminantCases.indicesForVerse(e.first.getSurah(),e.first.getAyah()).contains(i));
            assertTrue(AtlasDiscriminantCases.indicesForVerse(e.second.getSurah(),e.second.getAyah()).contains(i));
            assertTrue(AtlasDiscriminantCases.indicesForPage(e.firstPage).contains(i));
            assertTrue(AtlasDiscriminantCases.indicesForPage(e.secondPage).contains(i));
        }
    }
    @Test public void all604PagesHaveExactDeduplicatedMembership() {
        for(int page=1;page<=604;page++) {
            List<Integer> actual=AtlasDiscriminantCases.indicesForPage(page);
            List<Integer> expected=new ArrayList<>();
            for(int i=0;i<AtlasDiscriminantCases.PILOT.size();i++) {
                AtlasDiscriminantCases.Entry e=AtlasDiscriminantCases.PILOT.get(i);
                if(e.firstPage==page||e.secondPage==page)expected.add(i);
            }
            assertEquals("QCF page "+page,expected,actual);
            assertEquals(new LinkedHashSet<>(actual).size(),actual.size());
        }
        assertTrue(AtlasDiscriminantCases.indicesForPage(0).isEmpty());
        assertTrue(AtlasDiscriminantCases.indicesForPage(605).isEmpty());
        assertTrue(AtlasDiscriminantCases.indicesForVerse(0,0).isEmpty());
    }
    @Test public void lookupResultsCannotBeMutated() {
        AtlasDiscriminantCases.Entry e=AtlasDiscriminantCases.PILOT.get(0);
        try { AtlasDiscriminantCases.indicesForPage(e.firstPage).clear(); fail("Mutable page index"); }
        catch(UnsupportedOperationException expected) {}
        try { AtlasDiscriminantCases.indicesForVerse(e.first.getSurah(),e.first.getAyah()).clear(); fail("Mutable verse index"); }
        catch(UnsupportedOperationException expected) {}
    }
}

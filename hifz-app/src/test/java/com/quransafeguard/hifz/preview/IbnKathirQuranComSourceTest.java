package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Exact Qaf-source and whole-corpus browser route, no Hifz state involved. */
public final class IbnKathirQuranComSourceTest {
    @Test public void eachOf1903FrozenGroupsHasAnExactSafeQuranComTafsirRoute() {
        IbnKathirGroupIndex all=IbnKathirGroupIndex.shared();
        int count=0;
        for(int surah=1;surah<=114;surah++) {
            for(IbnKathirGroupIndex.Group g:all.groupsForSurah(surah)) {
                String url=IbnKathirQuranComSource.urlForGroup(g);
                assertEquals("https://quran.com/en/"+surah+":"+g.startAyah
                    +"/tafsirs/en-tafisr-ibn-kathir",url);
                assertTrue(url.startsWith("https://quran.com/en/"));
                assertTrue(url.endsWith("/tafsirs/en-tafisr-ibn-kathir"));
                count++;
            }
        }
        assertEquals(1903,count);
        assertEquals("https://quran.com/en/50:12/tafsirs/en-tafisr-ibn-kathir",
            IbnKathirQuranComSource.urlForGroup(all.containing(50,14)));
        assertEquals("https://quran.com/en/114:1/tafsirs/en-tafisr-ibn-kathir",
            IbnKathirQuranComSource.urlForGroup(all.containing(114,5)));
    }

    @Test public void exactOriginalQafEnglishSourceHeadingsAreNotQuranicCueData() {
        IbnKathirGroupIndex all=IbnKathirGroupIndex.shared();
        int groups=0;
        for(IbnKathirGroupIndex.Group g:all.groupsForSurah(50)) {
            String[] headings=IbnKathirQuranComSource.verifiedQafHeadings(g);
            assertTrue(headings.length>=1);
            for(String h:headings) {
                assertFalse(h.trim().isEmpty());
                assertFalse(h.contains("Al-Munir"));
            }
            groups++;
        }
        assertEquals(8,groups);
        String[] two=IbnKathirQuranComSource.verifiedQafHeadings(all.containing(50,15));
        assertArrayEquals(new String[] {
           "Reminding the Quraysh of the Destruction of earlier Disbelieving Nations",
           "Repeating the Creation is Easier than originating It"
        },two);
        assertEquals(0,IbnKathirQuranComSource.verifiedQafHeadings(
            all.containing(53,1)).length);
        assertEquals(0,IbnKathirQuranComSource.verifiedQafHeadings(null).length);
        assertEquals(0,IbnKathirQuranComSource.verifiedQafHeadings(
            new IbnKathirGroupIndex.Group(50,12,14)).length);
    }

    @Test public void forgedOrMissingGroupCannotLaunchLink() {
        try {
            IbnKathirQuranComSource.urlForGroup(null);
            fail("null group accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            IbnKathirQuranComSource.urlForGroup(
                new IbnKathirGroupIndex.Group(999,1,6));
            fail("invalid surah accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            IbnKathirQuranComSource.urlForGroup(
                new IbnKathirGroupIndex.Group(50,15,12));
            fail("invalid verse ordering accepted");
        } catch (IllegalArgumentException expected) {}
    }
}

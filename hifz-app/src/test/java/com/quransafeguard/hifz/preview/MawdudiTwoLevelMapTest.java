package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

/** One verified macro overview per surah, then original Ayah Themes passages.
 * Neither level creates a new canonical verse or invents an editorial theme. */
public final class MawdudiTwoLevelMapTest {
    @Test public void overviewHas114SurahsAndOnlySevenVerifiedDiscourseSubdivisions() throws Exception {
        MawdudiPassageIndex index=AyahThemesCueIndexTest.canonical();
        MawdudiMapData overview=MawdudiMapData.parse(MawdudiMapDataTest.synthetic().toString(),index);
        int total=0,whole=0,explicit=0;
        for(int surah=1;surah<=114;surah++){
            List<MawdudiMapData.Macro> units=overview.forSurah(surah);
            assertFalse(units.isEmpty());
            if(units.size()==1)whole++; else {
                assertTrue("No invented macro division",surah>=2&&surah<=9);
                explicit+=units.size();
            }
            total+=units.size();
            assertEquals(1,units.get(0).start);
            assertEquals(index.forSurah(surah).get(index.forSurah(surah).size()-1).lastAyah,
                units.get(units.size()-1).end);
            for(int i=1;i<units.size();i++)
                assertEquals("Macro discontinuity",units.get(i-1).end+1,units.get(i).start);
        }
        assertEquals(114,whole+8);
        assertEquals(106,whole);
        assertEquals(67,explicit);
        assertEquals(173,total);
    }

    @Test public void everyOriginalPassageIsReachableByAtLeastOneSourceUnit() throws Exception {
        MawdudiPassageIndex index=AyahThemesCueIndexTest.canonical();
        MawdudiMapData overview=MawdudiMapData.parse(MawdudiMapDataTest.synthetic().toString(),index);
        int count=0;
        for(int surah=1;surah<=114;surah++){
            List<MawdudiPassageIndex.Passage> originals=index.forSurah(surah);
            for(MawdudiPassageIndex.Passage passage:originals){
                int intersecting=0;
                for(MawdudiMapData.Macro m:overview.forSurah(surah))
                    if(passage.firstAyah<=m.end&&passage.lastAyah>=m.start)
                        intersecting++;
                assertTrue("Passage must be visible in detailed level: "+passage.id,intersecting>=1);
                assertTrue(intersecting<=overview.forSurah(surah).size());
                count++;
            }
        }
        assertEquals(1053,count);
    }

    @Test public void sourceSplitDoesNotCreateExtraCanonicalPassageForSurah27() throws Exception {
        MawdudiPassageIndex index=AyahThemesCueIndexTest.canonical();
        MawdudiPassageIndex.Passage p=index.at(27,67);
        assertSame(p,index.at(27,75));
        assertSame(p,index.at(27,76));
        assertSame(p,index.at(27,82));
        assertEquals(67,p.firstAyah);
        assertEquals(82,p.lastAyah);
    }
}

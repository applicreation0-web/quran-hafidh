package com.quransafeguard.hifz.preview;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual committed public navigation asset, not a synthetic 114-surah PASS. */
public final class MawdudiPassageIndexTest {
    private String source() throws Exception {
        File file=new File("src/main/assets/"+MawdudiPassageIndex.ASSET);
        if(!file.exists())file=new File("hifz-app/src/main/assets/"+MawdudiPassageIndex.ASSET);
        assertTrue("Canonical Android source asset must exist",file.isFile());
        return new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8);
    }
    @Test public void everyVerseHasExactlyOneCanonicalOwner() throws Exception {
        MawdudiPassageIndex m=MawdudiPassageIndex.parse(source());
        assertEquals(1053,m.count());
        int originals=0,adaptations=0,verses=0;
        for(int s=1;s<=114;s++) {
            assertFalse(m.forSurah(s).isEmpty());
            int expected=1;
            for(MawdudiPassageIndex.Passage p:m.forSurah(s)) {
                assertEquals(expected,p.firstAyah);
                for(int a=p.firstAyah;a<=p.lastAyah;a++) assertSame(p,m.at(s,a));
                expected=p.lastAyah+1; verses+=p.lastAyah-p.firstAyah+1;
                if(p.editorialAdaptation)adaptations++;else originals++;
            }
        }
        assertEquals(6236,verses);
        assertEquals(1044,originals);
        assertEquals(9,adaptations);
        assertEquals("s002a134b134",m.at(2,134).id);
        assertEquals("s055a056b078",m.at(55,78).id);
    }
    @Test public void navigationNeverSkipsCrossSurahBoundaries() throws Exception {
        MawdudiPassageIndex m=MawdudiPassageIndex.parse(source());
        assertEquals("s001a001b007",m.all().get(0).id);
        assertEquals("s114a001b006",m.all().get(m.count()-1).id);
        for(int i=0;i<m.count();i++){
            MawdudiPassageIndex.Passage p=m.all().get(i);
            assertSame(i==0?null:m.all().get(i-1),m.previous(p));
            assertSame(i==m.count()-1?null:m.all().get(i+1),m.next(p));
        }
    }
    @Test public void noUnattributedOrGapFillingMutationAllowed() throws Exception {
        String data=source();
        try{MawdudiPassageIndex.parse(data.replace(
            "s002a134b134,2,134,134,EDITORIAL_ADAPTATION_NOT_SOURCE_QUOTE",
            "s002a134b134,2,135,135,EDITORIAL_ADAPTATION_NOT_SOURCE_QUOTE"));
            fail("A missing verse must fail closed");
        }catch(IllegalArgumentException expected){}
        try{MawdudiPassageIndex.parse(data.replace(
            "EDITORIAL_ADAPTATION_NOT_SOURCE_QUOTE","MAWDUDI_ORIGINAL"));
            fail("Adapted heading cannot be represented as original");
        }catch(IllegalArgumentException expected){}
    }
}

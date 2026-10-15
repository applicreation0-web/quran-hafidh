package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Only real immutable Madinah QCF geometry, no fabricated page coordinates.
 * The test verifies that passage endpoints include any continuation of the
 * last ayah on subsequent pages, and never alter memorisation state.
 */
public final class MawdudiMapModelRealQcfTest {
    private static String read(String relative, String other) throws Exception {
        File f=new File(relative);
        if(!f.isFile()) f=new File(other);
        assertTrue("Required real geometry/asset is missing: "+relative,f.isFile());
        return new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8);
    }
    @Test public void allCanonicalPassagesHaveExactFirstAndLastPhysicalPages() throws Exception {
        MawdudiPassageIndex index=MawdudiPassageIndex.parse(read(
            "hifz-app/src/main/assets/mawdudi_public/passages_v1.csv",
            "src/main/assets/mawdudi_public/passages_v1.csv"));
        GeometryRepository g=GeometryRepository.fromJson(read(
            "app/src/main/assets/reader109/geometry.json",
            "../app/src/main/assets/reader109/geometry.json"));
        int multiPagePassages=0;
        for(MawdudiPassageIndex.Passage p:index.all()){
            VerseRef start=new VerseRef(p.surah,p.firstAyah);
            VerseRef last=new VerseRef(p.surah,p.lastAyah);
            int exactFirst=g.line(g.firstLineIndex(start)).page;
            int exactLast=g.line(g.lastLineIndex(last)).page;
            assertEquals(p.id,exactFirst,MawdudiMapModel.firstPage(p,g));
            assertEquals(p.id,exactLast,MawdudiMapModel.lastPage(p,g));
            assertTrue(p.id,exactLast>=exactFirst);
            assertEquals(p.id,exactFirst,MawdudiMapModel.clampPage(p,-100,g));
            assertEquals(p.id,exactLast,MawdudiMapModel.clampPage(p,1000,g));
            if(exactLast>exactFirst)multiPagePassages++;
        }
        assertTrue("Real QCF index should contain multipage passages",
            multiPagePassages>0);
    }
}

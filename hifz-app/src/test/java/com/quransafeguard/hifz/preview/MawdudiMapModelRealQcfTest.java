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
            assertEquals(p.id,exactLast,MawdudiMapModel.lastPageForVerse(last,g));
            assertTrue(p.id,exactLast>=exactFirst);
            assertEquals(p.id,exactFirst,MawdudiMapModel.clampPage(p,-100,g));
            assertEquals(p.id,exactLast,MawdudiMapModel.clampPage(p,1000,g));
            if(exactLast>exactFirst)multiPagePassages++;
        }
        assertTrue("Real QCF index should contain multipage passages",
            multiPagePassages>0);
    }

    @Test public void navigationKeepsQcfReadingStateAcrossAll114Surahs() throws Exception {
        MawdudiPassageIndex index=MawdudiPassageIndex.parse(read(
            "hifz-app/src/main/assets/mawdudi_public/passages_v1.csv",
            "src/main/assets/mawdudi_public/passages_v1.csv"));
        GeometryRepository g=GeometryRepository.fromJson(read(
            "app/src/main/assets/reader109/geometry.json",
            "../app/src/main/assets/reader109/geometry.json"));
        MawdudiMapModel.Navigation nav=new MawdudiMapModel.Navigation(index,g,1,1);
        assertFalse(nav.movePassage(-1));
        for(MawdudiPassageIndex.Passage passage:index.all()) {
            nav.select(passage);
            assertSame(passage,nav.passage());
            assertEquals(passage.id,MawdudiMapModel.firstPage(passage,g),nav.page());
            java.util.List<VerseRef> focus=nav.highlighted();
            assertFalse("Empty real QCF page "+passage.id,focus.isEmpty());
            for(VerseRef v:focus) {
                assertEquals(passage.id,passage.surah,v.getSurah());
                assertTrue(passage.id,passage.contains(v.getAyah()));
            }
            VerseRef jump=nav.readerJumpVerse();
            assertTrue(passage.contains(jump.getAyah()));
            nav.setMapScrollY(196);
            nav.setCommentaryScrollY(47);
            nav.setCommentaryOpen(true);
            MawdudiMapModel.Snapshot before=nav.snapshot();
            nav.turnPage(1);
            nav.movePassage(1);
            nav.restore(before);
            assertSame(passage,nav.passage());
            assertEquals(MawdudiMapModel.firstPage(passage,g),nav.page());
            assertEquals(196,nav.mapScrollY());
            assertEquals(47,nav.commentaryScrollY());
            assertTrue(nav.commentaryOpen());
            assertEquals(passage.id,nav.snapshot().id);
        }
        nav.select(index.all().get(index.count()-1));
        assertFalse(nav.movePassage(1));
        nav.select(index.at(1,1));
        assertTrue(nav.movePassage(1));
        assertEquals(2,nav.passage().surah);
        assertEquals(1,nav.passage().firstAyah);
    }
    @Test public void staleSnapshotFailsClosedInsteadOfJumpingToWrongVerse() throws Exception {
        MawdudiPassageIndex index=MawdudiPassageIndex.parse(read(
            "hifz-app/src/main/assets/mawdudi_public/passages_v1.csv",
            "src/main/assets/mawdudi_public/passages_v1.csv"));
        GeometryRepository g=GeometryRepository.fromJson(read(
            "app/src/main/assets/reader109/geometry.json",
            "../app/src/main/assets/reader109/geometry.json"));
        MawdudiMapModel.Navigation nav=new MawdudiMapModel.Navigation(index,g,2,97);
        try {
            nav.restore(new MawdudiMapModel.Snapshot("wrong-index",nav.passage().ordinal,
                nav.page(),0,0,false));
            fail("A changed canonical source must not restore to a different verse.");
        } catch(IllegalArgumentException expected) {}
    }

}

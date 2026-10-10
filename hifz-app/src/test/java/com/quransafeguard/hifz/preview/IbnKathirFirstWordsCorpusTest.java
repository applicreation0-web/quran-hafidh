package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** J1: actual 604-page QCF corpus, not generated editorial approvals. */
public final class IbnKathirFirstWordsCorpusTest {
    private static Map<Integer,List<WordGeometryRepository.WordBox>> pages() throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> result=new HashMap<>();
        for(String part:new String[]{"001-150","151-300","301-450","451-604"}) {
            Path p=Paths.get("hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-"+part+".json");
            if(!Files.exists(p))p=Paths.get("src/main/word-source/quran-ws-v1.1.2/word-boxes-"+part+".json");
            String s=new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
            result.putAll(WordGeometryRepository.parseChunk(s));
        }
        return result;
    }

    @Test public void exactTwoWordStartsCoverAll1903GroupsAnd604Pages() throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> pages=pages();
        IbnKathirApprovedRecallCorpus.Corpus corpus=
            IbnKathirFirstWordsCorpus.build(pages,IbnKathirGroupIndex.shared());
        assertEquals(1903,corpus.byId.size());
        assertEquals(604,corpus.byPage.size());
        int starts=0,shortGroups=0,spanningPage=0;
        for(Map.Entry<Integer,List<SemanticPassageRepository.Cue>> entry:corpus.byPage.entrySet()) {
            int page=entry.getKey();
            assertFalse(entry.getValue().isEmpty());
            for(SemanticPassageRepository.Cue cue:entry.getValue()) {
                starts++;
                IbnKathirGroupIndex.Group group=IbnKathirGroupIndex.shared()
                    .containing(cue.startVerse.getSurah(),cue.startVerse.getAyah());
                assertNotNull(group);
                assertEquals(group.id,cue.passageId);
                assertEquals(group.startAyah,cue.startVerse.getAyah());
                assertEquals(group.endAyah,cue.endVerse.getAyah());
                assertEquals(page,cue.startPage);
                assertTrue(cue.endPage>=page);
                List<WordGeometryRepository.WordBox> wordRows=pages.get(page);
                int first=-1;
                for(int i=0;i<wordRows.size();i++)
                    if(wordRows.get(i).key.equals(group.surah+":"+group.startAyah+":1"))
                        {first=i;break;}
                assertTrue("No first word for "+group.id,first>=0);
                org.json.JSONArray shapes=corpus.boxes(cue);
                assertEquals(wordRows.get(first).boxJson().toString(),
                    shapes.getJSONArray(0).toString());
                assertTrue(cue.anchorWordCount==1 || cue.anchorWordCount==2);
                if(cue.anchorWordCount==1)shortGroups++;
                if(cue.anchorWordCount==2 && shapes.length()==1) {
                    spanningPage++;
                    assertEquals("Second word must be on next page",wordRows.size()-1,first);
                    assertTrue("Must not invent next word beyond 604",page<604);
                } else {
                    assertEquals(cue.anchorWordCount,shapes.length());
                    if(cue.anchorWordCount==2)
                        assertEquals(wordRows.get(first+1).boxJson().toString(),
                            shapes.getJSONArray(1).toString());
                }
                assertFalse("Reference is NOT an invented tafsir title",
                    cue.title.toLowerCase(java.util.Locale.ROOT).contains("munir"));
            }
        }
        assertEquals(1903,starts);
        assertTrue(shortGroups>=0);
        // The precise counts are corpus-dependent and deliberately not guessed.
        System.out.println("Ibn Kathir QCF: groups="+starts+
            ", physical one-word groups="+shortGroups+", page-spanning two-word cues="+spanningPage);
        assertEquals("IKEN050_001_005",corpus.byPage.get(518).get(0).passageId);
        assertEquals(2,corpus.byPage.get(518).get(0).anchorWordCount);
    }

    @Test public void brokenOrDuplicateGeometryCanNeverProduceMaskedCueCorpus() throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> missing=pages();
        List<WordGeometryRepository.WordBox> altered=new ArrayList<>(missing.get(518));
        altered.remove(0);
        missing.put(518,altered);
        try {
            IbnKathirFirstWordsCorpus.build(missing,IbnKathirGroupIndex.shared());
            fail("Partial QCF unexpectedly activated Ibn Kathir cues");
        } catch(IllegalStateException expected){}
        Map<Integer,List<WordGeometryRepository.WordBox>> duplicate=pages();
        List<WordGeometryRepository.WordBox> row=new ArrayList<>(duplicate.get(518));
        row.set(0,row.get(1));
        duplicate.put(518,row);
        try {
            IbnKathirFirstWordsCorpus.build(duplicate,IbnKathirGroupIndex.shared());
            fail("Duplicated QCF word unexpectedly accepted");
        } catch(IllegalStateException expected){}
    }
}

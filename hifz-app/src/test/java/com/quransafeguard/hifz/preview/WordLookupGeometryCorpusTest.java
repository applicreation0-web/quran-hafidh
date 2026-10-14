package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Real 604-page QCF geometry, not synthetic glyph offsets. */
public final class WordLookupGeometryCorpusTest {
    private static Path source(String filename) {
        Path p=Paths.get("hifz-app/src/main/word-source/quran-ws-v1.1.2",filename);
        if(!Files.exists(p))p=Paths.get("src/main/word-source/quran-ws-v1.1.2",filename);
        return p;
    }

    @Test public void all77432QcfWordsAreAddressableOrExplicitlyAmbiguous() throws Exception {
        int words=0, ambiguous=0, narrow=0, overlap=0;
        Set<Integer> pages=new HashSet<>();
        Set<String> keys=new HashSet<>();
        Set<String> verses=new HashSet<>();
        for(String span:new String[]{"001-150","151-300","301-450","451-604"}){
            String raw=new String(Files.readAllBytes(source("word-boxes-"+span+".json")), StandardCharsets.UTF_8);
            Map<Integer,List<WordGeometryRepository.WordBox>> loaded=WordGeometryRepository.parseChunk(raw);
            for(Map.Entry<Integer,List<WordGeometryRepository.WordBox>> record:loaded.entrySet()){
                assertTrue(pages.add(record.getKey()));
                List<WordGeometryRepository.WordBox> boxes=record.getValue();
                List<WordLookupHitTest.Box> touches=new ArrayList<>();
                for(WordGeometryRepository.WordBox x:boxes){
                    assertTrue(keys.add(x.key));
                    verses.add(x.surah+":"+x.ayah);
                    touches.add(new WordLookupHitTest.Box(x.key,x.x0,x.y0,x.x1,x.y1));
                    if(x.x1-x.x0<=10)narrow++;
                }
                for(int i=0;i<boxes.size();i++){
                    WordGeometryRepository.WordBox a=boxes.get(i);
                    double centerX=(a.x0+a.x1)/2, centerY=(a.y0+a.y1)/2;
                    int contenders=0;
                    for(int j=0;j<boxes.size();j++){
                        WordGeometryRepository.WordBox b=boxes.get(j);
                        if(Math.min(a.x1,b.x1)-Math.max(a.x0,b.x0)>1
                            && Math.min(a.y1,b.y1)-Math.max(a.y0,b.y0)>1 && j>i)
                            overlap++;
                        if(b.x0<=centerX && centerX<=b.x1 && b.y0<=centerY && centerY<=b.y1)
                            contenders++;
                    }
                    String picked=WordLookupHitTest.exactUnambiguousKey(touches,centerX,centerY);
                    if(contenders!=1){ambiguous++;assertNull("Never guess overlapping word "+a.key,picked);}
                    else assertEquals("Wrong QCF word "+a.key,a.key,picked);
                    words++;
                }
            }
        }
        assertEquals(604,pages.size());
        assertEquals(6236,verses.size());
        assertEquals(77432,words);
        assertEquals(77432,keys.size());
        assertEquals(36,narrow);
        assertEquals(7053,overlap);
        assertEquals(18,ambiguous);
    }
}

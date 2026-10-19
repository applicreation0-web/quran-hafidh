package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

/** Real shipped geometry; altered boxes below are adversarial copies, never corpus changes. */
public final class CorrespondenceGeometryTest {
    private static GeometryRepository geometry;
    private static final Map<Integer,List<WordGeometryRepository.WordBox>> words=new HashMap<>();
    private static CorrespondenceGeometry adapter;
    private static String read(String relative) throws Exception {
        for(String prefix:new String[]{"","../","quran-hafidh/"}) {
            Path path=Paths.get(prefix+relative);
            if(Files.isRegularFile(path)) return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
        }
        throw new IllegalStateException("Required authentic geometry absent: "+relative);
    }
    @BeforeClass public static void loadRealGeometry() throws Exception {
        geometry=GeometryRepository.fromJson(read("app/src/main/assets/reader109/geometry.json"));
        for(String span:new String[]{"001-150","151-300","301-450","451-604"})
            words.putAll(WordGeometryRepository.parseChunk(read(
                "hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-"+span+".json")));
        adapter=new CorrespondenceGeometry(geometry,page->words.get(page));
    }
    @Test public void all6236VersesHaveExactFirstAndLastPhysicalPages() {
        Map<String,int[]> expected=new HashMap<>();
        for(int i=0;i<geometry.lineCount();i++) {
            GeometryRepository.LineMeta line=geometry.line(i);
            for(VerseRef v:line.verses) {
                int[] p=expected.get(v.toString());
                if(p==null) { p=new int[]{line.page,line.page}; expected.put(v.toString(),p); }
                p[1]=line.page;
            }
        }
        int count=0;
        for(int surah=1;surah<=114;surah++) {
            assertEquals(QuranCanon.INSTANCE.ayahCount(surah),adapter.ayahCount(surah));
            for(int ayah=1;ayah<=adapter.ayahCount(surah);ayah++) {
                String key=surah+":"+ayah; int[] page=expected.get(key); assertNotNull(key,page);
                assertEquals(key,page[0],adapter.firstPage(surah,ayah));
                assertEquals(key,page[1],adapter.lastPage(surah,ayah));
                count++;
            }
        }
        assertEquals(6236,count);
    }
    @Test public void everyCanonicalWordIndexIsAvailableForItsRealVerseAndPages() {
        Map<String,Integer> lastWords=new HashMap<>(); int count=0;
        for(List<WordGeometryRepository.WordBox> page:words.values())
            for(WordGeometryRepository.WordBox word:page) {
                String key=word.surah+":"+word.ayah;
                lastWords.put(key,Math.max(lastWords.getOrDefault(key,0),word.word)); count++;
            }
        assertEquals(77432,count); assertEquals(6236,lastWords.size());
        for(Map.Entry<String,Integer> verse:lastWords.entrySet()) {
            String[] ref=verse.getKey().split(":"); int s=Integer.parseInt(ref[0]),a=Integer.parseInt(ref[1]);
            assertTrue(verse.getKey(),adapter.hasSegmentGeometry(s,a,1,verse.getValue()));
        }
    }
    @Test public void kursiSegmentsAreGeometryChecksAndNeverScientificCentreApproval() {
        assertEquals(42,adapter.firstPage(2,255)); assertEquals(42,adapter.lastPage(2,255));
        assertTrue(adapter.hasSegmentGeometry(2,255,1,50));
        assertTrue(adapter.hasSegmentGeometry(2,255,20,23));
        assertFalse(adapter.hasSegmentGeometry(2,255,50,51));
        assertFalse(adapter.hasSegmentGeometry(2,255,0,3));
        assertFalse(adapter.hasSegmentGeometry(2,255,3,2));
        assertFalse(adapter.hasSegmentGeometry(0,255,1,1));
        assertFalse(adapter.hasSegmentGeometry(2,287,1,1));
        assertFalse(adapter.hasSegmentGeometry(2,255,1,Integer.MAX_VALUE));
    }
    @Test public void missingMiddleWordAndDuplicateWordFailEvenWithCorrectEndpoints() {
        List<WordGeometryRepository.WordBox> page=new ArrayList<>(words.get(42));
        page.removeIf(w->w.key.equals("2:255:2"));
        CorrespondenceGeometry gap=new CorrespondenceGeometry(geometry,p->p==42?page:words.get(p));
        assertFalse(gap.hasSegmentGeometry(2,255,1,3));
        List<WordGeometryRepository.WordBox> duplicate=new ArrayList<>(words.get(42));
        for(WordGeometryRepository.WordBox word:words.get(42)) if(word.key.equals("2:255:2")) duplicate.add(word);
        CorrespondenceGeometry repeated=new CorrespondenceGeometry(geometry,p->p==42?duplicate:words.get(p));
        assertFalse(repeated.hasSegmentGeometry(2,255,1,3));
    }
    @Test public void wordBoxesOnWrongPageOrUnavailableSidecarNeverPass() {
        CorrespondenceGeometry wrongPage=new CorrespondenceGeometry(geometry,p->p==41?words.get(42):Collections.emptyList());
        assertFalse(wrongPage.hasSegmentGeometry(2,255,1,3));
        assertFalse(new CorrespondenceGeometry(geometry,p->null).hasSegmentGeometry(2,255,1,3));
        assertFalse(new CorrespondenceGeometry(geometry,p->{throw new IllegalStateException("missing sidecar");})
            .hasSegmentGeometry(2,255,1,3));
    }
    @Test public void invalidOrNonfiniteRequestedWordBoxFailsClosed() {
        for(double[] box:new double[][]{{Double.NaN,1,2,3},{1,1,Double.POSITIVE_INFINITY,3},
            {-1,1,2,3},{1,1,346,3},{1,1,2,551},{2,1,2,3},{1,3,2,2}}) {
            List<WordGeometryRepository.WordBox> corrupted=new ArrayList<>(words.get(42));
            corrupted.removeIf(w->w.key.equals("2:255:2"));
            corrupted.add(new WordGeometryRepository.WordBox("2:255:2",box[0],box[1],box[2],box[3]));
            CorrespondenceGeometry unsafe=new CorrespondenceGeometry(geometry,p->p==42?corrupted:words.get(p));
            assertFalse(unsafe.hasSegmentGeometry(2,255,1,3));
        }
    }
    @Test public void syntheticContinuationResolvesLastLineAndChecksEveryAffectedPage() throws Exception {
        // Shipped Madinah data has no split ayah. This is explicitly a changed
        // in-memory fixture to exercise future continuation handling, not a real page claim.
        JSONObject changed=new JSONObject(read("app/src/main/assets/reader109/geometry.json"));
        changed.getJSONObject("pages").getJSONObject("43").getJSONArray("lines")
            .getJSONObject(0).getJSONArray("verses").put("2:255");
        GeometryRepository split=GeometryRepository.fromJson(changed.toString());
        List<WordGeometryRepository.WordBox> first=new ArrayList<>(words.get(42));
        List<WordGeometryRepository.WordBox> next=new ArrayList<>(words.get(43));
        for(WordGeometryRepository.WordBox box:words.get(42)) if(box.key.equals("2:255:50")) {
            first.remove(box); next.add(box);
        }
        CorrespondenceGeometry continuation=new CorrespondenceGeometry(split,
            page->page==42?first:page==43?next:words.get(page));
        assertEquals(42,continuation.firstPage(2,255)); assertEquals(43,continuation.lastPage(2,255));
        assertTrue(continuation.hasSegmentGeometry(2,255,49,50));
        CorrespondenceGeometry missingLast=new CorrespondenceGeometry(split,
            page->page==42?first:page==43?Collections.emptyList():words.get(page));
        assertFalse(missingLast.hasSegmentGeometry(2,255,49,50));
    }
}

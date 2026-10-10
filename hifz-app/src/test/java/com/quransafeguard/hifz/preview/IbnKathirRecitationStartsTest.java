package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * True Mushaf QCF word geometry, not approximate ink-cell coordinates.
 * One start marker per documentary group is NOT one mnemonic cue per group.
 */
public final class IbnKathirRecitationStartsTest {
    private static Path geometry(String name) {
        Path path=Paths.get("hifz-app/src/main/word-source/quran-ws-v1.1.2",name);
        if(!Files.exists(path))path=Paths.get("src/main/word-source/quran-ws-v1.1.2",name);
        return path;
    }
    private static String source(String local) throws Exception {
        Path p=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview",local);
        if(!Files.exists(p))p=Paths.get("src/main/java/com/quransafeguard/hifz/preview",local);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }
    @Test public void everyOneOf1903GroupsHasExactlyOneRealQcfLocator() throws Exception {
        IbnKathirGroupIndex index=IbnKathirGroupIndex.shared();
        Set<String> all=new HashSet<>();
        Set<Integer> pages=new HashSet<>();
        for(String suffix:new String[]{"001-150","151-300","301-450","451-604"}) {
            String raw=new String(Files.readAllBytes(geometry(
                "word-boxes-"+suffix+".json")),StandardCharsets.UTF_8);
            Map<Integer,List<WordGeometryRepository.WordBox>> data=
                WordGeometryRepository.parseChunk(raw);
            for(Map.Entry<Integer,List<WordGeometryRepository.WordBox>> part:data.entrySet()){
                int page=part.getKey();
                JSONArray markers=IbnKathirRecitationStarts.forPageWords(index,
                    part.getValue(),page,null);
                pages.add(page);
                assertTrue("Page lacks a verified group start: "+page,markers.length()>0);
                for(int i=0;i<markers.length();i++){
                    JSONObject marker=markers.getJSONObject(i);
                    String id=marker.getString("id");
                    assertTrue("Duplicate verified start "+id,all.add(id));
                    assertEquals(IbnKathirRecitationStarts.STATUS,
                        marker.getString("status"));
                    assertEquals(1,marker.getInt("anchorWordCount"));
                    assertEquals(1,marker.getJSONArray("boxes").length());
                    assertEquals(4,marker.getJSONArray("boxes").getJSONArray(0).length());
                    assertEquals(0,marker.getJSONArray("ranges").length());
                    assertEquals(1,IbnKathirRecitationStarts.forPageWords(index,
                        part.getValue(),page,id).length());
                }
            }
        }
        assertEquals("Every Mushaf page must have documented starts",604,pages.size());
        assertEquals("Every verified group is accessible",1903,all.size());
    }
    @Test public void qaf518HasThreeTrueStartingGroupsAndNoInventedInteriorKeys() throws Exception {
        String raw=new String(Files.readAllBytes(geometry("word-boxes-451-604.json")),
            StandardCharsets.UTF_8);
        List<WordGeometryRepository.WordBox> p518=
            WordGeometryRepository.parseChunk(raw).get(518);
        IbnKathirGroupIndex index=IbnKathirGroupIndex.shared();
        JSONArray starts=IbnKathirRecitationStarts.forPageWords(index,p518,518,null);
        assertEquals(3,starts.length());
        assertEquals("IKEN050_001_005",starts.getJSONObject(0).getString("id"));
        assertEquals("IKEN050_006_011",starts.getJSONObject(1).getString("id"));
        assertEquals("IKEN050_012_015",starts.getJSONObject(2).getString("id"));
        assertEquals(0,IbnKathirRecitationStarts.forPageWords(index,p518,
            518,"IKEN050_016_022").length());
        assertEquals(0,IbnKathirRecitationStarts.forPageWords(index,
            java.util.Collections.emptyList(),518,null).length());
    }
    @Test public void readerSourceNowUsesIbnKathirWhileHifzCoreKeepsLegacyApi() throws Exception {
        String reader=source("StudyReaderActivity.java");
        assertTrue(reader.contains("recitationStarts.forPage(page)"));
        assertTrue(reader.contains("ibn_kathir_start_markers_visible"));
        assertTrue(reader.contains("IbnKathirQuranComSource.urlForGroup(group)"));
        assertFalse(reader.contains("semanticPassages.readerCuesForPage(page)"));
        assertFalse(reader.contains("putBoolean(\"semantic_cues_enabled\""));
        String hifz=source("HifzSessionActivity.java");
        assertTrue(hifz.contains("semanticPassages.readerCuesForPage(page)"));
        assertTrue(hifz.contains("applyActiveLandmarks("));
        assertTrue(hifz.contains("applyItqanAnchors("));
    }
}

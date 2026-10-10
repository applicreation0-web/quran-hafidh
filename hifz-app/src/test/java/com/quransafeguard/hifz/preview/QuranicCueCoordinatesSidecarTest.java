package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.*;

/** Read-only equivalence gate. This tests new coordinates against actual legacy runtime
 *  parsing without changing any session engine, cursor, backup or rendering path. */
public final class QuranicCueCoordinatesSidecarTest {
    private static String read(String path) throws Exception {
        Path p=Paths.get(path);
        if (!Files.exists(p)) p=Paths.get("..",path);
        return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
    }

    @Test public void everyCoordinateAliasPageAndWordCountEqualsUnchangedRuntime() throws Exception {
        String historical=read("hifz-app/build/generated/semanticAssets/semantic/semantic_passages_v2_1.json");
        String titles=read("hifz-app/build/generated/semanticAssets/semantic/semantic_titles_v2_3.json");
        SemanticPassageRepository.ParsedForTest original =
            SemanticPassageRepository.parseForTest(historical,titles);
        String minimal=read("hifz-app/build/generated/quranicCueAssets/quranic/quranic_cues_v1.json");
        JSONObject root=new JSONObject(minimal);
        assertEquals("QURANIC_CUES_COORDINATES_V1",root.getString("schema_version"));
        assertEquals(1243,root.getInt("cue_count"));
        assertEquals(1256,root.getInt("legacy_alias_count"));
        assertEquals(604,root.getInt("page_count"));
        assertEquals(77432,root.getInt("word_box_count_verified"));
        assertFalse(minimal.contains("title_fr"));
        assertFalse(minimal.contains("titleMunirAr"));
        assertFalse(minimal.contains("tafsir_munir_grouping"));
        assertFalse(minimal.contains("anchor_arabic"));
        JSONArray cues=root.getJSONArray("cues");
        assertEquals(1243,cues.length());

        Map<String, SemanticPassageRepository.Cue> canonical=new HashMap<>();
        for (SemanticPassageRepository.Cue old:original.byId.values())
            canonical.put(old.passageId,old);
        assertEquals(1243,canonical.size());
        Set<String> aliases=new HashSet<>();
        int pageEvents=0;
        for (int i=0;i<cues.length();i++) {
            JSONObject row=cues.getJSONObject(i);
            String id=row.getString("id");
            SemanticPassageRepository.Cue old=canonical.remove(id);
            assertNotNull("missing runtime cue "+id,old);
            assertEquals(old.startVerse,new VerseRef(row.getInt("surah"),row.getInt("start_ayah")));
            assertEquals(old.endVerse,new VerseRef(row.getInt("surah"),row.getInt("end_ayah")));
            assertEquals(old.startPage,row.getInt("start_page"));
            assertEquals(old.endPage,row.getInt("end_page"));
            assertEquals(old.anchorWordCount,row.getInt("word_count"));
            assertEquals(old.startLine,row.getInt("start_line"));
            assertEquals(old.firstWordId,row.getInt("first_word_id"));
            assertEquals(old.lastWordId,row.getInt("last_word_id"));
            assertEquals(old.firstWordPosition,row.getInt("first_word_position"));
            assertEquals(old.lastWordPosition,row.getInt("last_word_position"));

            JSONArray names=row.getJSONArray("aliases");
            for (int j=0;j<names.length();j++) {
                String key=names.getString(j);
                assertTrue("duplicate old ID "+key,aliases.add(key));
                assertEquals("alias to wrong cue "+key,id,original.byId.get(key).passageId);
            }
            JSONArray spans=row.getJSONArray("page_occurrences");
            for (int j=0;j<spans.length();j++) {
                JSONObject location=spans.getJSONObject(j);
                int page=location.getInt("page"), index=location.getInt("index");
                boolean found=false;
                for(SemanticPassageRepository.Cue existing:original.byPage.get(page)) {
                    if (existing.passageId.equals(id) && existing.indexOnPage==index) {
                        found=true;break;
                    }
                }
                assertTrue("page reference not equivalent "+id+" p"+page,found);
                pageEvents++;
            }
        }
        assertTrue("canonical original rows not mapped",canonical.isEmpty());
        assertEquals(1256,aliases.size());
        assertEquals(1256,original.byId.size());
        int oldPageEvents=0;
        for(java.util.List<SemanticPassageRepository.Cue> old:original.byPage.values())
            oldPageEvents+=old.size();
        assertEquals(oldPageEvents,pageEvents);
        assertEquals(604,original.byPage.size());
    }
}

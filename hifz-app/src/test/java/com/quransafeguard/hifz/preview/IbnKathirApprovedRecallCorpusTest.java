package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/**
 * J5: synthetic complete schema fixture is NEVER editorial approval.
 * Production requires an independently reviewed, SHA-PINNED asset.
 */
public final class IbnKathirApprovedRecallCorpusTest {
    private static Path source(String suffix){
        Path path=Paths.get("hifz-app/src/main/word-source/quran-ws-v1.1.2",suffix);
        if(!Files.exists(path))path=Paths.get("src/main/word-source/quran-ws-v1.1.2",suffix);
        return path;
    }
    private static Map<Integer,List<WordGeometryRepository.WordBox>> readGeometry()
            throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> all=new HashMap<>();
        for(String chunk:new String[]{"001-150","151-300","301-450","451-604"}){
            String raw=new String(Files.readAllBytes(source("word-boxes-"+chunk+".json")),
                StandardCharsets.UTF_8);
            all.putAll(WordGeometryRepository.parseChunk(raw));
        }
        assertEquals(604,all.size());
        return all;
    }
    private static JSONObject createSyntheticFixture(
            Map<Integer,List<WordGeometryRepository.WordBox>> pages)throws Exception{
        Map<String,Integer> firstWordPage=new HashMap<>();
        for(Map.Entry<Integer,List<WordGeometryRepository.WordBox>> page:pages.entrySet())
            for(WordGeometryRepository.WordBox word:page.getValue())
                if(word.word==1)firstWordPage.put(word.key,page.getKey());
        JSONObject root=new JSONObject()
            .put("schema",IbnKathirApprovedRecallCorpus.SCHEMA)
            .put("source_commit",IbnKathirGroupIndex.SOURCE_COMMIT)
            .put("boundary_sha256",IbnKathirAmorceBridge.BOUNDARY_SHA256)
            .put("hafs_sha256",IbnKathirAmorceBridge.HAFS_SHA256)
            .put("full_editorial_review_complete",true)
            .put("ready_for_runtime",true);
        JSONArray groups=new JSONArray();
        IbnKathirGroupIndex index=IbnKathirGroupIndex.shared();
        for(int chapter=1;chapter<=114;chapter++){
            for(IbnKathirGroupIndex.Group g:index.groupsForSurah(chapter)){
                String key=chapter+":"+g.startAyah+":1";
                assertNotNull("Missing real word "+key,firstWordPage.get(key));
                JSONObject cue=new JSONObject()
                    .put("first_word_key",key)
                    .put("word_count",1)
                    .put("semantic_recitation_start_approved",true)
                    .put("minimality_checked",true)
                    .put("word_span_qcf_verified",true);
                groups.put(new JSONObject()
                    .put("id",g.id)
                    .put("human_recitation_review_approved",true)
                    .put("cues",new JSONArray().put(cue)));
            }
        }
        assertEquals(1903,groups.length());
        return root.put("groups",groups);
    }
    private static void expectReject(JSONObject fixture,
            Map<Integer,List<WordGeometryRepository.WordBox>> pages)throws Exception{
        try {
            IbnKathirApprovedRecallCorpus.parseForTest(fixture.toString(),pages,
                IbnKathirGroupIndex.shared());
            fail("Unverified synthetic mutation passed the runtime gate");
        } catch(IllegalStateException expected){}
    }
    @Test public void disabledUnlessReviewedShaIsPinned() {
        assertFalse("Never activate a staging-only lexical draft",
            IbnKathirApprovedRecallCorpus.activationPinned());
        assertTrue(IbnKathirApprovedRecallCorpus.EXPECTED_APPROVED_MANIFEST_SHA256.isEmpty());
    }
    @Test public void exact604Page1903GroupAdapterAndNegativeCases() throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> pages=readGeometry();
        JSONObject fixture=createSyntheticFixture(pages);
        IbnKathirApprovedRecallCorpus.Corpus parsed=
            IbnKathirApprovedRecallCorpus.parseForTest(fixture.toString(),pages,
                IbnKathirGroupIndex.shared());
        assertEquals(604,parsed.byPage.size());
        assertEquals(1903,parsed.byId.size());
        for(List<SemanticPassageRepository.Cue> cues:parsed.byPage.values()){
            assertFalse(cues.isEmpty());
            for(SemanticPassageRepository.Cue cue:cues){
                assertEquals(cue.anchorWordCount,parsed.boxes(cue).length());
                assertFalse("Never use legacy Al-Munir titles",
                    cue.title.toLowerCase(java.util.Locale.ROOT).contains("munir"));
            }
        }
        JSONObject missing=new JSONObject(fixture.toString());
        missing.getJSONArray("groups").remove(0);
        expectReject(missing,pages);

        JSONObject falseApproval=new JSONObject(fixture.toString());
        falseApproval.getJSONArray("groups").getJSONObject(0)
            .put("human_recitation_review_approved",false);
        expectReject(falseApproval,pages);

        JSONObject forgedWord=new JSONObject(fixture.toString());
        forgedWord.getJSONArray("groups").getJSONObject(0).getJSONArray("cues")
            .getJSONObject(0).put("first_word_key","1:99:1");
        expectReject(forgedWord,pages);

        JSONObject crossedBoundary=new JSONObject(fixture.toString());
        crossedBoundary.getJSONArray("groups").getJSONObject(0).getJSONArray("cues")
            .getJSONObject(0).put("word_count",80);
        expectReject(crossedBoundary,pages);

        JSONObject badSource=new JSONObject(fixture.toString());
        badSource.put("boundary_sha256","bad-data");
        expectReject(badSource,pages);

        JSONObject notReady=new JSONObject(fixture.toString());
        notReady.put("ready_for_runtime",false);
        expectReject(notReady,pages);
    }
    @Test public void noHardQuotaPerDocumentaryGroupButNeverBlindItqanPage() throws Exception {
        Map<Integer,List<WordGeometryRepository.WordBox>> pages=readGeometry();
        JSONObject raw=createSyntheticFixture(pages);
        JSONArray groups=raw.getJSONArray("groups");
        // A reviewed group may deliberately contribute ZERO cue starts.
        // In this synthetic corpus the removal is rejected only if a physical
        // page loses its last recitation start, never because of a per-group quota.
        int uniquePageGroup=-1;
        java.util.Map<Integer,Integer> countOnPage=new java.util.HashMap<>();
        for(int page:pages.keySet()){
            int count=0;
            for(int i=0;i<groups.length();i++){
                JSONObject row=groups.getJSONObject(i);
                String key=row.getJSONArray("cues").getJSONObject(0)
                    .getString("first_word_key");
                for(WordGeometryRepository.WordBox word:pages.get(page))
                    if(word.key.equals(key)){count++;break;}
            }
            countOnPage.put(page,count);
        }
        // Qaf 50:1 starts on a page with at least 3 groups. Removing one
        // contributes no quota violation, leaving the page safely anchored.
        JSONObject allow=new JSONObject(raw.toString());
        JSONArray gg=allow.getJSONArray("groups");
        int qafIndex=-1;
        for(int i=0;i<gg.length();i++)
            if(gg.getJSONObject(i).getString("id").equals("IKEN050_001_005"))
                {qafIndex=i;break;}
        assertTrue(qafIndex>=0);
        assertTrue(countOnPage.get(518)>=3);
        gg.getJSONObject(qafIndex).put("cues",new JSONArray());
        IbnKathirApprovedRecallCorpus.Corpus parsed=
            IbnKathirApprovedRecallCorpus.parseForTest(allow.toString(),pages,
                IbnKathirGroupIndex.shared());
        assertEquals(1902,parsed.byId.size());
        assertEquals(604,parsed.byPage.size());
    }
}

package com.quransafeguard.hifz.preview;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** J3 staging contract. Never approves a Quran word or activates an Android cue. */
public final class IbnKathirAmorceBridgeTest {
    private static IbnKathirGroupIndex source;
    private static GeometryRepository geometry;
    private static String review;

    @BeforeClass public static void setUp() throws Exception {
        source=IbnKathirGroupIndex.shared();
        Path file=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.isRegularFile(file))file=Paths.get("..","app/src/main/assets/reader109/geometry.json");
        geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(file),StandardCharsets.UTF_8));
        String path=System.getenv("IK_J2_REVIEW_PATH");
        if(path!=null&&!path.isEmpty()) {
            review=new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8);
        }
    }
    @Test public void completeVerifiedSourceHasAllOriginalIntervals() {
        assertEquals(1903,source.count());
        int words=0;
        for(int surah=1;surah<=114;surah++)
            for(IbnKathirGroupIndex.Group g:source.groupsForSurah(surah))
                words+=g.endAyah-g.startAyah+1;
        assertEquals(6236,words);
    }
    @Test public void productionCiReviewCanNeverBeRuntimeCueData() throws Exception {
        Assume.assumeTrue("CI pins IK_J2_REVIEW_PATH",review!=null);
        IbnKathirAmorceBridge.StagedGate gate =
            IbnKathirAmorceBridge.inspectReviewOnly(review,source,geometry);
        assertEquals(1903,gate.documentaryGroups);
        assertEquals(604,gate.pagesWithAtLeastOneStart);
        assertEquals(1903,gate.pending);
        assertEquals(0,gate.approved);
        assertFalse(gate.runtimeReady);
    }
    private void rejects(JSONObject modified) throws Exception {
        try {
            IbnKathirAmorceBridge.inspectReviewOnly(modified.toString(),source,geometry);
            fail("invalid or prematurely approved data cannot enter the J3 handoff");
        } catch(IllegalStateException expected) {
            assertTrue(expected.getMessage().startsWith("J3 Ibn Kathir handoff NO GO"));
        }
    }
    @Test public void corruptReviewRightsAndGeometryAlwaysFailClosed() throws Exception {
        // The staging gate's structure and fail-closed mutations can be tested without
        // an external corpus. The documentary payload has its own pinned-input test.
        String baseline = review == null ? syntheticReview().toString() : review;
        assertFalse(IbnKathirAmorceBridge.inspectReviewOnly(baseline,source,geometry).runtimeReady);
        JSONObject activated = new JSONObject(baseline);
        activated.put("runtime_ready",true);
        rejects(activated);

        JSONObject falseApproval = new JSONObject(baseline);
        falseApproval.getJSONArray("entries").getJSONObject(0)
                     .put("human_disposition","APPROVED");
        rejects(falseApproval);

        JSONObject fakeCoordinates = new JSONObject(baseline);
        fakeCoordinates.getJSONArray("entries").getJSONObject(0)
                       .put("start_page",604);
        rejects(fakeCoordinates);

        JSONObject fakeRights = new JSONObject(baseline);
        fakeRights.getJSONArray("entries").getJSONObject(0)
                  .put("text_reuse_rights_confirmed",true);
        rejects(fakeRights);

        JSONObject truncated = new JSONObject(baseline);
        JSONArray rows=truncated.getJSONArray("entries");
        rows.remove(rows.length()-1);
        rejects(truncated);
    }

    private static JSONObject syntheticReview() throws Exception {
        JSONArray rows = new JSONArray();
        for (int surah = 1; surah <= 114; surah++) {
            for (var group : source.groupsForSurah(surah)) {
                rows.put(new JSONObject().put("id",group.id).put("surah",surah)
                    .put("start_ayah",group.startAyah).put("end_ayah",group.endAyah)
                    .put("start_page",geometry.pageForVerse(new com.quransafeguard.hifz.core.VerseRef(surah,group.startAyah)))
                    .put("end_page",geometry.pageForVerse(new com.quransafeguard.hifz.core.VerseRef(surah,group.endAyah)))
                    .put("first_word_key",surah+":"+group.startAyah+":1")
                    .put("human_disposition","UNREVIEWED").put("approved_keys",new JSONArray())
                    .put("extra_cues_within_group",new JSONArray()).put("runtime_enabled",false)
                    .put("word_by_word_qcf_verified_for_selected_cue",false)
                    .put("mnemonic_context_minimality_approved",false).put("text_reuse_rights_confirmed",false));
            }
        }
        return new JSONObject().put("schema","IK_J2_CONTRADICTORY_REVIEW_V1").put("runtime_ready",false)
            .put("provenance",new JSONObject().put("ibn_kathir_boundaries_sha256",IbnKathirAmorceBridge.BOUNDARY_SHA256)
                .put("quran_ws_hafs_sha256",IbnKathirAmorceBridge.HAFS_SHA256))
            .put("counts",new JSONObject().put("groups",rows.length()).put("approved_keys",0)
                .put("groups_pending_editorial_decision",rows.length())).put("entries",rows);
    }
}

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
        Assume.assumeTrue(review!=null);
        JSONObject activated = new JSONObject(review);
        activated.put("runtime_ready",true);
        rejects(activated);

        JSONObject falseApproval = new JSONObject(review);
        falseApproval.getJSONArray("entries").getJSONObject(0)
                     .put("human_disposition","APPROVED");
        rejects(falseApproval);

        JSONObject fakeCoordinates = new JSONObject(review);
        fakeCoordinates.getJSONArray("entries").getJSONObject(0)
                       .put("start_page",604);
        rejects(fakeCoordinates);

        JSONObject fakeRights = new JSONObject(review);
        fakeRights.getJSONArray("entries").getJSONObject(0)
                  .put("text_reuse_rights_confirmed",true);
        rejects(fakeRights);

        JSONObject truncated = new JSONObject(review);
        JSONArray rows=truncated.getJSONArray("entries");
        rows.remove(rows.length()-1);
        rejects(truncated);
    }
}

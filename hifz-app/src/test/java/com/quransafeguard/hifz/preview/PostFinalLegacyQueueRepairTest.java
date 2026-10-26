package com.quransafeguard.hifz.preview;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Exercise real queue JSON migration, a process restart, and fail-closed old files. */
public final class PostFinalLegacyQueueRepairTest {
    private static GeometryRepository geometry() throws Exception {
        StringBuilder json=new StringBuilder("{\"pages\":{");
        for(int p=1;p<=604;p++){
            if(p>1)json.append(',');
            json.append('"').append(p).append("\":{\"lines\":[");
            json.append("{\"id\":\"p").append(p)
                .append("-l0\",\"top\":1,\"bottom\":2,\"verses\":[]}");
            json.append("]}");
        }
        return GeometryRepository.fromJson(json.append("}}").toString());
    }
    private static JSONObject entry(String family,String status,boolean completed) throws Exception {
        JSONArray pages=new JSONArray();
        for(int page:new int[]{250,290,291}) {
            pages.put(new JSONObject().put("page",page)
                .put("lines",new JSONArray().put("p"+page+"-l0"))
                .put("current",page!=250));
        }
        return new JSONObject().put("family",family).put("date","2026-11-01")
            .put("status",status).put("position",completed?3:0)
            .put("previous",1).put("pages",pages);
    }
    @Test public void alreadyPrepared1177QueueOnlyReplaysProvenSabqiPagesNotItqan() throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        String learning=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Collections.singletonList("p290-l0"));
        String itqan=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Collections.singletonList("p291-l0"));
        disk.disk.put("learningSnowball8WeekHistory",new JSONArray().put(
            new JSONObject().put("week","2026-10-26")
                .put("units",new JSONArray().put(learning))).toString());
        disk.disk.put("stabilizationSnowball8WeekHistory",new JSONArray().put(
            new JSONObject().put("week","2026-10-26")
                .put("units",new JSONArray().put(itqan))).toString());
        disk.disk.put(PostFinalRecallStore.KEY,
            new JSONArray().put(entry(PostFinalRecallStore.SABQI,"READY",false)).toString());
        LinkedHashSet<String> acquired=new LinkedHashSet<>(Arrays.asList(
            "p250-l0","p290-l0","p291-l0"));
        PostFinalRecallStore queue=new PostFinalRecallStore(disk);
        assertEquals(1,queue.repairLegacyPendingFamilyPlans(geometry(),acquired));
        assertEquals(0,new PostFinalRecallStore(disk)
            .repairLegacyPendingFamilyPlans(geometry(),acquired));
        PostFinalRecallStore.Review review=queue.nextReadyFor(PostFinalRecallStore.SABQI,
            "2026-11-01","");
        assertNotNull(review);
        assertEquals(1,review.total);
        assertEquals(290,review.page);
        assertEquals(Collections.singletonList("p290-l0"),review.lines);
        assertFalse("Never convert the unrelated Itqan p291 into Sabqi",review.lines.contains("p291-l0"));
        assertFalse(disk.disk.containsKey("v6AcquiredCreditLineIds"));
        assertEquals(3,acquired.size());
    }
    @Test public void unrecoverableLegacyQueueIsBlockedWithoutDeletingCreditsOrItsEvidence()
            throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put(PostFinalRecallStore.KEY,new JSONArray()
            .put(entry(PostFinalRecallStore.ITQAN,"READY",false)).toString());
        PostFinalRecallStore store=new PostFinalRecallStore(disk);
        assertEquals(1,store.repairLegacyPendingFamilyPlans(geometry(),
            new LinkedHashSet<>(Arrays.asList("p250-l0","p290-l0","p291-l0"))));
        assertTrue(store.hasBlockedLegacyFamily(PostFinalRecallStore.ITQAN));
        assertNull(store.nextReadyFor(PostFinalRecallStore.ITQAN,"","2026-11-01"));
        JSONObject frozen=new JSONArray(disk.disk.get(PostFinalRecallStore.KEY).toString())
            .getJSONObject(0);
        assertEquals("PROVENANCE_REVIEW",frozen.getString("status"));
        assertEquals(3,frozen.getJSONArray("pages").length());
        assertEquals(0,frozen.getInt("position"));
        assertFalse(disk.disk.containsKey("v6AcquiredCreditLineIds"));
    }
    @Test public void completed1177ReadingsRemainFrozenAuditableAndNeverGenerateNewDebt()
            throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        String raw=new JSONArray().put(entry(PostFinalRecallStore.SABQI,"DONE",true)).toString();
        disk.disk.put(PostFinalRecallStore.KEY,raw);
        assertEquals(0,new PostFinalRecallStore(disk).repairLegacyPendingFamilyPlans(
            geometry(),Collections.emptySet()));
        assertEquals(raw,disk.disk.get(PostFinalRecallStore.KEY));
    }
}

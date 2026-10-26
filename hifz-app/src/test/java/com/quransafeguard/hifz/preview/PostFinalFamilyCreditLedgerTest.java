package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** 1.17.8 isolation: physical post-final pages come ONLY from their proven family. */
public final class PostFinalFamilyCreditLedgerTest {
    static final ConsolidationCycleEngine.Family SABQI=ConsolidationCycleEngine.Family.LEARNING;
    static final ConsolidationCycleEngine.Family ITQAN=ConsolidationCycleEngine.Family.STABILIZATION;
    static GeometryRepository.LineMeta line(int page) {
        return new GeometryRepository.LineMeta(page*15,"p"+page+"-l0",page,
            Collections.<VerseRef>emptyList());
    }
    static List<GeometryRepository.LineMeta> metas(List<String> ids) {
        ArrayList<GeometryRepository.LineMeta> result=new ArrayList<>();
        for(String id:ids) {
            int first=id.indexOf('-');
            result.add(line(Integer.parseInt(id.substring(1,first))));
        }
        return result;
    }
    static JSONObject queueEntry(String family,String date,boolean tagged,String status,String... ids)
            throws Exception {
        JSONArray pages=new JSONArray();
        for(String id:ids) {
            int page=Integer.parseInt(id.substring(1,id.indexOf('-')));
            pages.put(new JSONObject().put("page",page).put("current",true)
                .put("lines",new JSONArray().put(id)));
        }
        JSONObject entry=new JSONObject().put("family",family).put("date",date)
            .put("status",status).put("position",0).put("previous",0)
            .put("pages",pages);
        if(tagged)entry.put("familySourceVersion",1);
        return entry;
    }

    @Test public void globalMixedAcquisCannotBleedBetweenSabqiAndItqanAcrossEightWeeks()
            throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        Set<String> acquired=new LinkedHashSet<>();
        JSONArray queue=new JSONArray();
        ArrayList<String> sabqi=new ArrayList<>(),itqan=new ArrayList<>();
        for(int p=100;p<125;p++){
            String s="p"+p+"-l0",t="p"+(p+100)+"-l0";
            acquired.add(s);acquired.add(t);sabqi.add(s);itqan.add(t);
            String date=java.time.LocalDate.of(2026,10,25).plusWeeks(p-100).toString();
            queue.put(queueEntry(PostFinalRecallStore.SABQI,date,true,"DONE",s));
            queue.put(queueEntry(PostFinalRecallStore.ITQAN,date,true,"DONE",t));
        }
        disk.disk.put(PostFinalRecallStore.KEY,queue.toString());
        List<String> selectedSabqi=PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired);
        List<String> selectedItqan=PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,acquired);
        assertEquals(sabqi,selectedSabqi);
        assertEquals(itqan,selectedItqan);
        List<PostFinalCumulativePolicy.Page> sabqiPages=PostFinalCumulativePolicy.plan(
            metas(selectedSabqi),Collections.singletonList(line(125)));
        List<PostFinalCumulativePolicy.Page> itqanPages=PostFinalCumulativePolicy.plan(
            metas(selectedItqan),Collections.singletonList(line(225)));
        assertEquals(20,sabqiPages.size());assertEquals(20,itqanPages.size());
        assertEquals(106,sabqiPages.get(0).mushafPage);
        assertEquals(206,itqanPages.get(0).mushafPage);
        assertEquals(125,sabqiPages.get(19).mushafPage);
        assertEquals(225,itqanPages.get(19).mushafPage);
        for(PostFinalCumulativePolicy.Page p:sabqiPages)assertTrue(p.mushafPage<200);
        for(PostFinalCumulativePolicy.Page p:itqanPages)assertTrue(p.mushafPage>=200);
        assertEquals(50,acquired.size()); // read-only, no fake credits
        assertFalse(disk.disk.containsKey("v6AcquiredCreditLineIds"));
    }

    @Test public void oldBackupBootstrapUsesOnlyFullyAcquiredPhysicalUnitsOfRequestedFamily()
            throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        String s1=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p31-l0","p31-l1"));
        String s2=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p32-l0","p32-l1"));
        String t1=ConsolidationPhysicalUnitPolicy.encodeLineUnit(Arrays.asList("p501-l0","p501-l1"));
        disk.disk.put("learningSnowball8WeekHistory",
            new JSONArray().put(new JSONObject().put("week","2026-10-19")
                .put("units",new JSONArray().put(s1).put(s2))).toString());
        disk.disk.put("stabilizationSnowball8WeekHistory",
            new JSONArray().put(new JSONObject().put("week","2026-10-19")
                .put("units",new JSONArray().put(t1))).toString());
        Set<String> acquired=new LinkedHashSet<>(Arrays.asList(
            "p31-l0","p31-l1","p32-l0","p501-l0","p501-l1","p400-l0"));
        assertEquals(Arrays.asList("p31-l0","p31-l1"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired));
        assertEquals(Arrays.asList("p501-l0","p501-l1"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,acquired));
    }

    @Test public void old1177CurrentPageCannotSmuggleOppositeFamilyAcquis()
            throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        String earned=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Collections.singletonList("p290-l0"));
        disk.disk.put("learningSnowball8WeekHistory",new JSONArray()
            .put(new JSONObject().put("week","2026-10-26")
                .put("units",new JSONArray().put(earned))).toString());
        JSONObject legacy=queueEntry(PostFinalRecallStore.SABQI,"2026-11-01",false,
            "READY","p290-l0","p290-l1");
        disk.disk.put(PostFinalRecallStore.KEY,new JSONArray().put(legacy).toString());
        Set<String> acquired=new LinkedHashSet<>(Arrays.asList("p290-l0","p290-l1"));
        assertEquals(Collections.singletonList("p290-l0"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired));
        assertTrue(PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,acquired).isEmpty());
    }

    @Test public void uncommittedPreparedReviewCannotBecomeFamilyProvenance() throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put(PostFinalRecallStore.KEY,new JSONArray().put(queueEntry(
            PostFinalRecallStore.SABQI,"2026-11-01",true,"PREPARED","p400-l0")).toString());
        Set<String> acquired=Collections.singleton("p400-l0");
        assertTrue(PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired).isEmpty());
        disk.disk.put("lastLearningConsolidationDate","2026-11-01");
        assertEquals(Collections.singletonList("p400-l0"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired));
    }

    @Test public void legacyQueueCannotTurnPartlyAcquiredUnitIntoFamilyProof() throws Exception {
        InMemoryPrefs disk=new InMemoryPrefs();
        String unit=ConsolidationPhysicalUnitPolicy.encodeLineUnit(
            Arrays.asList("p290-l0","p290-l1"));
        disk.disk.put("learningSnowball8WeekHistory",new JSONArray().put(
            new JSONObject().put("week","2026-10-26")
                .put("units",new JSONArray().put(unit))).toString());
        disk.disk.put(PostFinalRecallStore.KEY,new JSONArray().put(queueEntry(
            PostFinalRecallStore.SABQI,"2026-11-01",false,"READY","p290-l0")).toString());
        assertTrue("An incomplete physical unit is not proof, even with an old queue receipt",
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,
                Collections.singleton("p290-l0")).isEmpty());
    }

    @Test(expected=IllegalStateException.class)
    public void malformedFamilyHistoryFailsClosedInsteadOfTakingAllAcquis() {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put("stabilizationSnowball8WeekHistory","{bad");
        PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,
            new HashSet<>(Arrays.asList("p1-l0","p2-l0")));
    }

    @Test public void twentyOneCurrentFinalPagesNeverGrowBeyondTwenty() {
        ArrayList<GeometryRepository.LineMeta> current=new ArrayList<>();
        for(int p=300;p<=320;p++)current.add(line(p));
        List<PostFinalCumulativePolicy.Page> plan=PostFinalCumulativePolicy.plan(
            Arrays.asList(line(100),line(101)),current);
        assertEquals(20,plan.size());
        assertEquals(301,plan.get(0).mushafPage);
        assertEquals(320,plan.get(19).mushafPage);
        assertEquals(0,plan.stream().filter(page->!page.fromCurrentFinal).count());
    }
}

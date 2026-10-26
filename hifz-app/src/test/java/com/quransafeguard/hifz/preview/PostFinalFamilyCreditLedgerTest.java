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

/** Strict 1.17.8 invariant: twenty RECENT family pages, no global Acquis bleed. */
public final class PostFinalFamilyCreditLedgerTest {
    static final ConsolidationCycleEngine.Family SABQI=ConsolidationCycleEngine.Family.LEARNING;
    static final ConsolidationCycleEngine.Family ITQAN=ConsolidationCycleEngine.Family.STABILIZATION;

    static GeometryRepository.LineMeta line(int page) {
        return new GeometryRepository.LineMeta(page*15,"p"+page+"-l0",page,
            Collections.<VerseRef>emptyList());
    }
    static List<GeometryRepository.LineMeta> metas(List<String> ids) {
        ArrayList<GeometryRepository.LineMeta> out=new ArrayList<>();
        for(String id:ids) {
            int first=id.indexOf('-');
            out.add(line(Integer.parseInt(id.substring(1,first))));
        }
        return out;
    }

    @Test public void twoFamiliesAreIndependentEvenWhenGlobalAcquisIsMixed() {
        InMemoryPrefs disk=new InMemoryPrefs();
        Set<String> acquired=new LinkedHashSet<>();
        ArrayList<String> sabqi=new ArrayList<>(),itqan=new ArrayList<>();
        for(int p=100;p<125;p++){
            String s="p"+p+"-l0",t="p"+(p+100)+"-l0";
            acquired.add(s);acquired.add(t);sabqi.add(s);itqan.add(t);
        }
        JSONArray s=new JSONArray(),t=new JSONArray();
        for(String id:sabqi)s.put(id);
        for(String id:itqan)t.put(id);
        disk.disk.put(PostFinalFamilyCreditLedger.key(SABQI),s.toString());
        disk.disk.put(PostFinalFamilyCreditLedger.key(ITQAN),t.toString());
        List<String> selectedSabqi=PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired);
        List<String> selectedItqan=PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,acquired);
        assertEquals(sabqi,selectedSabqi);
        assertEquals(itqan,selectedItqan);
        List<PostFinalCumulativePolicy.Page> sabqiPages=PostFinalCumulativePolicy.plan(
            metas(selectedSabqi),Collections.singletonList(line(125)));
        List<PostFinalCumulativePolicy.Page> itqanPages=PostFinalCumulativePolicy.plan(
            metas(selectedItqan),Collections.singletonList(line(225)));
        assertEquals(20,sabqiPages.size());
        assertEquals(20,itqanPages.size());
        assertEquals(106,sabqiPages.get(0).mushafPage);
        assertEquals(206,itqanPages.get(0).mushafPage);
        assertEquals(125,sabqiPages.get(19).mushafPage);
        assertEquals(225,itqanPages.get(19).mushafPage);
        for(PostFinalCumulativePolicy.Page p:sabqiPages)assertTrue(p.mushafPage<200);
        for(PostFinalCumulativePolicy.Page p:itqanPages)assertTrue(p.mushafPage>=200);
        assertEquals(acquired.size(),50); // no credit side effects
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
        LinkedHashSet<String> acquired=new LinkedHashSet<>(Arrays.asList(
            "p31-l0","p31-l1","p32-l0","p501-l0","p501-l1","p400-l0"));
        assertEquals(Arrays.asList("p31-l0","p31-l1"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,acquired));
        assertEquals(Arrays.asList("p501-l0","p501-l1"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,acquired));
        // Global p400-l0 has no Sabqi or Itqan provenance: NEVER inferred.
    }

    @Test public void closingEachFamilyAtomicallyExtendsItsOwnPreviousCreditWithoutDoubleAttribution() {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put(PostFinalFamilyCreditLedger.key(SABQI),new JSONArray().put("p200-l0").toString());
        disk.disk.put(PostFinalFamilyCreditLedger.key(ITQAN),new JSONArray().put("p400-l0").toString());
        Set<String> before=new LinkedHashSet<>(Arrays.asList("p200-l0","p400-l0"));
        Set<String> afterSabqi=new LinkedHashSet<>(before);afterSabqi.add("p201-l0");
        String changedSabqi=PostFinalFamilyCreditLedger.afterFinal(disk,SABQI,before,afterSabqi,
            Arrays.asList("p201-l0","p200-l0"));
        disk.disk.put(PostFinalFamilyCreditLedger.key(SABQI),changedSabqi);
        assertEquals(Arrays.asList("p200-l0","p201-l0"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,SABQI,afterSabqi));
        assertEquals(Collections.singletonList("p400-l0"),
            PostFinalFamilyCreditLedger.previouslyAcquired(disk,ITQAN,afterSabqi));
        assertEquals(before.size()+1,afterSabqi.size());
    }

    @Test(expected=IllegalStateException.class)
    public void neverLabelUnacquiredLineAsFamilyCredit() {
        InMemoryPrefs disk=new InMemoryPrefs();
        PostFinalFamilyCreditLedger.afterFinal(disk,SABQI,
            Collections.emptySet(),Collections.emptySet(),
            Collections.singletonList("p999-l0"));
    }

    @Test(expected=IllegalStateException.class)
    public void malformedFamilyLedgerFailsClosedInsteadOfTakingAllAcquis() {
        InMemoryPrefs disk=new InMemoryPrefs();
        disk.disk.put(PostFinalFamilyCreditLedger.key(ITQAN),"{oops");
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

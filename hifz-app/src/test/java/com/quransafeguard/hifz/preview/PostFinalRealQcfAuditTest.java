package com.quransafeguard.hifz.preview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Public canonical geometry only; never embeds the private backup. */
public final class PostFinalRealQcfAuditTest {
    private static GeometryRepository geometry() throws Exception {
        Path p=Path.of("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(p))p=Path.of("../app/src/main/assets/reader109/geometry.json");
        return GeometryRepository.fromJson(Files.readString(p));
    }
    private static JSONObject week(String date,List<String> ids) throws Exception {
        return new JSONObject().put("week",date).put("units",new JSONArray()
            .put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(ids)));
    }
    @Test public void realPhysicalBoundariesAndNasWrapPreserveChronologyAndExactLines() throws Exception {
        GeometryRepository g=geometry();
        for(int n:new int[]{0,1,19,20,21,29}) {
            ArrayList<GeometryRepository.LineMeta> prior=new ArrayList<>();
            for(int i=0;i<n;i++)prior.addAll(g.linesForExactIds(g.lineIdsOnPage(604-i)));
            List<PostFinalCumulativePolicy.Page> plan=PostFinalCumulativePolicy.plan(prior,
                g.linesForExactIds(g.lineIdsOnPage(1)));
            assertEquals(Math.min(20,n+1),plan.size());
            assertEquals(1,plan.get(plan.size()-1).mushafPage);
            Set<Integer> seen=new HashSet<>();
            for(PostFinalCumulativePolicy.Page page:plan) {
                assertTrue(seen.add(page.mushafPage));
                assertEquals(g.lineIdsOnPage(page.mushafPage),page.lineIds);
            }
            if(n>0)assertEquals(604-Math.max(0,n-19),plan.get(0).mushafPage);
        }
        List<String> ids=g.lineIdsOnPage(522);
        List<GeometryRepository.LineMeta> oldLines=new ArrayList<>(g.linesForExactIds(ids.subList(0,2)));
        List<GeometryRepository.LineMeta> newLines=new ArrayList<>(g.linesForExactIds(ids.subList(1,3)));
        oldLines.add(oldLines.get(0));newLines.add(newLines.get(1));
        List<PostFinalCumulativePolicy.Page> grouped=PostFinalCumulativePolicy.plan(oldLines,newLines);
        assertEquals(1,grouped.size());
        assertEquals(ids.subList(0,3),grouped.get(0).lineIds);
    }
    @Test public void legacyPreparedReadyDoneAndPartialPrefixesSurviveRestartForBothFamilies() throws Exception {
        GeometryRepository g=geometry();
        for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN})
        for(String status:new String[]{"PREPARED","READY","DONE"})
        for(int position:new int[]{0,1}) {
            InMemoryPrefs disk=new InMemoryPrefs();
            String key=PostFinalRecallStore.SABQI.equals(family)
                ?"learningSnowball8WeekHistory":"stabilizationSnowball8WeekHistory";
            String completed=PostFinalRecallStore.SABQI.equals(family)
                ?"lastLearningConsolidationDate":"lastRecentSabqiReviewDate";
            disk.disk.put(completed,"2026-11-01");
            List<String> prior=g.lineIdsOnPage(522),current=g.lineIdsOnPage(523),alien=g.lineIdsOnPage(12);
            disk.disk.put(key,new JSONArray().put(week("2026-10-19",prior))
                .put(week("2026-10-26",current)).toString());
            JSONArray pages=new JSONArray();
            for(int page:new int[]{522,523,12})pages.put(new JSONObject().put("page",page)
                .put("current",page!=522).put("lines",new JSONArray(g.lineIdsOnPage(page))));
            JSONObject entry=new JSONObject().put("family",family).put("date","2026-11-01")
                .put("status",status).put("position",position).put("previous",1).put("pages",pages);
            String original=new JSONArray().put(entry).toString();disk.disk.put(PostFinalRecallStore.KEY,original);
            LinkedHashSet<String> acquired=new LinkedHashSet<>(prior);acquired.addAll(current);acquired.addAll(alien);
            Map<String,?> before=disk.getAll();
            PostFinalRecallStore queue=new PostFinalRecallStore(disk);
            assertEquals("DONE".equals(status)?0:1,queue.repairLegacyPendingFamilyPlans(g,acquired));
            if("DONE".equals(status)) {assertEquals(original,disk.getString(PostFinalRecallStore.KEY,""));continue;}
            queue=new PostFinalRecallStore(disk);
            assertEquals(0,queue.repairLegacyPendingFamilyPlans(g,acquired));
            String sabqi=PostFinalRecallStore.SABQI.equals(family)?"2026-11-01":"";
            String itqan=PostFinalRecallStore.ITQAN.equals(family)?"2026-11-01":"";
            PostFinalRecallStore.Review review=queue.nextReadyFor(family,sabqi,itqan);
            assertEquals(position,review.position);assertEquals(2,review.total);
            assertEquals(position==0?522:523,review.page);
            assertTrue(Collections.disjoint(review.lines,alien));
            assertTrue(queue.advance(review,sabqi,itqan));
            assertFalse(queue.advance(review,sabqi,itqan));
            assertEquals(before.get(completed),disk.getAll().get(completed));
            assertEquals(before.get(key),disk.getAll().get(key));
            assertFalse(disk.contains("v6AcquiredCreditLineIds"));
        }
    }
}

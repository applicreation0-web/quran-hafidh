package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.Assert.*;

/** Actual Android Activity: masked post-final queue, restart and unchanged Acquis/final states. */
public final class PostFinalRecallInstrumentedTest {
    @Test public void passiveCorpusReadsChronologicalLearnedLinesWithoutChangingAndroidPreferences() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            HifzPrefs prefs=new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            ArrayList<String> lines=new ArrayList<>(geometry.lineIdsOnPage(16));
            lines.addAll(geometry.lineIdsOnPage(520));
            assertTrue(raw.edit().putString("v6LearnedLineIds",new JSONArray(lines).toString()).commit());
            Object expected=prefs.murajaahCorpus(geometry).getRanges();
            java.util.Collections.reverse(lines);
            assertTrue(raw.edit().putString("v6LearnedLineIds",new JSONArray(lines).toString()).commit());
            Map<String,?> before=raw.getAll();
            assertEquals(expected,new HifzPrefs(c).murajaahCorpus(geometry).getRanges());
            assertEquals(before,raw.getAll());
        } finally {assertTrue(raw.edit().clear().commit());}
    }

    @Test public void unprovenLegacyRecallShowsReviewStateOnActualActivityWithoutCreditChange() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            JSONObject page=new JSONObject().put("page",12).put("current",true)
                .put("lines",new JSONArray(geometry.lineIdsOnPage(12)));
            JSONObject legacy=new JSONObject().put("family",PostFinalRecallStore.ITQAN)
                .put("date","2026-10-25").put("status","READY").put("position",0)
                .put("previous",0).put("pages",new JSONArray().put(page));
            assertTrue(raw.edit().putString(PostFinalRecallStore.KEY,new JSONArray().put(legacy).toString()).commit());
            String acquired=raw.getString("v6AcquiredCreditLineIds","");
            Intent intent=new Intent(c,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.POST_FINAL_RECALL)
                .putExtra(HifzSessionActivity.EXTRA_POST_FINAL_FAMILY,PostFinalRecallStore.ITQAN);
            try(ActivityScenario<HifzSessionActivity> screen=ActivityScenario.launch(intent)) {
                screen.onActivity(activity->{
                    try {
                        Field field=HifzSessionActivity.class.getDeclaredField("program");field.setAccessible(true);
                        assertTrue(((android.widget.TextView)field.get(activity)).getText().toString().contains("provenance à vérifier"));
                        field=HifzSessionActivity.class.getDeclaredField("currentPostFinalReview");field.setAccessible(true);
                        assertNull(field.get(activity));
                    } catch(ReflectiveOperationException e) {throw new AssertionError(e);}
                });
            }
            assertEquals(acquired,raw.getString("v6AcquiredCreditLineIds",""));
            JSONObject saved=new JSONArray(raw.getString(PostFinalRecallStore.KEY,"[]")).getJSONObject(0);
            assertEquals("PROVENANCE_REVIEW",saved.getString("status"));
            assertEquals(legacy.getJSONArray("pages").toString(),saved.getJSONArray("pages").toString());
        } finally {assertTrue(raw.edit().clear().commit());}
    }

    @Test public void migratedUserCadenceShowsSevenActualDayDetailsWithoutFallback()
            throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c); // real schema bootstrap
            assertTrue(raw.edit()
                .putString("programStartDate","2026-09-21")
                .putBoolean("sevenDayProgrammeMigratedV1",true)
                .putString("weeklyProgramTransitionsV1",
                    "[{\"from\":\"2026-10-23\",\"mode\":\"RECOMMENDED\"}]")
                .commit());
            HifzPrefs prefs=new HifzPrefs(c);
            List<WeeklyDashboardPlanner.Row> days=new WeeklyDashboardPlanner(
                prefs,GeometryRepository.get(c),new DashboardLedger(c),
                new HifzSpeedStore(c)).week(java.time.LocalDate.of(2026,10,26));
            assertEquals(7,days.size());
            assertEquals("Monday lesson",java.time.LocalDate.of(2026,10,26),days.get(0).date);
            assertEquals("Sunday finals",java.time.LocalDate.of(2026,11,1),days.get(6).date);
            assertTrue("Monday should show actual Sabqi",days.get(0).evening.contains("Apprentissage"));
            assertTrue("Tuesday should show actual Itqan",days.get(1).evening.contains("Stabilisation"));
            assertTrue("Sunday should show Sabqi final",days.get(6).morning.contains("Finale Sabqi"));
            assertTrue("Sunday should show Itqan final",days.get(6).evening.contains("Finale Itqān"));
            for(WeeklyDashboardPlanner.Row day:days){
                assertFalse("Valid week must not degrade to placeholders: "+day.date,
                    day.state.contains("Projection à vérifier"));
            }
        }finally{assertTrue(raw.edit().clear().commit());}
    }


    @Test public void fourFutureSundayProjectionsDoNotInventValidatedFinales()
            throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c);
            assertTrue(raw.edit().putString("programStartDate","2026-09-21")
                .putBoolean("sevenDayProgrammeMigratedV1",true)
                .putString("weeklyProgramTransitionsV1",
                    "[{\"from\":\"2026-10-23\",\"mode\":\"RECOMMENDED\"}]")
                .commit());
            HifzPrefs prefs=new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            DashboardLedger ledger=new DashboardLedger(c);
            HifzSpeedStore speed=new HifzSpeedStore(c);
            for(int w=0;w<4;w++){
                java.time.LocalDate monday=java.time.LocalDate.of(2026,10,26).plusWeeks(w);
                List<WeeklyDashboardPlanner.Row> rows=new WeeklyDashboardPlanner(
                    prefs,geometry,ledger,speed).week(monday);
                assertEquals(7,rows.size());
                assertEquals(monday.plusDays(6),rows.get(6).date);
                assertTrue(rows.get(6).morning.contains("Finale Sabqi"));
                assertTrue(rows.get(6).evening.contains("Finale Itqān"));
                assertFalse("No synthetic Sunday Sabqi validation "+monday,
                    rows.get(6).morning.contains("✓ Finale"));
                assertFalse("No synthetic Sunday Itqan validation "+monday,
                    rows.get(6).evening.contains("✓ Finale"));
                assertFalse("Sunday cannot count as validated without final receipts",
                    "Validé".equals(rows.get(6).state));
                assertFalse(rows.get(6).morning.contains("Révision active"));
                assertFalse(rows.get(6).morning.contains("Révision passive"));
            }
        }finally{assertTrue(raw.edit().clear().commit());}
    }
    @Test public void corruptFrozenItqanCannotEraseRealMondayCalendarOrSevenDates()
            throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            HifzPrefs prefs=new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            // Make both Itqan sources fail before row rendering. They must
            // remain OPEN/unmodified; independent Monday Sabqi still projects.
            String malformed="{unparseable frozen private session}";
            assertTrue(raw.edit().putString(ItqanFourSessionStore.KEY,malformed).commit());
            List<WeeklyDashboardPlanner.Row> days=new WeeklyDashboardPlanner(
                prefs,geometry,new DashboardLedger(c),new HifzSpeedStore(c))
                .week(java.time.LocalDate.of(2026,10,26));
            assertEquals(7,days.size());
            assertEquals(java.time.LocalDate.of(2026,10,26),days.get(0).date);
            assertEquals(java.time.LocalDate.of(2026,11,1),days.get(6).date);
            assertTrue("Actual Monday learning details must survive broken Itqan",
                days.get(0).evening.contains("Apprentissage"));
            assertEquals(malformed,raw.getString(ItqanFourSessionStore.KEY,""));
        }finally{assertTrue(raw.edit().clear().commit());}
    }

    @Test public void committedOctoberItqanRecoveryExcludesUnprovenOtherFamilyAcquis()
            throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            List<String> prior=geometry.lineIdsOnPage(522);
            List<String> current=geometry.lineIdsOnPage(523);
            List<String> otherFamily=geometry.lineIdsOnPage(12);
            LinkedHashSet<String> acquired=new LinkedHashSet<>();
            acquired.addAll(prior);acquired.addAll(current);acquired.addAll(otherFamily);
            JSONArray history=new JSONArray();
            history.put(new JSONObject().put("week","2026-10-12").put("units",
                new JSONArray().put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(prior))));
            history.put(new JSONObject().put("week","2026-10-19").put("units",
                new JSONArray().put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(current))));
            JSONArray acquiredJson=new JSONArray();
            for(String id:acquired)acquiredJson.put(id);
            assertTrue(raw.edit()
                .putString("stabilizationSnowball8WeekHistory",history.toString())
                .putString("lastRecentSabqiReviewDate","2026-10-25")
                .putString("v6AcquiredCreditLineIds",acquiredJson.toString())
                .commit());
            String beforeAcquired=raw.getString("v6AcquiredCreditLineIds","");
            String beforeFinal=raw.getString("v6ConsolidationStabilizationStateFinalV1","");
            PostFinalRecallStore queue=new PostFinalRecallStore(c);
            assertTrue(queue.recoverCommittedFromHistory(PostFinalRecallStore.ITQAN,
                "2026-10-25",acquired,geometry));
            PostFinalRecallStore.Review review=queue.nextReady("","2026-10-25");
            assertNotNull(review);
            assertEquals(522,review.page);
            assertEquals(2,review.total); // only proven Itqan pages 522 and 523
            assertEquals(1,review.previousCount);
            assertTrue(queue.advance(review,"","2026-10-25"));
            PostFinalRecallStore.Review last=queue.nextReady("","2026-10-25");
            assertNotNull(last);
            assertEquals(523,last.page);
            assertFalse(last.lines.stream().anyMatch(otherFamily::contains));
            assertTrue(queue.advance(last,"","2026-10-25"));
            assertNull(queue.nextReady("","2026-10-25"));
            assertFalse("No duplicate queue entry after repeated startup",
                queue.recoverCommittedFromHistory(PostFinalRecallStore.ITQAN,
                    "2026-10-25",acquired,geometry));
            assertEquals(beforeAcquired,raw.getString("v6AcquiredCreditLineIds",""));
            assertEquals(beforeFinal,raw.getString("v6ConsolidationStabilizationStateFinalV1",""));
        }finally{assertTrue(raw.edit().clear().commit());}
    }

    @Test public void legacyRecoveryRejectsUnacquiredHistoricalWeekWithoutInventingCredit()
            throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            JSONArray weeks=new JSONArray().put(new JSONObject().put("week","2026-10-19")
                .put("units",new JSONArray().put(ConsolidationPhysicalUnitPolicy.encodeLineUnit(
                    geometry.lineIdsOnPage(523)))));
            assertTrue(raw.edit().putString("stabilizationSnowball8WeekHistory",weeks.toString())
                .putString("lastRecentSabqiReviewDate","2026-10-25").commit());
            PostFinalRecallStore queue=new PostFinalRecallStore(c);
            assertFalse(queue.recoverCommittedFromHistory(PostFinalRecallStore.ITQAN,
                "2026-10-25",new LinkedHashSet<>(),geometry));
            assertNull(queue.nextReady("","2026-10-25"));
            assertEquals("No historical acquisition should be invented",0,
                new JSONArray(raw.getString("v6AcquiredCreditLineIds","[]")).length());
        }finally{assertTrue(raw.edit().clear().commit());}
    }

    @Test public void booxTodayMenuPreservesBothSabqiAndItqanPendingRecallEntries() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            new HifzPrefs(c); // prepare valid V6 defaults without invented credits
            PostFinalRecallStore queue=new PostFinalRecallStore(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            queue.prepare(PostFinalRecallStore.SABQI,"2026-10-18",PostFinalCumulativePolicy.plan(
                geometry.linesForExactIds(geometry.lineIdsOnPage(16)),
                geometry.linesForExactIds(geometry.lineIdsOnPage(17))));
            queue.prepare(PostFinalRecallStore.ITQAN,"2026-10-25",PostFinalCumulativePolicy.plan(
                geometry.linesForExactIds(geometry.lineIdsOnPage(523)),
                geometry.linesForExactIds(geometry.lineIdsOnPage(524))));
            assertTrue(queue.markReady(PostFinalRecallStore.SABQI,"2026-10-18"));
            assertTrue(queue.markReady(PostFinalRecallStore.ITQAN,"2026-10-25"));
            assertTrue(raw.edit().putString("lastLearningConsolidationDate","2026-10-18")
                .putString("lastRecentSabqiReviewDate","2026-10-25").commit());
            try(ActivityScenario<MainActivity> screen=ActivityScenario.launch(MainActivity.class)){
                screen.onActivity(activity->{
                    try{
                        Field method=MainActivity.class.getDeclaredField("postFinalStore");
                        method.setAccessible(true);
                        assertNotNull(method.get(activity));
                        java.lang.reflect.Method pending=MainActivity.class.getDeclaredMethod(
                            "unfinishedWeekendSessions",java.time.LocalDate.class);
                        pending.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        List<Object> sessions=(List<Object>)pending.invoke(activity,java.time.LocalDate.of(2026,10,26));
                        int sabqi=0,itqan=0;
                        for(Object row:sessions){
                            Field family=row.getClass().getDeclaredField("postFinalFamily");
                            family.setAccessible(true);
                            Object value=family.get(row);
                            if(PostFinalRecallStore.SABQI.equals(value))sabqi++;
                            if(PostFinalRecallStore.ITQAN.equals(value))itqan++;
                        }
                        assertEquals("Both recalls must appear, even when Sabqi was first",1,sabqi);
                        assertEquals("Completed Itqan masked recall must never be hidden",1,itqan);
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
            }
        }finally{assertTrue(raw.edit().clear().commit());}
    }

    @Test public void maskedPagesResumeWithoutReopeningTheSabqiFinalOrTouchingCredits() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",Context.MODE_PRIVATE);
        assertTrue(raw.edit().clear().commit());
        try {
            HifzPrefs prefs=new HifzPrefs(c);
            GeometryRepository geometry=GeometryRepository.get(c);
            List<GeometryRepository.LineMeta> prior=geometry.linesForExactIds(geometry.lineIdsOnPage(16));
            List<GeometryRepository.LineMeta> current=geometry.linesForExactIds(geometry.lineIdsOnPage(17));
            PostFinalRecallStore queue=new PostFinalRecallStore(c);
            String date="2026-10-25";
            queue.prepare(PostFinalRecallStore.SABQI,date,PostFinalCumulativePolicy.plan(prior,current));
            assertTrue(raw.edit().putString("lastLearningConsolidationDate",date).commit());
            assertNotNull(queue.nextReady(date,""));
            String acquired=raw.getString("v6AcquiredCreditLineIds","");
            String originalFinal=raw.getString("v6ConsolidationLearningStateFinalV1","");
            Intent intent=new Intent(c,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.POST_FINAL_RECALL);
            try(ActivityScenario<HifzSessionActivity> screen=ActivityScenario.launch(intent)){
                screen.onActivity(activity -> {
                    try{
                        Field f=HifzSessionActivity.class.getDeclaredField("currentPostFinalReview");
                        f.setAccessible(true);
                        PostFinalRecallStore.Review review=(PostFinalRecallStore.Review)f.get(activity);
                        assertNotNull("Real masked recall screen must render",review);
                        assertEquals(0,review.position);
                        assertEquals(16,review.page);
                        f=HifzSessionActivity.class.getDeclaredField("currentMask");
                        f.setAccessible(true);
                        assertEquals("Mask must be staged before user sees text",100,f.getInt(activity));
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
            }
            assertEquals(acquired,raw.getString("v6AcquiredCreditLineIds",""));
            assertEquals(originalFinal,raw.getString("v6ConsolidationLearningStateFinalV1",""));
            PostFinalRecallStore.Review first=queue.nextReady(date,"");
            assertTrue(queue.advance(first,date,""));
            assertFalse(queue.advance(first,date,""));
            assertEquals(1,new PostFinalRecallStore(c).nextReady(date,"").position);
            try(ActivityScenario<HifzSessionActivity> reopened=ActivityScenario.launch(intent)){
                reopened.onActivity(activity->{
                    try{
                        Field f=HifzSessionActivity.class.getDeclaredField("currentPostFinalReview");
                        f.setAccessible(true);
                        PostFinalRecallStore.Review saved=(PostFinalRecallStore.Review)f.get(activity);
                        assertNotNull(saved);
                        assertEquals("Saved page index must survive Activity death",1,saved.position);
                        assertEquals(17,saved.page);
                    }catch(ReflectiveOperationException e){throw new AssertionError(e);}
                });
            }
            assertEquals(acquired,raw.getString("v6AcquiredCreditLineIds",""));
            assertEquals(originalFinal,raw.getString("v6ConsolidationLearningStateFinalV1",""));
        }finally {assertTrue(raw.edit().clear().commit());}
    }
}

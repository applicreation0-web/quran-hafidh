package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
import android.content.Intent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import com.quransafeguard.hifz.core.VerseRange;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Real Android persistence: READ then whole-page RECALL, no flagged verses or Hifz credit. */
public final class ActiveReviewCorrectionInstrumentedTest {
    private static final String FORCE_STOP_PREFS="haafidh_active_replay_force_stop_fixture";

    /** CI opt-in only: run separately, externally force-stop the target, then run forceStopVerify. */
    @Test public void forceStopPrepare() {
        org.junit.Assume.assumeTrue("true".equals(androidx.test.platform.app.InstrumentationRegistry
            .getArguments().getString("activeReplayForceStop")));
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences(FORCE_STOP_PREFS,0);
        assertTrue(raw.edit().clear().putString("immutableHifzReceipt","synthetic-credit")
            .putString("activeMurajaahCursor","30:37").putString("activeMurajaahActualEnd","30:38")
            .putInt("preparedPid",android.os.Process.myPid()).commit());
        ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
        VerseRef start=new VerseRef(30,37),end=new VerseRef(30,38);
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.recordReveal("2026-10-29",start,407));
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.begin("2026-10-29",start,end,12345L,
            ActiveReviewCorrectionPlan.select(store.revealed("2026-10-29",start)),409));
        assertTrue(store.pageReread(store.pending()));
        assertTrue(store.pageRecalled(store.pending()));
        assertTrue(store.pageReread(store.pending()));
        assertTrue(store.revealDuringControl(store.pending()));
        assertTrue(raw.edit().putString("expectedCorrection",raw.getString("activeReviewCorrectionV2",""))
            .putString("expectedReveals",raw.getString("activeReviewRevealedPagesV1","")).commit());
    }

    @Test public void forceStopVerify() {
        org.junit.Assume.assumeTrue("true".equals(androidx.test.platform.app.InstrumentationRegistry
            .getArguments().getString("activeReplayForceStop")));
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences(FORCE_STOP_PREFS,0);
        try {
            assertTrue("Prepare must run in a separate process before external force-stop",raw.contains("preparedPid"));
            assertNotEquals(raw.getInt("preparedPid",-1),android.os.Process.myPid());
            assertEquals(raw.getString("expectedCorrection",null),raw.getString("activeReviewCorrectionV2",""));
            assertEquals(raw.getString("expectedReveals",null),raw.getString("activeReviewRevealedPagesV1",""));
            ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
            ActiveReviewCorrectionStore.State resumed=store.pending();
            assertEquals("2026-10-29",resumed.date);
            assertEquals(Arrays.asList(408,407),resumed.pages);
            assertEquals(1,resumed.pageIndex);
            assertEquals(407,resumed.page());
            assertEquals(409,resumed.originPage);
            assertEquals(12345L,resumed.activeElapsedMs);
            assertEquals(new VerseRef(30,37),resumed.start);
            assertEquals(new VerseRef(30,38),resumed.end);
            assertEquals("RECALL",resumed.stage);
            assertTrue(resumed.assisted);
            assertFalse(store.pageRecalled(resumed));
            assertFalse(store.recordReveal("2026-10-30",resumed.start,406));
            assertTrue(store.restartPage(resumed));
            assertTrue(store.pageReread(store.pending()));
            assertTrue(store.pageRecalled(store.pending()));
            assertEquals("DONE",store.pending().stage);
            assertEquals("synthetic-credit",raw.getString("immutableHifzReceipt",""));
            assertEquals("30:37",raw.getString("activeMurajaahCursor",""));
            assertEquals("30:38",raw.getString("activeMurajaahActualEnd",""));
        } finally {assertTrue(raw.edit().clear().commit());}
    }

    @Test public void revealedDateResumesBeforeReplayWasStarted() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",0);
        assertTrue(raw.edit().clear().commit());
        try {
            GeometryRepository geometry=GeometryRepository.get(c);
            HifzPrefs prefs=new HifzPrefs(c);
            VerseRef start=new VerseRef(30,35),end=new VerseRef(30,40);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geometry));
            prefs.setActiveMurajaahCursor(start);
            ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
            int page=geometry.pageForVerse(start);
            AnnotationStore notes=new AnnotationStore(c);
            float[] stroke=new float[]{0.2f,0.3f,0.5f,0.6f};
            assertTrue(notes.addStroke(page,stroke));
            assertTrue(store.recordReveal("2026-10-29",start,page));
            assertNull(store.pending());
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(new Intent(c,HifzSessionActivity.class)
                    .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                    .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,"2026-10-30"))) {
                scenario.onActivity(a->{
                    assertEquals("2026-10-29",field(a,"sessionDate").toString());
                    assertTrue(store.revealed("2026-10-29",start).contains(page));
                    assertNull(store.pending());
                    assertEquals(View.GONE,((View)field(a,"annotationOverlay")).getVisibility());
                    assertArrayEquals(stroke,notes.strokesForPage(page).get(0),0f);
                });
            }
        } finally {assertTrue(raw.edit().clear().commit());}
    }

    @Test public void realCorrectivePageHasNoRecallHolesAndHelpSurvivesRecreation() throws Exception {
        Context c=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=c.getSharedPreferences("quran_hifz_preview_v1",0);
        assertTrue(raw.edit().clear().commit());
        try {
            GeometryRepository geometry=GeometryRepository.get(c);
            HifzPrefs prefs=new HifzPrefs(c);
            VerseRef start=new VerseRef(30,35),end=new VerseRef(30,40);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geometry));
            prefs.setActiveMurajaahCursor(start);
            prefs.setActiveMurajaahActualEnd(end);
            int page=geometry.pageForVerse(start);
            AnnotationStore notes=new AnnotationStore(c);
            float[] stroke=new float[]{0.15f,0.25f,0.55f,0.65f};
            assertTrue(notes.addStroke(page,stroke));
            String savedNotes=raw.getString("pageAnnotationsV1_"+page,null);
            ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
            assertTrue(store.recordReveal("2026-10-29",start,page));
            assertTrue(store.begin("2026-10-29",start,end,12345L,Collections.singletonList(page),page));
            Intent intent=new Intent(c,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_SCHEDULED_DATE,"2026-10-30");
            try(ActivityScenario<HifzSessionActivity> scenario=ActivityScenario.launch(intent)) {
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Corrective visible page");
                assertEquals("true",js(scenario,"mask===0 && protectedWordBoxes().length===0"));
                scenario.onActivity(a->assertEquals("Saved mnemonics must not overlay corrective READ",
                    View.GONE,((View)field(a,"annotationOverlay")).getVisibility()));
                scenario.onActivity(a->find((View)field(a,"actions"),"Page relue").performClick());
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Corrective fully masked page");
                scenario.onActivity(a->assertEquals("Saved mnemonics must not overlay corrective RECALL",
                    View.GONE,((View)field(a,"annotationOverlay")).getVisibility()));
                assertEquals("true",js(scenario,"(function(){var m=document.querySelector('#mushaf');"
                    +"var layer=m.querySelector('svg g.masklayer');return mask===100"
                    +"&&m.style.visibility==='visible'&&!!layer&&layer.children.length>0"
                    +"&&protectedWordBoxes().length===0&&!layer.hasAttribute('mask')"
                    +"&&!document.querySelector('#hifz-exact-word-holes')"
                    +"&&!document.querySelector('#atlas-discriminant-marks');})()"));
                scenario.onActivity(a->{
                    View reveal=find((View)field(a,"actions"),"Révéler");
                    long now=android.os.SystemClock.uptimeMillis();
                    MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,1,1,0);
                    MotionEvent up=MotionEvent.obtain(now,now,MotionEvent.ACTION_UP,1,1,0);
                    reveal.dispatchTouchEvent(down);reveal.dispatchTouchEvent(up);
                    down.recycle();up.recycle();
                    assertTrue(store.pending().assisted);
                    assertFalse(find((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                scenario.recreate();
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(scenario,"mushaf","Assisted recall restored");
                scenario.onActivity(a->{
                    assertEquals("2026-10-29",field(a,"sessionDate").toString());
                    assertTrue(store.pending().assisted);
                    assertFalse(find((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    assertEquals(start,prefs.activeMurajaahCursor());
                    assertEquals(end,prefs.activeMurajaahActualEnd());
                    assertEquals(View.GONE,((View)field(a,"annotationOverlay")).getVisibility());
                    assertEquals(savedNotes,raw.getString("pageAnnotationsV1_"+page,null));
                    assertArrayEquals(stroke,notes.strokesForPage(page).get(0),0f);
                });
            }
        } finally {assertTrue(raw.edit().clear().commit());}
    }

    private static Object field(Object target,String name) {
        try {Field f=HifzSessionActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(target);}
        catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static View find(View root,String label) {
        if(label.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){
            View found=find(((ViewGroup)root).getChildAt(i),label);if(found!=null)return found;
        }
        return null;
    }
    private static String js(ActivityScenario<HifzSessionActivity> scenario,String script)throws Exception {
        CountDownLatch done=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        scenario.onActivity(a->((MushafView)field(a,"mushaf")).evaluateJavascript(script,v->{result.set(v);done.countDown();}));
        assertTrue("Reader JS result",done.await(15,TimeUnit.SECONDS));return result.get();
    }
    @Test public void pageRevealIsAutomaticAndCheckpointsSurviveReopen() {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("haafidh_all_reveals_test",Context.MODE_PRIVATE);
        prefs.edit().clear().putString("immutableHifzReceipt","never-change").commit();
        ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(prefs);
        VerseRef start=new VerseRef(30,37), end=new VerseRef(30,38);
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.recordReveal("2026-10-29",start,407));
        assertEquals(Arrays.asList(408,407),new ArrayList<>(store.revealed("2026-10-29",start)));
        assertTrue(store.revealed("2026-10-30",start).isEmpty());
        assertTrue(store.begin("2026-10-29",start,end,12345L,
            ActiveReviewCorrectionPlan.select(store.revealed("2026-10-29",start)),409));
        assertFalse(store.begin("2026-10-29",start,end,12345L,Collections.singletonList(200),409));
        ActiveReviewCorrectionStore reopened=new ActiveReviewCorrectionStore(prefs);
        ActiveReviewCorrectionStore.State state=reopened.pending();
        assertNotNull(state);
        assertEquals("READ",state.stage);
        assertEquals(408,state.page());
        assertEquals(409,state.originPage);
        assertEquals(end,state.end);
        assertEquals(12345L,state.activeElapsedMs);
        assertTrue(reopened.pageReread(state));
        state=reopened.pending();
        assertEquals("RECALL",state.stage);
        assertTrue(reopened.revealDuringControl(state));
        state=reopened.pending();
        assertTrue(state.assisted);
        assertFalse(reopened.pageRecalled(state));
        assertTrue(reopened.restartPage(state));
        state=reopened.pending();
        assertEquals("READ",state.stage);
        assertTrue(reopened.pageReread(state));
        assertTrue(reopened.pageRecalled(reopened.pending()));
        state=reopened.pending();
        assertEquals(407,state.page());
        assertEquals("READ",state.stage);
        assertTrue(reopened.pageReread(state));
        assertTrue(reopened.pageRecalled(reopened.pending()));
        assertEquals("DONE",reopened.pending().stage);
        assertEquals("never-change",prefs.getString("immutableHifzReceipt",""));
        assertTrue(reopened.clear());
        assertNull(reopened.pending());
        assertTrue(reopened.revealed("2026-10-29",start).isEmpty());
        prefs.edit().clear().commit();
    }

    @Test public void corruptCheckpointIsNotSilentlyErased() {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences prefs=context.getSharedPreferences("haafidh_replay_corrupt_test",Context.MODE_PRIVATE);
        prefs.edit().clear().putString("activeReviewCorrectionV2","{invalid").commit();
        ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(prefs);
        try{store.pending();fail("Must reject malformed checkpoint");}
        catch(IllegalStateException expected){
            assertTrue(prefs.contains("activeReviewCorrectionV2"));
        }finally{prefs.edit().clear().commit();}
    }
}

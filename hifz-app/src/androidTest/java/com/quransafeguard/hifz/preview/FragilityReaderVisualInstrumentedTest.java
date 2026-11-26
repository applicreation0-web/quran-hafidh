package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import com.quransafeguard.hifz.core.VerseRange;
import java.util.Collections;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real reader, actual buttons and fully painted pages; synthetic progress only. */
public final class FragilityReaderVisualInstrumentedTest {
    @Test public void standaloneVisibleMaskedAndDeferredPreserveAllAcquisitionAndActiveEvidence() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        try {
            assertTrue(raw.edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1),end=new VerseRef(2,74);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geo));
            prefs.setActiveMurajaahCursor(start);
            int page=geo.pageForVerse(new VerseRef(2,6));
            assertTrue(raw.edit().putString(ActiveReviewCorrectionStore.KEY_QUEUE,"["+page+"]").commit());
            int acquired=prefs.acquiredLineCountV6();
            int receipts=prefs.activeReviewSlots().receipts().size();
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_FRAGILITY_PAGE,page);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)){
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité visible");
                activity.onActivity(a->{
                    assertEquals("Fragilités",((TextView)field(a,"program")).getText().toString());
                    assertEquals(0,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"fragilityPageReady"));
                    assertNotNull(control((View)field(a,"actions"),"Masquer la page"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-visible.png");
                double visibleInk=inkFraction(context,"fragilite-visible.png");
                assertTrue("Visible folio is blank or not fully painted: "+visibleInk,visibleInk>0.02);
                activity.onActivity(a->click(a,"Masquer la page"));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                    assertTrue(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-masquee.png");
                double maskedInk=inkFraction(context,"fragilite-masquee.png");
                assertTrue("Masked folio exposes too much ink: "+maskedInk,maskedInk<visibleInk*0.25);
                activity.recreate();
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée reprise");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-reprise.png");
                assertTrue("Resumed masked folio exposes too much ink",inkFraction(context,"fragilite-reprise.png")<visibleInk*0.25);
                activity.onActivity(a->click(a,"Reporter cette page"));
            }
            assertTrue(prefs.activeReviewSlots().manualCorrection().queuedPages().contains(page));
            assertEquals(acquired,prefs.acquiredLineCountV6());
            assertEquals(start,prefs.activeMurajaahCursor());
            assertEquals(receipts,prefs.activeReviewSlots().receipts().size());
            for(int ayah=1;ayah<=74;ayah++)
                assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(2,ayah)));
        }finally{HifzBackup.restore(context,original);}
    }

    @Test public void threePageBatchAdvancesResumesDefersAndReturnsWithoutActiveCredit() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        new HifzPrefs(context);
        JSONObject original=HifzBackup.export(context);
        SharedPreferences raw=context.getSharedPreferences("quran_hifz_preview_v1",0);
        try {
            assertTrue(raw.edit().clear().commit());
            HifzPrefs prefs=new HifzPrefs(context);
            GeometryRepository geo=GeometryRepository.get(context);
            VerseRef start=new VerseRef(2,1),end=new VerseRef(2,74);
            assertTrue(prefs.setV6AcquiredRanges(Collections.singletonList(new VerseRange(start,end)),geo));
            prefs.setActiveMurajaahCursor(start);
            assertTrue(raw.edit().putString(ActiveReviewCorrectionStore.KEY_QUEUE,"[4,6,10]").commit());
            int acquired=prefs.acquiredLineCountV6();
            int receipts=prefs.activeReviewSlots().receipts().size();
            ActiveReviewCorrectionStore store=prefs.activeReviewSlots().manualCorrection();
            Intent intent=new Intent(context,HifzSessionActivity.class)
                .putExtra(HifzSessionActivity.EXTRA_MODE,HifzSessionActivity.MURAJAAH_ACTIVE)
                .putExtra(HifzSessionActivity.EXTRA_FRAGILITY_PAGE,6);
            try(ActivityScenario<HifzSessionActivity> activity=ActivityScenario.launch(intent)){
                assertPhase(activity,store,6,0,"READ");
                assertEquals(java.util.Arrays.asList(6,10,4),store.pending().pages);
                activity.onActivity(a->click(a,"Page suivante"));
                assertPhase(activity,store,10,0,"READ");
                activity.onActivity(a->click(a,"Page précédente"));
                assertPhase(activity,store,6,0,"READ");
                assertEquals(java.util.Arrays.asList(6,10,4),store.pending().pages);
                activity.onActivity(a->assertEquals("Libre",((TextView)field(a,"timerText")).getText().toString()));
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,6,0,"RECALL");
                activity.onActivity(a->click(a,"Afficher la page"));
                assertPhase(activity,store,6,0,"READ");
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,6,0,"RECALL");
                VerseRef aid=null;
                for(WordGeometryRepository.WordBox w:WordGeometryRepository.shared(context).wordsForPage(6)){
                    aid=new VerseRef(w.surah,w.ayah);break;
                }
                assertNotNull(aid);
                final VerseRef selected=aid;
                activity.onActivity(a->{
                    a.onVerseTap(selected);
                    assertTrue(store.pending().assisted);
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    assertNotNull(control((View)field(a,"actions"),"Masquer la page"));
                });
                activity.recreate();
                assertPhase(activity,store,6,0,"RECALL");
                activity.onActivity(a->{
                    assertFalse(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                    click(a,"Masquer la page");
                });
                assertPhase(activity,store,6,0,"RECALL");
                assertFalse(store.pending().assisted);
                activity.onActivity(a->click(a,"Page récitée sans aide"));
                assertPhase(activity,store,10,1,"READ");
                activity.recreate();
                assertPhase(activity,store,10,1,"READ");
                activity.onActivity(a->click(a,"Reporter cette page"));
                assertPhase(activity,store,4,2,"READ");
                assertEquals(Collections.singletonList(10),store.pending().deferred);
                activity.onActivity(a->click(a,"Masquer la page"));
                assertPhase(activity,store,4,2,"RECALL");
                activity.onActivity(a->click(a,"Page récitée sans aide"));
                for(int attempt=0;attempt<40
                        &&activity.getState()!=androidx.lifecycle.Lifecycle.State.DESTROYED;attempt++)
                    android.os.SystemClock.sleep(100);
                assertEquals("Manual batch returns instead of leaving masked completion screen",
                    androidx.lifecycle.Lifecycle.State.DESTROYED,activity.getState());
            }
            assertNull(store.pending());
            assertEquals(Collections.singletonList(10),store.queuedPages());
            assertEquals(acquired,prefs.acquiredLineCountV6());
            assertEquals(start,prefs.activeMurajaahCursor());
            assertEquals(receipts,prefs.activeReviewSlots().receipts().size());
            for(int ayah=1;ayah<=74;ayah++)
                assertTrue(prefs.activeMurajaahCorpus().contains(new VerseRef(2,ayah)));
        }finally{HifzBackup.restore(context,original);}
    }

    @Test public void continuationCannotCrossTheMorningEveningBoundary(){
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.SABQI));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.ITQAN));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.LEARNING_FINAL));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.MORNING,HifzSessionActivity.CONSOLIDATION_FINAL));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.LEARNING_CONSOLIDATION));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.RECENT_SABQI_REVIEW));
        assertTrue(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.CONSOLIDATION_FINAL));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,HifzSessionActivity.SABQI));
        assertFalse(MainActivity.isSameSlotContinuation(ActiveReviewSlots.Slot.EVENING,ActiveReviewSlots.Slot.MORNING.mode));
    }

    private static void assertPhase(ActivityScenario<HifzSessionActivity> activity,
            ActiveReviewCorrectionStore store,int page,int index,String stage){
        HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Batch "+page+" "+stage);
        ActiveReviewCorrectionStore.State pending=store.pending();
        assertNotNull(pending);
        assertEquals(page,pending.page());assertEquals(index,pending.pageIndex);assertEquals(stage,pending.stage);
        activity.onActivity(a->{
            assertEquals(page,((Integer)field(a,"currentPage")).intValue());
            assertTrue(((TextView)field(a,"progress")).getText().toString().contains("Page "+page+" · "+index+"/3 traitées"));
            assertEquals("READ".equals(stage)?0:100,((Integer)field(a,"currentMask")).intValue());
        });
    }

    /** Pixel proof from the actual screenshot: the central 60% excludes header,
     * footer and most margins. A blank cream page previously passed DOM tests. */
    private static double inkFraction(Context context,String name){
        java.io.File file=new java.io.File(context.getExternalFilesDir(null),name);
        android.graphics.Bitmap image=android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
        assertNotNull("Missing actual frame "+name,image);
        long dark=0,count=0;
        try{
            int w=image.getWidth(),h=image.getHeight();
            int[] row=new int[w];
            for(int y=h/5;y<4*h/5;y++){
                image.getPixels(row,0,w,0,y,w,1);
                for(int x=w/5;x<4*w/5;x++){
                    int p=row[x];count++;
                    if(android.graphics.Color.red(p)<100&&android.graphics.Color.green(p)<100
                        &&android.graphics.Color.blue(p)<100)dark++;
                }
            }
            return (double)dark/count;
        }finally{image.recycle();}
    }

    private static Object field(Object target,String name){
        try {
            java.lang.reflect.Field f=target.getClass().getDeclaredField(name);
            f.setAccessible(true);return f.get(target);
        }catch(ReflectiveOperationException error){throw new AssertionError(error);}
    }
    private static View control(View v,String label){
        if(label.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++){
                View hit=control(g.getChildAt(i),label);if(hit!=null)return hit;
            }
        }
        return null;
    }
    private static void click(HifzSessionActivity a,String label){
        View button=control((View)field(a,"actions"),label);
        assertNotNull(label,button);
        assertTrue(label,button.isEnabled());
        assertTrue(label,button.performClick());
    }
}

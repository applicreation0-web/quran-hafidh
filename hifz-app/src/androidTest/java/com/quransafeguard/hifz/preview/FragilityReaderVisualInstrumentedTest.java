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
                    assertNotNull(control((View)field(a,"actions"),"Page relue"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-visible.png");
                activity.onActivity(a->click(a,"Page relue"));
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                    assertTrue(control((View)field(a,"actions"),"Page récitée sans aide").isEnabled());
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-masquee.png");
                activity.recreate();
                HifzAndMapRuntimeSmokeInstrumentedTest.awaitMushafPage(activity,"mushaf","Fragilité masquée reprise");
                activity.onActivity(a->{
                    assertEquals(100,((Integer)field(a,"currentMask")).intValue());
                    assertTrue((Boolean)field(a,"correctionMaskedPageReady"));
                });
                HifzAndMapRuntimeSmokeInstrumentedTest.saveScreenshot("fragilite-reprise.png");
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

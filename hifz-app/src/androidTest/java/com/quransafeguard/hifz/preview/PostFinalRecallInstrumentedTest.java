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
import static org.junit.Assert.*;

/** Actual Android Activity: masked post-final queue, restart and unchanged Acquis/final states. */
public final class PostFinalRecallInstrumentedTest {
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

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

/** Real Android persistence: READ then whole-page RECALL, no flagged verses or Hifz credit. */
public final class ActiveReviewCorrectionInstrumentedTest {
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

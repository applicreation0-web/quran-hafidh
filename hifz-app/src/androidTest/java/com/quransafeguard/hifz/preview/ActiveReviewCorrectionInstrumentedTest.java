package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.test.core.app.ApplicationProvider;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

/** Real Android SharedPreferences commit/reopen: no fake Hifz credits on correction. */
public final class ActiveReviewCorrectionInstrumentedTest {
    @Test public void interruptionRestoresReadRecallAndNoDuplicatePage() {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=context.getSharedPreferences("haafidh_active_correction_test",Context.MODE_PRIVATE);
        raw.edit().clear().commit();
        ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
        VerseRef a=new VerseRef(30,37),b=new VerseRef(30,38);
        assertTrue(store.recordReveal("2026-10-29",a,408));
        assertTrue(store.recordReveal("2026-10-29",a,408));
        assertEquals(Collections.singleton(408),store.revealed("2026-10-29",a));
        assertTrue(store.revealed("2026-10-30",a).isEmpty());
        ActiveReviewCorrectionPlan.Page page=new ActiveReviewCorrectionPlan.Page(408,Arrays.asList(a,b));
        assertTrue(store.begin("2026-10-29",a,b,12345L,Collections.singletonList(page)));
        assertFalse(store.begin("2026-10-29",a,b,12345L,Collections.singletonList(page)));
        ActiveReviewCorrectionStore reopened=new ActiveReviewCorrectionStore(raw);
        ActiveReviewCorrectionStore.State state=reopened.pending();
        assertNotNull(state);
        assertEquals("READ",state.stage);
        assertEquals(a,state.start);
        assertEquals(b,state.end);
        assertEquals(12345L,state.activeElapsedMs);
        assertTrue(reopened.pageReread(state));
        state=reopened.pending();
        assertEquals("RECALL",state.stage);
        assertEquals(a,state.verse());
        assertTrue(reopened.revealDuringControl(state));
        assertTrue(reopened.pending().assisted);
        assertTrue(reopened.nextTarget(reopened.pending()));
        state=reopened.pending();
        assertEquals(b,state.verse());
        assertFalse(state.assisted);
        assertTrue(reopened.nextTarget(state));
        assertEquals("DONE",reopened.pending().stage);
        assertTrue(reopened.clear());
        assertNull(reopened.pending());
        assertTrue(reopened.revealed("2026-10-29",a).isEmpty());
    }

    @Test public void malformedSnapshotFailsClosedAndRemainsRecoverable() {
        Context context=ApplicationProvider.getApplicationContext();
        SharedPreferences raw=context.getSharedPreferences("haafidh_active_correction_corrupt",Context.MODE_PRIVATE);
        raw.edit().clear().putString("activeReviewCorrectionV1","{not-json").commit();
        ActiveReviewCorrectionStore store=new ActiveReviewCorrectionStore(raw);
        try {
            store.pending();
            fail("Must refuse a damaged checkpoint");
        } catch (IllegalStateException expected) {
            assertTrue(raw.contains("activeReviewCorrectionV1"));
        } finally { raw.edit().clear().commit(); }
    }
}

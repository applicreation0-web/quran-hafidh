package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import static org.junit.Assert.*;

/** Exercises the production checkpoint state machine; no Activity or WebView is simulated. */
public final class ActiveReviewCorrectionStoreTest {
    private final InMemoryPrefs disk = new InMemoryPrefs();
    private final VerseRef start = new VerseRef(30,37), end = new VerseRef(30,38);
    private final ActiveReviewCorrectionStore store = new ActiveReviewCorrectionStore(disk);

    @Test public void legitimateCorpusWrapMayEndBeforeItsStartingVerse() {
        assertTrue(store.begin("2026-10-29",end,start,12000L,Arrays.asList(408),409));
        assertEquals(end,store.pending().start);
        assertEquals(start,store.pending().end);
    }

    @Test public void restoredDoneCannotSkipRemainingPages() throws Exception {
        assertTrue(store.begin("2026-10-29",start,end,12000L,Arrays.asList(408,407),409));
        org.json.JSONObject malformed=new org.json.JSONObject(disk.getString("activeReviewCorrectionV2",""));
        malformed.put("stage","DONE");
        disk.edit().putString("activeReviewCorrectionV2",malformed.toString()).commit();
        try {store.pending();fail("DONE before the final page must be rejected");}
        catch(IllegalStateException expected){assertEquals(malformed.toString(),disk.getString("activeReviewCorrectionV2",""));}
    }

    @Test public void restoredReadAndDoneCannotCarryAssistance() throws Exception {
        for(String stage:Arrays.asList("READ","DONE")){
            disk.edit().clear().commit();
            assertTrue(store.begin("2026-10-29",start,end,12000L,Arrays.asList(408),409));
            org.json.JSONObject malformed=new org.json.JSONObject(disk.getString("activeReviewCorrectionV2",""));
            malformed.put("stage",stage).put("assisted",true);
            disk.edit().putString("activeReviewCorrectionV2",malformed.toString()).commit();
            try {store.pending();fail("Assistance belongs only to RECALL: "+stage);}
            catch(IllegalStateException expected){assertEquals(malformed.toString(),disk.getString("activeReviewCorrectionV2",""));}
        }
    }

    @Test public void laterDateCannotOverwriteAnUnfinishedRevealLedger() {
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertEquals("2026-10-29",new ActiveReviewCorrectionStore(disk).revealedSessionDate(start,""));
        assertFalse(store.recordReveal("2026-10-30",start,407));
        assertEquals(Arrays.asList(408),new java.util.ArrayList<>(store.revealed("2026-10-29",start)));
    }

    @Test public void receiptCrashGapKeepsOriginalDateAvailableForCleanup() {
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertEquals("2026-10-29",store.revealedSessionDate(end,"2026-10-29"));
        try {store.revealedSessionDate(end,"");fail("Unvalidated changed cursor must fail closed");}
        catch(IllegalStateException expected) {assertTrue(disk.contains("activeReviewRevealedPagesV1"));}
    }

    @Test public void staleUnassistedCallbackCannotValidateAfterHelp() {
        assertTrue(store.begin("2026-10-29",start,end,12000L,Arrays.asList(408,407),409));
        assertTrue(store.pageReread(store.pending()));
        ActiveReviewCorrectionStore.State beforeHelp=store.pending();
        assertTrue(store.revealDuringControl(beforeHelp));
        assertFalse("An earlier callback must not erase persisted assistance",store.pageRecalled(beforeHelp));
        assertTrue(new ActiveReviewCorrectionStore(disk).pending().assisted);
        assertEquals(0,store.pending().pageIndex);
    }

    @Test public void staleRereadCannotUndoAnAlreadyCompletedPage() {
        assertTrue(store.begin("2026-10-29",start,end,12000L,Arrays.asList(408,407),409));
        ActiveReviewCorrectionStore.State visible=store.pending();
        assertTrue(store.pageReread(visible));
        assertTrue(store.pageRecalled(store.pending()));
        assertFalse(store.pageReread(visible));
        assertEquals(407,store.pending().page());
        assertEquals("READ",store.pending().stage);
    }

    @Test public void reopenKeepsPartialOrderedReplayWithoutTouchingCreditOrEndpoint() {
        disk.edit().putString("v6AcquiredCreditLineIds","original-credit")
            .putString("activeMurajaahCursor",start.toString())
            .putString("activeMurajaahActualEnd",end.toString()).commit();
        java.util.Map<String,?> before=new HashMap<>(disk.getAll());
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.recordReveal("2026-10-29",start,407));
        assertTrue(store.recordReveal("2026-10-29",start,408));
        assertTrue(store.begin("2026-10-29",start,end,12000L,
            ActiveReviewCorrectionPlan.select(store.revealed("2026-10-29",start)),409));
        assertTrue(store.pageReread(store.pending()));
        assertTrue(store.pageRecalled(store.pending()));
        ActiveReviewCorrectionStore resumed=new ActiveReviewCorrectionStore(disk);
        assertEquals(Arrays.asList(408,407),resumed.pending().pages);
        assertEquals(407,resumed.pending().page());
        assertEquals("READ",resumed.pending().stage);
        assertEquals("2026-10-29",resumed.pending().date);
        assertEquals(12000L,resumed.pending().activeElapsedMs);
        for(String key:before.keySet())assertEquals(before.get(key),disk.getAll().get(key));
    }

    @Test public void failedCheckpointDoesNotAdvanceOrExposeAnUnrecordedReveal() {
        FaultInjectingPrefs faults=new FaultInjectingPrefs();
        ActiveReviewCorrectionStore guarded=new ActiveReviewCorrectionStore(faults);
        faults.failCommit=1;
        assertFalse(guarded.recordReveal("2026-10-29",start,408));
        assertTrue(guarded.revealed("2026-10-29",start).isEmpty());
        assertTrue(guarded.begin("2026-10-29",start,end,12000L,Arrays.asList(408),409));
        faults.failCommit=faults.commits+1;
        assertFalse(guarded.pageReread(guarded.pending()));
        assertEquals("READ",new ActiveReviewCorrectionStore(faults).pending().stage);
        assertTrue(guarded.pageReread(guarded.pending()));
        faults.failCommit=faults.commits+1;
        assertFalse(guarded.pageRecalled(guarded.pending()));
        assertEquals("RECALL",new ActiveReviewCorrectionStore(faults).pending().stage);
    }
}

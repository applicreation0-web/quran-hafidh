package com.quransafeguard.hifz.preview;
import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

/** Normal validation, crash gap, retry, process restart and no progress credits. */
public final class PostFinalRecallStoreTest {
    private static GeometryRepository.LineMeta line(int page,int n){
        return new GeometryRepository.LineMeta(page*15+n,"p"+page+"-l"+n,page,Collections.<VerseRef>emptyList());
    }
    private static List<PostFinalCumulativePolicy.Page> plan(){
        return PostFinalCumulativePolicy.plan(Arrays.asList(line(601,0),line(600,0)),
            Arrays.asList(line(599,1),line(599,2)));
    }
    @Test public void doesNotOfferAnUnvalidatedFinalOrRepeatAfterDoubleTap(){
        InMemoryPrefs pref=new InMemoryPrefs();
        PostFinalRecallStore store=new PostFinalRecallStore(pref);
        store.prepare(PostFinalRecallStore.SABQI,"2026-10-25",plan());
        assertNull(store.nextReady("",""));
        PostFinalRecallStore.Review ready=store.nextReady("2026-10-25","");
        assertNotNull(ready); // simulated process death after final commit before markReady()
        assertEquals(3,ready.total);
        assertEquals(2,ready.previousCount);
        assertEquals(601,ready.page);
        assertTrue(store.advance(ready,"2026-10-25",""));
        assertFalse(store.advance(ready,"2026-10-25","")); // no duplicated page credit
        PostFinalRecallStore resumed=new PostFinalRecallStore(pref);
        assertEquals(1,resumed.nextReady("2026-10-25","").position);
        assertTrue(resumed.markReady(PostFinalRecallStore.SABQI,"2026-10-25"));
        for(int i=1;i<3;i++) {
            PostFinalRecallStore.Review step=resumed.nextReady("2026-10-25","");
            assertEquals(i,step.position);
            assertTrue(resumed.advance(step,"2026-10-25",""));
        }
        assertNull(resumed.nextReady("2026-10-25",""));
        assertFalse(pref.disk.containsKey("v6AcquiredCreditLineIds"));
    }
    @Test public void recapReadsActualPageProgressButNeverAddsHistoricalMissedWork(){
        InMemoryPrefs disk=new InMemoryPrefs();
        PostFinalRecallStore store=new PostFinalRecallStore(disk);
        assertNull("Missed old final must not turn into a late reading",
            store.recapFor(PostFinalRecallStore.ITQAN,"2026-10-18","2026-10-25"));
        store.prepare(PostFinalRecallStore.ITQAN,"2026-11-01",plan());
        assertNull("PREPARED alone is not a validated final",
            store.recapFor(PostFinalRecallStore.ITQAN,"","2026-10-25"));
        assertTrue(store.markReady(PostFinalRecallStore.ITQAN,"2026-11-01"));
        PostFinalRecallStore.Recap pending=store.recapFor(
            PostFinalRecallStore.ITQAN,"","2026-11-01");
        assertNotNull(pending);
        assertFalse(pending.finished);
        assertEquals(3,pending.totalPages);
        assertEquals(0,pending.completedPages);
        assertEquals(2,pending.previousPages);
        assertTrue(store.advance(store.nextReadyFor(PostFinalRecallStore.ITQAN,
            "","2026-11-01"),"","2026-11-01"));
        assertEquals(1,store.recapFor(PostFinalRecallStore.ITQAN,
            "","2026-11-01").completedPages);
        for(int i=1;i<3;i++){
            PostFinalRecallStore.Review next=store.nextReadyFor(
                PostFinalRecallStore.ITQAN,"","2026-11-01");
            assertTrue(store.advance(next,"","2026-11-01"));
        }
        PostFinalRecallStore.Recap done=store.recapFor(
            PostFinalRecallStore.ITQAN,"","2026-11-01");
        assertTrue(done.finished);
        assertEquals(done.totalPages,done.completedPages);
        assertNull(store.nextReady("","2026-11-01"));
        assertFalse("Recap is read-only: Acquis cannot change",
            disk.disk.containsKey("v6AcquiredCreditLineIds"));
    }


    @Test public void fourConsecutiveSundayReadingsKeepTheirOwnDatesAndResumeInOrder(){
        InMemoryPrefs disk=new InMemoryPrefs();
        PostFinalRecallStore queue=new PostFinalRecallStore(disk);
        for(int w=0;w<4;w++){
            String date=java.time.LocalDate.of(2026,11,1).plusWeeks(w).toString();
            for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
                queue.prepare(family,date,plan());
                assertTrue(queue.markReady(family,date));
            }
        }
        String latest="2026-11-22";
        for(String family:new String[]{PostFinalRecallStore.SABQI,PostFinalRecallStore.ITQAN}){
            PostFinalRecallStore.Recap first=queue.recapFor(family,latest,latest);
            assertNotNull(first);
            assertEquals("2026-11-01",first.date);
            assertEquals(0,first.completedPages);
            PostFinalRecallStore.Review due=queue.nextReadyFor(family,latest,latest);
            assertNotNull(due);
            assertEquals(first.date,due.date);
            assertTrue(queue.advance(due,latest,latest));
            queue=new PostFinalRecallStore(disk); // restart between reviews
            assertEquals(1,queue.recapFor(family,latest,latest).completedPages);
            for(int i=1;i<3;i++){
                due=queue.nextReadyFor(family,latest,latest);
                assertEquals("2026-11-01",due.date);
                assertTrue(queue.advance(due,latest,latest));
            }
            assertEquals("2026-11-08",queue.recapFor(family,latest,latest).date);
            assertEquals("2026-11-08",queue.nextReadyFor(family,latest,latest).date);
        }
        assertEquals("2026-11-08",queue.recapFor(PostFinalRecallStore.SABQI,latest,latest).date);
        assertEquals("2026-11-08",queue.recapFor(PostFinalRecallStore.ITQAN,latest,latest).date);
        assertFalse("No preview or recap may write acquired credits",
            disk.disk.containsKey("v6AcquiredCreditLineIds"));
    }
    @Test public void pendingSabqiCannotHideAnIndependentCompletedItqanRecall(){
        InMemoryPrefs disk=new InMemoryPrefs();
        PostFinalRecallStore store=new PostFinalRecallStore(disk);
        store.prepare(PostFinalRecallStore.SABQI,"2026-10-18",plan());
        store.prepare(PostFinalRecallStore.ITQAN,"2026-10-25",plan());
        assertTrue(store.markReady(PostFinalRecallStore.SABQI,"2026-10-18"));
        assertTrue(store.markReady(PostFinalRecallStore.ITQAN,"2026-10-25"));
        assertEquals(PostFinalRecallStore.SABQI,store.nextReady("2026-10-18","2026-10-25").family);
        PostFinalRecallStore.Review itqan=store.nextReadyFor(PostFinalRecallStore.ITQAN,"2026-10-18","2026-10-25");
        assertNotNull("Itqan cannot be hidden behind older Sabqi",itqan);
        assertEquals(PostFinalRecallStore.ITQAN,itqan.family);
        assertEquals(2,itqan.previousCount);
        assertEquals(3,itqan.total);
        assertEquals(PostFinalRecallStore.SABQI,
            store.nextReadyFor(PostFinalRecallStore.SABQI,"2026-10-18","2026-10-25").family);
        assertTrue(store.advance(itqan,"2026-10-18","2026-10-25"));
        assertEquals(1,store.nextReadyFor(PostFinalRecallStore.ITQAN,"2026-10-18","2026-10-25").position);
        assertEquals(0,store.nextReadyFor(PostFinalRecallStore.SABQI,"2026-10-18","2026-10-25").position);
    }

    @Test public void independentItqanFinalAndHistoricalOpenRemainFrozen() throws Exception {
        InMemoryPrefs prefs=new InMemoryPrefs();
        PostFinalRecallStore store=new PostFinalRecallStore(prefs);
        store.prepare(PostFinalRecallStore.ITQAN,"2026-10-19",plan());
        assertNull(store.nextReady("2026-10-25",""));
        assertEquals(PostFinalRecallStore.ITQAN,store.nextReady("","2026-10-19").family);
        store.prepare(PostFinalRecallStore.ITQAN,"2026-10-19",plan());
        assertEquals(1,new org.json.JSONArray(prefs.disk.get(PostFinalRecallStore.KEY).toString()).length());
    }
}

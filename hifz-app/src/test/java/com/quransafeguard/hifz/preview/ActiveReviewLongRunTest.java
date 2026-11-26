package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.After;
import org.junit.Test;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.*;

/** Exercises the actual preference-backed active receipt, correction and coverage journals.
 * No UI/Android lifecycle or Sabqi/Itqan finale qualification is implied by these tests. */
public final class ActiveReviewLongRunTest {
    @After public void reset(){HifzClock.resetClockForTests();}
    @Test public void oneHundredEightyDays(){exercise(180);}
    @Test public void oneYear(){exercise(365);}
    @Test public void eighteenMonths(){exercise(548);}

    private void exercise(int days){
        InMemoryPrefs disk=new InMemoryPrefs();
        LocalDate start=LocalDate.of(2026,11,26);
        VerseRef[] verses={new VerseRef(2,1),new VerseRef(2,2),new VerseRef(2,3),new VerseRef(2,4)};
        ActiveReviewSlots slots=new ActiveReviewSlots(disk);
        slots.ensureProgramme(start);
        Set<Integer> expectedQueue=new HashSet<>();
        int cursor=0,expectedReceipts=0;
        for(int offset=0;offset<days;offset++){
            LocalDate day=start.plusDays(offset);
            HifzClock.setClockForTests(Clock.fixed(day.atStartOfDay(ZoneOffset.UTC).toInstant(),ZoneOffset.UTC));
            for(ActiveReviewSlots.Slot slot:ActiveReviewSlots.Slot.values()){
                slots=new ActiveReviewSlots(disk); // re-open the durable state at every slot
                if(!slots.scheduled(day,slot)||(offset%11==0&&slot==ActiveReviewSlots.Slot.MORNING)){
                    assertNull(slots.receipt(day,slot));
                    continue;
                }
                VerseRef first=verses[cursor],next=verses[(cursor+1)%verses.length];
                int duration=offset<days/3?40:offset<2*days/3?60:70;
                assertTrue(slots.checkpoint(day,slot,first,first,2,1000,false,duration));
                slots=new ActiveReviewSlots(disk);
                assertEquals(duration,slots.draft(day,slot).targetMinutes);
                assertTrue(slots.checkpoint(day,slot,first,first,2,duration*60_000L,false,70));
                assertTrue(slots.settleOtherCorrections(day,slot));
                ActiveReviewCorrectionStore correction=slots.correction(day,slot);
                int page=1+offset%13;
                expectedQueue.add(page);
                assertTrue(correction.recordReveal(day.toString(),first,page));
                List<Integer> batch=correction.pagesToCorrect(day.toString(),first);
                assertEquals(expectedQueue,new HashSet<>(batch));
                assertTrue(correction.begin(day.toString(),first,first,duration*60_000L,batch,2));
                String proof=ActiveReviewCoverage.encode(ActiveReviewCoverage.through(Collections.singletonList(first),first));
                assertTrue(slots.completeRecall(day,slot,first,next,"preuve",proof));
                expectedReceipts++;
                cursor=(cursor+1)%verses.length;
                assertEquals(next.toString(),disk.getString("activeMurajaahCursor",""));
                assertNull(slots.draft(day,slot));
                assertFalse(slots.completeRecall(day,slot,first,next,"double",proof));
                if(offset%5==0){ // interruption leaves the entire pending batch recoverable
                    assertEquals(expectedQueue,new HashSet<>(correction.queuedPages()));
                    continue;
                }
                int corrected=correction.pending().page();
                assertTrue(correction.pageReread(correction.pending()));
                assertTrue(correction.pageRecalled(correction.pending()));
                expectedQueue.remove(corrected);
                ActiveReviewCorrectionStore.State remaining=correction.pending();
                assertTrue(slots.finishIndependentCorrection(day,slot,remaining));
                assertEquals(expectedQueue,new HashSet<>(correction.queuedPages()));
                assertEquals(next.toString(),disk.getString("activeMurajaahCursor",""));
            }
        }
        assertEquals(expectedReceipts,slots.receipts().size());
        assertEquals(verses[cursor].toString(),disk.getString("activeMurajaahCursor",""));
    }
}

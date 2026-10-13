package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.*;

/** M9-P0: durable attendance must be independent of rolling 120-day display records. */
public final class DashboardLedgerP0CompletionTest {
    private static final String SABQI="SABQI";
    private static final String ITQAN="ITQAN";

    @Test public void day121DoesNotResurrectDayZeroOnceDisplayExpires(){
        LocalDate start=LocalDate.of(2026,10,5);
        List<DashboardLedger.Record> display=Collections.singletonList(
            new DashboardLedger.Record(start,SABQI,"done"));
        LinkedHashSet<String> durable=DashboardLedger.mergeCompletionKeys(
            Collections.emptySet(),display,start,SABQI);
        List<DashboardLedger.Record> expired=DashboardLedger.mergeRecords(display,
            start.plusDays(121),ITQAN,"today",start.plusDays(121));
        assertFalse("display still incorrectly includes day zero",expired.stream().anyMatch(r->r.date.equals(start)));
        assertTrue("completed day zero must still be known",DashboardLedger.containsCompletion(durable,start,SABQI));
        assertFalse("must not credit unearned Itqan",DashboardLedger.containsCompletion(durable,start,ITQAN));
    }

    @Test public void physicalDisplayRowsMigrateOnlyActuallyEarnedDates(){
        LocalDate start=LocalDate.of(2026,10,5);
        List<DashboardLedger.Record> display=new ArrayList<>();
        display.add(new DashboardLedger.Record(start,SABQI,"earned"));
        display.add(new DashboardLedger.Record(start.plusDays(1),ITQAN,"earned"));
        LinkedHashSet<String> keys=DashboardLedger.mergeCompletionKeys(
            Collections.emptySet(),display,null,null);
        assertEquals(2,keys.size());
        assertTrue(DashboardLedger.containsCompletion(keys,start,SABQI));
        assertTrue(DashboardLedger.containsCompletion(keys,start.plusDays(1),ITQAN));
        assertFalse(DashboardLedger.containsCompletion(keys,start.plusDays(2),SABQI));
    }

    @Test public void durableReceiptsSurvive548DaysAndDuplicatesWithoutExtraCredit(){
        LocalDate start=LocalDate.of(2026,10,5);
        LinkedHashSet<String> receipts=new LinkedHashSet<>();
        List<DashboardLedger.Record> visible=new ArrayList<>();
        for(int day=0;day<548;day++){
            LocalDate date=start.plusDays(day);
            String mode=day%2==0?SABQI:ITQAN;
            receipts=DashboardLedger.mergeCompletionKeys(receipts,visible,date,mode);
            receipts=DashboardLedger.mergeCompletionKeys(receipts,visible,date,mode);
            visible=DashboardLedger.mergeRecords(visible,date,mode,"done",date);
        }
        assertEquals("double validations must be idempotent",548,receipts.size());
        assertTrue(DashboardLedger.containsCompletion(receipts,start,SABQI));
        assertTrue(DashboardLedger.containsCompletion(receipts,start.plusDays(365),ITQAN));
        assertTrue(DashboardLedger.containsCompletion(receipts,start.plusDays(547),ITQAN));
        assertFalse(DashboardLedger.containsCompletion(receipts,start.plusDays(100),ITQAN));
        assertTrue("display remains bounded independently",visible.size()<=121);
    }

    @Test public void alreadyPurgedUnknownDaysAreNotInvented(){
        LocalDate oldDate=LocalDate.of(2026,1,1);
        LocalDate recent=LocalDate.of(2026,10,5);
        List<DashboardLedger.Record> visible=Collections.singletonList(
            new DashboardLedger.Record(recent,SABQI,"earned"));
        LinkedHashSet<String> migrated=DashboardLedger.mergeCompletionKeys(
            Collections.emptySet(),visible,recent,SABQI);
        assertFalse(DashboardLedger.containsCompletion(migrated,oldDate,SABQI));
    }
}

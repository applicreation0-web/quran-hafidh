package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent RED invariant for the remaining legacy Itqan completion payload. */
public final class AuditLegacyItqanPayload11711Test {
    @Test public void earnedItqanRepetitionsMustNotAuthenticateForeignLinesOrSkipToEnd() {
        InMemoryPrefs d=new InMemoryPrefs();d.disk.put("schema",6);
        for(String k:Arrays.asList("v6LearnedLineIds","v6StabilizedLineIds","v6AcquiredCreditLineIds",
                "v6QuarantineLineIds","v6LegacyPartialAcquiredLineIds"))d.disk.put(k,"[]");
        HifzPrefs p=new HifzPrefs(d);
        VerseRef start=new VerseRef(52,1),end=new VerseRef(52,10);
        AnchoringQueue.Entry entry=new AnchoringQueue.Entry(start.toString(),end.toString(),
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
        assertTrue(p.setItqanProgress(0,0,start,end));
        ItqanRegimeStore.UnitPlan plan=p.itqanUnitPlan(entry);
        assertTrue(p.stampItqanUnitPlan(entry,plan));
        for(int n=1;n<=ItqanMaintenancePolicy.totalReps(plan);n++)assertTrue(p.setItqanProgress(n,0,start,end));
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        boolean accepted=p.completeStabilizationBlockV6(Arrays.asList("604:0","604:1"),1,true,
            start,end,new VerseRef(114,6),"2026-11-06","foreign final payload",Collections.emptyList());
        System.out.println("LEGACY_ITQAN_FOREIGN_FINAL accepted="+accepted+" BEFORE="+before+" AFTER="+d.disk);
        assertFalse("earned repetitions for52:1..10 cannot credit page604 or jump to114:6",accepted);
        assertEquals(before,d.disk);
    }
}

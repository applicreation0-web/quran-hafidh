package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Physical evidence, positive canonical path and adversarial scalar/foreign payload. */
public final class ItqanPhysicalProof11711Test {
    private static GeometryRepository geometry;
    @BeforeClass public static void loadGeometry() throws Exception {
        Path p=Paths.get("app/src/main/assets/reader109/geometry.json");
        if(!Files.exists(p))p=Paths.get("../app/src/main/assets/reader109/geometry.json");
        geometry=GeometryRepository.fromJson(new String(Files.readAllBytes(p),StandardCharsets.UTF_8));
    }
    private static InMemoryPrefs disk(){
        InMemoryPrefs d=new InMemoryPrefs();
        d.disk.put("schema",6);
        for(String k:Arrays.asList("v6LearnedLineIds","v6StabilizedLineIds",
                "v6AcquiredCreditLineIds","v6QuarantineLineIds",
                "v6LegacyPartialAcquiredLineIds"))
            d.disk.put(k,"[]");
        d.disk.put("itqanRanges","[{\"start\":\"52:1\",\"end\":\"52:10\"}]");
        d.disk.put("promotedRanges","[]");
        d.disk.put("itqanRotationStart","52:1");
        d.disk.put("itqanCursor","52:1");
        return d;
    }
    private static AnchoringQueue.Entry entry(){
        return new AnchoringQueue.Entry("52:1","52:10",
            AnchoringQueue.Origin.PROMOTED,AnchoringQueue.ItqanProtocol.FULL,0);
    }
    private static List<StabilizationHalfPagePolicy.Unit> blocks(){
        List<String> ids=CorpusLinePolicy.ownedLineIdsForRangeOnPage(
            new VerseRef(52,1),new VerseRef(52,10),geometry);
        return StabilizationHalfPagePolicy.planPage(geometry.linesForExactIds(ids));
    }
    @Test public void canonicalCompleteAndSameBlockReplayAreStrict(){
        InMemoryPrefs d=disk();
        HifzPrefs h=new HifzPrefs(d);
        VerseRef start=new VerseRef(52,1),end=new VerseRef(52,10);
        AnchoringQueue.Entry e=entry();
        assertTrue(h.setItqanProgress(0,0,start,end));
        ItqanRegimeStore.UnitPlan plan=h.itqanUnitPlan(e);
        assertTrue(h.stampItqanUnitPlan(e,plan));
        List<StabilizationHalfPagePolicy.Unit> blocks=blocks();
        List<String> expected=blocks.get(0).lineIds;
        assertTrue("real geometry and exact protocol required",
            h.ensureItqanBlockProofV7(geometry,expected,start,end,plan,"2026-11-06"));
        String token=h.itqanBlockProofTokenV7();
        assertFalse(token.isEmpty());
        int quota=ItqanMaintenancePolicy.totalReps(plan);
        assertFalse("scalar counter cannot enter a verified session",
            h.setItqanProgress(quota,0,start,end));
        assertFalse("counter-only attempt did not advance",h.itqanRep()>0);
        assertFalse(h.recordItqanRepV7("stale",0,false,plan));
        for(int n=0;n<quota;n++){
            assertTrue("earned repetition "+n,
                h.recordItqanRepV7(token,n,false,plan));
            assertFalse("a repeated tap cannot double-credit",
                h.recordItqanRepV7(token,n,false,plan));
        }
        VerseRef next=h.itqanWorkCorpus().nextAnchored(end,h.repairedItqanRotationStart());
        boolean isFinal=blocks.size()==1;
        int sizeBefore=d.disk.size();
        assertFalse(h.completeStabilizationBlockV6(Arrays.asList("604:0","604:1"),
            1,true,start,end,new VerseRef(114,6),"2026-11-06",
            "foreign",Collections.emptyList()));
        assertEquals(sizeBefore,d.disk.size());
        assertTrue("a real complete physical block is accepted",
            h.completeStabilizationBlockV6(expected,1,isFinal,start,end,next,
                "2026-11-06","canonical",Collections.emptyList()));
        assertEquals("",h.itqanBlockProofTokenV7());
        assertFalse("duplicate payload cannot close twice",
            h.completeStabilizationBlockV6(expected,1,isFinal,start,end,next,
                "2026-11-06","duplicate",Collections.emptyList()));
    }
    @Test public void foreignPhysicalLinesCannotEvenOpenAProof(){
        InMemoryPrefs d=disk();
        HifzPrefs h=new HifzPrefs(d);
        VerseRef start=new VerseRef(52,1),end=new VerseRef(52,10);
        AnchoringQueue.Entry e=entry();
        ItqanRegimeStore.UnitPlan plan=h.itqanUnitPlan(e);
        assertTrue(h.stampItqanUnitPlan(e,plan));
        Map<String,Object> before=new LinkedHashMap<>(d.disk);
        assertFalse(h.ensureItqanBlockProofV7(geometry,Arrays.asList("604:0","604:1"),
            start,end,plan,"2026-11-06"));
        assertEquals(before,d.disk);
    }
}

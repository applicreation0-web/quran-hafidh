package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Every Juz gauge is a ratio of genuinely completed physical QCF lines. */
public final class JuzProgressSummaryTest {
    @Test public void percentCountsLinesNotVersesAndNeverPrematurelyRoundsUp() {
        assertEquals(0,JuzProgressSummary.percent(0,300));
        assertEquals(0,JuzProgressSummary.percent(1,300));
        assertEquals(50,JuzProgressSummary.percent(150,300));
        assertEquals(99,JuzProgressSummary.percent(299,300));
        assertEquals(100,JuzProgressSummary.percent(300,300));
    }

    @Test public void impossibleLineTotalsFailClosed() {
        for(int[] pair:new int[][]{{0,0},{2,1},{-1,20},{21,20}}){
            try{
                JuzProgressSummary.percent(pair[0],pair[1]);
                fail("No false Juz progress");
            }catch(IllegalArgumentException expected){}
        }
    }

    @Test public void eachPhysicalLineBelongsToOnlyOneCanonicalJuz() {
        ArrayList<GeometryRepository.LineMeta> sample=new ArrayList<>();
        int idx=0;
        for(int j=0;j<30;j++){
            int[] rub=QuranRubBoundaries.rowForIndex(j*8+1);
            sample.add(new GeometryRepository.LineMeta(idx++,"qcf-"+j,rub[3],
                Collections.singletonList(new VerseRef(rub[1],rub[2]))));
        }
        Set<String> acquired=new HashSet<>();
        acquired.add("qcf-0");
        Set<String> learned=new HashSet<>();
        learned.add("qcf-0");
        learned.add("qcf-1");
        Set<String> stabilized=new HashSet<>();
        stabilized.add("qcf-0");
        stabilized.add("qcf-2");
        int[][] counts=JuzProgressSummary.fromLines(sample,learned,stabilized,acquired);
        assertEquals(30,counts.length);
        int total=0;
        for(int[] c:counts)total+=c[JuzProgressSummary.TOTAL];
        assertEquals(30,total);
        assertEquals(1,counts[0][JuzProgressSummary.ACQUIRED]);
        assertEquals(0,counts[0][JuzProgressSummary.LEARNED]);
        assertEquals(0,counts[0][JuzProgressSummary.STABILIZED]);
        assertEquals(1,counts[1][JuzProgressSummary.LEARNED]);
        assertEquals(1,counts[2][JuzProgressSummary.STABILIZED]);
        assertEquals(0,JuzProgressSummary.done(counts[29]));
    }
}

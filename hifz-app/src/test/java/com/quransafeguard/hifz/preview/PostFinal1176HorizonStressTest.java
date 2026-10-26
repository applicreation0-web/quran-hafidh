package com.quransafeguard.hifz.preview;
import com.quransafeguard.hifz.core.VerseRef;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** 240 scenarios across 180, 365 and 548 days; actual backup-key prefs and restarts. */
public final class PostFinal1176HorizonStressTest {
    static GeometryRepository.LineMeta line(int page,int n){
        return new GeometryRepository.LineMeta(page*15+n,"p"+page+"-l"+n,page,Collections.<VerseRef>emptyList());
    }
    @Test public void twoHundredFortyDatedScenariosWithRebootAndDoubleTap(){
        int cases=0;
        for(int horizon:new int[]{180,365,548}){
            for(int scenario=0;scenario<80;scenario++){
                InMemoryPrefs disk=new InMemoryPrefs();
                List<GeometryRepository.LineMeta> acquired=new ArrayList<>();
                int priorCount=scenario%57;
                for(int p=0;p<priorCount;p++)acquired.add(line(100+p,0));
                int weeklyCount=1+scenario%4;
                List<GeometryRepository.LineMeta> newest=new ArrayList<>();
                for(int p=0;p<weeklyCount;p++)newest.add(line(500+p,scenario%3));
                List<PostFinalCumulativePolicy.Page> pages=PostFinalCumulativePolicy.plan(acquired,newest);
                assertEquals(Math.min(20,priorCount+weeklyCount),pages.size());
                String date=LocalDate.of(2026,10,26).plusDays(horizon).plusDays(scenario).toString();
                String family=scenario%2==0?PostFinalRecallStore.SABQI:PostFinalRecallStore.ITQAN;
                PostFinalRecallStore store=new PostFinalRecallStore(disk);
                store.prepare(family,date,pages);
                assertNull(store.nextReady("","")); // never before credits
                String sabqi=PostFinalRecallStore.SABQI.equals(family)?date:"";
                String itqan=PostFinalRecallStore.ITQAN.equals(family)?date:"";
                assertTrue(store.markReady(family,date));
                for(int page=0;page<pages.size();page++){
                    store=new PostFinalRecallStore(disk); // process death between each page
                    PostFinalRecallStore.Review step=store.nextReady(sabqi,itqan);
                    assertNotNull(step);
                    assertEquals(page,step.position);
                    assertTrue(store.advance(step,sabqi,itqan));
                    assertFalse(store.advance(step,sabqi,itqan)); // repeated button press
                }
                assertNull(new PostFinalRecallStore(disk).nextReady(sabqi,itqan));
                assertFalse(disk.disk.containsKey("v6AcquiredCreditLineIds"));
                cases++;
            }
        }
        assertEquals(240,cases);
    }
}

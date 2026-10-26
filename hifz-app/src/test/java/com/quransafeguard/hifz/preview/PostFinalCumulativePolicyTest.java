package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

public final class PostFinalCumulativePolicyTest {
    private static GeometryRepository.LineMeta line(int page,int offset) {
        return new GeometryRepository.LineMeta(page*15+offset,"p"+page+"-l"+offset,page,
            Collections.<VerseRef>emptyList());
    }
    private static List<GeometryRepository.LineMeta> prior(int count){
        ArrayList<GeometryRepository.LineMeta> lines=new ArrayList<>();
        for(int i=1;i<=count;i++)lines.add(line(100+i,0));
        return lines;
    }
    @Test public void boundaryZeroOneNineteenTwentyTwentyOne() {
        for(int count:new int[]{0,1,19,20,21,50}) {
            List<PostFinalCumulativePolicy.Page> pages=PostFinalCumulativePolicy.plan(
                prior(count),Arrays.asList(line(350,0),line(351,1)));
            assertEquals(Math.min(count+2,20),pages.size());
            assertEquals(350,pages.get(pages.size()-2).mushafPage);
            assertEquals(351,pages.get(pages.size()-1).mushafPage);
            assertTrue(pages.get(pages.size()-1).fromCurrentFinal);
            if(count>18)assertEquals(100+count-17,pages.get(0).mushafPage);
        }
    }
    @Test public void partialPagesDoNotInventMissingLinesOrDuplicateWeeklyPage() {
        List<PostFinalCumulativePolicy.Page> pages=PostFinalCumulativePolicy.plan(
            Arrays.asList(line(120,0),line(120,4),line(121,1),line(122,3)),
            Arrays.asList(line(122,1),line(122,2),line(123,1)));
        assertEquals(4,pages.size());
        assertEquals(Arrays.asList("p120-l0","p120-l4"),pages.get(0).lineIds);
        assertEquals(Arrays.asList("p122-l1","p122-l2","p122-l3"),pages.get(2).lineIds);
        assertEquals(123,pages.get(3).mushafPage);
        assertEquals(pages.size(),new HashSet<Integer>(Arrays.asList(
            pages.get(0).mushafPage,pages.get(1).mushafPage,pages.get(2).mushafPage,pages.get(3).mushafPage)).size());
    }
    @Test public void previousPagesFollowTheirAcquisitionOrderNotNumericalPageOrder() {
        List<PostFinalCumulativePolicy.Page> pages=PostFinalCumulativePolicy.plan(
            Arrays.asList(line(600,1),line(599,1),line(598,1)),
            Collections.singletonList(line(597,1)));
        assertEquals(Arrays.asList(600,599,598,597),Arrays.asList(
            pages.get(0).mushafPage,pages.get(1).mushafPage,pages.get(2).mushafPage,pages.get(3).mushafPage));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsEmptyNewlyValidatedFinal() {
        PostFinalCumulativePolicy.plan(prior(22),Collections.<GeometryRepository.LineMeta>emptyList());
    }
    @Test(expected=IllegalStateException.class) public void duplicatePhysicalPositionIsFailClosed(){
        PostFinalCumulativePolicy.plan(Collections.<GeometryRepository.LineMeta>emptyList(),
            Arrays.asList(new GeometryRepository.LineMeta(1,"p1-l01",1,Collections.<VerseRef>emptyList()),
                new GeometryRepository.LineMeta(1,"p1-l02",1,Collections.<VerseRef>emptyList())));
    }
}

package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class QcfAtlasLineWindowTest {
    private static WordGeometryRepository.WordBox word(String key,double top,double bottom){
        return new WordGeometryRepository.WordBox(key,10,top,40,bottom);
    }
    private static List<GeometryRepository.LineMeta> lines(){
        return Arrays.asList(
            new GeometryRepository.LineMeta(0,"10:0",10,0,0,40,Collections.emptyList()),
            new GeometryRepository.LineMeta(1,"10:1",10,1,40,80,Collections.emptyList()),
            new GeometryRepository.LineMeta(2,"10:2",10,2,80,120,Collections.emptyList()));
    }
    @Test public void completeRowIncludesOtherVersesAndTallDiacriticsWithoutNeighborFragments(){
        double[] window=QcfAtlasLineWindow.bounds(Arrays.asList(
            word("2:62:1",5,35),word("2:63:9",42,70),word("2:64:1",38,74),word("2:64:2",84,112)),
            lines(),new VerseRef(2,63),new int[]{9},0,new double[]{0,36.5,79,550});
        // The line's other verse reaches 38..74. Target-only +/-8 would expose
        // the preceding row (35) and clip a full-height source row elsewhere.
        assertArrayEquals(new double[]{36.5,79},window,0.001);
    }
    @Test public void overlappingRowInkKeepsWholeNeighborInsteadOfCuttingItsMarks(){
        double[] window=QcfAtlasLineWindow.bounds(Arrays.asList(
            word("2:62:1",5,45),word("2:63:9",39,70),word("2:64:1",84,112)),
            lines(),new VerseRef(2,63),new int[]{9},0,new double[]{0,Double.NaN,77,550});
        assertArrayEquals(new double[]{0,77},window,0.001);
    }
    @Test public void sourceRasterRejectsApparentlyClearWordGapAndKeepsWholeNeighbor(){
        assertArrayEquals(new double[]{0,77},QcfAtlasLineWindow.bounds(Arrays.asList(
            word("2:62:1",5,35),word("2:63:9",42,70),word("2:64:1",84,112)),
            lines(),new VerseRef(2,63),new int[]{9},0,
            new double[]{0,Double.NaN,77,550}),0.001);
    }
    @Test public void missingPhysicalAssignmentFailsClosed(){
        assertNull(QcfAtlasLineWindow.bounds(Arrays.asList(
            word("2:63:9",150,180)),lines(),new VerseRef(2,63),new int[]{9},0,new double[]{0,35,80,550}));
    }
    @Test public void shiftedOriginalSvgOriginDoesNotShiftWordGeometryTwice(){
        List<GeometryRepository.LineMeta> shifted=Arrays.asList(
            new GeometryRepository.LineMeta(0,"1:0",1,0,100,140,Collections.emptyList()),
            new GeometryRepository.LineMeta(1,"1:1",1,1,140,180,Collections.emptyList()),
            new GeometryRepository.LineMeta(2,"1:2",1,2,180,220,Collections.emptyList()));
        assertArrayEquals(new double[]{38.5,77},QcfAtlasLineWindow.bounds(Arrays.asList(
            word("1:1:1",5,35),word("1:2:1",42,70),word("1:3:1",84,112)),
            shifted,new VerseRef(1,2),new int[]{1},100,new double[]{0,38.5,77,550}),0.001);
    }
}

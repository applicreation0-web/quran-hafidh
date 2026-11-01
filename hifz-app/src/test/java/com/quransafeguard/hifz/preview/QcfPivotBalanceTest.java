package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Contract: REAL Quran words only, exactly n preceding and n following
 * either side's own authentic pivot, with the SAME n for both excerpts. */
public final class QcfPivotBalanceTest {
    private static List<WordGeometryRepository.WordBox> verse(int surah,int ayah,int count){
        List<WordGeometryRepository.WordBox> boxes=new ArrayList<>();
        for(int i=1;i<=count;i++){
            // Fake geometry is a unit-test fixture, never rendered as Quran.
            double x=10+(i-1)%7*33,y=10+(i-1)/7*35;
            boxes.add(new WordGeometryRepository.WordBox(
                surah+":"+ayah+":"+i,x,y,x+27,y+27));
        }
        return boxes;
    }
    @Test public void twoWordsEachSideInBothQcfExcerptsEvenForDifferentPivotLengths(){
        QcfPivotBalance.Selection[] x=QcfPivotBalance.paired(
            verse(2,63,11),new VerseRef(2,63),new int[]{5,6},
            verse(3,142,12),new VerseRef(3,142),new int[]{6});
        assertEquals(2,x[0].before);assertEquals(2,x[0].after);
        assertEquals(x[0].before,x[1].before);
        assertEquals(x[0].after,x[1].after);
        assertArrayEquals(new int[]{3,4,5,6,7,8},x[0].wordNumbers());
        assertArrayEquals(new int[]{4,5,6,7,8},x[1].wordNumbers());
        assertEquals(2,x[0].pivotWords);
        assertEquals(1,x[1].pivotWords);
    }
    @Test public void smallerQcfContextReducesBOTHExcerptsAndBothDirections(){
        QcfPivotBalance.Selection[] x=QcfPivotBalance.paired(
            verse(2,63,9),new VerseRef(2,63),new int[]{2},
            verse(3,142,12),new VerseRef(3,142),new int[]{6});
        assertEquals(1,x[0].before);assertEquals(1,x[0].after);
        assertEquals(1,x[1].before);assertEquals(1,x[1].after);
        assertArrayEquals(new int[]{1,2,3},x[0].wordNumbers());
        assertArrayEquals(new int[]{5,6,7},x[1].wordNumbers());
    }
    @Test public void wordAtVerseStartNeverForgesPrecedingQuranWords(){
        QcfPivotBalance.Selection[] x=QcfPivotBalance.paired(
            verse(2,63,9),new VerseRef(2,63),new int[]{1},
            verse(3,142,12),new VerseRef(3,142),new int[]{6});
        assertEquals(0,x[0].before);assertEquals(0,x[1].after);
        assertEquals(1,x[0].words.size());assertEquals(1,x[1].words.size());
    }
    @Test public void trueOmissionUsesOnlyExistingSourceWordAnchor(){
        QcfPivotBalance.Selection[] x=QcfPivotBalance.paired(
            verse(11,60,9),new VerseRef(11,60),new int[]{5},
            verse(11,99,8),new VerseRef(11,99),new int[]{4});
        assertEquals(x[0].before,x[1].before);
        assertEquals(x[0].after,x[1].after);
        assertEquals(1,x[1].pivotWords);
        assertArrayEquals(new int[]{2,3,4,5,6},x[1].wordNumbers());
    }
    @Test public void badOrRepeatedSidecarWordsFailClosedWithoutGuess(){
        try{
            QcfPivotBalance.paired(verse(2,63,10),new VerseRef(2,63),new int[]{99},
                verse(3,142,11),new VerseRef(3,142),new int[]{5});
            fail("Missing real source pivot accepted");
        }catch(IllegalArgumentException expected){}
        List<WordGeometryRepository.WordBox> duplicated=verse(2,63,10);
        duplicated.add(duplicated.get(4));
        try{
            QcfPivotBalance.single(duplicated,new VerseRef(2,63),new int[]{5});
            fail("Duplicate Quran word accepted");
        }catch(IllegalArgumentException expected){}
    }
    @Test public void sourceWordBoundsAreNeverReconstructedOrAltered(){
        List<WordGeometryRepository.WordBox> a=verse(2,63,10);
        WordGeometryRepository.WordBox original=a.get(4);
        QcfPivotBalance.Selection selected=QcfPivotBalance.single(a,new VerseRef(2,63),new int[]{5});
        assertSame("Original authenticated QCF word geometry must be reused",original,selected.words.get(2));
        assertEquals(original.x0,selected.words.get(2).x0,0);
        assertEquals(original.y1,selected.words.get(2).y1,0);
    }
}

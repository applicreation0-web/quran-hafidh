package com.quransafeguard.hifz.preview;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.*;

/** Jalalayn navigation on the private map cannot escape a selected Ayah Themes span. */
public final class MapJalalaynDialogTest {
    private MawdudiPassageIndex canon() throws Exception {
        File file=new File("src/main/assets/"+MawdudiPassageIndex.ASSET);
        if(!file.exists())file=new File("hifz-app/src/main/assets/"+MawdudiPassageIndex.ASSET);
        assertTrue(file.isFile());
        return MawdudiPassageIndex.parse(
            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }
    @Test public void all1053PassagesBoundAdjacentVerseNavigation() throws Exception {
        MawdudiPassageIndex index=canon();
        assertEquals(1053,index.count());
        for(MawdudiPassageIndex.Passage passage:index.all()) {
            final int first=passage.firstAyah,last=passage.lastAyah;
            assertEquals(first,MapJalalaynDialog.stepAyah(first,last,first,-1));
            assertEquals(last,MapJalalaynDialog.stepAyah(first,last,last,1));
            assertEquals(first,MapJalalaynDialog.boundedAyah(first,last,first-200));
            assertEquals(last,MapJalalaynDialog.boundedAyah(first,last,last+200));
            if(last>first) {
                assertEquals(first+1,MapJalalaynDialog.stepAyah(first,last,first,1));
                assertEquals(last-1,MapJalalaynDialog.stepAyah(first,last,last,-1));
            }
        }
    }
    @Test public void optionalSurah27SubnodesStayWithinSelectedBounds() {
        int[][] spans={{67,75},{76,81},{82,82}};
        for(int[] b:spans) {
            assertEquals(b[0],MapJalalaynDialog.stepAyah(b[0],b[1],b[0],-1));
            assertEquals(b[1],MapJalalaynDialog.stepAyah(b[0],b[1],b[1],1));
        }
    }
    @Test public void invalidStepsAndInvertedRangesAreRejected() {
        try{MapJalalaynDialog.stepAyah(1,10,5,0);fail();}catch(IllegalArgumentException expected){}
        try{MapJalalaynDialog.stepAyah(1,10,5,2);fail();}catch(IllegalArgumentException expected){}
        try{MapJalalaynDialog.boundedAyah(6,5,5);fail();}catch(IllegalArgumentException expected){}
    }
}

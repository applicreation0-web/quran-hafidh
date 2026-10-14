package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

/** No real translated text appears in public code or CI fixtures. */
public final class WordLookupCoreTest {
    private static boolean allow(int planned,int painted,boolean revealed) {
        return WordLookupAccessPolicy.allowed(true,true,true,planned,painted,revealed,false,false);
    }
    @Test public void neverLookupMaskedOrTemporarilyRevealedRep() {
        assertTrue(allow(0,0,false));
        for(int level:new int[]{25,50,75,100}){
            assertFalse(allow(level,level,false));
            assertFalse("fail-open cannot leak hidden words",allow(level,0,false));
            assertFalse("Reveal cannot authorize gloss",allow(level,0,true));
        }
        assertFalse(allow(0,0,true));
        assertFalse(allow(-1,0,false));
    }
    @Test public void requireUserActivationReviewedCorpusAndNonAnnotation() {
        for(int absent=0;absent<3;absent++) {
            assertFalse(WordLookupAccessPolicy.allowed(absent!=0,absent!=1,absent!=2,0,0,false,false,false));
        }
        assertFalse(WordLookupAccessPolicy.allowed(true,true,true,0,0,false,true,false));
        assertFalse(WordLookupAccessPolicy.allowed(true,true,true,0,0,false,false,true));
    }
    @Test public void stalePageOrRenderEpochIsRejected() {
        assertTrue(WordLookupAccessPolicy.acceptTap(true,518,518,7,7,"50:1:1"));
        assertFalse(WordLookupAccessPolicy.acceptTap(true,518,519,7,7,"50:1:1"));
        assertFalse(WordLookupAccessPolicy.acceptTap(true,518,518,7,8,"50:1:1"));
        assertFalse(WordLookupAccessPolicy.acceptTap(false,518,518,7,7,"50:1:1"));
        assertFalse(WordLookupAccessPolicy.acceptTap(true,518,518,7,7,"99:999:1"));
        assertFalse(WordLookupAccessPolicy.acceptTap(true,518,518,7,7,"50:1:0"));
    }
    @Test public void overlappingWordBoxesAreNeverGuessed() {
        WordLookupHitTest.Box a=new WordLookupHitTest.Box("1:1:3",10,12,23,20);
        WordLookupHitTest.Box b=new WordLookupHitTest.Box("1:1:4",21,12,35,20);
        assertEquals("1:1:3",WordLookupHitTest.exactUnambiguousKey(Arrays.asList(a,b),15,15));
        assertNull(WordLookupHitTest.exactUnambiguousKey(Arrays.asList(a,b),22,15));
        assertNull(WordLookupHitTest.exactUnambiguousKey(Arrays.asList(a,b),-1,15));
        assertNull(WordLookupHitTest.exactUnambiguousKey(Arrays.asList(a,b),Double.NaN,15));
    }
    @Test public void narrowWordsRemainSelectableIfUnique() {
        WordLookupHitTest.Box narrow=new WordLookupHitTest.Box("2:181:13",50,25,52,38);
        assertEquals("2:181:13",WordLookupHitTest.exactUnambiguousKey(Arrays.asList(narrow),51,30));
    }
}

package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
public final class QcfExcerptWindowTest {
    @Test public void retainsOnlyDifferencesAndOneWordOfContext(){
        int[] marked={4,9};
        for(int i=1;i<=12;i++)assertEquals("word "+i,
            (i>=3&&i<=5)||(i>=8&&i<=10),QcfExcerptWindow.includes(i,marked));
    }
    @Test public void invalidOrEmptyAnnotationsDoNotClaimAnExcerpt(){
        assertFalse(QcfExcerptWindow.includes(1,new int[0]));
        assertFalse(QcfExcerptWindow.includes(1,new int[]{0}));
        assertFalse(QcfExcerptWindow.includes(0,new int[]{1}));
        assertFalse(QcfExcerptWindow.includes(1,null));
    }
}

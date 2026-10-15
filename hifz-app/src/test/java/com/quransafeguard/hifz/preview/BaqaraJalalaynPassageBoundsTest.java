package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** The Jalalayn pane navigates verses, not Mawdudi commentary sections. */
public final class BaqaraJalalaynPassageBoundsTest {
    @Test public void everyMacroThemePreservesItsVerseBounds() {
        for (BaqaraPilotSpec.Theme theme : BaqaraPilotSpec.themes()) {
            int first=theme.first,last=theme.last;
            assertEquals(first,BaqaraPilotScreen.boundedTafsirAyah(first,last,-1));
            assertEquals(last,BaqaraPilotScreen.boundedTafsirAyah(first,last,1000));
            for(int v=first;v<=last;v++) {
                assertEquals(Math.max(first,v-1),BaqaraPilotScreen.stepTafsirAyah(first,last,v,-1));
                assertEquals(Math.min(last,v+1),BaqaraPilotScreen.stepTafsirAyah(first,last,v,1));
            }
        }
    }
    @Test public void editorialPassageSubrangesCannotLeakToNeighbor() {
        for(int first=1;first<=286;first+=7) {
            int last=Math.min(286,first+6);
            assertEquals(first,BaqaraPilotScreen.stepTafsirAyah(first,last,first,-1));
            assertEquals(last,BaqaraPilotScreen.stepTafsirAyah(first,last,last,1));
        }
    }
    @Test public void rejectNonAdjacentNavigationAndInvalidBounds() {
        try{BaqaraPilotScreen.stepTafsirAyah(1,6,3,2);fail();}catch(IllegalArgumentException expected){}
        try{BaqaraPilotScreen.boundedTafsirAyah(7,6,6);fail();}catch(IllegalArgumentException expected){}
        try{BaqaraPilotScreen.boundedTafsirAyah(1,287,5);fail();}catch(IllegalArgumentException expected){}
    }
}

package com.quransafeguard.hifz.preview;

import org.junit.Test;
import static org.junit.Assert.*;

/** Only exact source-reviewed 1.17.9 insertion/omission exceptions are projected.
 * Canonical Quran words and source-ordered catalogue entries are immutable.
 */
public final class AtlasDiscriminantOmissionsTest {
    private AtlasDiscriminantCases.Entry byId(int auditId) {
        for (AtlasDiscriminantCases.Entry item : AtlasDiscriminantCases.CATALOGUE)
            if (item.auditId == auditId) return item;
        throw new AssertionError("Missing approved al-Abbad comparison " + auditId);
    }

    @Test public void fiveReviewedCasesShowAbsentRatherThanFalselyUnderlineSharedWords() {
        assertOmission(142, new int[]{4}, new int[0]);
        assertOmission(145, new int[]{1}, new int[0]);
        assertOmission(149, new int[]{11}, new int[0]);
        assertOmission(161, new int[0], new int[]{18,19,20});
        assertOmission(185, new int[0], new int[]{5,6});
    }

    private void assertOmission(int id, int[] left, int[] right) {
        AtlasDiscriminantCases.Entry entry=byId(id);
        assertEquals("Ajout / omission", AtlasDiscriminantOmissions.relation(entry));
        assertArrayEquals("left QCF genuine differences: "+id,left,
            AtlasDiscriminantOmissions.actualDifference(entry,true));
        assertArrayEquals("right QCF genuine differences: "+id,right,
            AtlasDiscriminantOmissions.actualDifference(entry,false));
        assertEquals(left.length==0, AtlasDiscriminantOmissions.explicitlyAbsent(entry,true));
        assertEquals(right.length==0, AtlasDiscriminantOmissions.explicitlyAbsent(entry,false));
        // Historical common-word anchors remain for a faithful Quran crop, never an underline.
        assertArrayEquals(entry.firstWordIndices,AtlasDiscriminantOmissions.cropAnchor(entry,true));
        int[] expectedRightCrop=(id==161||id==185)?right:entry.secondWordIndices;
        assertArrayEquals("Crop every real inserted word, or preserve the non-underlined absent-side anchor",
            expectedRightCrop,AtlasDiscriminantOmissions.cropAnchor(entry,false));
        assertTrue("Exactly one side absent: "+id,(left.length==0)!=(right.length==0));
    }

    @Test public void completeQcfWordGroupsAreReviewedDisplayOnly() {
        assertGroup(124,new int[]{6,7},new int[]{3});
        assertGroup(147,new int[]{20,21},new int[]{13});
    }

    private void assertGroup(int id,int[] left,int[] right) {
        AtlasDiscriminantCases.Entry entry=byId(id);
        assertEquals("Bifurcation : groupe de mots",AtlasDiscriminantOmissions.relation(entry));
        assertArrayEquals(left,AtlasDiscriminantOmissions.actualDifference(entry,true));
        assertArrayEquals(right,AtlasDiscriminantOmissions.actualDifference(entry,false));
        assertArrayEquals(left,AtlasDiscriminantOmissions.cropAnchor(entry,true));
        assertArrayEquals(right,AtlasDiscriminantOmissions.cropAnchor(entry,false));
        assertFalse(AtlasDiscriminantOmissions.explicitlyAbsent(entry,true));
        assertFalse(AtlasDiscriminantOmissions.explicitlyAbsent(entry,false));
        // This presentation adapter cannot mutate the approved 279-case catalogue.
        if(id==124){
            assertArrayEquals(new int[]{6},entry.firstWordIndices);
            assertArrayEquals(new int[]{3},entry.secondWordIndices);
        }else{
            assertArrayEquals(new int[]{21},entry.firstWordIndices);
            assertArrayEquals(new int[]{13},entry.secondWordIndices);
        }
    }

    @Test public void twoIndependentQcfPivotsNeverFabricateAnOppositeWord(){
        AtlasDiscriminantCases.Entry first=byId(120);
        assertEquals("Deux bifurcations",AtlasDiscriminantOmissions.relation(first));
        assertArrayEquals(new int[]{2},AtlasDiscriminantOmissions.actualDifference(first,true));
        assertArrayEquals(new int[]{2},AtlasDiscriminantOmissions.actualDifference(first,false));
        assertArrayEquals(new int[]{11},AtlasDiscriminantOmissions.secondaryDifference(first,true));
        assertArrayEquals(new int[]{10},AtlasDiscriminantOmissions.secondaryDifference(first,false));
        AtlasDiscriminantCases.Entry addition=byId(199);
        assertEquals("Ajout puis bifurcation",AtlasDiscriminantOmissions.relation(addition));
        assertArrayEquals(new int[]{21,22},AtlasDiscriminantOmissions.actualDifference(addition,true));
        assertArrayEquals(new int[]{8},AtlasDiscriminantOmissions.actualDifference(addition,false));
        assertArrayEquals(new int[0],AtlasDiscriminantOmissions.secondaryDifference(addition,true));
        assertArrayEquals(new int[]{7},AtlasDiscriminantOmissions.secondaryDifference(addition,false));
        assertArrayEquals(new int[]{20},AtlasDiscriminantOmissions.secondaryCropAnchor(addition,true));
        assertArrayEquals(new int[]{7},AtlasDiscriminantOmissions.secondaryCropAnchor(addition,false));
        assertTrue(AtlasDiscriminantOmissions.hasSecondary(120));
        assertTrue(AtlasDiscriminantOmissions.hasSecondary(199));
        assertFalse(AtlasDiscriminantOmissions.hasSecondary(124));
        assertArrayEquals(new int[]{21},addition.firstWordIndices);
        assertArrayEquals(new int[]{8},addition.secondWordIndices);
    }

    @Test public void otherApprovedPairsAndBaqaraPriorityStayByteForByteStable() {
        assertEquals(279,AtlasDiscriminantCases.CATALOGUE.size());
        for(AtlasDiscriminantCases.Entry entry:AtlasDiscriminantCases.CATALOGUE) {
            if(entry.auditId==120||entry.auditId==199
                    ||entry.auditId==124||entry.auditId==147
                    ||entry.auditId==142||entry.auditId==145||entry.auditId==149
                    ||entry.auditId==161||entry.auditId==185)continue;
            assertEquals(entry.kind,AtlasDiscriminantOmissions.relation(entry));
            assertArrayEquals(entry.firstWordIndices,AtlasDiscriminantOmissions.actualDifference(entry,true));
            assertArrayEquals(entry.secondWordIndices,AtlasDiscriminantOmissions.actualDifference(entry,false));
            assertFalse(AtlasDiscriminantOmissions.explicitlyAbsent(entry,true));
            assertFalse(AtlasDiscriminantOmissions.explicitlyAbsent(entry,false));
        }
        assertNotNull(AtlasDiscriminantCases.forVerse(2,63));
        assertNotNull(AtlasDiscriminantCases.forVerse(13,18));
    }

    @Test public void returnedArraysCannotMutateApprovedCatalogue() {
        AtlasDiscriminantCases.Entry entry=byId(142);
        int[] selected=AtlasDiscriminantOmissions.actualDifference(entry,true);
        selected[0]=0;
        assertArrayEquals(new int[]{4},AtlasDiscriminantOmissions.actualDifference(entry,true));
        int[] location=AtlasDiscriminantOmissions.cropAnchor(entry,false);
        location[0]=0;
        assertArrayEquals(new int[]{4},AtlasDiscriminantOmissions.cropAnchor(entry,false));
    }
}

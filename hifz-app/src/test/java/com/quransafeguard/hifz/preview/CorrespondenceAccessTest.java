package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Synthetic approval/geometry fixtures only; no production corpus is approved here. */
public final class CorrespondenceAccessTest {
    private static final Geometry G=new Geometry() {
        public int ayahCount(int s) { return s>=1&&s<=114?20:0; }
        public int firstPage(int s,int a) { return s; }
        public int lastPage(int s,int a) { return s; }
        public boolean hasSegmentGeometry(int s,int a,int f,int l) { return false; }
    };
    private static VerseRange range(int s,int a,int b) { return new VerseRange(s,a,b,G); }
    private static Gates approved() { return new Gates(true,true,true,true,true); }
    private static Anchor anchor(int origin,VerseRange r) {
        return new Anchor(origin,r,true,"SYNTHETIC test locator",0,0);
    }
    private static Relation pair(String id,int aS,int aA,int bS,int bA,Gates gates) {
        VerseRange a=range(aS,aA,aA),b=range(bS,bA,bA);
        return new Relation(id,Type.RESSEMBLANCE,Mode.TWO_PAGES,Arrays.asList(a,b),
            Arrays.asList(anchor(0,a),anchor(0,a),anchor(1,b)),gates,G);
    }
    private static CorrespondenceAccess.Origin origin(CorrespondenceAccess.Surface surface) {
        ReaderSnapshot snapshot=new ReaderSnapshot(2,2,2,"unit-id","passage-id","mask-opaque",
            1.75,"selected-opaque",12,54);
        return new CorrespondenceAccess.Origin(surface,"mode-opaque",snapshot,range(2,1,5));
    }
    private static CorrespondenceAccess access(Relation... relations) {
        return new CorrespondenceAccess(new Index(Arrays.asList(relations)),G);
    }
    private static void rejects(Runnable operation) {
        try { operation.run(); fail("invalid access accepted"); } catch(IllegalArgumentException expected) { }
    }
    @Test public void noApprovedRelationProducesEmptyAccessAndNoPhantomAffordance() {
        CorrespondenceAccess.Result result=access().at(origin(CorrespondenceAccess.Surface.LECTURE),2,2);
        assertTrue(result.visible.isEmpty()); assertFalse(result.hasLinks());
        assertFalse(result.hasOthers()); assertTrue(result.others().isEmpty());
    }
    @Test public void contextualAccessCapsTwoUniqueLinksAndDiscoversTheRestOnDemand() {
        Relation a=pair("TEST-A",2,2,3,3,approved()),b=pair("TEST-B",2,2,4,4,approved()),
            c=pair("TEST-C",2,2,5,5,approved()),d=pair("TEST-D",2,2,6,6,approved());
        CorrespondenceAccess.Result result=access(a,b,c,d).at(origin(CorrespondenceAccess.Surface.ATLAS),2,2);
        assertTrue(result.hasLinks()); assertEquals(Arrays.asList(a,b),result.visible);
        assertTrue(result.hasOthers()); assertEquals(Arrays.asList(c,d),result.others());
        assertEquals(2,CorrespondenceAccess.DEFAULT_VISIBLE_LIMIT);
    }
    @Test public void sharedEndpointNeverInfersAnotherRelationOrQueriesOutsideWorkedRange() {
        Relation ab=pair("TEST-AB",2,2,3,3,approved());
        Relation bc=pair("TEST-BC",3,3,4,4,approved());
        CorrespondenceAccess access=access(ab,bc);
        CorrespondenceAccess.Origin original=origin(CorrespondenceAccess.Surface.LECTURE);
        assertEquals(Arrays.asList(ab),access.at(original,2,2).visible);
        assertTrue(access.at(original,2,1).visible.isEmpty());
        rejects(()->access.at(original,3,3));
    }
    @Test public void sabqiTargetMustRemainInsideItsExactWorkedInterval() {
        Relation r=pair("TEST-SABQI",2,2,3,3,approved());
        CorrespondenceAccess access=access(r);
        CorrespondenceAccess.Origin original=origin(CorrespondenceAccess.Surface.SABQI);
        CorrespondenceAccess.Result result=access.at(original,2,2);
        assertEquals(Arrays.asList(r),result.visible); assertSame(original,result.origin);
        assertEquals(1,original.workedRange.start); assertEquals(5,original.workedRange.end);
        rejects(()->access.at(original,2,6)); rejects(()->access.at(original,3,3));
        assertEquals(1,original.workedRange.start); assertEquals(5,original.workedRange.end);
    }
    @Test public void everyFalseGateKeepsTheContextCompletelyEmpty() {
        List<Relation> candidates=new ArrayList<>();
        for(int missing=0;missing<5;missing++) {
            boolean[] f={true,true,true,true,true}; f[missing]=false;
            candidates.add(pair("TEST-GATE-"+missing,2,2,3,3,new Gates(f[0],f[1],f[2],f[3],f[4])));
        }
        CorrespondenceAccess.Result result=new CorrespondenceAccess(new Index(candidates),G)
            .at(origin(CorrespondenceAccess.Surface.ATLAS),2,2);
        assertFalse(result.hasLinks()); assertTrue(result.visible.isEmpty()); assertFalse(result.hasOthers());
    }
    @Test public void contextualOriginalBoundsDoNotBecomeFocalLinks() {
        VerseRange context=range(2,1,5),focal=range(2,2,2),other=range(3,3,3);
        Relation r=new Relation("TEST-FOCUS",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            Arrays.asList(context,other),Arrays.asList(anchor(0,focal),anchor(1,other)),approved(),G);
        CorrespondenceAccess access=access(r); CorrespondenceAccess.Origin original=origin(CorrespondenceAccess.Surface.SABQI);
        assertTrue(access.at(original,2,1).visible.isEmpty()); assertEquals(1,access.at(original,2,2).visible.size());
        assertTrue(access.at(original,2,5).visible.isEmpty());
    }
    @Test public void originRetainsOpaqueModeAndExactImmutableReaderSnapshotOnEverySurface() {
        for(CorrespondenceAccess.Surface surface:CorrespondenceAccess.Surface.values()) {
            CorrespondenceAccess.Origin original=origin(surface);
            CorrespondenceAccess.Result result=access(pair("TEST-ORIGIN",2,2,3,3,approved())).at(original,2,2);
            assertSame(original,result.origin); assertSame(original.snapshot,result.origin.snapshot);
            assertEquals(surface,result.origin.surface); assertEquals("mode-opaque",result.origin.mode);
            assertEquals("unit-id",result.origin.snapshot.unitId);
            assertEquals("passage-id",result.origin.snapshot.passageId);
            assertEquals("mask-opaque",result.origin.snapshot.mask);
            assertEquals("selected-opaque",result.origin.snapshot.selection);
            assertEquals(1.75,result.origin.snapshot.zoom,0.0);
            assertEquals(12,result.origin.snapshot.scrollX); assertEquals(54,result.origin.snapshot.scrollY);
        }
    }
    @Test public void visibleAndDiscoveryListsAreImmutable() {
        CorrespondenceAccess.Result result=access(pair("TEST-1",2,2,3,3,approved()),
            pair("TEST-2",2,2,4,4,approved()),pair("TEST-3",2,2,5,5,approved()))
            .at(origin(CorrespondenceAccess.Surface.ATLAS),2,2);
        try { result.visible.clear(); fail(); } catch(UnsupportedOperationException expected) { }
        try { result.others().clear(); fail(); } catch(UnsupportedOperationException expected) { }
        assertEquals(2,result.visible.size()); assertEquals(1,result.others().size());
    }
    @Test public void invalidInputsAndCanonMismatchAreRejectedBeforeLookup() {
        CorrespondenceAccess access=access(); CorrespondenceAccess.Origin original=origin(CorrespondenceAccess.Surface.SABQI);
        rejects(()->access.at(null,2,2)); rejects(()->access.at(original,0,2));
        rejects(()->access.at(original,115,2)); rejects(()->access.at(original,2,0));
        rejects(()->access.at(original,2,21));
        rejects(()->new CorrespondenceAccess(null,G)); rejects(()->new CorrespondenceAccess(new Index(Collections.emptyList()),null));
        rejects(()->new CorrespondenceAccess.Origin(null,"mode",original.snapshot,original.workedRange));
        rejects(()->new CorrespondenceAccess.Origin(original.surface,null,original.snapshot,original.workedRange));
        rejects(()->new CorrespondenceAccess.Origin(original.surface,"mode",null,original.workedRange));
        rejects(()->new CorrespondenceAccess.Origin(original.surface,"mode",original.snapshot,null));
        Geometry inconsistent=new Geometry() {
            public int ayahCount(int s) { return 1; }
            public int firstPage(int s,int a) { return s; }
            public int lastPage(int s,int a) { return s; }
            public boolean hasSegmentGeometry(int s,int a,int f,int l) { return false; }
        };
        CorrespondenceAccess wrongCanon=new CorrespondenceAccess(new Index(Collections.emptyList()),inconsistent);
        rejects(()->wrongCanon.at(original,2,1));
    }
}

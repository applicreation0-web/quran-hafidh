package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Synthetic fixtures only: flags/coordinates below are not editorial or QCF approval. */
public final class CorrespondenceModelTest {
    private static final Geometry G = new Geometry() {
        public int ayahCount(int surah) { return surah >= 1 && surah <= 114 ? 300 : 0; }
        public int firstPage(int surah, int ayah) { return surah + (ayah - 1) / 10; }
        public int lastPage(int surah, int ayah) {
            return firstPage(surah, ayah) + (ayah == 10 ? 1 : 0);
        }
        public boolean hasSegmentGeometry(int surah, int ayah, int firstWord, int lastWord) {
            return ayah == 1 && firstWord >= 1 && lastWord <= 4;
        }
    };
    private static VerseRange range(int s, int a, int b) { return new VerseRange(s,a,b,G); }
    private static Gates gates() { return new Gates(true,true,true,true,true); }
    private static Anchor anchor(int origin, VerseRange range) {
        return new Anchor(origin,range,true,"TEST-ONLY locator",0,0);
    }
    private static Relation pair(String id, VerseRange a, VerseRange b, Gates gates) {
        return new Relation(id, Type.RESSEMBLANCE, Mode.TWO_PAGES,
            Arrays.asList(a,b), Arrays.asList(anchor(0,a),anchor(1,b)), gates, G);
    }
    private static Relation series(int count) {
        List<VerseRange> ranges=new ArrayList<>(); List<Anchor> anchors=new ArrayList<>();
        for(int i=0;i<count;i++) { VerseRange r=range(54,i+1,i+1); ranges.add(r); anchors.add(anchor(i,r)); }
        return new Relation("TEST-SERIES-"+count, Type.REPETITION,Mode.SERIES,ranges,anchors,gates(),G);
    }
    private static ReaderSnapshot reader() {
        return new ReaderSnapshot(2,10,3,"unit-unchanged","passage-unchanged","mask-state",1.75,"selection-state");
    }
    private static void rejects(Runnable action) {
        try { action.run(); fail("invalid state accepted"); } catch(IllegalArgumentException expected) { }
    }

    @Test public void publicTypesAndModesHaveExactlyContractualMembers() {
        List<String> names=new ArrayList<>();
        for(Type type:Type.values()) names.add(type.name());
        assertEquals(Arrays.asList("REPETITION","RESSEMBLANCE","OPPOSITION","PROGRESSION",
            "ALTERNANCE","INVERSION","CENTRE","DEBUT_FIN_INCLUSIO"),names);
        assertArrayEquals(new Mode[]{Mode.TWO_PAGES,Mode.SERIES,Mode.INTERNAL},Mode.values());
    }
    @Test public void rangesRejectInvalidVerseBoundsAndMissingGeometry() {
        rejects(()->range(0,1,1)); rejects(()->range(115,1,1));
        rejects(()->range(2,0,1)); rejects(()->range(2,5,4)); rejects(()->range(2,1,301));
        rejects(()->new VerseRange(2,1,1,null));
        Geometry missing=new Geometry() {
            public int ayahCount(int s) { return 3; }
            public int firstPage(int s,int a) { return a==2 ? 0 : 1; }
            public int lastPage(int s,int a) { return 1; }
            public boolean hasSegmentGeometry(int s,int a,int f,int l) { return false; }
        };
        rejects(()->new VerseRange(1,1,3,missing));
    }
    @Test public void rangeIncludesContinuationOfFinalAyahOnNextPhysicalPage() {
        VerseRange r=range(2,9,10);
        assertEquals(2,r.firstPage); assertEquals(3,r.lastPage);
        assertTrue(r.contains(2,10)); assertFalse(r.contains(3,10));
    }
    @Test public void focusAnchorsNeverReplaceOrExpandOriginalBounds() {
        VerseRange context=range(2,1,5), focal=range(2,2,2), other=range(3,3,3);
        Relation r=new Relation("TEST-CONTEXT",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            Arrays.asList(context,other),Arrays.asList(anchor(0,focal),anchor(1,other)),gates(),G);
        assertEquals(1,r.originals.get(0).start); assertEquals(5,r.originals.get(0).end);
        assertEquals(2,r.anchors.get(0).range.start);
        Index index=new Index(Collections.singletonList(r));
        assertTrue(index.at(2,1).isEmpty()); assertEquals(1,index.at(2,2).size());
        rejects(()->new Relation("TEST-ESCAPE",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            Arrays.asList(context,other),Arrays.asList(anchor(0,range(2,6,6))),gates(),G));
        rejects(()->new Relation("TEST-BAD-ORIGIN",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            Arrays.asList(context,other),Arrays.asList(anchor(2,focal)),gates(),G));
    }
    @Test public void everyApprovalGateIsRequiredForVisibility() {
        for(int missing=0;missing<5;missing++) {
            boolean[] f={true,true,true,true,true}; f[missing]=false;
            Relation r=pair("TEST-GATE-"+missing,range(1,6,6),range(2,2,2),
                new Gates(f[0],f[1],f[2],f[3],f[4]));
            assertFalse(r.displayable()); assertTrue(new Index(Arrays.asList(r)).at(1,6).isEmpty());
            rejects(()->new Navigation(r,reader()));
        }
    }
    @Test public void unresolvedTypeAndUnattestedOrMissingAnchorsFailClosed() {
        List<VerseRange> originals=Arrays.asList(range(1,1,1),range(2,1,1));
        Relation unresolved=new Relation("TEST-UNRESOLVED",null,Mode.TWO_PAGES,originals,
            Arrays.asList(anchor(0,originals.get(0)),anchor(1,originals.get(1))),gates(),G);
        assertFalse(unresolved.displayable());
        Relation absent=new Relation("TEST-NO-ANCHOR",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            originals,Collections.emptyList(),gates(),G);
        assertFalse(absent.displayable());
        Relation partial=new Relation("TEST-PARTIAL",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            originals,Arrays.asList(anchor(0,originals.get(0))),gates(),G);
        assertFalse(partial.displayable());
        Relation unverified=new Relation("TEST-UNVERIFIED",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            originals,Arrays.asList(new Anchor(0,originals.get(0),false,"TEST locator",0,0),
            anchor(1,originals.get(1))),gates(),G);
        assertFalse(unverified.displayable());
        rejects(()->new Anchor(0,originals.get(0),true,"",0,0));
    }
    @Test public void directIndexDoesNotInferTransitiveOrContextualLinks() {
        Relation ab=pair("TEST-AB",range(1,6,6),range(2,2,2),gates());
        Relation bc=pair("TEST-BC",range(2,2,2),range(3,3,3),gates());
        Index index=new Index(Arrays.asList(ab,bc));
        assertEquals(Arrays.asList(ab),index.at(1,6));
        assertEquals(Arrays.asList(ab,bc),index.at(2,2));
        assertEquals(Arrays.asList(bc),index.at(3,3));
        assertTrue(index.at(2,1).isEmpty());
    }
    @Test public void indexPreservesRelationsAndDeduplicatesOverlappingAnchors() {
        VerseRange r=range(2,1,4);
        Relation internal=new Relation("TEST-INTERNAL",Type.INVERSION,Mode.INTERNAL,Arrays.asList(r),
            Arrays.asList(anchor(0,range(2,1,2)),anchor(0,range(2,2,3))),gates(),G);
        assertEquals(1,new Index(Arrays.asList(internal)).at(2,2).size());
        rejects(()->new Index(Arrays.asList(internal,internal)));
        try { new Index(Arrays.asList(internal)).at(2,2).clear(); fail(); }
        catch(UnsupportedOperationException expected) { }
    }
    @Test public void modeCardinalityAndScopeAreValidatedWithoutDuplicatingInternalView() {
        VerseRange a=range(1,1,1),b=range(2,1,1);
        rejects(()->new Relation("TEST-TWO",Type.RESSEMBLANCE,Mode.TWO_PAGES,
            Arrays.asList(a),Arrays.asList(anchor(0,a)),gates(),G));
        rejects(()->new Relation("TEST-INTERNAL",Type.CENTRE,Mode.INTERNAL,
            Arrays.asList(a,b),Arrays.asList(anchor(0,a)),gates(),G));
        rejects(()->new Relation("TEST-SERIES",Type.REPETITION,Mode.SERIES,
            Arrays.asList(a,b),Arrays.asList(anchor(0,a)),gates(),G));
        assertEquals(Scope.INTER,pair("TEST-INTER",a,b,gates()).scope);
        assertEquals(Scope.INTRA,pair("TEST-INTRA",a,range(1,2,2),gates()).scope);
    }
    @Test public void segmentMarkingRequiresExactWordGeometryNotJustWholeVerseGeometry() {
        VerseRange whole=range(2,1,1);
        Anchor good=new Anchor(0,whole,true,"TEST segment",2,3);
        Relation precise=new Relation("TEST-PRECISE",Type.CENTRE,Mode.INTERNAL,
            Arrays.asList(whole),Arrays.asList(good),gates(),G);
        assertFalse(precise.displayable()); // Boolean-only geometry is not a word renderer.
        Anchor unknown=new Anchor(0,whole,true,"TEST absent segment",5,6);
        Relation absent=new Relation("TEST-ABSENT",Type.CENTRE,Mode.INTERNAL,
            Arrays.asList(whole),Arrays.asList(unknown),gates(),G);
        assertFalse(absent.displayable());
        rejects(()->new Anchor(0,range(2,1,2),true,"TEST multi-ayah word",1,2));
        rejects(()->new Anchor(0,whole,true,"TEST half segment",0,2));
    }
    @Test public void centreWithOnlyAyahPolygonNeverBecomesApproved() {
        VerseRange whole=range(2,1,1);
        Relation r=new Relation("TEST-AYAH-ONLY-CENTRE",Type.CENTRE,Mode.INTERNAL,
            Arrays.asList(whole),Arrays.asList(anchor(0,whole)),gates(),G);
        assertFalse(r.displayable()); assertTrue(new Index(Arrays.asList(r)).at(2,1).isEmpty());
        rejects(()->new Navigation(r,reader()));
    }
    @Test public void centreCannotBeApprovedAsArtificialTwoPageComparison() {
        VerseRange whole=range(2,1,1),other=range(3,1,1);
        Relation r=new Relation("TEST-TWO-PAGE-CENTRE",Type.CENTRE,Mode.TWO_PAGES,
            Arrays.asList(whole,other),Arrays.asList(new Anchor(0,whole,true,"TEST centre",2,3),
            anchor(1,other)),gates(),G);
        assertFalse(r.displayable());
    }
    @Test public void seriesRailNeverExceedsFiveAndEveryOccurrenceRemainsReachable() {
        for(int count:new int[]{4,31}) {
            Navigation nav=new Navigation(series(count),reader());
            for(int i=0;i<count;i++) {
                nav.selectOccurrence(i); Rail rail=nav.rail();
                assertTrue(rail.endExclusive-rail.start<=5);
                assertTrue(rail.start<=i && i<rail.endExclusive);
                assertEquals(rail.start>0,rail.hasPrevious);
                assertEquals(rail.endExclusive<count,rail.hasNext);
                assertEquals(i,rail.active);
                assertEquals(i+1,nav.activeAnchor().range.start);
            }
            assertEquals(Math.min(5,count),nav.rail().endExclusive-nav.rail().start);
            rejects(()->nav.selectOccurrence(count)); rejects(()->nav.selectOccurrence(-1));
        }
    }
    @Test public void aAndBKeepIndependentPageZoomSelectionAcrossEnlargementAndExactRestore() {
        Relation r=pair("TEST-NAV",range(2,9,10),range(4,1,20),gates());
        ReaderSnapshot origin=reader(); Navigation nav=new Navigation(r,origin);
        nav.setPane(0,3,2.0,"selected-A"); nav.setPane(1,5,1.5,"selected-B");
        nav.enlarge(1); Snapshot snapshot=nav.snapshot();
        nav.setPane(0,2,3.0,"changed-A"); nav.setPane(1,4,4.0,"changed-B");
        nav.enlarge(0); nav.restore(snapshot);
        assertEquals(3,nav.pane(0).page); assertEquals(2.0,nav.pane(0).zoom,0.0);
        assertEquals("selected-A",nav.pane(0).selection);
        assertEquals(5,nav.pane(1).page); assertEquals(1.5,nav.pane(1).zoom,0.0);
        assertEquals("selected-B",nav.pane(1).selection); assertEquals(1,nav.enlargedPane());
        nav.showComparison(); assertEquals(-1,nav.enlargedPane());
        assertSame(origin,nav.returnToOrigin());
        assertEquals("unit-unchanged",origin.unitId); assertEquals("passage-unchanged",origin.passageId);
        assertEquals("mask-state",origin.mask); assertEquals("selection-state",origin.selection);
        assertEquals(1.75,origin.zoom,0.0); assertEquals(2,origin.surah); assertEquals(10,origin.ayah);
        assertEquals(3,origin.page);
        assertEquals("selected-A",snapshot.panes.get(0).selection);
        rejects(()->nav.setPane(0,4,1.0,"outside")); rejects(()->nav.setPane(2,2,1,"bad"));
        rejects(()->nav.setPane(0,2,Double.NaN,"bad"));
    }
    @Test public void seriesSnapshotRestoresOccurrenceAndKeepsSingleDominantPane() {
        Navigation nav=new Navigation(series(31),reader()); nav.selectOccurrence(20);
        nav.setPane(0,56,2.5,"local-selection"); Snapshot s=nav.snapshot();
        nav.selectOccurrence(1); nav.restore(s);
        assertEquals(20,nav.rail().active); assertEquals(56,nav.pane(0).page);
        assertEquals(2.5,nav.pane(0).zoom,0.0); assertEquals("local-selection",nav.pane(0).selection);
        rejects(()->nav.pane(1)); rejects(()->nav.enlarge(1));
        Navigation other=new Navigation(series(31),reader()); rejects(()->other.restore(s));
    }
    @Test public void revisitingSeriesOccurrenceRecoversItsOwnZoomAndSelection() {
        Navigation nav=new Navigation(series(31),reader());
        nav.selectOccurrence(20); nav.setPane(0,56,2.5,"twenty");
        nav.selectOccurrence(1); nav.setPane(0,54,1.5,"one");
        nav.selectOccurrence(20);
        assertEquals(56,nav.pane(0).page); assertEquals(2.5,nav.pane(0).zoom,0.0);
        assertEquals("twenty",nav.pane(0).selection);
        nav.selectOccurrence(1); assertEquals("one",nav.pane(0).selection);
        assertEquals(1.5,nav.pane(0).zoom,0.0);
    }
    @Test public void progressionKeepsDocumentedDirectionAndInternalModeHasOnlyOnePane() {
        VerseRange later=range(12,100,100),earlier=range(12,4,4);
        Relation directed=new Relation("TEST-DIRECTION",Type.PROGRESSION,Mode.TWO_PAGES,
            Arrays.asList(later,earlier),Arrays.asList(anchor(0,later),anchor(1,earlier)),gates(),G);
        assertSame(later,directed.originals.get(0)); assertSame(earlier,directed.originals.get(1));
        VerseRange whole=range(2,1,1);
        Relation internal=new Relation("TEST-ONE-PANE",Type.INVERSION,Mode.INTERNAL,
            Arrays.asList(whole),Arrays.asList(new Anchor(0,whole,true,"TEST centre",2,3)),gates(),G);
        Navigation nav=new Navigation(internal,reader()); assertEquals(1,nav.snapshot().panes.size());
        rejects(()->nav.pane(1));
    }
    @Test public void invalidViewChangeLeavesExistingPositionUntouched() {
        Navigation nav=new Navigation(pair("TEST-ATOMIC",range(2,9,10),range(4,1,20),gates()),reader());
        nav.setPane(0,3,2.0,"keep");
        rejects(()->nav.setPane(0,2,Double.POSITIVE_INFINITY,"bad"));
        rejects(()->nav.setPane(0,2,0.0,"bad"));
        rejects(()->nav.setPane(0,2,1.0,null));
        assertEquals(3,nav.pane(0).page); assertEquals(2.0,nav.pane(0).zoom,0.0);
        assertEquals("keep",nav.pane(0).selection);
    }
    @Test public void relationAndSnapshotCollectionsCannotBeMutatedByCallers() {
        VerseRange a=range(1,1,1), b=range(2,1,1);
        List<VerseRange> originals=new ArrayList<>(Arrays.asList(a,b));
        List<Anchor> anchors=new ArrayList<>(Arrays.asList(anchor(0,a),anchor(1,b)));
        Relation r=new Relation("TEST-COPY",Type.PROGRESSION,Mode.TWO_PAGES,originals,anchors,gates(),G);
        originals.clear(); anchors.clear(); assertEquals(2,r.originals.size()); assertEquals(2,r.anchors.size());
        try { r.originals.clear(); fail(); } catch(UnsupportedOperationException expected) { }
        try { r.anchors.clear(); fail(); } catch(UnsupportedOperationException expected) { }
        Snapshot s=new Navigation(r,reader()).snapshot();
        try { s.panes.clear(); fail(); } catch(UnsupportedOperationException expected) { }
    }

    @Test public void seriesNeighborOnNextPhysicalPageIsReachableWithoutChangingOriginal() {
        Navigation nav=new Navigation(series(31),reader()); nav.selectOccurrence(19);
        VerseRange original=nav.relation.originals.get(19);
        assertEquals(20,original.start); assertEquals(20,original.end);
        nav.setPane(0,56,1.0,"neighbor");
        assertEquals(56,nav.pane(0).page);
        assertEquals(20,original.end);
        rejects(()->nav.setPane(0,57,1.0,"outside context"));
    }
    @Test public void refrainShowsPreviousAndNextVerseAcrossRealPageBoundariesWithoutRewritingAnchor() {
        Navigation nav=new Navigation(series(31),reader());
        nav.selectOccurrence(20); // ayah 21; previous ayah 20 belongs to preceding QCF folio.
        VerseRange source=nav.relation.originals.get(20);
        assertEquals(21,source.start);
        assertEquals(21,source.end);
        assertEquals(20,nav.previousNeighbor());
        assertEquals(22,nav.normalNeighbor());
        VerseRange context=nav.viewport(0);
        assertEquals(20,context.start);
        assertEquals(22,context.end);
        assertEquals(55,context.firstPage);
        assertEquals(56,context.lastPage);
        nav.setPane(0,55,1.0,"previous neighbor QCF page");
        nav.setPane(0,56,1.0,"refrain and following verse");
        assertEquals(21,source.start);
        assertEquals(21,source.end);
        assertEquals(21,nav.activeAnchor().range.start);
    }
    @Test public void refrainAtFirstAyahUsesOnlyExistingNeighbors() {
        Navigation nav=new Navigation(series(4),reader());
        assertEquals(0,nav.previousNeighbor());
        assertEquals(2,nav.normalNeighbor());
        assertEquals(1,nav.viewport(0).start);
        assertEquals(2,nav.viewport(0).end);
    }
    @Test public void enlargedPaneAndOriginRetainPanCoordinatesExactly() {
        ReaderSnapshot origin=new ReaderSnapshot(2,10,3,"u","p","mask",2.0,"s",85,120);
        Navigation nav=new Navigation(pair("TEST-PAN",range(2,9,10),range(4,1,20),gates()),origin);
        nav.setPane(0,3,2.0,"A",21,87);nav.setPane(1,5,1.5,"B",3,44);
        Snapshot saved=nav.snapshot();nav.setPane(0,2,1.0,"changed",0,0);nav.restore(saved);
        assertEquals(21,nav.pane(0).scrollX);assertEquals(87,nav.pane(0).scrollY);
        assertEquals(3,nav.pane(1).scrollX);assertEquals(44,nav.pane(1).scrollY);
        assertEquals(85,nav.returnToOrigin().scrollX);assertEquals(120,nav.returnToOrigin().scrollY);
        rejects(()->nav.setPane(0,3,2.0,"bad",-1,0));
    }
}

package com.quransafeguard.hifz.preview;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Entirely synthetic: these records certify no production content or QCF polygons. */
public class CorrespondenceCompatibilityTest {
 private final Geometry g=new Geometry(){
  public int ayahCount(int s){return 300;}
  public int firstPage(int s,int a){return s;}
  public int lastPage(int s,int a){return s;}
  public boolean hasSegmentGeometry(int s,int a,int f,int l){return true;}
 };
 private Presentation presentation(){return new Presentation("SYNTHETIC title","SYNTHETIC fact",Arrays.asList(
  new SourceReference("SYNTHETIC author","SYNTHETIC work","SYNTHETIC edition","SYNTHETIC locator")));}
 private VerseRange range(int s,int a,int b){return new VerseRange(s,a,b,g);}
 private Relation internal(Type type,boolean word,boolean gap){
  VerseRange original=range(2,1,5),a=range(2,2,2);
  List<Anchor> anchors=new ArrayList<>();
  anchors.add(new Anchor(0,a,true,"SYNTHETIC locator",word?1:0,word?2:0));
  if(gap)anchors.add(new Anchor(0,range(2,5,5),true,"SYNTHETIC locator",0,0));
  return new Relation("SYNTHETIC",type,Mode.INTERNAL,Arrays.asList(original),anchors,
   new Gates(true,true,true,true,true),g,presentation());
 }
 private CorrespondenceAccess.Result access(Relation r){
  VerseRange worked=range(2,1,5);
  CorrespondenceAccess.Origin origin=new CorrespondenceAccess.Origin(CorrespondenceAccess.Surface.LECTURE,
   "opaque",new ReaderSnapshot(2,2,2,"unit","passage","mask",1,"selection",10,20),worked);
  CorrespondenceAccess.Result result=new CorrespondenceAccess(new Index(Arrays.asList(r)),g).at(origin,2,2);
  assertSame(origin,result.origin);assertSame(worked,result.origin.workedRange);
  assertEquals(10,result.origin.snapshot.scrollX);assertEquals(20,result.origin.snapshot.scrollY);
  return result;
 }
 private void noButton(Relation r){
  CorrespondenceAccess.Result result=access(r);
  assertFalse(result.hasLinks());assertFalse(result.hasOthers());assertTrue(result.visible.isEmpty());
 }
 @Test public void centreWithDeclaredWordGeometryButNoExactRectanglesHasNoGhostButton(){
  Relation r=internal(Type.CENTRE,true,false);assertFalse(r.displayable());noButton(r);
 }
 @Test public void wholeAyahCentreHasNoGhostButton(){noButton(internal(Type.CENTRE,false,false));}
 @Test public void disjointAnchorsExposeOnlyAttestedVersesAfterExactQcfSupport(){
  Relation r=internal(Type.INVERSION,false,true);assertTrue(r.displayable());
  assertTrue(CorrespondenceDocument.displayable(r,g));
  assertEquals(Arrays.asList(r),access(r).visible);
  assertTrue(new Index(Arrays.asList(r)).at(2,3).isEmpty());
  assertTrue(new Index(Arrays.asList(r)).at(2,4).isEmpty());
  assertEquals(2,CorrespondenceDocument.focus(r,0,0,g).focalRanges.size());
  assertEquals(1,r.originals.get(0).start);
  assertEquals(5,r.originals.get(0).end);
 }
 @Test public void alternanceRequiresRoleAwareRendererNotTwoIndistinguishableGrayBlocks(){
  VerseRange first=range(109,2,3),second=range(109,4,5);
  Relation r=new Relation("SYNTHETIC FOUR ROLES",Type.ALTERNANCE,Mode.TWO_PAGES,
    Arrays.asList(first,second),Arrays.asList(
     new Anchor(0,range(109,2,2),true,"role1",0,0),
     new Anchor(0,range(109,3,3),true,"role2",0,0),
     new Anchor(1,range(109,4,4),true,"role3",0,0),
     new Anchor(1,range(109,5,5),true,"role4",0,0)),
    new Gates(true,true,true,true,true),g,presentation());
  assertTrue("Generic scientific flags are independent of rendering",r.displayable());
  assertFalse("Must not conflate four roles in two gray panels",CorrespondenceDocument.displayable(r,g));
  CorrespondenceAccess.Origin origin=new CorrespondenceAccess.Origin(CorrespondenceAccess.Surface.LECTURE,
    "opaque",new ReaderSnapshot(109,2,109,"u","p","mask",1,"s"),range(109,2,5));
  assertTrue(new CorrespondenceAccess(new Index(Arrays.asList(r)),g).at(origin,109,2).visible.isEmpty());
 }
 @Test public void ordinaryWordSegmentHasNoGhostButton(){
  Relation r=internal(Type.RESSEMBLANCE,true,false);assertTrue(r.displayable());noButton(r);
 }
 @Test public void absentPresentationDataHasNoGhostButton(){
  Relation r=internal(Type.RESSEMBLANCE,false,false);
  noButton(new Relation(r.id,r.type,r.mode,r.originals,r.anchors,r.gates,g));
 }
 @Test public void ordinaryVerseWithCompleteDocumentationRemainsAvailable(){
  Relation r=internal(Type.RESSEMBLANCE,false,false);
  assertTrue(CorrespondenceDocument.displayable(r,g));assertEquals(Arrays.asList(r),access(r).visible);
  assertEquals(1,r.originals.get(0).start);assertEquals(5,r.originals.get(0).end);
 }
 @Test public void everyFalseGateStaysClosedEvenWithPresentation(){
  Relation r=internal(Type.RESSEMBLANCE,false,false);
  for(int missing=0;missing<5;missing++){
   boolean[] flags={true,true,true,true,true};flags[missing]=false;
   Relation denied=new Relation(r.id,r.type,r.mode,r.originals,r.anchors,
    new Gates(flags[0],flags[1],flags[2],flags[3],flags[4]),g,presentation());
   assertFalse(CorrespondenceDocument.displayable(denied,g));noButton(denied);
  }
 }
 private Relation series(int surah,int[] verses){
  List<VerseRange> ranges=new ArrayList<>();List<Anchor> anchors=new ArrayList<>();
  for(int i=0;i<verses.length;i++){
   VerseRange range=range(surah,verses[i],verses[i]);ranges.add(range);
   anchors.add(new Anchor(i,range,true,"SYNTHETIC locator",0,0));
  }
  return new Relation("SYNTHETIC SERIES",Type.REPETITION,Mode.SERIES,ranges,anchors,
   new Gates(true,true,true,true,true),g,presentation());
 }
 @Test public void qamarFourReferencesAndRahmanThirtyOneUseFiveSlotWindow(){
  int[] qamar={17,22,32,40};
  int[] rahman={13,16,18,21,23,25,28,30,32,34,36,38,40,42,45,47,49,51,53,55,57,59,61,63,65,67,69,71,73,75,77};
  for(Relation r:Arrays.asList(series(54,qamar),series(55,rahman))){
   assertTrue(CorrespondenceDocument.displayable(r,g));
   Navigation nav=new Navigation(r,new ReaderSnapshot(r.originals.get(0).surah,r.originals.get(0).start,r.originals.get(0).firstPage,"u","p","mask",1,"selection"));
   for(int i=0;i<r.anchors.size();i++){
    nav.selectOccurrence(i);Rail rail=nav.rail();assertTrue(rail.endExclusive-rail.start<=5);
    assertTrue(rail.start<=i && rail.endExclusive>i);
    assertTrue(CorrespondenceDocument.focus(r,0,i,g).supported);
    assertEquals(r.originals.get(i).start,nav.activeAnchor().range.start);
   }
  }
 }
 @Test public void compatibilityValidatesEveryOccurrenceAndBothPanes(){
  Relation base=series(54,new int[]{17,22,32,40});
  List<Anchor> anchors=new ArrayList<>(base.anchors);Anchor last=anchors.get(3);
  anchors.set(3,new Anchor(3,last.range,true,"SYNTHETIC locator",1,2));
  Relation bad=new Relation(base.id,base.type,base.mode,base.originals,anchors,base.gates,g,presentation());
  assertFalse(CorrespondenceDocument.displayable(bad,g));
  VerseRange a=range(2,2,2),b=range(3,3,3);
  Relation pair=new Relation("SYNTHETIC PAIR",Type.RESSEMBLANCE,Mode.TWO_PAGES,Arrays.asList(a,b),Arrays.asList(
   new Anchor(0,a,true,"SYNTHETIC locator",0,0),new Anchor(1,b,true,"SYNTHETIC locator",1,2)),base.gates,g,presentation());
  noButton(pair);
 }
 @Test public void incompleteSourceMetadataIsRejected(){
  try{new SourceReference("author","work","", "locator");fail();}catch(IllegalArgumentException expected){}
  try{new Presentation("title","fact",Collections.emptyList());fail();}catch(IllegalArgumentException expected){}
 }

 @Test public void missingOrChangedGeometryClosesDisplayEligibility(){
  Relation r=internal(Type.RESSEMBLANCE,false,false);
  Geometry incompatible=new Geometry(){
   public int ayahCount(int s){return 300;}
   public int firstPage(int s,int a){return 0;}
   public int lastPage(int s,int a){return 0;}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return true;}
  };
  assertFalse(CorrespondenceDocument.displayable(r,incompatible));
  assertFalse(CorrespondenceDocument.displayable(r,null));
  assertFalse(CorrespondenceDocument.displayable(null,g));
 }
 @Test public void presentationRetainsExactSuppliedProvenanceAndFreezesItsList(){
  List<SourceReference> supplied=new ArrayList<>();
  supplied.add(new SourceReference("SYNTHETIC author","SYNTHETIC work","SYNTHETIC edition","SYNTHETIC locator"));
  Presentation p=new Presentation("SYNTHETIC title","SYNTHETIC fact",supplied);supplied.clear();
  assertEquals("SYNTHETIC title",p.shortTitle);assertEquals("SYNTHETIC fact",p.factualLink);
  assertEquals("SYNTHETIC edition",p.sources.get(0).edition);assertEquals("SYNTHETIC locator",p.sources.get(0).locator);
  try{p.sources.clear();fail();}catch(UnsupportedOperationException expected){}
 }
}

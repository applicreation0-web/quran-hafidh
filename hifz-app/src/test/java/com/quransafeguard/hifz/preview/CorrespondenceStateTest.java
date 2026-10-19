package com.quransafeguard.hifz.preview;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;
/** Synthetic fixtures, never production approval or Android lifecycle evidence. */
public final class CorrespondenceStateTest {
 private static final Geometry G=new Geometry(){
  public int ayahCount(int s){return 100;}
  public int firstPage(int s,int a){return s+(a-1)/10;}
  public int lastPage(int s,int a){return firstPage(s,a);}
  public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}
 };
 private static ReaderSnapshot origin(){return new ReaderSnapshot(2,3,2,"u","p","mask",1.5,"sel",7,19);}
 private static Relation relation(boolean series,String id){
  List<VerseRange> rs=new ArrayList<>();List<Anchor> as=new ArrayList<>();
  for(int i=0;i<(series?12:2);i++){VerseRange r=new VerseRange(i+2,1,20,G);rs.add(r);as.add(new Anchor(i,r,true,"synthetic locator",0,0));}
  return new Relation(id,Type.REPETITION,series?Mode.SERIES:Mode.TWO_PAGES,rs,as,new Gates(true,true,true,true,true),G);
 }
 private static void rejects(Runnable r){try{r.run();fail("accepted incompatible checkpoint");}catch(IllegalArgumentException expected){}}
 private static Relation centreAt(double dx){
  Geometry g=new Geometry(){
   public int ayahCount(int s){return 100;}public int firstPage(int s,int a){return 2;}public int lastPage(int s,int a){return 2;}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return true;}
   public List<SegmentBox> segmentBoxes(int s,int a,int f,int l){return Arrays.asList(new SegmentBox(s,a,1,2,10+dx,20,20+dx,30),new SegmentBox(s,a,2,2,30+dx,20,40+dx,30));}
  };
  VerseRange r=new VerseRange(2,3,3,g);
  return new Relation("centre",Type.CENTRE,Mode.INTERNAL,Arrays.asList(r),Arrays.asList(new Anchor(0,r,true,"synthetic",1,2)),new Gates(true,true,true,true,true),g);
 }
 @Test public void changedSourceWordCoordinatesInvalidateDurableCentreCheckpoint(){
  CorrespondenceState state=CorrespondenceState.capture(new Navigation(centreAt(0),origin()),0,null);
  rejects(()->state.restore(centreAt(1),origin()));
 }
 @Test public void seriesSurvivesCodecAndFreshRelationNavigation(){
  Navigation n=new Navigation(relation(true,"fixture"),origin());
  n.selectOccurrence(2);n.setPane(0,5,2.5,"two",30,40);
  n.selectOccurrence(10);n.setPane(0,13,3.5,"ten",50,60);
  CorrespondenceState s=CorrespondenceState.decode(CorrespondenceState.capture(n,7,null).encode());
  Navigation restored=s.restore(relation(true,"fixture"),origin());
  assertEquals(10,restored.rail().active);assertEquals(7,s.windowStart);
  assertEquals(13,restored.pane(0).page);assertEquals(50,restored.pane(0).scrollX);
  restored.selectOccurrence(2);assertEquals(2.5,restored.pane(0).zoom,0);assertEquals("two",restored.pane(0).selection);
  assertEquals(40,restored.pane(0).scrollY);assertEquals(19,restored.returnToOrigin().scrollY);
 }
 @Test public void independentEnlargedAndComparisonPositionsSurviveCodec(){
  Navigation n=new Navigation(relation(false,"pair"),origin());
  n.setPane(0,3,1.5,"A",1,2);n.setPane(1,4,2,"B",3,4);
  List<Pane> comparison=n.snapshot().panes;
  n.enlarge(1);n.setPane(1,3,3,"enlarged B",99,88);
  CorrespondenceState s=CorrespondenceState.decode(CorrespondenceState.capture(n,0,comparison).encode());
  Navigation restored=s.restore(relation(false,"pair"),origin());
  assertEquals(1,restored.enlargedPane());assertEquals(99,restored.pane(1).scrollX);
  assertEquals(1,restored.pane(0).scrollX);assertEquals(4,s.comparison.get(1).scrollY);
  assertEquals("B",s.comparison.get(1).selection);
 }
 @Test public void corruptVersionTruncationTrailingDataAndChangedContextFailClosed(){
  CorrespondenceState s=CorrespondenceState.capture(new Navigation(relation(false,"pair"),origin()),0,null);
  byte[] bytes=s.encode();byte[] bad=bytes.clone();bad[0]^=1;
  rejects(()->CorrespondenceState.decode(bad));
  rejects(()->CorrespondenceState.decode(Arrays.copyOf(bytes,bytes.length-1)));
  rejects(()->CorrespondenceState.decode(Arrays.copyOf(bytes,bytes.length+1)));
  rejects(()->s.restore(relation(false,"changed"),origin()));
  rejects(()->s.restore(relation(false,"pair"),new ReaderSnapshot(2,3,2,"u","p","different mask",1.5,"sel",7,19)));
 }

 @Test public void malformedNavigationFieldsAreRejectedBeforeRestoreEscapes(){
  CorrespondenceState s=CorrespondenceState.capture(new Navigation(relation(false,"pair"),origin()),0,null);
  for(int offset:new int[]{12,8,4}){
   byte[] invalid=s.encode();java.nio.ByteBuffer.wrap(invalid).putInt(invalid.length-offset,999);
   CorrespondenceState decoded=CorrespondenceState.decode(invalid);
   rejects(()->decoded.restore(relation(false,"pair"),origin()));
  }
 }
 @Test public void withdrawnApprovalAndChangedBoundsCannotReuseOldCheckpoint(){
  Relation relation=relation(false,"pair");
  CorrespondenceState s=CorrespondenceState.capture(new Navigation(relation,origin()),0,null);
  Relation withdrawn=new Relation(relation.id,relation.type,relation.mode,relation.originals,relation.anchors,
   new Gates(true,true,true,true,false),G);
  rejects(()->s.restore(withdrawn,origin()));
  List<VerseRange> ranges=Arrays.asList(new VerseRange(2,1,19,G),new VerseRange(3,1,20,G));
  List<Anchor> anchors=Arrays.asList(new Anchor(0,ranges.get(0),true,"synthetic locator",0,0),new Anchor(1,ranges.get(1),true,"synthetic locator",0,0));
  Relation changed=new Relation("pair",Type.REPETITION,Mode.TWO_PAGES,ranges,anchors,new Gates(true,true,true,true,true),G);
  rejects(()->s.restore(changed,origin()));
 }
 @Test public void geometryBindingChecksInteriorVersesEvenWhenEndpointsMatch(){
  Relation relation=relation(false,"pair");
  Geometry changed=new Geometry(){
   public int ayahCount(int s){return G.ayahCount(s);}
   public int firstPage(int s,int a){return G.firstPage(s,a);}
   public int lastPage(int s,int a){return a==10?G.lastPage(s,a)+1:G.lastPage(s,a);}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}
  };
  assertTrue(CorrespondenceState.sameGeometry(relation,G));
  assertFalse(CorrespondenceState.sameGeometry(relation,changed));
 }
 @Test public void checkpointCollectionsAreImmutableAndCannotMutateLiveNavigation(){
  Navigation n=new Navigation(relation(true,"fixture"),origin());
  CorrespondenceState s=CorrespondenceState.capture(n,0,null);n.selectOccurrence(5);
  assertEquals(0,s.selectedOccurrence);
  try{s.occurrences.clear();fail();}catch(UnsupportedOperationException expected){}
  rejects(()->CorrespondenceState.capture(n,-1,null));
 }
}

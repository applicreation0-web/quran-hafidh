package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static com.quransafeguard.hifz.preview.CorrespondenceModel.*;

/** Mnemonic labels never create a relation, amend QCF ranges or infer missing occurrences. */
public final class CorrespondenceRefrainCueTest {
 private final Geometry geometry=new Geometry(){
  public int ayahCount(int s){return s==55?78:55;}
  public int firstPage(int s,int a){return s==55?530+a/20:525+a/20;}
  public int lastPage(int s,int a){return firstPage(s,a);}
  public boolean hasSegmentGeometry(int s,int a,int first,int last){return false;}
 };
 private Relation make(int surah,int[] verses){
  List<VerseRange> originals=new ArrayList<>();
  List<Anchor> anchors=new ArrayList<>();
  for(int i=0;i<verses.length;i++){
   VerseRange r=new VerseRange(surah,verses[i],verses[i],geometry);
   originals.add(r);
   anchors.add(new Anchor(i,r,true,"fixture original Arabic",0,0));
  }
  return new Relation("SYNTHETIC-MNEMONIC",Type.REPETITION,Mode.SERIES,originals,anchors,
   new Gates(true,true,true,true,true),geometry);
 }
 @Test public void thirtyOneRahmanOccurrencesHaveDistinctVettedAdjacentWordCues(){
  assertEquals(31,CorrespondenceRefrainCue.RAHMAN.length);
  Relation r=make(55,CorrespondenceRefrainCue.RAHMAN);
  for(int i=0;i<31;i++){
   String cue=CorrespondenceRefrainCue.forOccurrence(r,i);
   assertTrue("Missing "+i,!cue.isEmpty());
   assertTrue(cue.contains(" → "));
   assertTrue(cue.startsWith("Repère · "));
  }
  assertTrue(CorrespondenceRefrainCue.forOccurrence(r,0).contains("ريحان → إنسان"));
  assertTrue(CorrespondenceRefrainCue.forOccurrence(r,3).contains("برزخ → لؤلؤ"));
  assertTrue(CorrespondenceRefrainCue.forOccurrence(r,23).contains("جنتان → مدهامتان"));
  assertEquals("",CorrespondenceRefrainCue.forOccurrence(r,31));
 }
 @Test public void fourQamarContextsRecallNarrativeTransitions(){
  Relation r=make(54,new int[]{17,22,32,40});
  assertTrue(CorrespondenceRefrainCue.forOccurrence(r,0).contains("Nūḥ → ʿĀd"));
  assertTrue(CorrespondenceRefrainCue.forOccurrence(r,3).contains("Lūṭ → Pharaon"));
 }
 @Test public void alteredOrUnverifiedSeriesNeverReceivesMisleadingCues(){
  int[] modified=CorrespondenceRefrainCue.RAHMAN.clone();modified[17]++;
  assertEquals("",CorrespondenceRefrainCue.forOccurrence(make(55,modified),17));
  assertEquals("",CorrespondenceRefrainCue.forOccurrence(make(55,new int[]{13,16,18,21}),0));
  assertEquals("",CorrespondenceRefrainCue.forOccurrence(make(54,new int[]{17,22,32,41}),0));
 }
 @Test public void originalVerseBoundsAreNotChangedByRecallCue(){
  Relation r=make(55,CorrespondenceRefrainCue.RAHMAN);
  CorrespondenceRefrainCue.forOccurrence(r,17);
  assertEquals(51,r.originals.get(17).start);
  assertEquals(51,r.originals.get(17).end);
  assertEquals(51,r.anchors.get(17).range.start);
 }
}

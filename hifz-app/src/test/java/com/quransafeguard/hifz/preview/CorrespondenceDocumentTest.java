package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class CorrespondenceDocumentTest {
 private final CorrespondenceModel.Geometry geometry=new CorrespondenceModel.Geometry(){
  public int ayahCount(int s){return 10;}
  public int firstPage(int s,int a){return a;}
  public int lastPage(int s,int a){return a;}
  public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}
 };
 private CorrespondenceModel.Relation series(){
  java.util.List<CorrespondenceModel.VerseRange> ranges=new java.util.ArrayList<>();
  java.util.List<CorrespondenceModel.Anchor> anchors=new java.util.ArrayList<>();
  for(int i=0;i<4;i++){
   CorrespondenceModel.VerseRange range=new CorrespondenceModel.VerseRange(1,i+1,i+1,geometry);
   ranges.add(range);anchors.add(new CorrespondenceModel.Anchor(i,range,true,"test source",0,0));
  }
  return new CorrespondenceModel.Relation("fixture",CorrespondenceModel.Type.REPETITION,
   CorrespondenceModel.Mode.SERIES,ranges,anchors,new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
 }
 @Test public void seriesContextUsesFollowingAuthenticPageWithoutExtendingCanonicalRange(){
  CorrespondenceModel.Relation relation=series();
  CorrespondenceDocument.Focus focus=CorrespondenceDocument.focus(relation,0,0,geometry);
  assertEquals(1,focus.firstPage);assertEquals(2,focus.lastPage);assertEquals(2,focus.normalNeighbor);
  assertEquals(1,relation.originals.get(0).lastPage);
 }
 @Test public void lastVerseUsesPreviousNeighbor(){
  java.util.List<CorrespondenceModel.VerseRange> ranges=new java.util.ArrayList<>(series().originals);
  java.util.List<CorrespondenceModel.Anchor> anchors=new java.util.ArrayList<>(series().anchors);
  CorrespondenceModel.VerseRange range=new CorrespondenceModel.VerseRange(1,10,10,geometry);
  ranges.set(3,range);anchors.set(3,new CorrespondenceModel.Anchor(3,range,true,"fixture",0,0));
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("end",CorrespondenceModel.Type.REPETITION,
   CorrespondenceModel.Mode.SERIES,ranges,anchors,new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
  CorrespondenceDocument.Focus focus=CorrespondenceDocument.focus(relation,0,3,geometry);
  assertEquals(9,focus.normalNeighbor);assertEquals(9,focus.firstPage);assertEquals(10,focus.lastPage);
 }
 @Test public void preservesUnfocusedSvgAndFitsWholePageOnTransparentPaper(){
  String svg="<svg viewBox=\"0 0 345 550\"><path d=\"M0 0L1 1\"/></svg>";
  String html=CorrespondenceDocument.html(svg);
  assertTrue(html.contains(svg));assertTrue(html.contains("#faf8f2"));
  assertTrue(html.contains("width:100%;height:100%"));assertFalse(html.contains("<script"));
  assertTrue(html.contains("default-src 'none'"));assertTrue(html.contains("background:transparent"));
 }
 @Test public void noFocusPolygonDoesNotDimAuthenticPage(){
  String svg="<svg viewBox=\"0 0 345 550\"><path d=\"M0 0L1 1\"/></svg>";
  CorrespondenceDocument.Render render=CorrespondenceDocument.render(svg,CorrespondenceDocument.focus(series(),0,0,geometry),1);
  assertFalse(render.focusVerified);assertEquals(svg,render.svg);
 }
 @Test public void neighborPagePreservesNormalVerseAndFadesOthers(){
  String svg="<svg viewBox=\"0 0 345 550\"><path class=\"ayahPolygon\" surah=\"1\" ayah=\"2\" d=\"M0 0L1 1\"/></svg>";
  CorrespondenceDocument.Render render=CorrespondenceDocument.render(svg,CorrespondenceDocument.focus(series(),0,0,geometry),2);
  assertTrue(render.focusVerified);assertTrue(render.svg.contains("corr-focus-mask"));
  assertFalse(render.svg.contains("fill-opacity=\"0.23\""));
 }
 @Test public void centreWithoutWordRendererStaysIntactEvenWhenRequested(){
  CorrespondenceModel.VerseRange range=new CorrespondenceModel.VerseRange(1,1,1,geometry);
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("centre",CorrespondenceModel.Type.CENTRE,
   CorrespondenceModel.Mode.INTERNAL,Arrays.asList(range),Arrays.asList(new CorrespondenceModel.Anchor(0,range,true,"fixture",0,0)),
   new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
  String svg="<svg viewBox=\"0 0 345 550\"><path class=\"ayahPolygon\" surah=\"1\" ayah=\"1\" d=\"M0 0L1 1\"/></svg>";
  CorrespondenceDocument.Render render=CorrespondenceDocument.render(svg,CorrespondenceDocument.focus(relation,0,0,geometry),1);
  assertEquals(svg,render.svg);assertFalse(render.focusVerified);
 }
 @Test public void separateAnchorsPaintOnlyExactSourceVersesAndLeaveGapDimmed(){
  CorrespondenceModel.VerseRange original=new CorrespondenceModel.VerseRange(1,1,5,geometry);
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("gap",CorrespondenceModel.Type.INVERSION,
   CorrespondenceModel.Mode.INTERNAL,Arrays.asList(original),Arrays.asList(
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,1,1,geometry),true,"fixture",0,0),
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,5,5,geometry),true,"fixture",0,0)),
   new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
  CorrespondenceDocument.Focus f=CorrespondenceDocument.focus(relation,0,0,geometry);
  assertTrue(f.supported);assertEquals(2,f.focalRanges.size());
  assertEquals(1,f.focalRanges.get(0).start);assertEquals(5,f.focalRanges.get(1).start);
  String svg="<svg viewBox=\"0 0 345 550\">"
   +"<path class=\"ayahPolygon\" surah=\"1\" ayah=\"1\" d=\"M1 1L5 5Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"1\" ayah=\"3\" d=\"M6 6L10 10Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"1\" ayah=\"5\" d=\"M11 11L15 15Z\"/></svg>";
  CorrespondenceDocument.Render output=CorrespondenceDocument.render(svg,f,1);
  assertTrue(output.focusVerified);
  assertTrue(output.svg.contains("d=\"M1 1L5 5Z\" fill=\"#999\""));
  assertTrue(output.svg.contains("d=\"M11 11L15 15Z\" fill=\"#999\""));
  assertFalse(output.svg.contains("d=\"M6 6L10 10Z\" fill=\"#999\""));
  assertFalse(output.svg.contains("d=\"M6 6L10 10Z\" fill=\"black\""));
  assertEquals(1,relation.originals.get(0).start);assertEquals(5,relation.originals.get(0).end);
 }
 @Test public void segmentWordsCannotBeFakedAsDiscreteWholeVerseHighlights(){
  CorrespondenceModel.VerseRange whole=new CorrespondenceModel.VerseRange(1,1,5,geometry);
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("word-unsupported",
   CorrespondenceModel.Type.INVERSION,CorrespondenceModel.Mode.INTERNAL,Arrays.asList(whole),
   Arrays.asList(
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,1,1,geometry),true,"exact-words",1,2),
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,5,5,geometry),true,"context",0,0)),
   new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
  assertFalse(CorrespondenceDocument.focus(relation,0,0,geometry).supported);
 }

 @Test public void exactSyntheticWordBoxesEnableOnlyApprovedSegmentHatching(){
  CorrespondenceModel.Geometry exact=new CorrespondenceModel.Geometry(){
   public int ayahCount(int s){return 10;}public int firstPage(int s,int a){return 1;}
   public int lastPage(int s,int a){return 1;}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return true;}
   public java.util.List<CorrespondenceModel.SegmentBox> segmentBoxes(int s,int a,int f,int l){
    return Arrays.asList(new CorrespondenceModel.SegmentBox(s,a,2,1,20,30,40,50),
     new CorrespondenceModel.SegmentBox(s,a,3,1,80,90,100,110));
   }
  };
  CorrespondenceModel.VerseRange r=new CorrespondenceModel.VerseRange(1,1,1,exact);
  CorrespondenceModel.Presentation presentation=new CorrespondenceModel.Presentation("Synthetic centre","Synthetic link",Arrays.asList(
   new CorrespondenceModel.SourceReference("test author","test work","test edition","test locator")));
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("synthetic-centre",CorrespondenceModel.Type.CENTRE,
   CorrespondenceModel.Mode.INTERNAL,Arrays.asList(r),Arrays.asList(new CorrespondenceModel.Anchor(0,r,true,"synthetic locator",2,3)),
   new CorrespondenceModel.Gates(true,true,true,true,true),exact,presentation);
  assertTrue(relation.displayable());assertTrue(CorrespondenceDocument.displayable(relation,exact));
  String glyph="<g id=\"original-glyphs\"><path d=\"M1 2L3 4\"/></g>";
  String svg="<svg viewBox=\"0 0 345 550\">"+glyph+"</svg>";
  CorrespondenceDocument.Focus focus=CorrespondenceDocument.focus(relation,0,0,exact);
  CorrespondenceDocument.Render render=CorrespondenceDocument.render(svg,focus,1);
  assertTrue(render.focusVerified);assertTrue(render.svg.contains(glyph));
  assertTrue(render.svg.startsWith(svg.substring(0,svg.length()-6)));
  assertTrue(render.svg.contains("corr-centre-hatch"));assertTrue(render.svg.matches("(?s).*x=\"20(?:\\.0)?\" y=\"30(?:\\.0)?\" width=\"20(?:\\.0)?\" height=\"20(?:\\.0)?\".*"));
  assertFalse(render.svg.matches("(?s).*x=\"20(?:\\.0)?\" y=\"30(?:\\.0)?\" width=\"80(?:\\.0)?\".*"));
  assertEquals(svg,CorrespondenceDocument.render(svg,focus,2).svg);
  CorrespondenceModel.Relation qaClosed=new CorrespondenceModel.Relation("synthetic-centre",relation.type,relation.mode,
   relation.originals,relation.anchors,new CorrespondenceModel.Gates(true,true,true,true,false),exact,presentation);
  assertFalse(qaClosed.displayable());assertFalse(CorrespondenceDocument.displayable(qaClosed,exact));
  assertEquals(svg,CorrespondenceDocument.render(svg,CorrespondenceDocument.focus(qaClosed,0,0,exact),1).svg);
 }

 @Test public void refrainKeepsBothRealNeighborAyatNormalAndFadesUnrelatedAyat(){
  CorrespondenceModel.Geometry qcf=new CorrespondenceModel.Geometry(){
   public int ayahCount(int surah){return 78;}
   public int firstPage(int surah,int ayah){return ayah<=16?530:ayah<=17?531:532;}
   public int lastPage(int surah,int ayah){return firstPage(surah,ayah);}
   public boolean hasSegmentGeometry(int s,int a,int f,int l){return false;}
  };
  int[] verses={13,16,18,21};
  java.util.List<CorrespondenceModel.VerseRange> originals=new java.util.ArrayList<>();
  java.util.List<CorrespondenceModel.Anchor> anchors=new java.util.ArrayList<>();
  for(int i=0;i<verses.length;i++){
   CorrespondenceModel.VerseRange x=new CorrespondenceModel.VerseRange(55,verses[i],verses[i],qcf);
   originals.add(x);anchors.add(new CorrespondenceModel.Anchor(i,x,true,"fixture source",0,0));
  }
  CorrespondenceModel.Relation r=new CorrespondenceModel.Relation("SYNTHETIC-REFRAIN",
   CorrespondenceModel.Type.REPETITION,CorrespondenceModel.Mode.SERIES,originals,anchors,
   new CorrespondenceModel.Gates(true,true,true,true,true),qcf);
  CorrespondenceDocument.Focus focus=CorrespondenceDocument.focus(r,0,1,qcf);
  assertEquals(15,focus.previousNeighbor);
  assertEquals(17,focus.normalNeighbor);
  assertEquals(530,focus.firstPage);assertEquals(531,focus.lastPage);
  assertEquals(16,r.originals.get(1).start);assertEquals(16,r.originals.get(1).end);
  String pageBefore="<svg viewBox=\"0 0 345 550\">"
   +"<path class=\"ayahPolygon\" surah=\"55\" ayah=\"15\" d=\"M1 1L5 5Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"55\" ayah=\"16\" d=\"M6 6L10 10Z\"/></svg>";
  String pageAfter="<svg viewBox=\"0 0 345 550\">"
   +"<path class=\"ayahPolygon\" surah=\"55\" ayah=\"17\" d=\"M11 11L15 15Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"55\" ayah=\"18\" d=\"M16 16L20 20Z\"/></svg>";
  CorrespondenceDocument.Render before=CorrespondenceDocument.render(pageBefore,focus,530);
  CorrespondenceDocument.Render after=CorrespondenceDocument.render(pageAfter,focus,531);
  assertTrue(before.focusVerified);assertTrue(after.focusVerified);
  assertTrue(before.svg.contains("<path d=\"M1 1L5 5Z\" fill=\"black\""));
  assertTrue(before.svg.contains("<path d=\"M6 6L10 10Z\" fill=\"#999\""));
  assertTrue(after.svg.contains("<path d=\"M11 11L15 15Z\" fill=\"black\""));
  assertFalse(after.svg.contains("<path d=\"M16 16L20 20Z\" fill=\"black\""));
 }
}

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
 @Test public void separateAnchorsDoNotInventFocusAcrossTheGap(){
  CorrespondenceModel.VerseRange original=new CorrespondenceModel.VerseRange(1,1,5,geometry);
  CorrespondenceModel.Relation relation=new CorrespondenceModel.Relation("gap",CorrespondenceModel.Type.INVERSION,
   CorrespondenceModel.Mode.INTERNAL,Arrays.asList(original),Arrays.asList(
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,1,1,geometry),true,"fixture",0,0),
    new CorrespondenceModel.Anchor(0,new CorrespondenceModel.VerseRange(1,5,5,geometry),true,"fixture",0,0)),
   new CorrespondenceModel.Gates(true,true,true,true,true),geometry);
  assertFalse(CorrespondenceDocument.focus(relation,0,0,geometry).supported);
 }
}

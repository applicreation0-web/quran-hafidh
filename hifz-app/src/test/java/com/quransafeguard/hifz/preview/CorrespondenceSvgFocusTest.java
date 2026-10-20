package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
public class CorrespondenceSvgFocusTest {
 private static final String INK="<g transform=\"matrix(1 0 0 1 0 0)\"><path id=\"glyph\" d=\"M1 2L3 4\"/></g>";
 private static String polygon(int ayah,String d){return "<path class=\"ayahPolygon\" fill-opacity=\"0\" surah=\"54\" ayah=\""+ayah+"\" d=\""+d+"\"/>";}
 private static final String A="M5 7L9 8Z", B="M9 8L12 13Z";
 private static final String PAGE="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 345 550\">"+INK+polygon(17,A)+polygon(18,B)+"</svg>";
 @Test public void fadesOutsideOriginalVerseGeometryWithoutChangingGlyphs(){
  String out=CorrespondenceSvgFocus.apply(PAGE,54,17,17,0);
  assertTrue(out.contains(INK)); assertTrue(out.contains("<mask"));
  assertTrue(out.contains("d=\""+A+"\" fill=\"black\""));
  assertTrue(out.contains("d=\""+A+"\" fill=\"#999\""));
  assertFalse(out.contains("d=\""+B+"\" fill=\"black\""));
 }
 @Test public void refrainNeighborIsNormalAndNeverGray(){
  String out=CorrespondenceSvgFocus.apply(PAGE,54,17,17,18);
  assertTrue(out.contains("d=\""+B+"\" fill=\"black\""));
  assertFalse(out.contains("d=\""+B+"\" fill=\"#999\""));
  assertFalse(out.contains("pattern"));
 }
 @Test public void neighborOnNextPhysicalPageRemainsReadable(){
  String page="<svg viewBox=\"0 0 345 550\">"+INK+polygon(18,B)+"</svg>";
  String out=CorrespondenceSvgFocus.apply(page,54,17,17,18);
  assertTrue(out.contains("<mask"));assertTrue(out.contains("d=\""+B+"\" fill=\"black\""));
  assertFalse(out.contains("fill=\"#999\""));
 }
 @Test public void missingGeometryLeavesAuthenticPageIntact(){assertEquals(PAGE,CorrespondenceSvgFocus.apply(PAGE,54,22,22,0));}
 @Test public void foreignSurahDoesNotHighlightMatchingAyahNumber(){assertEquals(PAGE,CorrespondenceSvgFocus.apply(PAGE,55,17,17,0));}
 @Test public void allPhysicalComponentsOfSplitVerseAreReused(){
  String page=PAGE.replace("</svg>",polygon(17,"M40 50L60 70Z")+"</svg>");
  String out=CorrespondenceSvgFocus.apply(page,54,17,17,0);
  assertTrue(out.contains("d=\"M40 50L60 70Z\" fill=\"black\""));
 }
 @Test public void noGlobalFadeOrCenterMarkOnUntouchedSource(){assertEquals(PAGE,CorrespondenceSvgFocus.untouched(PAGE));}
 @Test(expected=IllegalArgumentException.class) public void invalidBoundsRejected(){CorrespondenceSvgFocus.apply(PAGE,54,17,16,0);}

 @Test public void wordHatchingRejectsInvalidRectanglesAndRepeatedOverlay(){
  String svg="<svg viewBox=\"0 0 345 550\"><g id=\"glyphs\"/></svg>";
  for(double[] box:new double[][]{{Double.NaN,1,2,3},{1,1,346,3},{1,3,2,2},{1,2,3}}){
   try{CorrespondenceSvgFocus.applyWords(svg,java.util.Collections.singletonList(box));fail();}
   catch(IllegalArgumentException expected){}
  }
  String rendered=CorrespondenceSvgFocus.applyWords(svg,java.util.Collections.singletonList(new double[]{1,2,3,4}));
  try{CorrespondenceSvgFocus.applyWords(rendered,java.util.Collections.singletonList(new double[]{1,2,3,4}));fail();}
  catch(IllegalArgumentException expected){}
 }
 @Test public void openingFoliosKeepTheirNegativeOriginAndOriginalGlyphBytes(){
  String svg="<svg viewBox=\"-53.3109 -198.4777 345 550\"><g id=\"glyphs\"/><path class=\"ayahPolygon\" surah=\"1\" ayah=\"6\" d=\"M1 2L3 4Z\"/></svg>";
  String out=CorrespondenceSvgFocus.apply(svg,1,6,6,0);
  assertTrue(out.startsWith(svg.substring(0,svg.indexOf("</svg>"))));
  assertTrue(out.contains("x=\"-53.3109\" y=\"-198.4777\""));
  String words=CorrespondenceSvgFocus.applyWords(svg,java.util.Collections.singletonList(new double[]{100,200,110,220}));
  assertTrue(words.contains("x=\"46.6891\""));
  assertTrue(words.contains("viewBox=\"-53.3109 -198.4777 345 550\""));
 }
 @Test public void disjointFocusPreservesEveryGlyphAndDoesNotPretendGapIsFocal(){
  CorrespondenceModel.Geometry g=new CorrespondenceModel.Geometry(){
   public int ayahCount(int s){return 10;}
   public int firstPage(int s,int a){return 1;}
   public int lastPage(int s,int a){return 1;}
   public boolean hasSegmentGeometry(int s,int a,int first,int last){return false;}
  };
  String glyph="<g id=\"glyphs\"><path id=\"original\" d=\"M4 4L5 5\"/></g>";
  String svg="<svg viewBox=\"0 0 345 550\">"+glyph
   +"<path class=\"ayahPolygon\" surah=\"54\" ayah=\"1\" d=\"M1 1L2 2Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"54\" ayah=\"3\" d=\"M3 3L4 4Z\"/>"
   +"<path class=\"ayahPolygon\" surah=\"54\" ayah=\"5\" d=\"M5 5L6 6Z\"/></svg>";
  java.util.List<CorrespondenceModel.VerseRange> ranges=java.util.Arrays.asList(
   new CorrespondenceModel.VerseRange(54,1,1,g),new CorrespondenceModel.VerseRange(54,5,5,g));
  String highlighted=CorrespondenceSvgFocus.applyDiscrete(svg,54,ranges);
  assertTrue(highlighted.contains(glyph));
  assertTrue(highlighted.contains("d=\"M1 1L2 2Z\" fill=\"#999\""));
  assertTrue(highlighted.contains("d=\"M5 5L6 6Z\" fill=\"#999\""));
  assertFalse(highlighted.contains("d=\"M3 3L4 4Z\" fill=\"#999\""));
  String missing="<svg viewBox=\"0 0 345 550\">"+glyph+"</svg>";
  assertEquals(missing,CorrespondenceSvgFocus.applyDiscrete(missing,54,ranges));
  try{CorrespondenceSvgFocus.applyDiscrete(highlighted,54,ranges);fail();}
  catch(IllegalArgumentException expected){}
 }
 @Test public void emptyWordGeometryNeverFadesTheAuthenticPage(){
  String svg="<svg viewBox=\"0 0 345 550\"><g id=\"glyphs\"/></svg>";
  assertEquals(svg,CorrespondenceSvgFocus.applyWords(svg,java.util.Collections.emptyList()));
 }
}

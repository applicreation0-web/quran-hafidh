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
}

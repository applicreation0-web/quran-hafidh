package com.quransafeguard.hifz.preview;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import static org.junit.Assert.*;

/** Free Révision active in Lecture must remain strictly visual and unscheduled. */
public final class ReaderFreeActiveMaskSourceContractTest {
 private static String file(String relative)throws Exception {
  Path p=Paths.get(relative);if(!Files.exists(p))p=Paths.get("..",relative);
  return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
 }
 private static final String ROOT="hifz-app/src/main/java/com/quransafeguard/hifz/preview/";
 @Test public void readingModeHasSingleToggleAndSameExactAnchorsAsActiveRevision()throws Exception {
  String reading=file(ROOT+"StudyReaderActivity.java");
  String active=file(ROOT+"HifzSessionActivity.java");
  assertTrue(reading.contains("freeActiveMaskButton=Ui.iconButton"));
  assertTrue(reading.contains("R.drawable.ic_ui_revision"));
  assertTrue(reading.contains("WordGeometryRepository.shared(this).pageLandmarkBoxes(page)"));
  assertTrue(reading.contains("ayahThemesCues.forPage(page)"));
  assertTrue(reading.contains("landmarks.length()==6"));
  assertTrue(reading.contains("cue.getInt(\"anchorWordCount\")!=1"));
  assertTrue(reading.contains("cue.getJSONArray(\"boxes\").length()!=1"));
  assertTrue(reading.contains("mushaf.setSemanticCues(anchors,true,false)"));
  assertTrue(reading.contains("mushaf.setMask(100)"));
  assertTrue(reading.contains("mushaf.setMask(0)"));
  assertTrue(reading.contains("mushaf.setMaskFollowsSelection(false)"));
  assertTrue("Free mask must boot with the real physical QCF lines, never an empty selection",
      reading.contains("pageLines=GeometryRepository.get(this).lineIdsOnPage(page)"));
  assertTrue("Boot must be masked on the very first SVG render, not after onPageShown",
      reading.contains("mushaf.show(page,java.util.Collections.emptyList(),pageLines,")
      && reading.contains("freeActiveMaskPageReady ? 100 : 0"));
  assertFalse("No unmasked first-frame page may be loaded during active mask",
      reading.contains("mushaf.show(page,java.util.Collections.emptyList(),pageLines,0)"));
  int maskLoad=reading.indexOf("private void showFreeActiveMaskPage()");
  int refresh=reading.indexOf("private void applyFreeActiveMask()",maskLoad);
  String boot=reading.substring(maskLoad,refresh);
  assertTrue("Validate physical Quran lines/anchors before showing page",
      boot.indexOf("pageLandmarkBoxes(page)") < boot.indexOf("mushaf.show(page,"));
  assertTrue("First load must already have the hole geometry",
      boot.indexOf("mushaf.setSemanticCues(anchors,true,false)") < boot.indexOf("mushaf.show(page,"));
  int toggle=reading.indexOf("private void toggleFreeActiveMask()");
  int endToggle=reading.indexOf("private void updateFreeActiveMaskButton()",toggle);
  assertTrue("Switching ON must not first call setMask(0)",
      reading.substring(toggle,endToggle).contains("if(!freeActiveMask) {\n            mushaf.setMask(0);"));
  assertTrue("After masked boot, no second multi-pass 100% render is required",
      reading.contains("if(freeActiveMaskPageReady) { updateFreeActiveMaskButton(); return; }"));
  assertTrue(reading.contains("if(freeActiveMask)showFreeActiveMaskPage();"));
  String readerJs=file("hifz-app/src/main/assets/hifzreader/reader.js");
  assertTrue("The JS eraser is blocked unless lineIds are provided",
      readerJs.contains("if(clamped&&pageGeo&&lineIds.length&&lines.length)"));

  assertTrue(active.contains("mushaf.setSemanticCues(newStarts, true, false)"));
  assertTrue(active.contains("firstLast.length() == 6"));
 }
 @Test public void freeMaskIsNotASecondScheduledRevision()throws Exception {
  String reading=file(ROOT+"StudyReaderActivity.java");
  assertFalse(reading.contains("new HifzSessionActivity("));
  assertFalse(reading.contains("setActiveMurajaahActualEnd("));
  assertFalse(reading.contains("setActiveMurajaahPage("));
  assertFalse(reading.contains("completeMurajaahValidation("));
  assertTrue(reading.contains("out.putBoolean(\"readerFreeActiveMask\",freeActiveMask)"));
  assertTrue(reading.contains("if(freeActiveMask)return false"));
  assertTrue(reading.contains("if(freeActiveMask){applyFreeActiveMask();return;}"));
  assertTrue(reading.contains("if(!mapUnitPreview&&!freeActiveMask)mushaf.setSelection"));
 }
}

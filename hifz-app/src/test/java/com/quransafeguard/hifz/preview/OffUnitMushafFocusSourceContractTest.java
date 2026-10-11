package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Regression: if selected physical lines are on p.515, p.516 must remain faint. */
public final class OffUnitMushafFocusSourceContractTest {
  private static String reader() throws Exception {
    Path p=Paths.get("hifz-app/src/main/assets/hifzreader/reader.js");
    if(!Files.exists(p))p=Paths.get("..","hifz-app/src/main/assets/hifzreader/reader.js");
    return new String(Files.readAllBytes(p),StandardCharsets.UTF_8);
  }
  @Test public void offUnitPagesCarryTheSamePaperVeilAsDuePages() throws Exception {
    String js=reader();
    assertTrue(js.contains("function fadedOffUnitPage(svg)"));
    assertTrue(js.contains("}else if(contextFocus&&lineIds.length&&pageGeo&&(pageGeo.lines||[]).length){"));
    assertTrue(js.contains("const context=fadedOffUnitPage(svg);"));
    assertTrue(js.contains("paper.setAttribute('fill-opacity',eink?'0.72':'0.65');"));
  }
  @Test public void focusDoesNotTouchMaskAndCredits() throws Exception {
    String js=reader();
    String scope=js.substring(js.indexOf("function fadedOffUnitPage(svg)"),js.indexOf("const AMORCE_HIGHLIGHT_HEIGHT"));
    assertFalse(scope.contains("setMask("));
    assertFalse(scope.contains("credit"));
    assertTrue(scope.contains("pointer-events','none'"));
  }
}

package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** Stop-aware cutting, step 2: the reader can start/end a block inside a line. */
public final class LineCutsReaderSourceContractTest {
    private static String read(String path) throws Exception {
        Path direct = Paths.get(path);
        Path file = Files.exists(direct) ? direct : Paths.get("..", path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @Test public void focusMaskAndRosettesAllHonourTheLineCuts() throws Exception {
        String js = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("start keeps the words read after the cut (left of x)",
            js.contains("lineCuts.start.lineId)===id&&Number.isFinite(Number(lineCuts.start.x)))hi=Number(lineCuts.start.x);"));
        assertTrue("end keeps the words read before the cut (right of x)",
            js.contains("lineCuts.end.lineId)===id&&Number.isFinite(Number(lineCuts.end.x)))lo=Number(lineCuts.end.x);"));
        assertTrue("mask cells are clipped to the span",
            js.contains("const x0=Math.max(Number(cell[0]),span.lo),x1=Math.min(Number(cell[1]),span.hi);"));
        assertTrue("the focus window is clipped to the span",
            js.contains("const span=lineSpan(line.id);x0=Math.max(x0,span.lo);x1=Math.min(x1,span.hi);"));
        assertTrue("a rosette outside the span is not drawn as part of the block",
            js.contains("const span=lineSpan(line.id);return c.x>=span.lo&&c.x<=span.hi;"));
        assertTrue(js.contains("setLineCuts(cuts){lineCuts=cuts&&typeof cuts==='object'?cuts:null;render()},"));
    }

    @Test public void mushafViewForwardsTheCuts() throws Exception {
        String view = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue(view.contains(".put(\"lineCuts\", lineCuts == null ? JSONObject.NULL : lineCuts)"));
        assertTrue(view.contains("public void setLineCuts(String startLineId, double startX, String endLineId, double endX) {"));
    }
}

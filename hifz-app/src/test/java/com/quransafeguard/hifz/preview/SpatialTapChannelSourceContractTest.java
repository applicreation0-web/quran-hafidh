package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * The spatial quiz's TEXT_TO_POSITION question needs to resolve a tap to the exact physical line
 * under it, not the tapped verse (a verse can span several lines). This is a net-new, isolated
 * channel added to MushafView/reader.js: MushafView.Listener.onSpatialLineTap, gated behind
 * setSpatialTapEnabled so every other mode's tap behavior (onVerseTap) stays byte-for-byte
 * unchanged when it's off (the default).
 */
public final class SpatialTapChannelSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void mushafViewExposesTheChannelAndPersistsItAcrossPageReloads() throws Exception {
        String source = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue("the Listener must gain a default no-op method, so every existing implementor "
                + "keeps compiling unchanged",
            source.contains("default void onSpatialLineTap(String lineId) {}"));
        assertTrue("must be included in the boot JSON so it survives MushafView reloading the "
                + "whole WebView on every page turn, exactly like maskFollowsSelection",
            source.contains(".put(\"spatialTapEnabled\", spatialTapEnabled)"));
        assertTrue("the setter must push the flag into the live page via HifzReader",
            source.contains("public void setSpatialTapEnabled(boolean enabled) {"));
        assertTrue(source.contains(
            "\"window.HifzReader&&window.HifzReader.setSpatialTapEnabled(\" + enabled + \");\""));
        assertTrue("the JS bridge method must route straight to the listener, never touching V6 "
                + "state itself",
            source.contains("@JavascriptInterface public void spatialLineTap(String lineId) {\n"
                + "            post(() -> { if (listener != null) listener.onSpatialLineTap(lineId); });\n        }"));
    }

    @Test public void readerJsResolvesTheTapToALineOnlyWhenEnabledAndFallsBackUnchangedOtherwise() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("boot flag, defaulting to disabled",
            reader.contains("let spatialTapEnabled=!!boot.spatialTapEnabled;"));
        assertTrue("must reuse the same SVG-space coordinate transform pattern already relied on "
                + "elsewhere in this file (insideSelection/markerLayer), not invent a new one",
            reader.contains("const ctm=svg.getScreenCTM();if(!ctm)return null;")
                && reader.contains("pt.matrixTransform(ctm.inverse());"));
        assertTrue("must look up the real physical line by its top/bottom band from pageGeo, the "
                + "same source of truth the renderer itself uses — never an invented grid",
            reader.contains("const line=(pageGeo.lines||[]).find(l=>svgPoint.y>=Number(l.top)&&svgPoint.y<=Number(l.bottom));"));
        assertTrue("when a line is found in spatial mode, it must return early and never also fire "
                + "the normal verseTap for that same tap",
            reader.contains("if(spatialTapEnabled){\n        const lineId=spatialLineIdAt(e);\n        if(lineId){N?.spatialLineTap(lineId);return}\n      }"));
        assertTrue("when spatial mode is off (every other screen), the tap handler must fall "
                + "through to the exact same verseTap call as before this channel existed",
            reader.contains("const [s,a]=k.split(':').map(Number);N?.verseTap(s,a);"));
        assertTrue("exposed so MushafView.setSpatialTapEnabled can actually reach it",
            reader.contains("setSpatialTapEnabled(value){spatialTapEnabled=!!value;},"));
    }
}

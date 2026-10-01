package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported: Révision's masking looks noticeably cleaner than Sabqi/Itqan's, because Révision never
 * clips its mask rectangles to the selected verses' exact polygon outline (maskFollowsSelection is
 * false there), while Sabqi/Itqan always did — every mask rectangle got cut down from its raw
 * geometric line-cell box to the precise glyph shape of the selected verse's polygon.
 *
 * That clip is not purely cosmetic for Sabqi/Itqan, though: a physical line can list more than one
 * verse (confirmed against the real Mushaf geometry — 3882 of 8820 physical lines do, e.g. Al-
 * Fatiha's third line holds both 1:3 and 1:4), and a cell's rectangle can slightly overhang a
 * neighbor verse sharing that same line. Removing the clip everywhere would risk a mask rectangle
 * visibly bleeding onto a verse that isn't part of today's memorization block.
 *
 * The fix only relaxes the clip where it's provably safe: cells on a line that lists exactly one
 * verse render as plain rectangles (nothing else's ink is on that line to bleed onto); cells on a
 * multi-verse line keep the exact clip they always had. A standalone Node run against every page in
 * the real geometry (8820 lines, 3882 multi-verse) confirmed the open/clipped split never
 * misclassifies a segment by its line's actual verse count.
 */
public final class SabqiItqanMaskCleanRectangleOnSingleVerseLinesSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void segmentsCarryTheirLineIdSoTheSplitCanBeDoneByLineVerseCount() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("each rendered segment must carry which physical line it came from",
            reader.contains("segments.push({key:String(cell.key),lineId:String(cell.lineId),"));
    }

    @Test public void renderOnlyClipsSegmentsFromLinesThatActuallyShareInkWithAnotherVerse() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String render = method(reader, "function render(){", "/* Move only when the selected passage");

        assertTrue("must classify lines by their own verse count, not by the selection as a whole",
            render.contains("const multiVerseLines=new Set(lines.filter(l=>(l.verses||[]).length>1).map(l=>String(l.id)));"));
        assertTrue("must partition segments by whether their line is in that multi-verse set",
            render.contains("segments.forEach(segment=>(multiVerseLines.has(segment.lineId)?clipped:open).push(segment));"));
        assertTrue("single-verse-line segments must render as plain, unclipped rectangles",
            render.contains("open.forEach(segment=>openGroup.appendChild(maskRect(segment)));"));
        assertTrue("multi-verse-line segments must keep the exact polygon clip, unchanged",
            render.contains("clippedGroup.setAttribute('clip-path','url(#hifz-selection-clip)');")
                && render.contains("clipped.forEach(segment=>clippedGroup.appendChild(maskRect(segment)));"));
        assertTrue("this split must only run when there's an actual selection to clip against "
                + "(Révision's maskFollowsSelection=false path is untouched, already fully unclipped)",
            render.contains("if(polys.length){") && render.contains("} else {\n        const group=document.createElementNS(NS,'g');"));
    }
}

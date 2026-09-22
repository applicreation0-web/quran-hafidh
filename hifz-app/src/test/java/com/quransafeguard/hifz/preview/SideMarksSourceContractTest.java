package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Right/left-page memory cue (odd page = right-hand page, even = left-hand page): 3 short bars
 * drawn ONLY in the natural blank gutter beside #mushaf (a side-effect of its fixed 345:550
 * aspect ratio on wider screens), never inside the Mushaf element itself. Real per-page margins
 * inside the Mushaf image were measured too thin on most of the 604 pages (as little as ~4px) to
 * safely guarantee zero contact with the actual Quran text there, and the reading zone's own
 * sizing formula must never be touched — so this feature only ever reads #mushaf's rendered
 * rect, never writes to its size, and hides itself entirely when no gutter is wide enough.
 */
public final class SideMarksSourceContractTest {
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

    @Test public void mushafReadingZoneSizingIsUntouched() throws Exception {
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue("the Mushaf's own sizing formula must stay byte-for-byte identical — this "
                + "feature may only read its rendered rect, never resize it",
            index.contains("#mushaf{width:min(100vw,calc((100vh - 4px) * 345 / 550));flex:none;background:var(--sheet);"
                + "transform:translateY(var(--reveal-shift));transform-origin:center center}"));
        assertTrue("the bars must be hidden by default, appearing only once JS confirms a safe gutter",
            index.contains("#sidemarks{position:absolute;display:none;flex-direction:row;align-items:stretch;"
                + "justify-content:center;gap:3px;pointer-events:none}"));
        assertTrue("e-ink must get a slightly thicker stroke, like every other mark in this reader "
                + "(.ayahPolygon.audio, .weakoutline) — thin/low-contrast marks risk vanishing under "
                + "a fast 1-bit e-ink refresh",
            index.contains("body.eink #sidemarks span{width:1.7px}"));
        assertTrue("must never intercept touches — it's a passive memory cue, not a control",
            index.contains("aria-hidden=\"true\""));
    }

    @Test public void neverWritesToMushafSizeOnlyReadsItsRect() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String fn = method(reader, "function updateSideMarks(){", "\nwindow.addEventListener('resize'");
        assertTrue("must measure the Mushaf's real rendered box, never assume a size",
            fn.contains("const rect=mushafEl.getBoundingClientRect();"));
        assertTrue("must never assign to the Mushaf element's own style — only #sidemarks may move",
            !fn.contains("mushafEl.style."));
        assertTrue("odd page = right-hand page, the same parity as any bound book",
            fn.contains("const onOuterRight=currentPage%2===1;"));
        assertTrue("must hide rather than risk clipping into the Quran text when the gutter is too "
                + "narrow — real per-page margins inside the image are too thin to rely on instead "
                + "(measured as low as ~4px on some of the 604 pages)",
            fn.contains("if(!(gutter>=minGutter)){marks.classList.remove('show');return}"));
        assertTrue("the safety threshold must be derived from the actual bar geometry, never a "
                + "disconnected magic number",
            fn.contains("const minGutter=marksWidth+2*safety;"));
    }

    @Test public void recomputesWhenTheLayoutCanActuallyChange() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("a screen rotation can change which gutter (if any) exists",
            reader.contains("window.addEventListener('resize',()=>requestAnimationFrame(updateSideMarks));"));
        assertTrue("the very first render must position the bars before anything is visible",
            reader.contains("render();\n  requestAnimationFrame(updateSideMarks);\n  N?.pageShown(currentPage);"));
        assertTrue("a Tafsir reveal shifts #mushaf vertically (translateY) without changing its "
                + "gutter width, but the bars' vertical position is measured from the same rect and "
                + "must be refreshed afterwards or it visibly drifts from the shifted page",
            reader.contains("updateSideMarks();\n  });\n}\nfunction clearReveal()"));
        assertTrue("clearing the reveal must also re-sync the bars",
            reader.contains("function clearReveal(){document.documentElement.style.setProperty('--reveal-shift','0px');updateSideMarks()}"));
        assertTrue("toggling e-ink mode changes the bars' own stroke width, so it must recompute too",
            reader.contains("setEink(value){eink=!!value;render();updateSideMarks()},"));
    }
}

package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Center/spine memory cue: a single "tasbih" thread — a thin vertical line strung with small
 * diamond beads, running the Mushaf's full rendered height — drawn in the gutter on the OPPOSITE
 * side from #sidemarks. Two distinct signs (a tapered 3-bar flourish at the outer edge, a beaded
 * thread at the spine) so a reader can never confuse "this is the book's edge" with "this is its
 * binding" — user-requested after confirming the outer mark alone could be read either way.
 */
public final class CenterMarkSourceContractTest {
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

    @Test public void threadAndBeadsAreStyledDistinctlyFromTheSideFlourish() throws Exception {
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue("hidden by default, revealed only once JS confirms a safe gutter",
            index.contains("#centermark{position:absolute;display:none;width:12px;pointer-events:none}")
                && index.contains("#centermark.show{display:block}"));
        assertTrue("a continuous thread, not a dashed border — visually distinct from the discrete "
                + "tapered bars of #sidemarks",
            index.contains("#centermark .thread{position:absolute;left:50%;top:0;bottom:0;width:1px;"
                + "background:var(--sidemark);opacity:.45;transform:translateX(-50%)}"));
        assertTrue("beads are rotated squares (facets), the same plain, self-authored geometry style "
                + "as the rest of this reader's marks",
            index.contains("#centermark .bead{position:absolute;left:50%;width:5px;height:5px;"
                + "border-radius:1.5px;background:var(--sidemark);transform:translate(-50%,-50%) rotate(45deg)}"));
        assertTrue("e-ink must get a bolder thread and bigger beads, like every other mark in this reader",
            index.contains("body.eink #centermark .thread{width:1.3px;opacity:.55}")
                && index.contains("body.eink #centermark .bead{width:6px;height:6px}"));
        assertTrue("must never intercept touches — a passive cue, not a control",
            index.contains("<div id=\"centermark\" aria-hidden=\"true\"><div class=\"thread\"></div></div>"));
    }

    @Test public void drawsInTheOppositeGutterFromTheSideMarksAndSpansTheFullHeight() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String fn = method(reader, "function updateCenterMark(){", "\nwindow.addEventListener('resize'");
        assertTrue("must measure the Mushaf's real rendered box, never assume a size",
            fn.contains("const rect=mushafEl.getBoundingClientRect();"));
        assertTrue("must never assign to the Mushaf element's own style — only #centermark may move",
            !fn.contains("mushafEl.style."));
        assertTrue("the spine sits on the OPPOSITE side from the outer edge #sidemarks uses — the "
                + "whole point is to disambiguate the two",
            fn.contains("const gutter=onOuterRight?leftGutter:rightGutter;"));
        assertTrue("must run the Mushaf's full rendered height, not a fraction of it — there is no "
                + "natural midpoint to draw the eye to for a spine cue",
            fn.contains("mark.style.top=rect.top+'px';") && fn.contains("mark.style.height=rect.height+'px';"));
        assertTrue("must hide rather than risk clipping into the Quran text when the gutter is too "
                + "narrow for even one bead",
            fn.contains("if(!(gutter>=minGutter)){mark.classList.remove('show');"
                + "while(mark.children.length>1)mark.removeChild(mark.lastChild);return}"));
    }

    @Test public void recomputesEverywhereTheSideMarksDo() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("a screen rotation can change which gutter (if any) exists on either side",
            reader.contains("window.addEventListener('resize',()=>requestAnimationFrame(updateCenterMark));"));
        assertTrue("the very first render must position the thread before anything is visible",
            reader.contains("requestAnimationFrame(updateSideMarks);\n  requestAnimationFrame(updateCenterMark);"
                + "\n  N?.pageShown(currentPage);"));
        assertTrue("a Tafsir reveal shifts #mushaf vertically without changing its gutters, but the "
                + "thread's own position is measured from the same rect and must be refreshed too",
            reader.contains("updateSideMarks();\n    updateCenterMark();\n  });\n}\nfunction clearReveal()"));
        assertTrue("clearing the reveal must also re-sync the thread",
            reader.contains("function clearReveal(){document.documentElement.style.setProperty("
                + "'--reveal-shift','0px');updateSideMarks();updateCenterMark()}"));
        assertTrue("toggling e-ink mode changes the thread's and beads' own size, so it must recompute too",
            reader.contains("setEink(value){eink=!!value;render();updateSideMarks();updateCenterMark()},"));
    }
}

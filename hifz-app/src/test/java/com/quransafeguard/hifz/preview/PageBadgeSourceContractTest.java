package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Universal page-number badge (P1): a folded page corner (dog-ear) in the book's outer-margin
 * gutter, bottom of the page — odd (right-hand) pages get a white flap/black digit outlined on a
 * black backing triangle (so the fold stays visible even in body.eink mode, where the page
 * background becomes the same pure white as the flap), even (left-hand) pages get a plain black
 * flap/white digit, the printer's recto/verso convention. Centralized once in the shared
 * reader.js/index.html (not duplicated per-Activity) so Lecture, Mémorisation libre, Sabqi,
 * Itqān, Consolidation, Renforcement and Révision all get the identical badge for free.
 */
public final class PageBadgeSourceContractTest {
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

    @Test public void badgeExistsWithOddEvenStylingAndNeverInterceptsTouch() throws Exception {
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue("the badge div must be a sibling of #mushaf, not nested inside it",
            index.contains("<div id=\"mushaf\" aria-label=\"Mushaf de Médine\"><!--MUSHAF_SVG--></div>"
                + "<div id=\"pagebadge\" aria-hidden=\"true\"></div>"));
        assertTrue("must never intercept touches — it's a passive memory cue, not a control",
            index.contains("#pagebadge{position:absolute;display:none;align-items:flex-end;"
                + "font-size:11px;font-weight:700;line-height:1;pointer-events:none}"));
        assertTrue("hidden by default, revealed only once JS confirms a safe gutter",
            index.contains("#pagebadge.show{display:flex}"));
        assertTrue("even (left-hand/verso) pages: black folded flap, white digit, a bottom-left "
                + "corner triangle (right angle at bottom-left)",
            index.contains("#pagebadge.even{background:#000;color:#fff;"
                + "clip-path:polygon(0 0,0 100%,100% 100%);justify-content:flex-start;padding:0 0 3px 5px}"));
        assertTrue("odd (right-hand/recto) pages: white folded flap on a black backing triangle, "
                + "black digit, a bottom-right corner triangle (right angle at bottom-right) — the "
                + "mirror of .even",
            index.contains("#pagebadge.odd{background:#000;color:#000;"
                + "clip-path:polygon(100% 0,100% 100%,0 100%);justify-content:flex-end;padding:0 5px 3px 0}"));
        assertTrue("the backing triangle must be inset by a uniform amount on all sides so it reads "
                + "as a border on every edge, including the diagonal fold edge a plain CSS border "
                + "can't reach once clip-path has cut it",
            index.contains("#pagebadge.odd::before{content:'';position:absolute;top:1.4px;left:1.4px;"
                + "right:1.4px;bottom:1.4px;background:#fff;"
                + "clip-path:polygon(100% 0,100% 100%,0 100%);z-index:-1}"));
        assertFalse("no shadow — the spec forbids it for BOOX",
            index.contains("#pagebadge") && index.contains("box-shadow"));
    }

    @Test public void positionsInTheSameOuterGutterAsSideMarksAndHidesWhenTooNarrow() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String fn = method(reader, "function updatePageBadge(){", "\nwindow.addEventListener('resize'");
        assertTrue("must measure the Mushaf's real rendered box, never assume a size",
            fn.contains("const rect=mushafEl.getBoundingClientRect();"));
        assertTrue("must never assign to the Mushaf element's own style — only #pagebadge may move",
            !fn.contains("mushafEl.style."));
        assertTrue("odd page = right-hand page, the same parity #sidemarks and #centermark use",
            fn.contains("const onOuterRight=currentPage%2===1;"));
        assertTrue("must sit at the bottom of the rendered page, not centered like #sidemarks",
            fn.contains("badge.style.top=(rect.bottom-size)+'px';"));
        assertTrue("must hide rather than risk clipping into the Quran text when the gutter is too "
                + "narrow for even the reserved footprint",
            fn.contains("if(!(gutter>=minGutter)){badge.classList.remove('show');return}"));
        assertTrue("must read the live page number every time, since MushafView reloads the whole "
                + "WebView per page turn rather than patching currentPage incrementally",
            fn.contains("badge.textContent=String(currentPage);"));
    }

    @Test public void recomputesEverywhereTheOtherMarksDo() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("a screen rotation can change which gutter (if any) exists",
            reader.contains("window.addEventListener('resize',()=>requestAnimationFrame(updatePageBadge));"));
        assertTrue("the very first render must position the badge before anything is visible",
            reader.contains("requestAnimationFrame(updateCenterMark);\n  requestAnimationFrame(updatePageBadge);\n  N?.pageShown(currentPage);"));
        assertTrue("a Tafsir reveal shifts #mushaf vertically, and the badge sits at its bottom edge, "
                + "so it must be refreshed afterwards or it visibly drifts from the shifted page",
            reader.contains("updateCenterMark();\n    updatePageBadge();\n  });\n}\nfunction clearReveal()"));
        assertTrue("clearing the reveal must also re-sync the badge",
            reader.contains("updateCenterMark();updatePageBadge()}"));
        assertTrue("toggling e-ink mode changes the badge's own diameter, so it must recompute too",
            reader.contains("setEink(value){eink=!!value;render();updateSideMarks();updateCenterMark();updatePageBadge()},"));
    }

    @Test public void studyReaderAndFreeMemNoLongerDrawTheirOwnBadge() throws Exception {
        String studyReader = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String freeMem = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        assertFalse("the badge is centralized in the shared reader now — StudyReaderActivity must "
                + "not keep its own duplicate",
            studyReader.contains("pageNumberBadge"));
        assertFalse("the badge is centralized in the shared reader now — FreeMemActivity must not "
                + "keep its own duplicate",
            freeMem.contains("pageNumberBadge"));
    }
}

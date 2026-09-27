package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Universal page-number badge (P1): a round token in the book's outer-margin gutter, bottom of
 * the page — odd (right-hand) pages get a white disc/black digit with a black outline (so it
 * stays visible even in body.eink mode, where the page background becomes the same pure white as
 * the disc's own fill), even (left-hand) pages get a plain black disc/white digit, the printer's
 * recto/verso convention. Centralized once in the shared reader.js/index.html (not duplicated
 * per-Activity) so Lecture, Mémorisation libre, Sabqi, Itqān, Consolidation, Renforcement and
 * Révision all get the identical badge for free.
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
            index.contains("#pagebadge{position:absolute;display:none;align-items:center;"
                + "justify-content:center;border-radius:50%;font-size:15px;font-weight:700;"
                + "line-height:1;pointer-events:none;box-sizing:border-box}"));
        assertTrue("hidden by default, revealed only once JS confirms a safe gutter",
            index.contains("#pagebadge.show{display:flex}"));
        assertTrue("even (left-hand/verso) pages: plain black disc, white digit",
            index.contains("#pagebadge.even{background:#000;color:#fff}"));
        assertTrue("odd (right-hand/recto) pages: white disc, black digit, outlined so it stays "
                + "visible even in body.eink mode where --paper becomes the same pure white as "
                + "the disc's own fill",
            index.contains("#pagebadge.odd{background:#fff;color:#000;border:1.6px solid #000}"));
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
            fn.contains("badge.style.top=(rect.bottom-d)+'px';"));
        assertTrue("the ideal disc size must stay generous enough that a real 3-digit page number "
                + "(up to 604) stays legible when the gutter has room for it",
            fn.contains("const idealD=eink?50:46,floorD=32,safety=1;"));
        assertTrue("diameter and font must both shrink together toward the real available gutter "
                + "instead of an all-or-nothing fixed size — #mushaf's width breakpoint only "
                + "guarantees 36px per side, below the old fixed 52/56px threshold, which hid the "
                + "badge entirely there (confirmed on real captures)",
            fn.contains("const d=Math.min(idealD,gutter-2*safety);")
                && fn.contains("const font=floorFont+(idealFont-floorFont)*(d-floorD)/(idealD-floorD);"));
        assertTrue("must hide only below the still-legible floor, never risk clipping into the "
                + "Quran text",
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

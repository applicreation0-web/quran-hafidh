package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported from physical-device screenshots: once a verse was tapped to record "Fin réelle" in
 * Révision active, the mask pool collapsed to a narrow band around that one verse instead of
 * staying spread across the whole page — worse than the earlier lineIds-collapse bug (already
 * fixed), because this survived even after currentLineIds was restored to the full page.
 *
 * Root cause: reader.js's maskCandidates(lines,polys) drops any cell outside the `selected`
 * verses' own polygons whenever polys is non-empty (selectedPolygons(svg) — see the shared
 * `insideSelection` check), and render() always fed it that filter. That's correct for Sabqi/Itqan,
 * where the selected verses ARE the memorization block and can share a physical line with
 * un-selected neighbors that must not become maskable. But Murajaah only sets `selected` to flag
 * the last verse actually revised for display — it never means "restrict masking to this verse" —
 * so the two modes need genuinely different behavior, not just a shared default.
 *
 * The fix threads a maskFollowsSelection flag (default true, preserving Sabqi/Itqan) that
 * HifzSessionActivity sets false for both Murajaah submodes right where each render*() method
 * already establishes the page's masking scope.
 */
public final class RevisionMaskIgnoresLastRevisedVerseSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0, from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) return count;
            count++;
            from = at + needle.length();
        }
    }

    @Test public void readerLetsMaskingIgnoreTheHighlightedVerseWhenAsked() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        assertTrue("must default to the legacy Sabqi/Itqan-safe behavior unless explicitly turned off",
            reader.contains("let maskFollowsSelection=boot.maskFollowsSelection!==false;"));
        assertTrue("render() must actually gate selectedPolygons() behind the flag, not call it unconditionally",
            reader.contains("const polys=maskFollowsSelection?selectedPolygons(svg):[],cells=maskCandidates(lines,polys);"));
        assertTrue("must expose a live setter the native side can call once per session",
            reader.contains("setMaskFollowsSelection(value){maskFollowsSelection=!!value;render()}"));
    }

    @Test public void mushafForwardsTheFlagIntoTheBootPayloadAndLiveUpdates() throws Exception {
        String mushaf = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MushafView.java");
        assertTrue("a fresh page load must carry the current flag value",
            mushaf.contains(".put(\"maskFollowsSelection\", maskFollowsSelection)"));
        assertTrue("must expose a setter mirroring setLandmarkLines/setHighlightVerses",
            mushaf.contains("public void setMaskFollowsSelection(boolean value) {"));
        assertTrue("must default true so Sabqi/Itqan (which never call this setter) keep their "
                + "existing selection-clipped masking behavior",
            mushaf.contains("private boolean maskFollowsSelection = true;"));
        assertTrue("live updates (without a full reload) must call the reader's own setter",
            mushaf.contains("window.HifzReader.setMaskFollowsSelection("));
    }

    @Test public void bothMurajaahSubmodesDisableItRightWhereTheyEstablishTheirMaskingScope() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("passive Révision must turn it off before the page is first shown",
            session.contains("program.setText(\"Révision · objectif \" + murajaahObjectiveLabel());\n"
                + "        updateMurajaahProgress();\n"
                + "        mushaf.setMaskFollowsSelection(false);"));
        assertTrue("active Révision must turn it off before the page is first shown",
            session.contains("program.setText(\"Révision active · objectif \" + murajaahObjectiveLabel());\n"
                + "        updateMurajaahProgress();\n"
                + "        mushaf.setMaskFollowsSelection(false);"));
        assertTrue("exactly the two Murajaah render entry points may disable it — Sabqi/Itqan must "
                + "keep the default so their own selection-clipped masking is unaffected",
            countOccurrences(session, "mushaf.setMaskFollowsSelection(false);") == 2);
    }
}

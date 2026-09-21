package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Révision active/passive weak-spot highlight (task #33): a verse flagged during the masked
 * Révision active pass stays outlined — lightly, no fill, to avoid E-Ink ghosting — through the
 * passive Entretien that follows, so the same struggle stays visible until cleared. These are
 * source-contract checks rather than a JUnit exercise of the feature itself because the reader is
 * a WebView/JS component (no Robolectric in this project) and HifzPrefs/MushafView require an
 * Android Context this JVM test suite cannot construct.
 */
public final class RevisionWeakSpotSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void prefsPersistWeakVersesAsAGlobalToggleableSet() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must expose a readable set of flagged verses", prefs.contains("public List<VerseRef> murajaahWeakVerses()"));
        assertTrue("must expose a single add/remove toggle, not separate add/remove calls",
            prefs.contains("public boolean toggleMurajaahWeakVerse(VerseRef verse)"));
    }

    @Test public void sessionArmsMarkingOnlyInActiveAndNeverMovesTheCursorWhileArmed() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("marking must be gated to Révision active, never passive", session.contains("weakMarkMode"));
        assertTrue("armed tap must short-circuit before the normal cursor-tracking tap logic",
            session.contains("if (MURAJAAH_ACTIVE.equals(mode) && weakMarkMode) { toggleWeakVerse(verse); return; }"));
        assertTrue("marking mode must reset on every mode entry so it never leaks across sessions",
            session.contains("weakMarkMode = false;"));
        assertTrue("both renderMurajaah and renderMurajaahActive must push the flagged set to the reader",
            countOccurrences(session, "mushaf.setHighlightVerses(prefs.murajaahWeakVerses());") >= 2);
    }

    @Test public void readerDrawsAThinDashedOutlineNeverAFilledShade() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");
        assertTrue("reader must expose a setter the native side can call after a toggle",
            reader.contains("setHighlights(list){highlighted=new Set((list||[]).map(String));render()}"));
        assertTrue("weak layer must clone the real polygon shape, not draw a synthetic rect",
            reader.contains("function weakLayer(svg,weakSet){") && reader.contains("p.cloneNode(false)"));
        assertTrue("the outline must draw after (on top of) any active mask so a flagged verse stays visible while masked",
            reader.indexOf("svg.appendChild(layer);") < reader.indexOf("weakLayer(svg,highlighted)"));
        assertTrue("outline must never fill, only stroke, to minimize E-Ink ink coverage",
            index.contains(".weakoutline{fill:none;stroke:var(--weak)"));
        assertTrue("outline layer must not intercept taps meant for the underlying verse polygons",
            index.contains(".weaklayer{pointer-events:none}"));
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
}

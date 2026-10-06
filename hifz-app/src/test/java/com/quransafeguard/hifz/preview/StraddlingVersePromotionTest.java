package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Device report: Révision split into "2:1→81 · 83→84 · 86→88" although 2:1–2:88 was acquired.
 * Verses straddling two Renforcement units were fully covered by neither and never promoted.
 */
public final class StraddlingVersePromotionTest {
    private static GeometryRepository geometry;

    @BeforeClass public static void loadCanonicalGeometry() throws Exception {
        String repoPath = "app/src/main/assets/reader109/geometry.json";
        Path direct = Paths.get(repoPath);
        Path file = Files.exists(direct) ? direct : Paths.get("..", repoPath);
        geometry = GeometryRepository.fromJson(new String(Files.readAllBytes(file), StandardCharsets.UTF_8));
    }

    private static String range(String start, String end) {
        return "{\"start\":\"" + start + "\",\"end\":\"" + end + "\"}";
    }

    @Test public void acquiredButUnpromotedStraddlingVersesAreRepaired() {
        LinkedHashSet<String> acquired = new LinkedHashSet<>();
        for (int i = geometry.firstLineIndex(new VerseRef(2, 1)); i <= geometry.lastLineIndex(new VerseRef(2, 88)); i++) {
            acquired.add(geometry.line(i).id);
        }
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("schema", 6);
        store.disk.put("v6LearnedLineIds", "[]");
        store.disk.put("v6StabilizedLineIds", "[]");
        store.disk.put("v6AcquiredCreditLineIds", new JSONArray(acquired).toString());
        store.disk.put("v6QuarantineLineIds", "[]");
        store.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
        store.disk.put("itqanRanges", "[" + range("2:1", "2:74") + "]");
        store.disk.put("promotedRanges", "[" + range("2:75", "2:81") + "," + range("2:83", "2:84") + "," + range("2:86", "2:88") + "]");
        HifzPrefs prefs = new HifzPrefs(store);
        assertFalse(prefs.murajaahCorpus().contains(new VerseRef(2, 82)));

        assertTrue(prefs.repairStraddlingAcquiredVerses(geometry));
        for (int ayah = 1; ayah <= 88; ayah++) {
            assertTrue("2:" + ayah + " must be in Révision", prefs.murajaahCorpus().contains(new VerseRef(2, ayah)));
        }
        assertFalse("never beyond the acquired lines", prefs.murajaahCorpus().contains(new VerseRef(2, 120)));
        assertTrue("idempotent", prefs.repairStraddlingAcquiredVerses(geometry));
    }

    @Test public void creditedVersesIncludeOnesStartingInAnEarlierCreditedBlock() {
        VerseRef straddling = null;
        for (int i = 1; i < geometry.lineCount() && straddling == null; i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            VerseRef first = line.verses.get(0);
            if (geometry.firstLineIndex(first) < i) straddling = first;
        }
        int first = geometry.firstLineIndex(straddling), last = geometry.lastLineIndex(straddling);
        LinkedHashSet<String> credited = new LinkedHashSet<>();
        for (int i = first; i <= last; i++) credited.add(geometry.line(i).id);
        List<VerseRef> fromLaterBlock = geometry.versesFullyCredited(last, last, credited);
        assertTrue(fromLaterBlock.contains(straddling));
        assertFalse(geometry.versesFullyCoveredByLines(last, last).contains(straddling));
    }

    /** No regression: a declared range's trailing edge is never extended by the repair. */
    @Test public void repairNeverExtendsADeclaredRangesTrailingEdge() {
        // Find a verse Y wholly on a line owned by the verse X just before it.
        VerseRef x = null, y = null;
        for (int i = 0; i < geometry.lineCount() && y == null; i++) {
            GeometryRepository.LineMeta line = geometry.line(i);
            for (int k = 1; k < line.verses.size(); k++) {
                VerseRef v = line.verses.get(k);
                if (geometry.firstLineIndex(v) == i && geometry.lastLineIndex(v) == i
                        && line.verses.get(k - 1).getSurah() == v.getSurah()) {
                    x = line.verses.get(k - 1); y = v; break;
                }
            }
        }
        LinkedHashSet<String> acquired = new LinkedHashSet<>();
        VerseRef start = new VerseRef(x.getSurah(), 1);
        for (int i = geometry.firstLineIndex(start); i <= geometry.lastLineIndex(x); i++) acquired.add(geometry.line(i).id);
        InMemoryPrefs store = new InMemoryPrefs();
        store.disk.put("schema", 6);
        store.disk.put("v6LearnedLineIds", "[]");
        store.disk.put("v6StabilizedLineIds", "[]");
        store.disk.put("v6AcquiredCreditLineIds", new JSONArray(acquired).toString());
        store.disk.put("v6QuarantineLineIds", "[]");
        store.disk.put("v6LegacyPartialAcquiredLineIds", "[]");
        store.disk.put("itqanRanges", "[" + range(start.toString(), x.toString()) + "]");
        store.disk.put("promotedRanges", "[]");
        HifzPrefs prefs = new HifzPrefs(store);
        assertTrue(prefs.repairStraddlingAcquiredVerses(geometry));
        assertFalse(y + " sits on " + x + "'s credited line but beyond the declared range",
            prefs.murajaahCorpus().contains(y));
        assertTrue(prefs.murajaahCorpus().contains(x));
    }
}

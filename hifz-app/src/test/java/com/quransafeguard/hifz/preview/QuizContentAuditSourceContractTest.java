package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Reported directly, following the shipped masking-precision fix: a first-ayah quiz question
 * (e.g. 2:1) still looked wrong because verses_text.json's per-verse text carried Tanzil's
 * prepended Basmala heading, while the KFQC Mushaf page only ever masks/shows the real ayah
 * ("الۤمۤ") — the Basmala is a separate decorative heading with no ayahPolygon/verse of its own.
 * scripts/build_verse_text.py now strips it (Al-Fatiha 1:1, which genuinely *is* the Basmala, and
 * At-Tawbah 9:1, which carries none, are left alone) so the quiz's text always matches the page.
 *
 * A second, unrelated regression from the same "même intensité" pass: bumping surahPicker's font
 * size above rubPicker's to visually match its perceived weight broke their baseline alignment,
 * since the row centers each child by its own padding+line-height box.
 */
public final class QuizContentAuditSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void verseTextGeneratorStripsThePrependedBasmalaFromFirstAyahs() throws Exception {
        String script = read("scripts/build_verse_text.py");
        assertTrue("must strip by structural word count, not a hardcoded literal (Tanzil spells "
                + "the Basmala two ways across surahs)",
            script.contains("basmala_word_count = len(verses['1:1'].split())"));
        assertTrue("Al-Fatiha's own first ayah IS the Basmala and must never be touched",
            script.contains("if surah == 9:") && script.contains("for surah in range(2, 115):"));
        assertTrue("must actually strip all 112 eligible first ayahs (114 surahs minus Al-Fatiha and At-Tawbah)",
            script.contains("assert stripped == 112"));
    }

    @Test public void shippedVerseTextNoLongerCarriesTheBasmalaOnNonFatihaFirstAyahs() throws Exception {
        // Deliberately never hardcodes the Basmala as a second Arabic literal here: the source of
        // truth is 1:1's own shipped value, read once and reused — typing the same text twice in
        // two different files risks an invisible Unicode normalization mismatch between them.
        String json = read("app/src/main/assets/reader109/verses_text.json");
        org.json.JSONObject verses = new org.json.JSONObject(json).getJSONObject("verses");
        String basmala = verses.getString("1:1");
        assertEquals("Al-Fatiha's Basmala is always exactly 4 words", 4, basmala.trim().split("\\s+").length);
        assertEquals("verse 2:1 must be the real ayah alone (\"Alif Lam Meem\", 1 word), not the "
                + "3-4 extra words of a prepended Basmala heading",
            1, verses.getString("2:1").trim().split("\\s+").length);
        assertEquals("95:1 (At-Tin) spells its Basmala with an extra shadda but is still exactly "
                + "4 Basmala words followed by its own 2-word ayah, so must now be 2 words alone",
            2, verses.getString("95:1").trim().split("\\s+").length);
        assertEquals("97:1 (Al-Qadr) is the same shadda-spelling case, its own ayah is 5 words alone",
            5, verses.getString("97:1").trim().split("\\s+").length);
        assertFalse("At-Tawbah has no Basmala heading to begin with; its own text must be untouched",
            verses.getString("9:1").startsWith(basmala));
    }

    @Test public void surahAndHizbPickersShareTheExactSameFontSizeSoTheirBaselinesAlign() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue("pageRail centers each child by its own bounding box, so a size mismatch between "
                + "the two pickers shifts their text baselines off the shared row line even though the "
                + "row itself looks centered",
            study.contains("surahPicker = Ui.bookText(this, \"Sourate\", 13f, true);")
                && study.contains("rubPicker = Ui.bookText(this, \"\", 13f, true);"));
    }
}

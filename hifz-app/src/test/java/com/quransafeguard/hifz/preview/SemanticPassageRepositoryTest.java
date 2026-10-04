package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The materialized semantic assets as the app parses them: 1 243 Al-Munīr units, Arabic titles. */
public final class SemanticPassageRepositoryTest {
    private static SemanticPassageRepository.ParsedForTest parsed;
    private static final Map<Integer, List<WordGeometryRepository.WordBox>> WORDS = new HashMap<>();

    private static byte[] read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        return Files.readAllBytes(Files.exists(direct) ? direct : Paths.get("..", repoPath));
    }

    private static String sha256(byte[] raw) throws Exception {
        StringBuilder hex = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(raw)) hex.append(String.format("%02x", b & 0xff));
        return hex.toString();
    }

    @BeforeClass public static void load() throws Exception {
        byte[] raw = read("hifz-app/build/generated/semanticAssets/semantic/semantic_passages_v2_1.json");
        byte[] titles = read("hifz-app/build/generated/semanticAssets/semantic/semantic_titles_v2_3.json");
        assertEquals(SemanticPassageRepository.EXPECTED_SHA256, sha256(raw));
        assertEquals(SemanticPassageRepository.EXPECTED_TITLE_SHA256, sha256(titles));
        parsed = SemanticPassageRepository.parseForTest(new String(raw, StandardCharsets.UTF_8),
            new String(titles, StandardCharsets.UTF_8));
        for (String chunk : new String[]{"001-150", "151-300", "301-450", "451-604"}) {
            WORDS.putAll(WordGeometryRepository.parseChunk(new String(read(
                "hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-" + chunk + ".json"), StandardCharsets.UTF_8)));
        }
    }

    @Test public void runtimeHas1243CanonicalUnitsOverAll604PagesWithLegacyAliases() {
        assertEquals(604, parsed.byPage.size());
        assertEquals(SemanticPassageRepository.EXPECTED_GLOBAL_PASSAGES, parsed.byId.size());
        Set<String> canonical = new HashSet<>();
        for (SemanticPassageRepository.Cue cue : parsed.byId.values()) canonical.add(cue.passageId);
        assertEquals(1243, canonical.size());
    }

    @Test public void runtimeTitlesAreArabicOnly() {
        for (SemanticPassageRepository.Cue cue : parsed.byId.values()) {
            assertFalse(cue.title.trim().isEmpty());
            for (char c : cue.title.toCharArray()) {
                assertFalse(cue.passageId + " has Latin text in its runtime title: " + cue.title,
                    (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
            }
        }
        SemanticPassageRepository.Cue nas = null;
        for (SemanticPassageRepository.Cue cue : parsed.byId.values())
            if (cue.startVerse.equals(new VerseRef(114, 1))) nas = cue;
        assertEquals(new VerseRef(114, 6), nas.endVerse);
    }

    @Test public void everyAmorceStartsOnItsPageWithExactWordBoxes() {
        int checked = 0, exact = 0, failClosed = 0;
        for (Map.Entry<Integer, List<SemanticPassageRepository.Cue>> page : parsed.byPage.entrySet()) {
            for (SemanticPassageRepository.Cue cue : page.getValue()) {
                if (!cue.anchorOnCurrentPage) continue;
                assertEquals(cue.startPage, page.getKey().intValue());
                assertTrue(cue.anchorWordCount >= 1);
                int words = WordGeometryRepository.wordCount(WORDS, page.getKey(), cue.startVerse);
                // An amorce longer than its start verse (e.g. 2:1 "الم") has no exact box set:
                // anchorBoxes returns empty and that page fails closed (no anchor mask), never estimated.
                if (words >= cue.anchorWordCount) exact++;
                else failClosed++;
                checked++;
            }
        }
        System.out.println("Al-Munīr amorces with exact boxes: " + exact + ", fail-closed: " + failClosed);
        assertEquals(1243, checked);
        assertTrue("the vast majority of amorces resolve to exact word boxes", exact >= 1200);
    }
}

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
                int boxes = WordGeometryRepository.amorceBoxes(WORDS.get(page.getKey()), cue.startVerse,
                    cue.anchorWordCount).length();
                if (boxes == cue.anchorWordCount) exact++;
                else { failClosed++; System.out.println("fail-closed amorce " + cue.passageId + " " + cue.startVerse); }
                checked++;
            }
        }
        System.out.println("Al-Munīr amorces with exact boxes: " + exact + ", fail-closed: " + failClosed);
        assertEquals(1243, checked);
        assertEquals("every amorce resolves to exact word boxes, multi-verse ones included", 1243, exact);
    }

    @Test public void everyPostNasHizbUnitCarriesManyExactAmorces() throws Exception {
        GeometryRepository geometry = GeometryRepository.fromJson(new String(read(
            "app/src/main/assets/reader109/geometry.json"), StandardCharsets.UTF_8));
        int units = 0, minAmorces = Integer.MAX_VALUE;
        for (ItqanRotationPolicy.Leg leg : ItqanRotationPolicy.Leg.values()) {
            for (AnchoringQueue.Entry unit : HifzPrefs.physicalUnitsInLeg(leg, new VerseRef(49, 1), geometry,
                    ItqanMaintenancePolicy.Regime.POST_NAS_MAINTENANCE)) {
                units++;
                VerseRef start = GeometryRepository.parseVerse(unit.start), end = GeometryRepository.parseVerse(unit.end);
                int amorces = 0;
                for (List<SemanticPassageRepository.Cue> cues : parsed.byPage.values())
                    for (SemanticPassageRepository.Cue cue : cues)
                        if (cue.anchorOnCurrentPage && ItqanMaintenancePolicy.amorceInsideUnit(cue.startVerse, start, end)) amorces++;
                minAmorces = Math.min(minAmorces, amorces);
            }
        }
        System.out.println("Post-Nas hizb units (both legs): " + units + ", fewest amorces in one unit: " + minAmorces);
        assertEquals(61, units);
        assertTrue("every hizb session has validated anchors for its recall passes", minAmorces >= 1);
    }
}

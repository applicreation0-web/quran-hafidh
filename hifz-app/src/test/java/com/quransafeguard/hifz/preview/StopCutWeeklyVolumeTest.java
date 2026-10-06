package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * User decision (general idea): about one page a week of Apprentissage (3 × ~5 lines) and one page
 * and a half of Stabilisation (3 × ~7 lines), in short surahs as in long ones. Blocks therefore run
 * on through surah boundaries when a surah's remainder is too short, but always close a surah when
 * its end falls inside the block's window.
 */
public final class StopCutWeeklyVolumeTest {
    private static GeometryRepository geometry;
    private static StopCutPolicy policy;
    static final StringBuilder report = new StringBuilder();

    private static String read(String path) throws Exception {
        Path direct = Paths.get(path);
        Path file = Files.exists(direct) ? direct : Paths.get("..", path);
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    @BeforeClass public static void load() throws Exception {
        geometry = GeometryRepository.fromJson(read("app/src/main/assets/reader109/geometry.json"));
        Map<Integer, List<WordGeometryRepository.WordBox>> words = new HashMap<>();
        for (String chunk : new String[]{"001-150", "151-300", "301-450", "451-604"}) {
            words.putAll(WordGeometryRepository.parseChunk(read(
                "hifz-app/src/main/word-source/quran-ws-v1.1.2/word-boxes-" + chunk + ".json")));
        }
        policy = StopCutPolicy.build(geometry, words, read("app/src/main/assets/reader109/waqf.json"));
    }

    private static List<Double> walk(VerseRef from, VerseRef to, double min, double target, double max, int[] crossing) {
        double x = geometry.firstLineIndex(from), end = geometry.lastLineIndex(to) + 1;
        List<Double> sizes = new ArrayList<>();
        while (end - x > 1e-6) {
            StopCutPolicy.Stop cut = policy.cutWithin(x, min, target, max, end);
            int s0 = geometry.line((int) Math.floor(x + 1e-6)).verses.get(0).getSurah();
            int s1 = geometry.line(Math.max(0, (int) Math.ceil(cut.position - 1e-6) - 1)).verses.get(0).getSurah();
            if (s0 != s1) crossing[0]++;
            sizes.add(cut.position - x);
            x = cut.position;
        }
        return sizes;
    }

    private static void check(String name, VerseRef from, VerseRef to, double min, double target, double max,
                              double weekMin, double weekMax) throws Exception {
        int[] crossing = {0};
        List<Double> sizes = walk(from, to, min, target, max, crossing);
        double lo = Double.MAX_VALUE, hi = 0;
        for (int i = 0; i + 3 <= sizes.size(); i += 3) {
            double week = sizes.get(i) + sizes.get(i + 1) + sizes.get(i + 2);
            lo = Math.min(lo, week);
            hi = Math.max(hi, week);
        }
        for (int i = 0; i + 1 < sizes.size(); i++) {
            assertTrue(name + " block " + i + " = " + sizes.get(i), sizes.get(i) >= min - 1e-6 && sizes.get(i) <= max + 1e-6);
        }
        report.append(String.format("%s: séances=%d semaines=%d lignes/semaine %.1f–%.1f, blocs à cheval sur 2 sourates=%d%n",
            name, sizes.size(), sizes.size() / 3, lo, hi, crossing[0]));
        Files.write(Paths.get(System.getProperty("java.io.tmpdir"), "stop-cut-weekly.txt"),
            report.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(name + " weekly min " + lo, lo >= weekMin);
        assertTrue(name + " weekly max " + hi, hi <= weekMax);
    }

    @Test public void apprentissageIsAboutOnePageAWeek() throws Exception {
        check("Apprentissage البقرة 75–286", new VerseRef(2, 75), new VerseRef(2, 286), 4, 5, 6, 12, 18);
        check("Apprentissage Coran", new VerseRef(2, 1), new VerseRef(114, 6), 4, 5, 6, 11, 18);
        check("Apprentissage Juz ʿAmma", new VerseRef(78, 1), new VerseRef(114, 6), 4, 5, 6, 12, 18);
    }

    @Test public void stabilisationIsAboutOnePageAndAHalfAWeek() throws Exception {
        check("Stabilisation Ḥujurāt→Nās", new VerseRef(49, 1), new VerseRef(114, 6), 6, 7, 8, 18, 24);
        check("Stabilisation Baqara→Fatḥ", new VerseRef(2, 1), new VerseRef(48, 29), 6, 7, 8, 18, 24);
    }
}

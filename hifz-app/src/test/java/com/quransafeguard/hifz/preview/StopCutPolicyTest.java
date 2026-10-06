package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Step 1 of the stop-aware cutting (user decision): the rule itself, on the whole Mushaf. */
public final class StopCutPolicyTest {
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

    @Test public void alHujuratNineHasItsVerifiedPermittedStop() {
        boolean found = false;
        for (StopCutPolicy.Stop stop : policy.stops()) {
            if (stop.verse != null && stop.verse.equals(new VerseRef(49, 7)) && stop.afterWord == 5) {
                assertEquals(StopCutPolicy.Kind.PERMITTED, stop.kind);
                found = true;
            }
        }
        assertTrue("49:7 ج after رسول الله (word 5)", found);
    }

    @Test public void apprentissageWalkStopsWithinTwoLinesOnRealStops() throws Exception {
        EnumMap<StopCutPolicy.Kind, Integer> kinds = new EnumMap<>(StopCutPolicy.Kind.class);
        int[] histogram = new int[5];
        List<String> examples = new ArrayList<>();
        int cuts = 0;
        for (int surah = 2; surah <= 114; surah++) {
            double start = geometry.firstLineIndex(new VerseRef(surah, 1));
            double end = geometry.lastLineIndex(lastVerse(surah)) + 1;
            while (end - start > 5 + 0.5) {
                StopCutPolicy.Stop cut = policy.cut(start, 5, end);
                double deviation = cut.position - (start + 5);
                assertTrue("never backwards", cut.position > start);
                assertTrue("never past the surah", cut.position <= end + 1e-9);
                if (cut.kind != StopCutPolicy.Kind.LINE_END) assertTrue(cut + " deviates " + deviation, Math.abs(deviation) <= 2.0 + 1e-9);
                kinds.merge(cut.kind, 1, Integer::sum);
                histogram[Math.min(4, (int) Math.floor(Math.abs(deviation) * 2))]++;
                if (examples.size() < 12 && surah == 2) examples.add(String.format("%.2f→%s (%+.2f l.)", start, cut, deviation));
                start = cut.position;
                cuts++;
            }
        }
        report.append("Apprentissage (5 lignes), coupes=").append(cuts).append(" ").append(kinds)
            .append(" écart |0–0.5|,|0.5–1|,|1–1.5|,|1.5–2|,>2 = ").append(java.util.Arrays.toString(histogram)).append('\n');
        for (String e : examples) report.append("  ").append(e).append('\n');
        write();
    }

    @Test public void stabilisationInnerCutsStopWithinTwoLinesOnRealStops() throws Exception {
        EnumMap<StopCutPolicy.Kind, Integer> kinds = new EnumMap<>(StopCutPolicy.Kind.class);
        int cuts = 0;
        double sumAbs = 0;
        for (ItqanRotationPolicy.Leg leg : ItqanRotationPolicy.Leg.values()) {
            for (AnchoringQueue.Entry unit : HifzPrefs.physicalUnitsInLeg(leg, new VerseRef(114, 6), geometry,
                    ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS)) {
                List<String> owned = CorpusLinePolicy.ownedLineIdsForRangeOnPage(
                    GeometryRepository.parseVerse(unit.start), GeometryRepository.parseVerse(unit.end), geometry);
                List<GeometryRepository.LineMeta> lines = geometry.linesForExactIds(owned);
                List<StabilizationHalfPagePolicy.Unit> blocks = StabilizationHalfPagePolicy.planPage(lines);
                double start = lines.get(0).globalIndex, end = lines.get(lines.size() - 1).globalIndex + 1;
                for (int b = 0; b + 1 < blocks.size(); b++) {
                    StopCutPolicy.Stop cut = policy.cut(start, blocks.get(b).lineIds.size(), end - 3);
                    double deviation = cut.position - (start + blocks.get(b).lineIds.size());
                    if (cut.kind != StopCutPolicy.Kind.LINE_END) assertTrue(Math.abs(deviation) <= 2.0 + 1e-9);
                    kinds.merge(cut.kind, 1, Integer::sum);
                    sumAbs += Math.abs(deviation);
                    start = cut.position;
                    cuts++;
                }
            }
        }
        report.append("Stabilisation (coupes internes 8/7/7), coupes=").append(cuts).append(" ").append(kinds)
            .append(String.format(" écart moyen %.2f l.", sumAbs / Math.max(1, cuts))).append('\n');
        write();
    }

    /** User decision: Apprentissage 4–6 lines, Stabilisation 6–8, except a surah's last block. */
    @Test public void boundedBlocksStayInsideTheirWindowExceptASurahsLast() {
        double[][] windows = {{4, 5, 6}, {6, 7, 8}};
        for (double[] window : windows) {
            for (int surah = 2; surah <= 114; surah++) {
                double start = geometry.firstLineIndex(new VerseRef(surah, 1));
                double end = geometry.lastLineIndex(lastVerse(surah)) + 1;
                int guard = 0;
                while (end - start > 1e-6) {
                    StopCutPolicy.Stop cut = policy.cutWithin(start, window[0], window[1], window[2], end);
                    double size = cut.position - start;
                    assertTrue("progress", size > 0);
                    if (cut.position < end - 1e-9) {
                        assertTrue(surah + ": " + size + " lines outside " + window[0] + "–" + window[2],
                            size >= window[0] - 1e-6 && size <= window[2] + 1e-6);
                    }
                    start = cut.position;
                    assertTrue(++guard < 1000);
                }
            }
        }
    }

    private static VerseRef lastVerse(int surah) {
        VerseRef v = new VerseRef(surah, 1);
        while (true) {
            VerseRef next = com.quransafeguard.hifz.core.QuranCanon.INSTANCE.next(v);
            if (next == null || next.getSurah() != surah) return v;
            v = next;
        }
    }

    private static void write() throws Exception {
        Path out = Paths.get(System.getProperty("java.io.tmpdir"), "stop-cut-report.txt");
        Files.write(out, report.toString().getBytes(StandardCharsets.UTF_8));
    }
}

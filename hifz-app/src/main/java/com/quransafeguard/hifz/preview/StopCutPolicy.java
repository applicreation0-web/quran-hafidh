package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User decision (travail de fond, step 1): an Apprentissage or Stabilisation block stops as close
 * as possible to its line limit, on a real stopping point — possibly inside a line, right after a
 * rosette or a waqf mark — never more than {@link #MAX_DEVIATION} lines away:
 * <ol>
 *   <li>a rosette within {@link #ROSETTE_FIRST} line of the limit;</li>
 *   <li>otherwise a recommended stop (مـ lāzim, قلى);</li>
 *   <li>then a permitted stop (ج);</li>
 *   <li>then a rosette up to {@link #MAX_DEVIATION} lines away;</li>
 *   <li>otherwise the line end at the limit (today's behaviour).</li>
 * </ol>
 * صلى, ∴ and سكتة are never cutting points. Positions are measured in lines: a cut after a word
 * sits at its line index plus the share of that line already read (right to left). Waqf marks come
 * from Tanzil and are trusted only where Tanzil's word count for the verse equals the quran-ws one
 * (327 verses split words differently); anything that does not check out falls back, never guesses.
 * Pure and Android-free.
 */
final class StopCutPolicy {
    enum Kind { ROSETTE, RECOMMENDED, PERMITTED, LINE_END }

    static final double ROSETTE_FIRST = 1.0;
    static final double MAX_DEVIATION = 2.0;

    /** A stopping point right after one word, at a continuous position in lines. */
    static final class Stop {
        final int line;
        final VerseRef verse;
        final int afterWord;
        final Kind kind;
        final double position;
        /** Page x where the line is cut (step 2): everything right of it is read before the stop. */
        final double x;

        Stop(int line, VerseRef verse, int afterWord, Kind kind, double position, double x) {
            this.line = line; this.verse = verse; this.afterWord = afterWord; this.kind = kind; this.position = position;
            this.x = x;
        }

        @Override public String toString() { return kind + "@" + verse + ":" + afterWord + "(" + position + ")"; }
    }

    private final List<Stop> stops;

    private StopCutPolicy(List<Stop> stops) { this.stops = stops; }

    List<Stop> stops() { return stops; }

    /**
     * Every stopping point of the Mushaf in reading order. Words are placed on the geometry line
     * whose band holds their vertical centre; a page whose words do not all land on a line, or
     * whose verse ends cannot be located, contributes no stop (its blocks keep line ends).
     */
    static StopCutPolicy build(GeometryRepository geometry, Map<Integer, List<WordGeometryRepository.WordBox>> words,
                               String waqfJson) throws Exception {
        Map<String, List<int[]>> waqf = parseWaqf(waqfJson);
        Map<String, Integer> wordCount = new HashMap<>();
        for (List<WordGeometryRepository.WordBox> page : words.values()) {
            for (WordGeometryRepository.WordBox w : page) wordCount.merge(w.surah + ":" + w.ayah, 1, Integer::sum);
        }
        ArrayList<Stop> out = new ArrayList<>();
        int index = 0;
        while (index < geometry.lineCount()) {
            int page = geometry.line(index).page;
            int first = index;
            while (index < geometry.lineCount() && geometry.line(index).page == page) index++;
            List<WordGeometryRepository.WordBox> pageWords = words.get(page);
            if (pageWords == null || page < 3) continue;
            List<Stop> pageStops = pageStops(geometry, first, index, pageWords, waqf, wordCount);
            out.addAll(pageStops);
        }
        Collections.sort(out, (a, b) -> Double.compare(a.position, b.position));
        return new StopCutPolicy(Collections.unmodifiableList(out));
    }

    private static List<Stop> pageStops(GeometryRepository geometry, int firstLine, int endLine,
                                        List<WordGeometryRepository.WordBox> pageWords,
                                        Map<String, List<int[]>> waqf, Map<String, Integer> wordCount) {
        int lines = endLine - firstLine;
        double[] right = new double[lines], left = new double[lines];
        java.util.Arrays.fill(right, Double.NEGATIVE_INFINITY);
        java.util.Arrays.fill(left, Double.POSITIVE_INFINITY);
        HashMap<String, Integer> lineOf = new HashMap<>();
        HashMap<String, WordGeometryRepository.WordBox> byKey = new HashMap<>();
        HashMap<String, Integer> lastWord = new HashMap<>();
        for (WordGeometryRepository.WordBox w : pageWords) {
            double centre = (w.y0 + w.y1) / 2.0;
            int found = -1;
            for (int i = 0; i < lines; i++) {
                GeometryRepository.LineMeta line = geometry.line(firstLine + i);
                if (centre >= line.top && centre <= line.bottom) { found = i; break; }
            }
            if (found < 0) return Collections.emptyList();
            lineOf.put(w.key, found);
            byKey.put(w.key, w);
            right[found] = Math.max(right[found], w.x1);
            left[found] = Math.min(left[found], w.x0);
            lastWord.merge(w.surah + ":" + w.ayah, w.word, Math::max);
        }
        ArrayList<Stop> out = new ArrayList<>();
        for (WordGeometryRepository.WordBox w : pageWords) {
            String verseKey = w.surah + ":" + w.ayah;
            VerseRef verse = new VerseRef(w.surah, w.ayah);
            Kind kind = null;
            Integer total = wordCount.get(verseKey);
            boolean verseEnd = total != null && w.word == total && lastWord.get(verseKey) == w.word;
            if (verseEnd) kind = Kind.ROSETTE;
            else {
                List<int[]> marks = waqf.get(verseKey);
                if (marks != null && total != null) {
                    for (int[] mark : marks) {
                        // mark[0] = Tanzil's 0-based afterWord, mark[1] = its kind, mark[2] = Tanzil count
                        if (mark[2] != total || mark[0] + 1 != w.word) continue;
                        if (mark[1] == 1) kind = Kind.RECOMMENDED;
                        else if (mark[1] == 2 && kind == null) kind = Kind.PERMITTED;
                    }
                }
            }
            if (kind == null) continue;
            int i = lineOf.get(w.key);
            double width = right[i] - left[i];
            if (width <= 0) continue;
            boolean lastOnLine = w.x0 <= left[i] + 0.5;
            double share = lastOnLine ? 1.0 : Math.max(0.0, Math.min(1.0, (right[i] - w.x0) / width));
            // The cut sits just right of the next word read on this line, so a rosette or waqf
            // sign drawn between the two words stays with the block that ends here.
            double nextRight = Double.NEGATIVE_INFINITY;
            for (WordGeometryRepository.WordBox other : pageWords) {
                Integer otherLine = lineOf.get(other.key);
                if (otherLine == null || otherLine != i || other == w || other.x1 > w.x0 + 0.5) continue;
                nextRight = Math.max(nextRight, other.x1);
            }
            double cutX = lastOnLine || nextRight == Double.NEGATIVE_INFINITY ? left[i] - 1.0 : nextRight + 0.6;
            out.add(new Stop(firstLine + i, verse, w.word, kind, firstLine + i + share, cutX));
        }
        return out;
    }

    /** verse → [afterWord(0-based), kind(1 recommended, 2 permitted), tanzilWordCount]. */
    private static Map<String, List<int[]>> parseWaqf(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        JSONObject marks = root.getJSONObject("marks");
        JSONObject counts = root.optJSONObject("wordCounts");
        HashMap<String, List<int[]>> out = new HashMap<>();
        if (counts == null) return out; // no verification data: never trust afterWord
        java.util.Iterator<String> keys = marks.keys();
        while (keys.hasNext()) {
            String verse = keys.next();
            if (!counts.has(verse)) continue;
            int tanzilCount = counts.getInt(verse);
            JSONArray array = marks.getJSONArray(verse);
            ArrayList<int[]> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject mark = array.getJSONObject(i);
                String type = mark.getString("type");
                int kind = "lazim".equals(type) || "qili".equals(type) ? 1 : "jaiz".equals(type) ? 2 : 0;
                if (kind != 0) list.add(new int[]{mark.getInt("afterWord"), kind, tanzilCount});
            }
            if (!list.isEmpty()) out.put(verse, list);
        }
        return out;
    }

    /**
     * Where a block that starts at {@code start} (in lines) and should hold {@code target} lines
     * stops, never past {@code hardEnd} (unit or surah end). Returns a LINE_END stop when no real
     * stopping point qualifies.
     */
    Stop cut(double start, double target, double hardEnd) {
        double aim = start + target;
        Stop best = pick(Kind.ROSETTE, start, aim, hardEnd, ROSETTE_FIRST);
        if (best == null) best = pick(Kind.RECOMMENDED, start, aim, hardEnd, MAX_DEVIATION);
        if (best == null) best = pick(Kind.PERMITTED, start, aim, hardEnd, MAX_DEVIATION);
        if (best == null) best = pick(Kind.ROSETTE, start, aim, hardEnd, MAX_DEVIATION);
        if (best != null) return best;
        double lineEnd = Math.min(hardEnd, Math.max(Math.floor(start) + 1, Math.round(aim)));
        return new Stop((int) lineEnd - 1, null, 0, Kind.LINE_END, lineEnd, Double.NEGATIVE_INFINITY);
    }

    private Stop pick(Kind kind, double start, double aim, double hardEnd, double within) {
        Stop best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int lo = firstAtOrAfter(aim - within);
        for (int i = lo; i < stops.size(); i++) {
            Stop stop = stops.get(i);
            if (stop.position > aim + within) break;
            if (stop.kind != kind || stop.position <= start + 0.5 || stop.position > hardEnd + 1e-9) continue;
            double distance = Math.abs(stop.position - aim);
            if (distance < bestDistance) { best = stop; bestDistance = distance; }
        }
        return best;
    }

    private int firstAtOrAfter(double position) {
        int lo = 0, hi = stops.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (stops.get(mid).position < position) lo = mid + 1; else hi = mid;
        }
        return lo;
    }
}

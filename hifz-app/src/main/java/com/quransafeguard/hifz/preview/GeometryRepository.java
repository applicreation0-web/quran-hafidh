package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/** Exact read-only index derived from the same shipped KFQC SVG corpus as the renderer. */
public final class GeometryRepository {
    public static final class LineMeta {
        public final int globalIndex;
        public final String id;
        public final int page;
        public final List<VerseRef> verses;

        LineMeta(int globalIndex, String id, int page, List<VerseRef> verses) {
            this.globalIndex = globalIndex;
            this.id = id;
            this.page = page;
            this.verses = Collections.unmodifiableList(verses);
        }
    }

    public static final class FiveLineBlock {
        public final int startLineIndex;
        public final int endLineIndex;
        public final VerseRef startVerse;
        public final VerseRef endVerse;
        public final boolean startsInsideVerse;
        public final boolean endsInsideVerse;
        public final List<String> lineIds;
        public final List<VerseRef> verses;

        FiveLineBlock(int startLineIndex, int endLineIndex, VerseRef startVerse, VerseRef endVerse,
                      boolean startsInsideVerse, boolean endsInsideVerse,
                      List<String> lineIds, List<VerseRef> verses) {
            this.startLineIndex = startLineIndex;
            this.endLineIndex = endLineIndex;
            this.startVerse = startVerse;
            this.endVerse = endVerse;
            this.startsInsideVerse = startsInsideVerse;
            this.endsInsideVerse = endsInsideVerse;
            this.lineIds = Collections.unmodifiableList(lineIds);
            this.verses = Collections.unmodifiableList(verses);
        }

        public String verseLabel() {
            String prefix = startsInsideVerse ? "suite de " : "";
            String end = endVerse.toString() + (endsInsideVerse ? " (partiel)" : "");
            return prefix + startVerse + " → " + end;
        }
    }

    public static final class VerseUnit {
        public final int page;
        public final VerseRef start;
        public final VerseRef end;
        public final List<VerseRef> verses;
        public final List<String> lineIds;

        VerseUnit(int page, VerseRef start, VerseRef end, List<VerseRef> verses, List<String> lineIds) {
            this.page = page;
            this.start = start;
            this.end = end;
            this.verses = Collections.unmodifiableList(verses);
            this.lineIds = Collections.unmodifiableList(lineIds);
        }
    }

    public static final class EligibleLinePlan {
        public final VerseRef start;
        public final VerseRef actualPlannedEnd;
        public final int requestedLines;
        public final List<VerseRef> traversalVerses;

        EligibleLinePlan(VerseRef start, VerseRef actualPlannedEnd, int requestedLines,
                         List<VerseRef> traversalVerses) {
            this.start = start;
            this.actualPlannedEnd = actualPlannedEnd;
            this.requestedLines = requestedLines;
            this.traversalVerses = Collections.unmodifiableList(traversalVerses);
        }
    }

    private static volatile GeometryRepository INSTANCE;
    private final List<LineMeta> lines;
    private final JSONObject pages;

    private GeometryRepository(Context context) throws Exception {
        JSONObject root = new JSONObject(readAsset(context, "reader109/geometry.json"));
        pages = root.getJSONObject("pages");
        ArrayList<LineMeta> result = new ArrayList<>();
        int index = 0;
        for (int page = 1; page <= 604; page++) {
            JSONObject pageObject = pages.getJSONObject(Integer.toString(page));
            JSONArray pageLines = pageObject.getJSONArray("lines");
            for (int i = 0; i < pageLines.length(); i++) {
                JSONObject line = pageLines.getJSONObject(i);
                JSONArray refs = line.getJSONArray("verses");
                ArrayList<VerseRef> verses = new ArrayList<>();
                for (int j = 0; j < refs.length(); j++) {
                    VerseRef ref = parseVerse(refs.getString(j));
                    if (!verses.contains(ref)) verses.add(ref);
                }
                verses.sort(Comparator.comparingInt(GeometryRepository::ordinal));
                result.add(new LineMeta(index++, line.getString("id"), page, verses));
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("Empty Mushaf geometry");
        lines = Collections.unmodifiableList(result);
    }

    public static GeometryRepository get(Context context) {
        GeometryRepository local = INSTANCE;
        if (local != null) return local;
        synchronized (GeometryRepository.class) {
            local = INSTANCE;
            if (local == null) {
                try {
                    local = new GeometryRepository(context.getApplicationContext());
                } catch (Exception error) {
                    throw new IllegalStateException("Canonical Mushaf geometry unavailable", error);
                }
                INSTANCE = local;
            }
            return local;
        }
    }

    public int lineCount() { return lines.size(); }
    public LineMeta line(int index) { return lines.get(index); }

    /** Resolve exact physical line ids in canonical order; missing, duplicate or reordered ids fail closed. */
    List<LineMeta> linesForExactIds(List<String> lineIds) {
        if (lineIds == null || lineIds.isEmpty()) throw new IllegalArgumentException("line ids required");
        LinkedHashSet<String> wanted = new LinkedHashSet<>(lineIds);
        if (wanted.size() != lineIds.size()) throw new IllegalStateException("Duplicate physical line id");
        ArrayList<LineMeta> result = new ArrayList<>();
        for (LineMeta line : lines) if (wanted.contains(line.id)) result.add(line);
        if (result.size() != wanted.size()) throw new IllegalStateException("Unknown physical Mushaf line id");
        for (int i = 0; i < result.size(); i++) {
            if (!result.get(i).id.equals(lineIds.get(i)))
                throw new IllegalStateException("Physical line ids are not in canonical order");
        }
        return Collections.unmodifiableList(result);
    }

    /** Ordered Quran verses touching exactly these physical lines. */
    List<VerseRef> versesOnLines(List<String> lineIds) {
        LinkedHashSet<VerseRef> selected = new LinkedHashSet<>();
        for (LineMeta line : linesForExactIds(lineIds)) selected.addAll(line.verses);
        ArrayList<VerseRef> result = new ArrayList<>(selected);
        result.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return Collections.unmodifiableList(result);
    }

    /** Exact per-page geometry already parsed by the singleton; avoids a second full JSON parse in the renderer. */
    public String pageGeometryJson(int page) {
        if (page < 1 || page > 604) throw new IllegalArgumentException("page outside 1..604");
        JSONObject pageObject = pages.optJSONObject(Integer.toString(page));
        if (pageObject == null) throw new IllegalStateException("geometry missing for page " + page);
        return pageObject.toString();
    }

    public int firstLineIndex(VerseRef verse) {
        for (LineMeta line : lines) if (line.verses.contains(verse)) return line.globalIndex;
        throw new IllegalArgumentException("Verse absent from geometry: " + verse);
    }

    public int lastLineIndex(VerseRef verse) {
        for (int i = lines.size() - 1; i >= 0; i--) if (lines.get(i).verses.contains(verse)) return i;
        throw new IllegalArgumentException("Verse absent from geometry: " + verse);
    }

    public int pageForVerse(VerseRef verse) { return lines.get(firstLineIndex(verse)).page; }

    /** The surah printed at the top of the given page (its first physical line's surah). */
    public int firstSurahOnPage(int page) {
        for (LineMeta line : lines) if (line.page == page) return line.verses.get(0).getSurah();
        throw new IllegalArgumentException("No canonical physical lines for page " + page);
    }

    /** Every physical line printed on this page, in Mushaf order — used to mask a whole page at once. */
    public List<String> lineIdsOnPage(int page) {
        ArrayList<String> ids = new ArrayList<>();
        for (LineMeta line : lines) if (line.page == page) ids.add(line.id);
        return Collections.unmodifiableList(ids);
    }

    /**
     * Clips to at most SABQI_LINES physical lines, but never crosses a surah boundary: a Mushaf
     * line always belongs to exactly one surah (a new surah always starts its own line), so a
     * block can legitimately be shorter than 5 lines when the surah ends first. The next block
     * then naturally starts at the following surah's first line.
     */
    public FiveLineBlock fiveLineBlock(int startLineIndex) {
        if (startLineIndex < 0 || startLineIndex >= lines.size()) {
            throw new IllegalArgumentException("Invalid global Mushaf line " + startLineIndex);
        }
        int startSurah = lines.get(startLineIndex).verses.get(0).getSurah();
        int maxIndex = Math.min(lines.size() - 1, startLineIndex + PreviewConfig.SABQI_LINES - 1);
        int endIndex = startLineIndex;
        while (endIndex < maxIndex && lines.get(endIndex + 1).verses.get(0).getSurah() == startSurah) endIndex++;
        ArrayList<String> ids = new ArrayList<>();
        LinkedHashSet<VerseRef> refs = new LinkedHashSet<>();
        for (int i = startLineIndex; i <= endIndex; i++) {
            LineMeta line = lines.get(i);
            ids.add(line.id);
            refs.addAll(line.verses);
        }
        ArrayList<VerseRef> verses = new ArrayList<>(refs);
        verses.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        if (verses.isEmpty()) throw new IllegalStateException("Five-line block has no Quran verses");
        VerseRef first = verses.get(0);
        VerseRef last = verses.get(verses.size() - 1);
        boolean startPartial = startLineIndex > 0 && lines.get(startLineIndex - 1).verses.contains(first);
        boolean endPartial = endIndex + 1 < lines.size() && lines.get(endIndex + 1).verses.contains(last);
        return new FiveLineBlock(startLineIndex, endIndex, first, last, startPartial, endPartial, ids, verses);
    }

    /** Verses whose complete physical line span lies inside the supplied stable line interval. */
    public List<VerseRef> versesFullyCoveredByLines(int startLineIndex, int endLineIndex) {
        if (startLineIndex < 0 || endLineIndex >= lines.size() || endLineIndex < startLineIndex) {
            throw new IllegalArgumentException("Invalid stable line interval " + startLineIndex + ".." + endLineIndex);
        }
        LinkedHashSet<VerseRef> candidates = new LinkedHashSet<>();
        for (int i = startLineIndex; i <= endLineIndex; i++) candidates.addAll(lines.get(i).verses);
        ArrayList<VerseRef> complete = new ArrayList<>();
        for (VerseRef verse : candidates) {
            if (firstLineIndex(verse) >= startLineIndex && lastLineIndex(verse) <= endLineIndex) complete.add(verse);
        }
        complete.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return complete;
    }

    /** Count physical Mushaf lines touched by a non-wrapping canonical verse interval. */
    public int lineCountForVerseRange(VerseRef start, VerseRef end) {
        if (ordinal(end) < ordinal(start)) throw new IllegalArgumentException("Wrapped range not supported for calibration");
        int low = ordinal(start), high = ordinal(end), count = 0;
        for (LineMeta line : lines) {
            boolean hit = false;
            for (VerseRef ref : line.verses) {
                int o = ordinal(ref);
                if (o >= low && o <= high) { hit = true; break; }
            }
            if (hit) count++;
        }
        return count;
    }

    /** Itqan working unit: the eligible verse segment on the current canonical Mushaf page. */
    public VerseUnit eligiblePageUnit(VerseRef cursor, EligibleCorpus corpus) {
        if (!corpus.contains(cursor)) throw new IllegalArgumentException("Itqan cursor is not eligible: " + cursor);
        int firstIndex = firstLineIndex(cursor);
        int page = lines.get(firstIndex).page;
        LinkedHashSet<VerseRef> selected = new LinkedHashSet<>();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        int cursorOrdinal = ordinal(cursor);
        for (int i = firstIndex; i < lines.size() && lines.get(i).page == page; i++) {
            LineMeta line = lines.get(i);
            boolean lineUsed = false;
            for (VerseRef ref : line.verses) {
                if (ordinal(ref) >= cursorOrdinal && corpus.contains(ref)) {
                    selected.add(ref);
                    lineUsed = true;
                }
            }
            if (lineUsed) ids.add(line.id);
        }
        if (selected.isEmpty()) throw new IllegalStateException("No eligible verses on cursor page " + page);
        ArrayList<VerseRef> ordered = new ArrayList<>(selected);
        ordered.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return new VerseUnit(page, cursor, ordered.get(ordered.size() - 1), ordered, new ArrayList<>(ids));
    }

    /**
     * Like eligiblePageUnit, but for Stabilisation's weekly working unit: extends up to
     * PreviewConfig.STABILIZATION_WEEKLY_LINES touched physical lines (about 1.5 pages) instead
     * of stopping at the end of cursor's page, so a week's three sessions (8/7/7) can be planned
     * as one unit. The only hard stop besides that line budget is a surah change or the caller's
     * own rangeEnd — a shorter, single-surah week within one pending range is accepted rather
     * than ever spanning two surahs, or two unrelated pending ranges, in one weekly unit.
     *
     * Mirrors eligiblePageUnit's own contract exactly: this only locates the candidate verse
     * range (start/end). It deliberately does not resolve owned physical lines itself — the
     * caller does that afterwards via CorpusLinePolicy.ownedLineIdsForRangeOnPage(start, end,
     * this), the same ownership rule ("a shared line belongs to the earliest verse printed on
     * it") already used for single-page units. Resolving ownership here too would double the
     * source of truth and risk a shared boundary line being claimed by two consecutive units.
     */
    public VerseUnit eligibleWeeklyStabilizationUnit(VerseRef cursor, VerseRef rangeEnd, EligibleCorpus corpus) {
        if (!corpus.contains(cursor)) throw new IllegalArgumentException("Itqan cursor is not eligible: " + cursor);
        int firstIndex = firstLineIndex(cursor);
        int page = lines.get(firstIndex).page;
        int startSurah = lines.get(firstIndex).verses.get(0).getSurah();
        int cursorOrdinal = ordinal(cursor);
        int rangeEndOrdinal = ordinal(rangeEnd);
        LinkedHashSet<VerseRef> selected = new LinkedHashSet<>();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (int i = firstIndex; i < lines.size() && ids.size() < PreviewConfig.STABILIZATION_WEEKLY_LINES; i++) {
            LineMeta line = lines.get(i);
            if (line.verses.get(0).getSurah() != startSurah) break;
            boolean lineUsed = false;
            for (VerseRef ref : line.verses) {
                int refOrdinal = ordinal(ref);
                if (refOrdinal >= cursorOrdinal && refOrdinal <= rangeEndOrdinal && corpus.contains(ref)) {
                    selected.add(ref);
                    lineUsed = true;
                }
            }
            if (lineUsed) ids.add(line.id);
            else if (!selected.isEmpty()) break;
        }
        if (selected.isEmpty()) throw new IllegalStateException("No eligible verses on cursor page " + page);
        ArrayList<VerseRef> ordered = new ArrayList<>(selected);
        ordered.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return new VerseUnit(page, cursor, ordered.get(ordered.size() - 1), ordered, new ArrayList<>(ids));
    }

    /**
     * Plans physical Quran lines through the cyclic eligible corpus. Gaps are skipped and
     * wrap after An-Nas is explicit. The returned end is predictive only; UI must persist
     * the actual user-validated endpoint instead.
     */
    public EligibleLinePlan planEligibleLines(VerseRef cursor, int requestedLines, EligibleCorpus corpus) {
        if (requestedLines <= 0) throw new IllegalArgumentException("requestedLines must be positive");
        if (!corpus.contains(cursor)) throw new IllegalArgumentException("Murajaah cursor is not eligible: " + cursor);

        int lineIndex = firstLineIndex(cursor);
        int counted = 0;
        int guard = 0;
        VerseRef last = cursor;
        ArrayList<VerseRef> traversal = new ArrayList<>();
        VerseRef expectedCursor = cursor;
        while (counted < requestedLines) {
            if (guard++ > lines.size() * 20) throw new IllegalStateException("Eligible line planner failed to make progress");
            LineMeta line = lines.get(lineIndex);
            boolean eligibleLine = false;
            for (VerseRef ref : line.verses) {
                if (corpus.contains(ref)) {
                    if (counted == 0 && ordinal(ref) < ordinal(expectedCursor)) continue;
                    eligibleLine = true;
                    if (traversal.isEmpty() || !traversal.get(traversal.size() - 1).equals(ref)) traversal.add(ref);
                    last = ref;
                }
            }
            if (eligibleLine) counted++;
            lineIndex++;
            if (lineIndex >= lines.size()) lineIndex = 0;
        }
        return new EligibleLinePlan(cursor, last, requestedLines, traversal);
    }

    /**
     * Physical-line counts for each consecutive surah segment touched by a non-wrapping verse range.
     * Geometry has already been audited so one physical line never mixes surahs; fail closed if that
     * invariant is ever broken by a future asset.
     */
    public int[] surahSegmentLineCounts(VerseRef start, VerseRef end) {
        if (start == null || end == null) throw new IllegalArgumentException("verse range required");
        int low = ordinal(start);
        int high = ordinal(end);
        if (high < low) throw new IllegalArgumentException("Wrapped range not supported for Anchoring layout");

        ArrayList<Integer> counts = new ArrayList<>();
        int currentSurah = -1;
        int currentCount = 0;
        for (LineMeta line : lines) {
            VerseRef hit = null;
            Integer lineSurah = null;
            for (VerseRef ref : line.verses) {
                int surah = ref.getSurah();
                if (lineSurah == null) lineSurah = surah;
                else if (lineSurah != surah) {
                    throw new IllegalStateException("Physical Mushaf line crosses surah boundary: " + line.id);
                }
                int o = ordinal(ref);
                if (o >= low && o <= high) hit = ref;
            }
            if (hit == null) continue;
            int surah = hit.getSurah();
            if (surah != currentSurah) {
                if (currentCount > 0) counts.add(currentCount);
                currentSurah = surah;
                currentCount = 1;
            } else {
                currentCount++;
            }
        }
        if (currentCount > 0) counts.add(currentCount);
        if (counts.isEmpty()) throw new IllegalArgumentException("Verse range absent from geometry: " + start + " → " + end);
        int[] result = new int[counts.size()];
        for (int i = 0; i < counts.size(); i++) result[i] = counts.get(i);
        return result;
    }

    public List<String> lineIdsForVerseRange(VerseRef a, VerseRef b) {
        int low = Math.min(ordinal(a), ordinal(b));
        int high = Math.max(ordinal(a), ordinal(b));
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (LineMeta line : lines) {
            for (VerseRef ref : line.verses) {
                int o = ordinal(ref);
                if (o >= low && o <= high) { ids.add(line.id); break; }
            }
        }
        return new ArrayList<>(ids);
    }

    public List<VerseRef> versesForRange(VerseRef a, VerseRef b) {
        int low = Math.min(ordinal(a), ordinal(b));
        int high = Math.max(ordinal(a), ordinal(b));
        ArrayList<VerseRef> result = new ArrayList<>();
        for (int o = low; o <= high; o++) result.add(QuranCanon.INSTANCE.fromOrdinal(o));
        return result;
    }

    /** Ordered verses touching the supplied physical lines, restricted to the current Ancrage unit. */
    public List<VerseRef> versesOnLines(List<String> lineIds, List<VerseRef> allowedVerses) {
        if (lineIds == null || lineIds.isEmpty() || allowedVerses == null || allowedVerses.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> wantedLines = new LinkedHashSet<>(lineIds);
        LinkedHashSet<VerseRef> allowed = new LinkedHashSet<>(allowedVerses);
        LinkedHashSet<VerseRef> selected = new LinkedHashSet<>();
        for (LineMeta line : lines) {
            if (!wantedLines.contains(line.id)) continue;
            for (VerseRef verse : line.verses) if (allowed.contains(verse)) selected.add(verse);
        }
        ArrayList<VerseRef> result = new ArrayList<>(selected);
        result.sort(Comparator.comparingInt(GeometryRepository::ordinal));
        return result;
    }

    public static VerseRef parseVerse(String value) {
        String[] parts = value.split(":");
        if (parts.length != 2) throw new IllegalArgumentException("Invalid verse reference " + value);
        return new VerseRef(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public static int ordinal(VerseRef ref) { return QuranCanon.INSTANCE.ordinal(ref); }

    public static VerseRef previous(VerseRef ref) {
        int current = ordinal(ref);
        return current <= 1 ? null : QuranCanon.INSTANCE.fromOrdinal(current - 1);
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream in = context.getAssets().open(path); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}

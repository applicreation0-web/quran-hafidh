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
import java.util.Set;

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

    public FiveLineBlock fiveLineBlock(int startLineIndex) {
        if (startLineIndex < 0 || startLineIndex >= lines.size()) {
            throw new IllegalArgumentException("Invalid global Mushaf line " + startLineIndex);
        }
        int endIndex = Math.min(lines.size() - 1, startLineIndex + PreviewConfig.SABQI_LINES - 1);
        if (endIndex - startLineIndex + 1 != PreviewConfig.SABQI_LINES) {
            throw new IllegalStateException("Not enough Quran lines for a five-line Sabqi block");
        }
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

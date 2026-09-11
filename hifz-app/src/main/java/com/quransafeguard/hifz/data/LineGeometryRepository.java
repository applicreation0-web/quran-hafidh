package com.quransafeguard.hifz.data;

import android.content.Context;
import android.graphics.RectF;
import android.util.SparseArray;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Physical Quran-line index derived only from the canonical KFQC ayah polygons.
 *
 * The KFQC ayah geometry is verse-oriented: a long ayah polygon may span several printed
 * lines. Therefore one polygon center cannot be treated as one Mushaf line. Standard Madinah
 * pages use a stable 15-row vertical grid; this class reconstructs that grid from the actual
 * polygon edges on each page and then maps ayahs to the physical rows they overlap.
 *
 * No Tarteel/layout database is bundled or required at runtime. The source of truth shipped by
 * Quran Hifz remains the pinned quran-svg KFQC geometry. The derived grid has been independently
 * cross-checked against a mature QPC-v4 layout during development.
 */
public final class LineGeometryRepository {
    private static volatile LineGeometryRepository INSTANCE;

    /** Median KFQC row boundaries, derived from the pinned 604-page geometry itself. */
    private static final float[] STANDARD_BOUNDARY_TEMPLATE = new float[]{
        0.00f, 41.25f, 77.00f, 113.25f, 148.75f, 184.50f, 220.25f, 256.50f,
        292.25f, 327.75f, 363.75f, 399.25f, 435.25f, 470.75f, 507.00f, 547.44f
    };
    private static final float BOUNDARY_MATCH_TOLERANCE = 12f;
    private static final float MIN_VERTICAL_OVERLAP = 1.5f;
    private static final float OVERLAP_FRACTION = 0.35f;

    public static LineGeometryRepository get(Context context) {
        LineGeometryRepository local = INSTANCE;
        if (local != null) return local;
        synchronized (LineGeometryRepository.class) {
            local = INSTANCE;
            if (local == null) {
                local = new LineGeometryRepository(context.getApplicationContext());
                INSTANCE = local;
            }
        }
        return local;
    }

    public static final class PageLine {
        public final int page;
        /** Physical row number on the printed page. Decorative rows are simply absent. */
        public final int number;
        public final RectF documentBounds;
        public final List<VerseRef> verses;

        PageLine(int page, int number, RectF documentBounds, List<VerseRef> verses) {
            this.page = page;
            this.number = number;
            this.documentBounds = new RectF(documentBounds);
            this.verses = Collections.unmodifiableList(new ArrayList<>(verses));
        }

        public String id() { return page + ":" + number; }
    }

    public static final class FiveLineBlock {
        public final int startGlobalIndex;
        public final int endGlobalIndex;
        public final VerseRef startVerse;
        public final VerseRef endVerse;
        public final boolean startsInsideVerse;
        public final boolean endsInsideVerse;
        public final List<PageLine> lines;
        public final List<VerseRef> verses;

        FiveLineBlock(int startGlobalIndex, int endGlobalIndex, VerseRef startVerse, VerseRef endVerse,
                      boolean startsInsideVerse, boolean endsInsideVerse,
                      List<PageLine> lines, List<VerseRef> verses) {
            this.startGlobalIndex = startGlobalIndex;
            this.endGlobalIndex = endGlobalIndex;
            this.startVerse = startVerse;
            this.endVerse = endVerse;
            this.startsInsideVerse = startsInsideVerse;
            this.endsInsideVerse = endsInsideVerse;
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
            this.verses = Collections.unmodifiableList(new ArrayList<>(verses));
        }
    }

    public static final class VerseUnit {
        public final int page;
        public final VerseRef start;
        public final VerseRef end;
        public final List<VerseRef> verses;
        public final List<PageLine> lines;

        VerseUnit(int page, VerseRef start, VerseRef end, List<VerseRef> verses, List<PageLine> lines) {
            this.page = page;
            this.start = start;
            this.end = end;
            this.verses = Collections.unmodifiableList(new ArrayList<>(verses));
            this.lines = Collections.unmodifiableList(new ArrayList<>(lines));
        }
    }

    public static final class EligibleLinePlan {
        public final VerseRef start;
        public final VerseRef predictedEnd;
        public final int requestedLines;
        public final List<VerseRef> traversalVerses;

        EligibleLinePlan(VerseRef start, VerseRef predictedEnd, int requestedLines, List<VerseRef> traversalVerses) {
            this.start = start;
            this.predictedEnd = predictedEnd;
            this.requestedLines = requestedLines;
            this.traversalVerses = Collections.unmodifiableList(new ArrayList<>(traversalVerses));
        }
    }

    private final GeometryRepository ayahGeometry;
    private final SparseArray<List<PageLine>> pageCache = new SparseArray<>();
    private volatile List<PageLine> allLines;

    private LineGeometryRepository(Context context) {
        ayahGeometry = new GeometryRepository(context);
    }

    public synchronized List<PageLine> linesForPage(int page) throws IOException {
        List<PageLine> cached = pageCache.get(page);
        if (cached != null) return cached;

        List<GeometryRepository.AyahRegion> regions = ayahGeometry.loadPage(page);
        final List<PageLine> built;
        if (page == 1) {
            // Opening spread: one surah-name row, then seven Quran text rows.
            built = buildOpeningRows(page, regions, 7, 2);
        } else if (page == 2) {
            // Opening spread: surah-name + basmallah, then six Quran text rows.
            built = buildOpeningRows(page, regions, 6, 3);
        } else {
            built = buildRowsFromEdges(page, regions, deriveStandardBoundaries(regions), 1);
        }

        if (built.isEmpty()) throw new IOException("No Quran text lines derived on page " + page);
        List<PageLine> immutable = Collections.unmodifiableList(built);
        pageCache.put(page, immutable);
        return immutable;
    }

    private static List<PageLine> buildOpeningRows(int page,
                                                    List<GeometryRepository.AyahRegion> regions,
                                                    int rowCount,
                                                    int physicalStartRow) throws IOException {
        float minMarker = Float.POSITIVE_INFINITY;
        float maxMarker = Float.NEGATIVE_INFINITY;
        for (GeometryRepository.AyahRegion region : regions) {
            if (region.markerY == null || !Float.isFinite(region.markerY)) continue;
            minMarker = Math.min(minMarker, region.markerY);
            maxMarker = Math.max(maxMarker, region.markerY);
        }
        if (!Float.isFinite(minMarker) || !Float.isFinite(maxMarker) || maxMarker <= minMarker || rowCount < 2) {
            throw new IOException("Cannot derive opening-spread line grid on page " + page);
        }

        float step = (maxMarker - minMarker) / (rowCount - 1f);
        float[] centers = new float[rowCount];
        for (int i = 0; i < rowCount; i++) centers[i] = minMarker + step * i;
        float[] edges = new float[rowCount + 1];
        edges[0] = centers[0] - step * 0.5f;
        for (int i = 1; i < rowCount; i++) edges[i] = (centers[i - 1] + centers[i]) * 0.5f;
        edges[rowCount] = centers[rowCount - 1] + step * 0.5f;
        return buildRowsFromEdges(page, regions, edges, physicalStartRow);
    }

    private static float[] deriveStandardBoundaries(List<GeometryRepository.AyahRegion> regions) throws IOException {
        ArrayList<Float> polygonEdges = new ArrayList<>();
        for (GeometryRepository.AyahRegion region : regions) {
            for (GeometryRepository.Polygon polygon : region.getPolygons()) {
                RectF bounds = polygon.getBounds();
                if (bounds.height() <= 0f) continue;
                polygonEdges.add(bounds.top);
                polygonEdges.add(bounds.bottom);
            }
        }
        if (polygonEdges.isEmpty()) throw new IOException("No polygon edges for KFQC line grid");

        final int n = STANDARD_BOUNDARY_TEMPLATE.length;
        Float[] observed = new Float[n];
        for (int i = 0; i < n; i++) {
            ArrayList<Float> near = new ArrayList<>();
            float target = STANDARD_BOUNDARY_TEMPLATE[i];
            for (Float y : polygonEdges) {
                if (Math.abs(y - target) <= BOUNDARY_MATCH_TOLERANCE) near.add(y);
            }
            if (!near.isEmpty()) observed[i] = median(near);
        }

        // The standard KFQC viewBox is 550 high. Keep the outer frame deterministic even when
        // no ayah polygon happens to touch the first/last boundary on a particular page.
        if (observed[0] == null) observed[0] = 0f;
        if (observed[n - 1] == null) observed[n - 1] = 550f;

        float[] offsets = new float[n];
        boolean[] known = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (observed[i] != null) {
                offsets[i] = observed[i] - STANDARD_BOUNDARY_TEMPLATE[i];
                known[i] = true;
            }
        }

        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            float offset;
            if (known[i]) {
                offset = offsets[i];
            } else {
                int lo = i - 1;
                while (lo >= 0 && !known[lo]) lo--;
                int hi = i + 1;
                while (hi < n && !known[hi]) hi++;
                if (lo < 0 || hi >= n) throw new IOException("Cannot interpolate KFQC row boundary " + i);
                float t = (i - lo) / (float) (hi - lo);
                offset = offsets[lo] * (1f - t) + offsets[hi] * t;
            }
            out[i] = STANDARD_BOUNDARY_TEMPLATE[i] + offset;
        }

        for (int i = 1; i < n; i++) {
            if (!(out[i] > out[i - 1])) out[i] = out[i - 1] + 1f;
        }
        return out;
    }

    private static float median(List<Float> values) {
        ArrayList<Float> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if ((n & 1) == 1) return sorted.get(n / 2);
        return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) * 0.5f;
    }

    private static List<PageLine> buildRowsFromEdges(int page,
                                                      List<GeometryRepository.AyahRegion> regions,
                                                      float[] edges,
                                                      int physicalStartRow) throws IOException {
        ArrayList<PageLine> lines = new ArrayList<>();
        for (int row = 0; row + 1 < edges.length; row++) {
            float top = edges[row];
            float bottom = edges[row + 1];
            if (!(bottom > top)) throw new IOException("Invalid KFQC row band on page " + page);
            float lineHeight = bottom - top;
            float left = Float.POSITIVE_INFINITY;
            float right = Float.NEGATIVE_INFINITY;
            LinkedHashSet<VerseRef> verseSet = new LinkedHashSet<>();

            for (GeometryRepository.AyahRegion region : regions) {
                boolean overlapsLine = false;
                float regionLeft = Float.POSITIVE_INFINITY;
                float regionRight = Float.NEGATIVE_INFINITY;
                for (GeometryRepository.Polygon polygon : region.getPolygons()) {
                    RectF b = polygon.getBounds();
                    float overlap = Math.min(bottom, b.bottom) - Math.max(top, b.top);
                    float threshold = Math.max(MIN_VERTICAL_OVERLAP,
                        Math.min(lineHeight, b.height()) * OVERLAP_FRACTION);
                    if (overlap >= threshold) {
                        overlapsLine = true;
                        regionLeft = Math.min(regionLeft, b.left);
                        regionRight = Math.max(regionRight, b.right);
                    }
                }
                if (overlapsLine) {
                    verseSet.add(new VerseRef(region.surah, region.ayah));
                    left = Math.min(left, regionLeft);
                    right = Math.max(right, regionRight);
                }
            }

            // Surah-name/basmallah/decorative rows have no ayah polygons. They are not counted as
            // Quran memorization lines, but physical row numbers are preserved for exact masking.
            if (verseSet.isEmpty()) continue;
            if (!Float.isFinite(left) || !Float.isFinite(right) || !(right > left)) {
                throw new IOException("Invalid Quran row horizontal extent on page " + page);
            }
            ArrayList<VerseRef> verses = new ArrayList<>(verseSet);
            verses.sort(Comparator.comparingInt(LineGeometryRepository::ordinal));
            lines.add(new PageLine(page, physicalStartRow + row,
                new RectF(left, top, right, bottom), verses));
        }
        return lines;
    }

    public List<PageLine> allLines() throws IOException {
        List<PageLine> local = allLines;
        if (local != null) return local;
        synchronized (this) {
            local = allLines;
            if (local == null) {
                ArrayList<PageLine> built = new ArrayList<>();
                for (int page = 1; page <= 604; page++) built.addAll(linesForPage(page));
                local = Collections.unmodifiableList(built);
                allLines = local;
            }
        }
        return local;
    }

    public int firstLineIndex(VerseRef verse) throws IOException {
        List<PageLine> lines = allLines();
        for (int i = 0; i < lines.size(); i++) if (lines.get(i).verses.contains(verse)) return i;
        throw new IllegalArgumentException("Verse absent from geometry: " + verse);
    }

    public int pageForVerse(VerseRef verse) throws IOException { return allLines().get(firstLineIndex(verse)).page; }

    public FiveLineBlock fiveLineBlock(int startGlobalIndex) throws IOException {
        List<PageLine> source = allLines();
        if (startGlobalIndex < 0 || startGlobalIndex + 4 >= source.size()) throw new IllegalArgumentException("Invalid five-line start");
        int end = startGlobalIndex + 4;
        ArrayList<PageLine> lines = new ArrayList<>(source.subList(startGlobalIndex, end + 1));
        LinkedHashSet<VerseRef> set = new LinkedHashSet<>();
        for (PageLine line : lines) set.addAll(line.verses);
        ArrayList<VerseRef> verses = new ArrayList<>(set);
        verses.sort(Comparator.comparingInt(LineGeometryRepository::ordinal));
        VerseRef first = verses.get(0), last = verses.get(verses.size() - 1);
        boolean startsInside = startGlobalIndex > 0 && source.get(startGlobalIndex - 1).verses.contains(first);
        boolean endsInside = end + 1 < source.size() && source.get(end + 1).verses.contains(last);
        return new FiveLineBlock(startGlobalIndex, end, first, last, startsInside, endsInside, lines, verses);
    }

    public VerseUnit eligiblePageUnit(VerseRef cursor, EligibleCorpus corpus) throws IOException {
        if (!corpus.contains(cursor)) throw new IllegalArgumentException("Itqan cursor outside eligible corpus: " + cursor);
        List<PageLine> all = allLines();
        int first = firstLineIndex(cursor);
        int page = all.get(first).page;
        int cursorOrdinal = ordinal(cursor);
        LinkedHashSet<VerseRef> verses = new LinkedHashSet<>();
        ArrayList<PageLine> usedLines = new ArrayList<>();
        for (int i = first; i < all.size() && all.get(i).page == page; i++) {
            PageLine line = all.get(i);
            boolean used = false;
            for (VerseRef verse : line.verses) {
                if (ordinal(verse) >= cursorOrdinal && corpus.contains(verse)) { verses.add(verse); used = true; }
            }
            if (used) usedLines.add(line);
        }
        ArrayList<VerseRef> ordered = new ArrayList<>(verses);
        ordered.sort(Comparator.comparingInt(LineGeometryRepository::ordinal));
        if (ordered.isEmpty()) throw new IllegalStateException("No eligible Itqan verses on page " + page);
        return new VerseUnit(page, cursor, ordered.get(ordered.size() - 1), ordered, usedLines);
    }

    public EligibleLinePlan planEligibleLines(VerseRef cursor, int requestedLines, EligibleCorpus corpus) throws IOException {
        if (requestedLines <= 0) throw new IllegalArgumentException("requestedLines must be positive");
        if (!corpus.contains(cursor)) throw new IllegalArgumentException("Murajaah cursor outside eligible corpus");
        List<PageLine> lines = allLines();
        int index = firstLineIndex(cursor);
        int counted = 0, guard = 0;
        VerseRef last = cursor;
        ArrayList<VerseRef> traversal = new ArrayList<>();
        while (counted < requestedLines) {
            if (guard++ > lines.size() * 20) throw new IllegalStateException("Eligible line planner made no progress");
            PageLine line = lines.get(index);
            boolean eligible = false;
            for (VerseRef verse : line.verses) {
                if (corpus.contains(verse) && (counted > 0 || ordinal(verse) >= ordinal(cursor))) {
                    eligible = true;
                    if (traversal.isEmpty() || !traversal.get(traversal.size() - 1).equals(verse)) traversal.add(verse);
                    last = verse;
                }
            }
            if (eligible) counted++;
            index = (index + 1) % lines.size();
        }
        return new EligibleLinePlan(cursor, last, requestedLines, traversal);
    }

    public List<PageLine> linesForVerseRange(VerseRef a, VerseRef b) throws IOException {
        int low = Math.min(ordinal(a), ordinal(b));
        int high = Math.max(ordinal(a), ordinal(b));
        ArrayList<PageLine> out = new ArrayList<>();
        for (PageLine line : allLines()) {
            for (VerseRef verse : line.verses) {
                int o = ordinal(verse);
                if (o >= low && o <= high) { out.add(line); break; }
            }
        }
        return out;
    }

    public List<VerseRef> versesForRange(VerseRef a, VerseRef b) {
        int low = Math.min(ordinal(a), ordinal(b));
        int high = Math.max(ordinal(a), ordinal(b));
        ArrayList<VerseRef> out = new ArrayList<>(high - low + 1);
        for (int i = low; i <= high; i++) out.add(QuranCanon.INSTANCE.fromOrdinal(i));
        return out;
    }

    public VerseRef previous(VerseRef verse) { return QuranCanon.INSTANCE.previous(verse); }
    public VerseRef next(VerseRef verse) { return QuranCanon.INSTANCE.next(verse); }
    public static int ordinal(VerseRef verse) { return QuranCanon.INSTANCE.ordinal(verse); }
}

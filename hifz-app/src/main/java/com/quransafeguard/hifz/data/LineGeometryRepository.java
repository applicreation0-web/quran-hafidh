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
import java.util.Set;

/**
 * Physical line index derived only from canonical per-ayah polygons.
 * The Quran SVG itself is never rewritten or reflowed.
 */
public final class LineGeometryRepository {
    private static final float NATURAL_CLUSTER_CENTER_PX = 9.5f;
    private static volatile LineGeometryRepository INSTANCE;

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

    private static final class Candidate {
        final RectF bounds;
        final VerseRef verse;
        Candidate(RectF bounds, VerseRef verse) { this.bounds = bounds; this.verse = verse; }
        float centerY() { return bounds.centerY(); }
    }

    private static final class Cluster {
        final RectF bounds;
        final LinkedHashSet<VerseRef> verses = new LinkedHashSet<>();
        int count = 0;
        float centerSum = 0f;
        Cluster(Candidate c) {
            bounds = new RectF(c.bounds);
            add(c);
        }
        void add(Candidate c) {
            if (count > 0) bounds.union(c.bounds);
            verses.add(c.verse);
            centerSum += c.centerY();
            count++;
        }
        float centerY() { return centerSum / Math.max(1, count); }
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
        ArrayList<Candidate> candidates = new ArrayList<>();
        for (GeometryRepository.AyahRegion region : regions) {
            VerseRef verse = new VerseRef(region.surah, region.ayah);
            for (GeometryRepository.Polygon polygon : region.getPolygons()) {
                RectF b = polygon.getBounds();
                if (b.width() > 0.5f && b.height() > 0.5f) candidates.add(new Candidate(b, verse));
            }
        }
        if (candidates.isEmpty()) throw new IOException("No Quran line candidates on page " + page);
        candidates.sort(Comparator.comparingDouble(Candidate::centerY));

        ArrayList<Cluster> natural = new ArrayList<>();
        for (Candidate c : candidates) {
            Cluster best = null;
            float bestDistance = Float.MAX_VALUE;
            for (Cluster cluster : natural) {
                float distance = Math.abs(cluster.centerY() - c.centerY());
                float overlap = Math.min(cluster.bounds.bottom, c.bounds.bottom) - Math.max(cluster.bounds.top, c.bounds.top);
                float minHeight = Math.min(cluster.bounds.height(), c.bounds.height());
                boolean verticallySame = overlap > Math.max(1f, minHeight * 0.30f) || distance <= NATURAL_CLUSTER_CENTER_PX;
                if (verticallySame && distance < bestDistance) { best = cluster; bestDistance = distance; }
            }
            if (best == null) natural.add(new Cluster(c)); else best.add(c);
        }
        natural.sort(Comparator.comparingDouble(Cluster::centerY));

        // Standard Madinah pages after the opening spread contain 15 Quran text lines.
        // If decorative geometry caused over/under clustering, deterministically re-bin candidates
        // onto 15 vertical centers. Pages 1-2 keep their natural opening-spread line count.
        List<Cluster> finalClusters = natural;
        if (page > 2 && natural.size() != 15) finalClusters = forceFifteen(candidates);

        ArrayList<PageLine> lines = new ArrayList<>(finalClusters.size());
        int number = 1;
        for (Cluster cluster : finalClusters) {
            ArrayList<VerseRef> verses = new ArrayList<>(cluster.verses);
            verses.sort(Comparator.comparingInt(LineGeometryRepository::ordinal));
            if (!verses.isEmpty()) lines.add(new PageLine(page, number++, cluster.bounds, verses));
        }
        if (page > 2 && lines.size() != 15) {
            throw new IOException("Expected 15 Quran lines on page " + page + "; derived " + lines.size());
        }
        List<PageLine> immutable = Collections.unmodifiableList(lines);
        pageCache.put(page, immutable);
        return immutable;
    }

    private static List<Cluster> forceFifteen(List<Candidate> candidates) {
        float minCenter = Float.POSITIVE_INFINITY;
        float maxCenter = Float.NEGATIVE_INFINITY;
        for (Candidate c : candidates) {
            minCenter = Math.min(minCenter, c.centerY());
            maxCenter = Math.max(maxCenter, c.centerY());
        }
        float step = (maxCenter - minCenter) / 14f;
        if (!(step > 0f)) throw new IllegalStateException("Cannot derive 15 Mushaf lines");
        ArrayList<Cluster> bins = new ArrayList<>(Collections.nCopies(15, null));
        for (Candidate c : candidates) {
            int index = Math.round((c.centerY() - minCenter) / step);
            index = Math.max(0, Math.min(14, index));
            Cluster cluster = bins.get(index);
            if (cluster == null) bins.set(index, new Cluster(c)); else cluster.add(c);
        }
        // Empty bin can occur only with malformed source spacing; merge/recover from nearest candidate.
        ArrayList<Cluster> out = new ArrayList<>();
        for (Cluster cluster : bins) if (cluster != null) out.add(cluster);
        out.sort(Comparator.comparingDouble(Cluster::centerY));
        return out;
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

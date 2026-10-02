package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.EligibleCorpus;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only index over the frozen V2.1 semantic corpus.
 *
 * The semantic JSON is never repaired or normalized at runtime. Any schema/hash/completeness
 * mismatch disables this optional feature while the rest of Quran Haafidh keeps its prior behavior.
 * Exact visual geometry is deliberately NOT inferred from reader109 line cells: those cells are
 * source-ink groups, not linguistic words.
 */
final class SemanticPassageRepository {
    static final String ASSET_PATH = "semantic/semantic_passages_v2_1.json";
    static final String EXPECTED_SHA256 =
        "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7";
    static final int EXPECTED_GLOBAL_PASSAGES = 1256;
    static final int EXPECTED_PAGE_RECORDS = 1644;

    static final class CellRange {
        final String lineId;
        final int fromCell;
        final int toCell;

        CellRange(String lineId, int fromCell, int toCell) {
            this.lineId = lineId;
            this.fromCell = fromCell;
            this.toCell = toCell;
        }
    }

    static final class Cue {
        final String passageId;
        final int page;
        final int indexOnPage;
        final String title;
        final String anchorArabic;
        final int anchorWordCount;
        final VerseRef startVerse;
        final VerseRef endVerse;
        final int startPage;
        final int endPage;
        final int startLine;
        final int firstWordId;
        final int lastWordId;
        final int firstWordPosition;
        final int lastWordPosition;
        final boolean anchorOnCurrentPage;
        final List<CellRange> visualRanges;

        Cue(String passageId, int page, int indexOnPage, String title, String anchorArabic,
            int anchorWordCount, VerseRef startVerse, VerseRef endVerse, int startPage, int endPage,
            int startLine, int firstWordId, int lastWordId, int firstWordPosition,
            int lastWordPosition, boolean anchorOnCurrentPage, List<CellRange> visualRanges) {
            this.passageId = passageId;
            this.page = page;
            this.indexOnPage = indexOnPage;
            this.title = title;
            this.anchorArabic = anchorArabic;
            this.anchorWordCount = anchorWordCount;
            this.startVerse = startVerse;
            this.endVerse = endVerse;
            this.startPage = startPage;
            this.endPage = endPage;
            this.startLine = startLine;
            this.firstWordId = firstWordId;
            this.lastWordId = lastWordId;
            this.firstWordPosition = firstWordPosition;
            this.lastWordPosition = lastWordPosition;
            this.anchorOnCurrentPage = anchorOnCurrentPage;
            this.visualRanges = Collections.unmodifiableList(new ArrayList<>(visualRanges));
        }

        boolean hasExactVisualRange() {
            return !visualRanges.isEmpty();
        }
    }

    private static final class PassageMeta {
        final VerseRef start;
        final VerseRef end;
        final int startPage;
        final int endPage;

        PassageMeta(VerseRef start, VerseRef end, int startPage, int endPage) {
            this.start = start;
            this.end = end;
            this.startPage = startPage;
            this.endPage = endPage;
        }
    }

    private final Map<Integer, List<Cue>> byPage = new HashMap<>();
    private final Map<String, Cue> byId = new HashMap<>();
    private final List<Cue> orderedCues = new ArrayList<>();
    private final boolean available;

    SemanticPassageRepository(Context context) {
        boolean loaded = false;
        try {
            byte[] raw = readAsset(context, ASSET_PATH);
            if (!EXPECTED_SHA256.equals(sha256(raw))) {
                throw new IllegalStateException("semantic V2.1 SHA-256 mismatch");
            }
            parseInto(new String(raw, StandardCharsets.UTF_8), byPage, byId);
            orderedCues.addAll(byId.values());
            orderedCues.sort((a, b) -> a.startVerse.compareTo(b.startVerse));
            loaded = byPage.size() == 604 && byId.size() == EXPECTED_GLOBAL_PASSAGES
                && orderedCues.size() == EXPECTED_GLOBAL_PASSAGES;
        } catch (Throwable unavailableAsset) {
            // Fail closed: semantic cues are an optional enhancement, never a reader dependency.
            byPage.clear();
            byId.clear();
            orderedCues.clear();
        }
        available = loaded;
    }

    boolean isAvailable() {
        return available;
    }

    List<Cue> cuesForPage(int page) {
        List<Cue> cues = byPage.get(page);
        return cues == null ? Collections.emptyList() : cues;
    }

    Cue cue(String passageId) {
        return passageId == null ? null : byId.get(passageId);
    }

    /** Passage containing a verse, using only the frozen audited V2.1 verse boundaries. */
    Cue cueContaining(VerseRef verse) {
        if (verse == null) return null;
        for (Cue cue : orderedCues) {
            if (verse.compareTo(cue.startVerse) < 0) return null;
            if (verse.compareTo(cue.endVerse) <= 0) return cue;
        }
        return null;
    }

    /** First complete semantic passage usable from this acquired-corpus cursor, with canonical wrap. */
    Cue firstEligibleCueAtOrContaining(VerseRef cursor, EligibleCorpus corpus) {
        if (cursor == null || corpus == null || orderedCues.isEmpty()) return null;
        Cue containing = cueContaining(cursor);
        if (fullyEligible(containing, corpus)) return containing;

        int start = 0;
        while (start < orderedCues.size()
                && orderedCues.get(start).startVerse.compareTo(cursor) < 0) start++;
        for (int offset = 0; offset < orderedCues.size(); offset++) {
            Cue candidate = orderedCues.get((start + offset) % orderedCues.size());
            if (fullyEligible(candidate, corpus)) return candidate;
        }
        return null;
    }

    /** Next complete acquired passage after the current semantic unit, wrapping canonically. */
    Cue nextEligibleCue(Cue current, EligibleCorpus corpus) {
        if (current == null || corpus == null || orderedCues.isEmpty()) return null;
        int index = -1;
        for (int i = 0; i < orderedCues.size(); i++) {
            if (orderedCues.get(i).passageId.equals(current.passageId)) {
                index = i;
                break;
            }
        }
        if (index < 0) return firstEligibleCueAtOrContaining(current.endVerse, corpus);
        for (int offset = 1; offset <= orderedCues.size(); offset++) {
            Cue candidate = orderedCues.get((index + offset) % orderedCues.size());
            if (fullyEligible(candidate, corpus)) return candidate;
        }
        return null;
    }

    private static boolean fullyEligible(Cue cue, EligibleCorpus corpus) {
        return cue != null && corpus.contains(cue.startVerse) && corpus.contains(cue.endVerse);
    }

    /**
     * Exact blind-recall mode is enabled only when every anchor actually starting on this page has
     * separately verified visual geometry. V2.1 itself contains no word geometry, so importing only
     * the frozen corpus intentionally keeps this false and preserves the proven legacy landmarks.
     */
    boolean hasCompleteExactGeometryForPage(int page) {
        boolean found = false;
        for (Cue cue : cuesForPage(page)) {
            if (!cue.anchorOnCurrentPage) continue;
            found = true;
            if (!cue.hasExactVisualRange()) return false;
        }
        return found;
    }

    JSONArray readerCuesForPage(int page) {
        try {
            JSONArray out = new JSONArray();
            for (Cue cue : cuesForPage(page)) {
                if (!cue.anchorOnCurrentPage) continue;
                JSONObject item = new JSONObject();
                item.put("id", cue.passageId);
                item.put("index", cue.indexOnPage);
                item.put("title", cue.title);
                item.put("anchor", cue.anchorArabic);
                item.put("anchorWordCount", cue.anchorWordCount);
                item.put("startLine", cue.startLine);
                JSONArray ranges = new JSONArray();
                for (CellRange range : cue.visualRanges) {
                    ranges.put(new JSONObject()
                        .put("lineId", range.lineId)
                        .put("fromCell", range.fromCell)
                        .put("toCell", range.toCell));
                }
                item.put("ranges", ranges);
                out.put(item);
            }
            return out;
        } catch (JSONException invalidReaderPayload) {
            // Parsed corpus values are already validated. If JSON serialization still fails,
            // fail closed instead of leaking a partial/approximate cue payload to the reader.
            throw new IllegalStateException("semantic cue serialization failed", invalidReaderPayload);
        }
    }

    private static void parseInto(String raw, Map<Integer, List<Cue>> byPage, Map<String, Cue> byId) throws JSONException {
        JSONObject root = new JSONObject(raw);
        require("V2.1".equals(root.optString("schema_version", "")), "wrong semantic schema");
        require(!root.optBoolean("boundaries_changed", true), "semantic boundaries changed");
        require(root.optInt("global_passage_count", 0) == EXPECTED_GLOBAL_PASSAGES,
            "wrong global passage count");

        JSONArray globals = root.optJSONArray("global_passages");
        JSONArray records = root.optJSONArray("page_passage_records");
        require(globals != null && globals.length() == EXPECTED_GLOBAL_PASSAGES,
            "global_passages incomplete");
        require(records != null && records.length() == EXPECTED_PAGE_RECORDS,
            "page_passage_records incomplete");

        Set<String> globalIds = new HashSet<>();
        Map<String, PassageMeta> globalMeta = new HashMap<>();
        for (int i = 0; i < globals.length(); i++) {
            JSONObject row = globals.getJSONObject(i);
            String id = requiredText(row, "passage_global_id");
            require(globalIds.add(id), "duplicate global passage id " + id);
            requiredAuditedFields(row);
            VerseRef startVerse = new VerseRef(positiveInt(row, "surah_start"), positiveInt(row, "ayah_start"));
            VerseRef endVerse = new VerseRef(positiveInt(row, "surah_end"), positiveInt(row, "ayah_end"));
            require(startVerse.compareTo(endVerse) <= 0, "invalid global verse span for " + id);
            int startPage = positiveInt(row, "starts_on_page");
            int endPage = positiveInt(row, "ends_on_page");
            require(startPage <= endPage && endPage <= 604, "invalid global page span for " + id);
            globalMeta.put(id, new PassageMeta(startVerse, endVerse, startPage, endPage));
        }

        Set<Integer> pages = new HashSet<>();
        Set<String> recordIds = new HashSet<>();
        Map<String, Integer> onPageCounts = new HashMap<>();

        for (int i = 0; i < records.length(); i++) {
            JSONObject row = records.getJSONObject(i);
            int page = row.optInt("page", 0);
            require(page >= 1 && page <= 604, "page outside Mushaf");
            pages.add(page);

            String id = requiredText(row, "passage_global_id");
            require(globalIds.contains(id), "unknown passage id " + id);
            recordIds.add(id);
            int index = row.optInt("passage_index_on_page", 0);
            require(index >= 1, "missing passage_index_on_page for " + id);

            int startLine = row.optInt("start_line", 0);
            int endLine = row.optInt("end_line", 0);
            require(startLine >= 1 && endLine >= startLine, "invalid line span for " + id);

            String title = requiredText(row, "title_fr_v2_1");
            String anchor = requiredText(row, "anchor_arabic_v2_1");
            PassageMeta meta = globalMeta.get(id);
            require(meta != null, "missing global passage metadata for " + id);
            int anchorWordCount = row.optInt("anchor_word_count_v2_1", 0);
            require(anchorWordCount >= 1, "invalid audited anchor length for " + id);
            require("AUDITED_V2_1".equals(requiredText(row, "minimality_verified_v2_1")),
                "anchor is not audited V2.1 for " + id);

            // These are source locator fields only. They are retained for future exact geometry
            // joins but never converted into line-cell positions by arithmetic or inference.
            int firstWordId = positiveInt(row, "first_word_id");
            int lastWordId = positiveInt(row, "last_word_id");
            int firstWordPosition = positiveInt(row, "first_word_position");
            int lastWordPosition = positiveInt(row, "last_word_position");
            boolean anchorOnPage = yes(row.opt("anchor_is_on_current_page"));
            if (anchorOnPage) {
                onPageCounts.put(id, onPageCounts.getOrDefault(id, 0) + 1);
            }

            // The frozen V2.1 JSON has no visual_ranges. Keeping this empty is intentional:
            // reader109 cells are ink groups, not words, so manufacturing ranges here would create
            // false precision and leak semantic help into blind recall.
            List<CellRange> ranges = Collections.emptyList();

            Cue cue = new Cue(id, page, index, title, anchor, anchorWordCount,
                meta.start, meta.end, meta.startPage, meta.endPage, startLine,
                firstWordId, lastWordId, firstWordPosition, lastWordPosition, anchorOnPage, ranges);
            byPage.computeIfAbsent(page, ignored -> new ArrayList<>()).add(cue);
            Cue previous = byId.get(id);
            if (previous == null || (!previous.anchorOnCurrentPage && anchorOnPage)) byId.put(id, cue);
        }

        require(pages.size() == 604, "semantic page coverage incomplete");
        require(recordIds.equals(globalIds), "page records do not cover global passages");
        require(byId.size() == EXPECTED_GLOBAL_PASSAGES, "semantic id index incomplete");
        require(onPageCounts.size() == EXPECTED_GLOBAL_PASSAGES,
            "not every passage has an on-page anchor occurrence");
        for (String id : globalIds) {
            require(onPageCounts.getOrDefault(id, 0) == 1,
                "passage must have exactly one on-page anchor occurrence: " + id);
        }

        for (Map.Entry<Integer, List<Cue>> entry : byPage.entrySet()) {
            entry.getValue().sort((a, b) -> Integer.compare(a.indexOnPage, b.indexOnPage));
            entry.setValue(Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
    }

    private static void requiredAuditedFields(JSONObject row) {
        requiredText(row, "title_fr_v2_1");
        requiredText(row, "anchor_arabic_v2_1");
        require(row.optInt("anchor_word_count_v2_1", 0) >= 1, "invalid audited anchor length");
        require("AUDITED_V2_1".equals(requiredText(row, "minimality_verified_v2_1")),
            "global anchor is not audited V2.1");
    }

    private static String requiredText(JSONObject row, String key) {
        String value = row.optString(key, "").trim();
        require(!value.isEmpty(), "missing " + key);
        return value;
    }

    private static int positiveInt(JSONObject row, String key) {
        int value = row.optInt(key, 0);
        require(value >= 1, "invalid " + key);
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static boolean yes(Object raw) {
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        String value = raw == null ? "" : raw.toString().trim();
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static byte[] readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] data) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return hex.toString();
    }
}

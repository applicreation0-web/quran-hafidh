package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only semantic passage index.
 *
 * The app deliberately treats this corpus as optional data: if the audited V2.1 asset is absent,
 * malformed, or incomplete, every existing Quran/Hifz flow keeps working exactly as before.
 * Passage boundaries are never inferred at runtime.
 */
final class SemanticPassageRepository {
    static final String ASSET_PATH = "semantic/semantic_passages_v2_1.json";

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
        final int startLine;
        final boolean anchorOnCurrentPage;
        final List<CellRange> visualRanges;

        Cue(String passageId, int page, int indexOnPage, String title, String anchorArabic,
            int startLine, boolean anchorOnCurrentPage, List<CellRange> visualRanges) {
            this.passageId = passageId;
            this.page = page;
            this.indexOnPage = indexOnPage;
            this.title = title;
            this.anchorArabic = anchorArabic;
            this.startLine = startLine;
            this.anchorOnCurrentPage = anchorOnCurrentPage;
            this.visualRanges = Collections.unmodifiableList(new ArrayList<>(visualRanges));
        }

        boolean hasExactVisualRange() {
            return !visualRanges.isEmpty();
        }
    }

    private final Map<Integer, List<Cue>> byPage = new HashMap<>();
    private final Map<String, Cue> byId = new HashMap<>();
    private final boolean available;

    SemanticPassageRepository(Context context) {
        boolean loaded = false;
        try {
            String raw = readAsset(context, ASSET_PATH);
            parseInto(raw, byPage, byId);
            loaded = !byPage.isEmpty();
        } catch (Throwable unavailableAsset) {
            // Fail closed: semantic cues are an optional enhancement, never a reader dependency.
            byPage.clear();
            byId.clear();
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

    /**
     * Exact blind-recall mode is enabled only when every anchor actually starting on this page has
     * audited visual cell ranges. A page with no on-page anchor, or only line-level metadata,
     * deliberately falls back to the existing first/last-line landmarks.
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
        JSONArray out = new JSONArray();
        for (Cue cue : cuesForPage(page)) {
            if (!cue.anchorOnCurrentPage) continue;
            JSONObject item = new JSONObject();
            item.put("id", cue.passageId);
            item.put("index", cue.indexOnPage);
            item.put("title", cue.title);
            item.put("anchor", cue.anchorArabic);
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
    }

    private static void parseInto(String raw, Map<Integer, List<Cue>> byPage, Map<String, Cue> byId) {
        JSONObject root = new JSONObject(raw);
        JSONArray records = root.optJSONArray("page_passage_records");
        if (records == null) throw new IllegalStateException("semantic passage records missing");

        for (int i = 0; i < records.length(); i++) {
            JSONObject row = records.getJSONObject(i);
            int page = row.optInt("page", 0);
            if (page < 1 || page > 604) continue;
            String id = row.optString("passage_global_id", "").trim();
            if (id.isEmpty()) continue;
            boolean anchorOnPage = yes(row.opt("anchor_is_on_current_page"));
            int index = Math.max(1, row.optInt("passage_index_on_page", 1));
            int startLine = Math.max(1, row.optInt("start_line", 1));
            String title = row.optString("title_fr", "").trim();
            String anchor = row.optString("anchor_arabic", "").trim();

            ArrayList<CellRange> ranges = new ArrayList<>();
            JSONArray geometry = row.optJSONArray("visual_ranges");
            if (geometry == null) geometry = row.optJSONArray("anchor_visual_ranges");
            if (geometry != null) {
                for (int j = 0; j < geometry.length(); j++) {
                    JSONObject range = geometry.optJSONObject(j);
                    if (range == null) continue;
                    String lineId = range.optString("line_id", range.optString("lineId", "")).trim();
                    int from = range.has("from_cell") ? range.optInt("from_cell", -1) : range.optInt("fromCell", -1);
                    int to = range.has("to_cell") ? range.optInt("to_cell", -1) : range.optInt("toCell", -1);
                    if (!lineId.isEmpty() && from >= 0 && to > from) ranges.add(new CellRange(lineId, from, to));
                }
            }

            Cue cue = new Cue(id, page, index, title, anchor, startLine, anchorOnPage, ranges);
            byPage.computeIfAbsent(page, ignored -> new ArrayList<>()).add(cue);
            // Prefer the occurrence that actually contains the anchor for title lookup.
            Cue previous = byId.get(id);
            if (previous == null || (!previous.anchorOnCurrentPage && anchorOnPage)) byId.put(id, cue);
        }

        for (Map.Entry<Integer, List<Cue>> entry : byPage.entrySet()) {
            entry.getValue().sort((a, b) -> Integer.compare(a.indexOnPage, b.indexOnPage));
            entry.setValue(Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
    }

    private static boolean yes(Object raw) {
        if (raw instanceof Boolean) return (Boolean) raw;
        if (raw instanceof Number) return ((Number) raw).intValue() != 0;
        String value = raw == null ? "" : raw.toString().trim();
        return "YES".equalsIgnoreCase(value) || "TRUE".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String readAsset(Context context, String path) throws Exception {
        try (InputStream input = context.getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}

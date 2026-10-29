package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.content.SharedPreferences;
import com.quransafeguard.hifz.core.VerseRef;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Persistent, local and isolated correction phase for Révision active. No Hifz counters,
 * corpus boundaries, Sabqi/Itqān credits or final receipts are written here.
 *
 * The frozen end verse is preserved while the user rereads pages; a process death
 * cannot turn the correction page into the next active-review cursor.
 */
final class ActiveReviewCorrectionStore {
    private static final String PREFS = "quran_hifz_preview_v1";
    private static final String KEY_REVEALED = "activeReviewRevealedPagesV1";
    private static final String KEY_STATE = "activeReviewCorrectionV1";
    private final SharedPreferences prefs;

    ActiveReviewCorrectionStore(Context context) {
        this(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE));
    }
    ActiveReviewCorrectionStore(SharedPreferences prefs) {
        if (prefs == null) throw new IllegalArgumentException("Missing preference store");
        this.prefs = prefs;
    }

    static final class State {
        final String date;
        final VerseRef start;
        final VerseRef end;
        final long activeElapsedMs;
        final List<ActiveReviewCorrectionPlan.Page> pages;
        final int pageIndex;
        final int verseIndex;
        final String stage; // READ, RECALL, DONE
        final boolean assisted;

        State(String date, VerseRef start, VerseRef end, long elapsed,
              List<ActiveReviewCorrectionPlan.Page> pages, int pageIndex,
              int verseIndex, String stage, boolean assisted) {
            this.date = date;
            this.start = start;
            this.end = end;
            this.activeElapsedMs = elapsed;
            this.pages = Collections.unmodifiableList(new ArrayList<>(pages));
            this.pageIndex = pageIndex;
            this.verseIndex = verseIndex;
            this.stage = stage;
            this.assisted = assisted;
            if (date == null || start == null || end == null || elapsed < 0
                    || !date.equals(LocalDate.parse(date).toString())
                    || pages.isEmpty() || pageIndex < 0 || pageIndex >= pages.size()
                    || (!"READ".equals(stage) && !"RECALL".equals(stage) && !"DONE".equals(stage))
                    || verseIndex < 0 || verseIndex >= pages.get(pageIndex).flagged.size()
                    || pages.size() > 604) throw new IllegalArgumentException("Invalid correction checkpoint");
        }
        ActiveReviewCorrectionPlan.Page page() { return pages.get(pageIndex); }
        VerseRef verse() { return page().flagged.get(verseIndex); }
        State move(int page, int verse, String nextStage, boolean nextAssisted) {
            return new State(date, start, end, activeElapsedMs,
                pages, page, verse, nextStage, nextAssisted);
        }
    }

    State pending() {
        String raw = prefs.getString(KEY_STATE, "");
        if (raw == null || raw.isEmpty()) return null;
        try {
            JSONObject doc = new JSONObject(raw);
            JSONArray rows = doc.getJSONArray("pages");
            if (rows.length() == 0 || rows.length() > 604)
                throw new IllegalArgumentException("Unexpected corrective page count");
            List<ActiveReviewCorrectionPlan.Page> pages = new ArrayList<>();
            Set<Integer> uniquePages = new LinkedHashSet<>();
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                int page = row.getInt("page");
                if (page < 1 || page > 604 || !uniquePages.add(page))
                    throw new IllegalArgumentException("Duplicated / invalid corrective page");
                JSONArray marked = row.getJSONArray("flagged");
                if (marked.length() < 1 || marked.length() > 300)
                    throw new IllegalArgumentException("Invalid flagged count");
                List<VerseRef> verses = new ArrayList<>();
                Set<VerseRef> seen = new LinkedHashSet<>();
                for (int j = 0; j < marked.length(); j++) {
                    VerseRef verse = GeometryRepository.parseVerse(marked.getString(j));
                    if (!seen.add(verse)) throw new IllegalArgumentException("Repeated marked verse");
                    verses.add(verse);
                }
                pages.add(new ActiveReviewCorrectionPlan.Page(page, verses));
            }
            return new State(doc.getString("date"),
                GeometryRepository.parseVerse(doc.getString("start")),
                GeometryRepository.parseVerse(doc.getString("end")),
                doc.getLong("activeElapsedMs"), pages,
                doc.getInt("pageIndex"), doc.getInt("verseIndex"),
                doc.getString("stage"), doc.optBoolean("assisted", false));
        } catch (Exception invalid) {
            // Fail closed. Never delete potentially recoverable progress after a malformed checkpoint.
            throw new IllegalStateException("État de correction à vérifier ; données conservées.", invalid);
        }
    }

    boolean begin(String date, VerseRef start, VerseRef end, long elapsed,
                  List<ActiveReviewCorrectionPlan.Page> pages) {
        if (pending() != null) return false; // no accidental overwrite of a frozen session
        if (pages == null || pages.isEmpty()) return false;
        return save(new State(date, start, end, elapsed, pages, 0, 0, "READ", false));
    }

    boolean pageReread(State state) {
        if (state == null || !"READ".equals(state.stage)) return false;
        return save(state.move(state.pageIndex, 0, "RECALL", false));
    }

    boolean revealDuringControl(State state) {
        if (state == null || !"RECALL".equals(state.stage)) return false;
        return save(state.move(state.pageIndex, state.verseIndex, "RECALL", true));
    }

    boolean nextTarget(State state) {
        if (state == null || !"RECALL".equals(state.stage)) return false;
        if (state.verseIndex + 1 < state.page().flagged.size())
            return save(state.move(state.pageIndex, state.verseIndex + 1, "RECALL", false));
        if (state.pageIndex + 1 < state.pages.size())
            return save(state.move(state.pageIndex + 1, 0, "READ", false));
        return save(state.move(state.pageIndex, state.verseIndex, "DONE", false));
    }

    private boolean save(State state) {
        try {
            JSONObject doc = new JSONObject();
            doc.put("date", state.date);
            doc.put("start", state.start.toString());
            doc.put("end", state.end.toString());
            doc.put("activeElapsedMs", state.activeElapsedMs);
            doc.put("pageIndex", state.pageIndex);
            doc.put("verseIndex", state.verseIndex);
            doc.put("stage", state.stage);
            doc.put("assisted", state.assisted);
            JSONArray rows = new JSONArray();
            for (ActiveReviewCorrectionPlan.Page page : state.pages) {
                JSONObject row = new JSONObject();
                row.put("page", page.page);
                JSONArray marked = new JSONArray();
                for (VerseRef verse : page.flagged) marked.put(verse.toString());
                row.put("flagged", marked);
                rows.put(row);
            }
            doc.put("pages", rows);
            return prefs.edit().putString(KEY_STATE, doc.toString()).commit();
        } catch (Exception invalid) {
            return false;
        }
    }

    Set<Integer> revealed(String date, VerseRef start) {
        try {
            String raw = prefs.getString(KEY_REVEALED, "");
            if (raw == null || raw.isEmpty()) return Collections.emptySet();
            JSONObject state = new JSONObject(raw);
            if (!date.equals(state.optString("date"))
                    || !start.toString().equals(state.optString("start"))) return Collections.emptySet();
            Set<Integer> result = new LinkedHashSet<>();
            JSONArray pages = state.getJSONArray("pages");
            for (int i = 0; i < pages.length(); i++) {
                int page = pages.getInt(i);
                if (page >= 1 && page <= 604) result.add(page);
            }
            return result;
        } catch (Exception invalid) {
            throw new IllegalStateException("Revealed-page checkpoint requires review", invalid);
        }
    }

    boolean recordReveal(String date, VerseRef start, int page) {
        if (date == null || start == null || page < 1 || page > 604) return false;
        try {
            Set<Integer> pages = new LinkedHashSet<>(revealed(date, start));
            pages.add(page);
            JSONObject row = new JSONObject();
            row.put("date", date);
            row.put("start", start.toString());
            JSONArray array = new JSONArray();
            for (Integer value : pages) array.put(value);
            row.put("pages", array);
            return prefs.edit().putString(KEY_REVEALED, row.toString()).commit();
        } catch (Exception invalid) {
            return false;
        }
    }

    boolean clear() {
        return prefs.edit().remove(KEY_STATE).remove(KEY_REVEALED).commit();
    }
}

package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exact Quran-word geometry derived from quran-ws/quran-svg-elements v1.1.2.
 *
 * Boxes use the same 0 0 345 550 page coordinate space as the shipped Madinah SVG reader.
 * This repository never infers word positions from ink cells: missing or malformed data fails closed.
 */
final class WordGeometryRepository implements QuizCorpus.WordCounts {
    private static final String SOURCE_RELEASE = "v1.1.2";
    private static final String SCHEMA = "quran-haafidh-word-boxes-v1";
    private static final String ASSET_DIR = "reader109/word-boxes/";
    private static final String[] CHUNKS = {
        "word-boxes-001-150.json",
        "word-boxes-151-300.json",
        "word-boxes-301-450.json",
        "word-boxes-451-604.json"
    };

    static final class WordBox {
        final String key;
        final int surah;
        final int ayah;
        final int word;
        final double x0;
        final double y0;
        final double x1;
        final double y1;

        WordBox(String key, double x0, double y0, double x1, double y1) {
            this.key = key;
            String[] parts = key.split(":");
            if (parts.length != 3) throw new IllegalArgumentException("invalid Quran word key");
            this.surah = Integer.parseInt(parts[0]);
            this.ayah = Integer.parseInt(parts[1]);
            this.word = Integer.parseInt(parts[2]);
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
        }

        JSONArray boxJson() {
            JSONArray out = new JSONArray();
            try {
                out.put(x0).put(y0).put(x1).put(y1);
            } catch (JSONException invalidNumber) {
                throw new IllegalStateException("invalid Quran word box", invalidNumber);
            }
            return out;
        }
    }

    private final Context context;
    private final Map<Integer, List<WordBox>> pages = new HashMap<>();
    private final Set<Integer> loadedChunks = new HashSet<>();
    private boolean failed;

    private static volatile WordGeometryRepository shared;

    /** One parsed sidecar per process (the four ~750 KB chunks are parsed at most once). */
    static WordGeometryRepository shared(Context context) {
        WordGeometryRepository local = shared;
        if (local != null) return local;
        synchronized (WordGeometryRepository.class) {
            if (shared == null) shared = new WordGeometryRepository(context);
            return shared;
        }
    }

    /** Parses all four chunks off the UI thread. */
    void preloadAll() {
        for (int page : new int[]{1, 151, 301, 451}) wordsForPage(page);
    }

    WordGeometryRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Every exact word box on the page, in canonical order (eraser glyph-extent cover). */
    synchronized JSONArray pageBoxes(int page) {
        JSONArray out = new JSONArray();
        for (WordBox word : wordsForPage(page)) out.put(word.boxJson());
        return out;
    }

    synchronized List<WordBox> wordsForPage(int page) {
        if (failed || page < 1 || page > 604) return Collections.emptyList();
        int chunk = page <= 150 ? 0 : page <= 300 ? 1 : page <= 450 ? 2 : 3;
        if (!loadedChunks.contains(chunk)) {
            try {
                loadChunk(chunk);
                loadedChunks.add(chunk);
            } catch (Throwable invalidGeometry) {
                failed = true;
                pages.clear();
                return Collections.emptyList();
            }
        }
        List<WordBox> words = pages.get(page);
        return words == null ? Collections.emptyList() : words;
    }

    synchronized JSONArray boxesForVerse(int page, VerseRef verse) {
        JSONArray out = new JSONArray();
        if (verse == null) return out;
        for (WordBox word : wordsForPage(page)) {
            if (word.surah == verse.getSurah() && word.ayah == verse.getAyah()) out.put(word.boxJson());
        }
        return out;
    }

    @Override public synchronized int wordCountForVerse(int page, VerseRef verse) {
        if (verse == null) return 0;
        int count = 0;
        for (WordBox word : wordsForPage(page)) {
            if (word.surah == verse.getSurah() && word.ayah == verse.getAyah()) count++;
        }
        return count;
    }

    /**
     * Exact boxes of an Al-Munīr amorce: the wordCount real Quran words that follow, in canonical
     * order on this page, from word 1 of its start verse — continuing into the next verse(s) when
     * the amorce is longer than its start verse (e.g. "الٓمٓ ذَٰلِكَ ٱلْكِتَـٰبُ" = 2:1 + 2:2). The
     * sidecar is verified canonical and gap-free (verify_quran_word_boxes.py); an amorce running
     * past the page end returns empty (fail closed), never an estimate.
     */
    synchronized JSONArray anchorBoxes(int page, VerseRef start, int wordCount) {
        return amorceBoxes(wordsForPage(page), start, wordCount);
    }

    static JSONArray amorceBoxes(List<WordBox> pageWords, VerseRef start, int wordCount) {
        JSONArray out = new JSONArray();
        if (pageWords == null || start == null || wordCount < 1) return out;
        int at = -1;
        for (int i = 0; i < pageWords.size(); i++) {
            WordBox word = pageWords.get(i);
            if (word.surah == start.getSurah() && word.ayah == start.getAyah() && word.word == 1) { at = i; break; }
        }
        if (at < 0 || at + wordCount > pageWords.size()) return out;
        for (int i = at; i < at + wordCount; i++) out.put(pageWords.get(i).boxJson());
        return out;
    }

    /** Révision active page landmarks: the page's first three and last three real words. */
    synchronized JSONArray pageLandmarkBoxes(int page) {
        List<WordBox> words = wordsForPage(page);
        if (words.size() < 6) return new JSONArray();
        JSONArray out = new JSONArray();
        for (int i = 0; i < 3; i++) out.put(words.get(i).boxJson());
        for (int i = words.size() - 3; i < words.size(); i++) out.put(words.get(i).boxJson());
        return out;
    }

    private void loadChunk(int chunk) throws Exception {
        byte[] raw = readAsset(ASSET_DIR + CHUNKS[chunk]);
        pages.putAll(parseChunk(new String(raw, StandardCharsets.UTF_8)));
    }

    /** Fail-closed parse of one audited quran-ws v1.1.2 chunk; shared with the JVM tests. */
    static Map<Integer, List<WordBox>> parseChunk(String json) throws Exception {
        JSONObject root = new JSONObject(json);
        if (!SCHEMA.equals(root.optString("schema", ""))) throw new IllegalStateException("word geometry schema");
        if (!SOURCE_RELEASE.equals(root.optString("source_release", ""))) throw new IllegalStateException("word geometry source");
        if (!"viewBox 0 0 345 550".equals(root.optString("box_space", "")))
            throw new IllegalStateException("word geometry coordinate space");

        HashMap<Integer, List<WordBox>> out = new HashMap<>();
        JSONObject pageMap = root.getJSONObject("pages");
        java.util.Iterator<String> keys = pageMap.keys();
        while (keys.hasNext()) {
            String pageKey = keys.next();
            int page = Integer.parseInt(pageKey);
            JSONArray rows = pageMap.getJSONArray(pageKey);
            ArrayList<WordBox> words = new ArrayList<>(rows.length());
            for (int i = 0; i < rows.length(); i++) {
                JSONArray row = rows.getJSONArray(i);
                if (row.length() != 5) throw new IllegalStateException("word geometry row");
                double x0 = row.getDouble(1), y0 = row.getDouble(2);
                double x1 = row.getDouble(3), y1 = row.getDouble(4);
                if (x0 < 0 || y0 < 0 || x1 > 345 || y1 > 550 || x1 <= x0 || y1 <= y0)
                    throw new IllegalStateException("word geometry box");
                words.add(new WordBox(row.getString(0), x0, y0, x1, y1));
            }
            if (words.isEmpty()) throw new IllegalStateException("empty Quran page words");
            out.put(page, Collections.unmodifiableList(words));
        }
        return out;
    }

    /** Word count of one verse on one page from already-parsed pages (JVM tests). */
    static int wordCount(Map<Integer, List<WordBox>> parsed, int page, VerseRef verse) {
        int count = 0;
        List<WordBox> words = parsed.get(page);
        if (words == null || verse == null) return 0;
        for (WordBox word : words) if (word.surah == verse.getSurah() && word.ayah == verse.getAyah()) count++;
        return count;
    }

    private byte[] readAsset(String path) throws Exception {
        try (InputStream in = context.getAssets().open(path);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) out.write(buffer, 0, read);
            return out.toByteArray();
        }
    }
}

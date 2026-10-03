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
final class WordGeometryRepository {
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

    WordGeometryRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    synchronized boolean isPageAvailable(int page) {
        return !wordsForPage(page).isEmpty();
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

    synchronized JSONArray anchorBoxes(int page, VerseRef start, int wordCount) {
        if (start == null || wordCount < 1) return new JSONArray();
        List<WordBox> words = wordsForPage(page);
        if (words.isEmpty()) return new JSONArray();

        int at = -1;
        for (int i = 0; i < words.size(); i++) {
            WordBox word = words.get(i);
            if (word.surah == start.getSurah() && word.ayah == start.getAyah() && word.word == 1) {
                at = i;
                break;
            }
        }
        if (at < 0 || at + wordCount > words.size()) return new JSONArray();

        JSONArray out = new JSONArray();
        for (int i = 0; i < wordCount; i++) out.put(words.get(at + i).boxJson());
        return out;
    }

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
        JSONObject root = new JSONObject(new String(raw, StandardCharsets.UTF_8));
        if (!SCHEMA.equals(root.optString("schema", ""))) throw new IllegalStateException("word geometry schema");
        if (!SOURCE_RELEASE.equals(root.optString("source_release", ""))) throw new IllegalStateException("word geometry source");
        if (!"viewBox 0 0 345 550".equals(root.optString("box_space", "")))
            throw new IllegalStateException("word geometry coordinate space");

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
            pages.put(page, Collections.unmodifiableList(words));
        }
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

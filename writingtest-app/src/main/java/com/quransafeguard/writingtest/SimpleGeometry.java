package com.quransafeguard.writingtest;

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
import java.util.List;

/**
 * Minimal read-only index over reader109/geometry.json for this test app: just enough to
 * navigate every physical Mushaf line in the whole corpus and read its page/band/verses/viewBox.
 * Unlike hifz-app's own GeometryRepository, this deliberately carries none of the Sabqi/Itqan/
 * Stabilization scheduling logic — this app only ever needs "the next/previous line to test".
 */
final class SimpleGeometry {
    static final class LineMeta {
        final String id;
        final int page;
        final int lineIndexOnPage;
        final double top;
        final double bottom;
        final List<VerseRef> verses;

        LineMeta(String id, int page, int lineIndexOnPage, double top, double bottom, List<VerseRef> verses) {
            this.id = id;
            this.page = page;
            this.lineIndexOnPage = lineIndexOnPage;
            this.top = top;
            this.bottom = bottom;
            this.verses = Collections.unmodifiableList(verses);
        }
    }

    private static volatile SimpleGeometry INSTANCE;
    private final List<LineMeta> lines;
    private final JSONObject pages;

    private SimpleGeometry(Context context) throws Exception {
        JSONObject root = new JSONObject(readAsset(context, "reader109/geometry.json"));
        pages = root.getJSONObject("pages");
        ArrayList<LineMeta> result = new ArrayList<>();
        for (int page = 1; page <= 604; page++) {
            JSONObject pageObject = pages.getJSONObject(Integer.toString(page));
            JSONArray pageLines = pageObject.getJSONArray("lines");
            for (int i = 0; i < pageLines.length(); i++) {
                JSONObject line = pageLines.getJSONObject(i);
                JSONArray refs = line.getJSONArray("verses");
                ArrayList<VerseRef> verses = new ArrayList<>();
                for (int j = 0; j < refs.length(); j++) {
                    String[] parts = refs.getString(j).split(":");
                    VerseRef ref = new VerseRef(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                    if (!verses.contains(ref)) verses.add(ref);
                }
                result.add(new LineMeta(line.getString("id"), page, i, line.getDouble("top"), line.getDouble("bottom"), verses));
            }
        }
        if (result.isEmpty()) throw new IllegalStateException("Empty Mushaf geometry");
        lines = Collections.unmodifiableList(result);
    }

    static SimpleGeometry get(Context context) {
        SimpleGeometry local = INSTANCE;
        if (local != null) return local;
        synchronized (SimpleGeometry.class) {
            local = INSTANCE;
            if (local == null) {
                try {
                    local = new SimpleGeometry(context.getApplicationContext());
                } catch (Exception error) {
                    throw new IllegalStateException("Mushaf geometry unavailable", error);
                }
                INSTANCE = local;
            }
            return local;
        }
    }

    int lineCount() { return lines.size(); }

    LineMeta line(int index) { return lines.get(index); }

    float[] viewBoxForPage(int page) {
        JSONObject pageObject = pages.optJSONObject(Integer.toString(page));
        if (pageObject == null) throw new IllegalStateException("geometry missing for page " + page);
        try {
            JSONArray box = pageObject.getJSONArray("viewBox");
            return new float[] { (float) box.getDouble(0), (float) box.getDouble(1), (float) box.getDouble(2), (float) box.getDouble(3) };
        } catch (JSONException error) {
            throw new IllegalStateException("malformed viewBox for page " + page, error);
        }
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

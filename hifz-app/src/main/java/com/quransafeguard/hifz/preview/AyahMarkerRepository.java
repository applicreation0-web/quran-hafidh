package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the per-page ayah-end marker positions generated at build time by
 * scripts/generate_hifz_ayah_markers.py (assets/ayahmarkers/NNN.json) — the real
 * verse-end rosette position for every verse on the page, taken directly from the
 * source SVG's own ayah:x/ayah:y attributes (already in geometry.json's page-space
 * coordinates). Used by the writing exercise to keep showing the real verse-end
 * signs, exactly like the printed Mushaf, while the user writes from memory.
 *
 * Pages are parsed lazily and cached, since eagerly loading all 604 files would
 * waste memory for a session that only ever visits a handful of pages.
 */
public final class AyahMarkerRepository {
    private final Context context;
    private final Map<Integer, float[][]> cache = new HashMap<>();

    public AyahMarkerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** markersForPage(page)[markerIndex] = {x, y}, in reading order (top to bottom, right to left). */
    public synchronized float[][] markersForPage(int page) {
        float[][] cached = cache.get(page);
        if (cached != null) return cached;
        float[][] loaded = load(page);
        cache.put(page, loaded);
        return loaded;
    }

    private float[][] load(int page) {
        String path = String.format(Locale.ROOT, "ayahmarkers/%03d.json", page);
        try (InputStream in = context.getAssets().open(path)) {
            JSONArray markers = new JSONArray(readAll(in));
            float[][] result = new float[markers.length()][];
            for (int i = 0; i < markers.length(); i++) {
                JSONArray xy = markers.getJSONArray(i);
                result[i] = new float[] { (float) xy.getDouble(0), (float) xy.getDouble(1) };
            }
            return result;
        } catch (Exception e) {
            return new float[0][];
        }
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[64 * 1024];
        int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }
}

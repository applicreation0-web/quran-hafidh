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
 * Loads the per-page word-shape geometry generated at build time by
 * scripts/generate_hifz_word_shapes.py (assets/wordshapes/NNN.json) — the real
 * ink shape of each word/cell on the Mushaf page, derived from the same KFQC
 * SVG glyphs the reader itself renders. Used by the writing exercise to check
 * whether the user's handwriting covers the real letterform (Palier 2).
 *
 * Pages are parsed lazily and cached, since eagerly loading all 604 files
 * would waste memory for a session that only ever visits a handful of pages.
 */
public final class WordShapeRepository {
    private final Context context;
    private final Map<Integer, float[][][][]> cache = new HashMap<>();

    public WordShapeRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** shapesForPage(page)[lineIndex][cellIndex][subpathIndex] = flat [x0,y0,x1,y1,...]. */
    public synchronized float[][][][] shapesForPage(int page) {
        float[][][][] cached = cache.get(page);
        if (cached != null) return cached;
        float[][][][] loaded = load(page);
        cache.put(page, loaded);
        return loaded;
    }

    /** The shape for one specific cell, or an empty array if the page/line/cell is out of range. */
    public float[][] cellShape(int page, int lineIndex, int cellIndex) {
        float[][][][] pageData = shapesForPage(page);
        if (lineIndex < 0 || lineIndex >= pageData.length) return new float[0][];
        float[][][] line = pageData[lineIndex];
        if (cellIndex < 0 || cellIndex >= line.length) return new float[0][];
        return line[cellIndex];
    }

    private float[][][][] load(int page) {
        String path = String.format(Locale.ROOT, "wordshapes/%03d.json", page);
        try (InputStream in = context.getAssets().open(path)) {
            JSONArray lines = new JSONArray(readAll(in));
            float[][][][] result = new float[lines.length()][][][];
            for (int li = 0; li < lines.length(); li++) {
                JSONArray cells = lines.getJSONArray(li);
                float[][][] cellArr = new float[cells.length()][][];
                for (int ci = 0; ci < cells.length(); ci++) {
                    JSONArray subpaths = cells.getJSONArray(ci);
                    float[][] subpathArr = new float[subpaths.length()][];
                    for (int si = 0; si < subpaths.length(); si++) {
                        JSONArray points = subpaths.getJSONArray(si);
                        float[] flat = new float[points.length() * 2];
                        for (int pi = 0; pi < points.length(); pi++) {
                            JSONArray xy = points.getJSONArray(pi);
                            flat[pi * 2] = (float) xy.getDouble(0);
                            flat[pi * 2 + 1] = (float) xy.getDouble(1);
                        }
                        subpathArr[si] = flat;
                    }
                    cellArr[ci] = subpathArr;
                }
                result[li] = cellArr;
            }
            return result;
        } catch (Exception e) {
            return new float[0][][][];
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

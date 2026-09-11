package com.quransafeguard.hifz.data;

import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Read-only ayah geometry bundled from the exact same pinned quran-svg source as the Mushaf.
 * Coordinates stay in the SVG document coordinate space; the renderer owns screen transforms.
 */
public final class GeometryRepository {
    public static final String ASSET_DIR = "geometry/hafs/kfqc";

    private final Context appContext;

    public GeometryRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public InputStream openPage(int page) throws IOException {
        validatePage(page);
        return appContext.getAssets().open(String.format(Locale.ROOT, "%s/%03d.json", ASSET_DIR, page));
    }

    public boolean isBundled(int page) {
        try (InputStream ignored = openPage(page)) {
            return true;
        } catch (IOException | IllegalArgumentException missing) {
            return false;
        }
    }

    public List<AyahRegion> loadPage(int page) throws IOException {
        validatePage(page);
        final String json;
        try (InputStream in = openPage(page)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            json = out.toString(StandardCharsets.UTF_8.name());
        }

        try {
            JSONArray array = new JSONArray(json);
            List<AyahRegion> regions = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                int surah = item.getInt("surahNumber");
                int ayah = item.getInt("ayahNumber");
                boolean continuation = item.optBoolean("continuation", false);
                Float markerX = item.isNull("x") ? null : (float) item.getDouble("x");
                Float markerY = item.isNull("y") ? null : (float) item.getDouble("y");
                List<Polygon> polygons = parsePolygon(item.getString("polygon"));
                if (polygons.isEmpty()) {
                    throw new IOException("Empty ayah polygon on page " + page + " for " + surah + ":" + ayah);
                }
                regions.add(new AyahRegion(page, surah, ayah, markerX, markerY, continuation, polygons));
            }
            return Collections.unmodifiableList(regions);
        } catch (JSONException | IllegalArgumentException malformed) {
            throw new IOException("Invalid Mushaf geometry on page " + page, malformed);
        }
    }

    /** Supports both the legacy comma-pair polygon format and the current SVG M/L/Z path format. */
    static List<Polygon> parsePolygon(String raw) {
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();

        String normalized = raw.replace(',', ' ').trim();
        String[] tokens = normalized.split("\\s+");
        boolean commandFormat = false;
        for (String token : tokens) {
            if ("M".equalsIgnoreCase(token) || "L".equalsIgnoreCase(token) || "Z".equalsIgnoreCase(token)) {
                commandFormat = true;
                break;
            }
        }

        List<Polygon> result = new ArrayList<>();
        if (!commandFormat) {
            if ((tokens.length & 1) != 0) {
                throw new IllegalArgumentException("Odd number of polygon coordinates");
            }
            List<PointF> points = new ArrayList<>(tokens.length / 2);
            for (int i = 0; i < tokens.length; i += 2) {
                points.add(new PointF(Float.parseFloat(tokens[i]), Float.parseFloat(tokens[i + 1])));
            }
            if (points.size() >= 3) result.add(new Polygon(points));
            return result;
        }

        List<PointF> current = null;
        int i = 0;
        while (i < tokens.length) {
            String token = tokens[i++];
            if ("M".equalsIgnoreCase(token)) {
                if (current != null && current.size() >= 3) result.add(new Polygon(current));
                current = new ArrayList<>();
                if (i + 1 >= tokens.length) throw new IllegalArgumentException("Incomplete M command");
                current.add(new PointF(Float.parseFloat(tokens[i++]), Float.parseFloat(tokens[i++])));
            } else if ("L".equalsIgnoreCase(token)) {
                if (current == null) throw new IllegalArgumentException("L before M");
                if (i + 1 >= tokens.length) throw new IllegalArgumentException("Incomplete L command");
                current.add(new PointF(Float.parseFloat(tokens[i++]), Float.parseFloat(tokens[i++])));
            } else if ("Z".equalsIgnoreCase(token)) {
                if (current != null && current.size() >= 3) {
                    result.add(new Polygon(current));
                    current = null;
                }
            } else {
                throw new IllegalArgumentException("Unsupported polygon command: " + token);
            }
        }
        if (current != null && current.size() >= 3) result.add(new Polygon(current));
        return result;
    }

    private static void validatePage(int page) {
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604");
        }
    }

    public static final class AyahRegion {
        public final int page;
        public final int surah;
        public final int ayah;
        public final Float markerX;
        public final Float markerY;
        public final boolean continuation;
        private final List<Polygon> polygons;
        private final RectF bounds;

        AyahRegion(int page, int surah, int ayah, Float markerX, Float markerY,
                   boolean continuation, List<Polygon> polygons) {
            this.page = page;
            this.surah = surah;
            this.ayah = ayah;
            this.markerX = markerX;
            this.markerY = markerY;
            this.continuation = continuation;
            this.polygons = Collections.unmodifiableList(new ArrayList<>(polygons));
            RectF combined = new RectF(polygons.get(0).bounds);
            for (int i = 1; i < polygons.size(); i++) combined.union(polygons.get(i).bounds);
            this.bounds = combined;
        }

        public List<Polygon> getPolygons() {
            return polygons;
        }

        public RectF getBounds() {
            return new RectF(bounds);
        }

        public boolean contains(float documentX, float documentY) {
            if (!bounds.contains(documentX, documentY)) return false;
            for (Polygon polygon : polygons) {
                if (polygon.contains(documentX, documentY)) return true;
            }
            return false;
        }

        public String verseKey() {
            return surah + ":" + ayah;
        }
    }

    public static final class Polygon {
        private final List<PointF> points;
        private final RectF bounds;

        Polygon(List<PointF> source) {
            if (source.size() < 3) throw new IllegalArgumentException("Polygon needs at least 3 points");
            List<PointF> copy = new ArrayList<>(source.size());
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            for (PointF point : source) {
                PointF p = new PointF(point.x, point.y);
                copy.add(p);
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
            points = Collections.unmodifiableList(copy);
            bounds = new RectF(minX, minY, maxX, maxY);
        }

        public List<PointF> getPoints() {
            List<PointF> copy = new ArrayList<>(points.size());
            for (PointF point : points) copy.add(new PointF(point.x, point.y));
            return Collections.unmodifiableList(copy);
        }

        public RectF getBounds() {
            return new RectF(bounds);
        }

        public boolean contains(float x, float y) {
            if (!bounds.contains(x, y)) return false;
            boolean inside = false;
            int n = points.size();
            for (int i = 0, j = n - 1; i < n; j = i++) {
                PointF pi = points.get(i);
                PointF pj = points.get(j);
                boolean crosses = ((pi.y > y) != (pj.y > y))
                    && (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x);
                if (crosses) inside = !inside;
            }
            return inside;
        }
    }
}

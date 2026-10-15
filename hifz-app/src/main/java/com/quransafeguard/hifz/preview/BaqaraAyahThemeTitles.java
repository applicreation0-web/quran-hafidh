package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Private English navigation-title pack. The public repository NEVER holds
 * the Ayah Themes corpus or Mawdudi's translated commentary.
 *
 * Private Android asset: assets/mawdudi_baqara_private/ayah_themes.json
 * {
 *   "surah": 2, "source": "Ayah Themes",
 *   "passages": [{"start":1,"end":5,"title":"..."}]
 * }
 */
final class BaqaraAyahThemeTitles {
    static final String ASSET = "mawdudi_baqara_private/ayah_themes.json";

    static final class Passage {
        final int first, last;
        final String title;
        final boolean verified;
        Passage(int first, int last, String title, boolean verified) {
            this.first = first; this.last = last; this.title = title; this.verified = verified;
        }
        boolean contains(int ayah) { return first <= ayah && last >= ayah; }
        boolean intersects(int a, int b) { return first <= b && last >= a; }
        String range() {
            return first == last ? "2:" + first : "2:" + first + "–" + last;
        }
    }

    private BaqaraAyahThemeTitles() {}

    /** Fail closed when a private file is absent or invalid: NEVER substitute Ibn Kathir titles. */
    static List<Passage> load(Context context) {
        ArrayList<Passage> passages = new ArrayList<>();
        try (InputStream in = context.getAssets().open(ASSET);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read, size = 0;
            while ((read = in.read(buffer)) != -1) {
                size += read;
                if (size > 160000) throw new IllegalStateException("Unexpected title-pack size");
                out.write(buffer, 0, read);
            }
            JSONObject pack = new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            if (pack.getInt("surah") != 2 || !"Ayah Themes".equals(pack.getString("source")))
                throw new IllegalStateException("Wrong English navigation corpus");
            JSONArray rows = pack.getJSONArray("passages");
            if (rows.length() < 140 || rows.length() > 160)
                throw new IllegalStateException("Unexpected Baqarah passage count");
            int expectedStart = 1;
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                int first = row.getInt("start"), last = row.getInt("end");
                String title = row.getString("title").trim();
                if (first < expectedStart || last < first || last > 286
                        || title.isEmpty() || title.length() > 350)
                    throw new IllegalStateException("Invalid title or overlapping ayat at " + first);
                if (first > expectedStart)
                    passages.add(new Passage(expectedStart, first - 1,
                        "Title under source verification", false));
                passages.add(new Passage(first, last, title, true));
                expectedStart = last + 1;
            }
            if (expectedStart <= 286)
                passages.add(new Passage(expectedStart, 286,
                    "Title under source verification", false));
            if (passages.isEmpty() || passages.get(0).first != 1
                    || passages.get(passages.size() - 1).last != 286)
                throw new IllegalStateException("Incomplete range");
            return Collections.unmodifiableList(passages);
        } catch (Exception invalidOrMissing) {
            return Collections.singletonList(new Passage(1, 286,
                "PRIVATE AYAH THEMES TITLES NOT INSTALLED — NOT A USER DEBUG", false));
        }
    }
}

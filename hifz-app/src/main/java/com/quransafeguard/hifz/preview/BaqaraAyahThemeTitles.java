package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.text.Html;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PRIVATE data for phone pilot. Never commit or bundle the third-party
 * Ayah Themes editorial index to the public repository / public CI artifact.
 *
 * Priority: user-provided local private asset; then private internal offline
 * cache; otherwise the phone fetches the original public page on demand.
 * Never use an Ibn Kathir title to fill an Ayah Themes gap.
 */
final class BaqaraAyahThemeTitles {
    static final String ASSET = "mawdudi_baqara_private/ayah_themes.json";
    static final String CACHE = "baqara-ayah-themes-cache.json";
    static final String SOURCE_URL =
        "https://islamicstudies.info/reference/reference.php?sura=2";
    private static final int LIMIT = 160000;

    static final class Passage {
        final int first, last;
        final String title;
        final boolean verified;
        Passage(int first, int last, String title, boolean verified) {
            this.first = first; this.last = last; this.title = title; this.verified = verified;
        }
        boolean contains(int ayah) { return first <= ayah && last >= ayah; }
        String range() {
            return first == last ? "2:" + first : "2:" + first + "–" + last;
        }
    }

    private BaqaraAyahThemeTitles() {}

    static boolean isMissing(List<Passage> data) {
        return data.size() == 1 && !data.get(0).verified;
    }

    /** Called on UI thread; this never fetches remotely. */
    static List<Passage> load(Context context) {
        try (InputStream in = context.getAssets().open(ASSET)) {
            return parse(read(in));
        } catch (Exception notPackaged) {
            File cache = new File(context.getFilesDir(), CACHE);
            if (cache.isFile()) {
                try (InputStream in = new FileInputStream(cache)) {
                    return parse(read(in));
                } catch (Exception invalidCache) { /* no fabricated fallback */ }
            }
            return Collections.singletonList(new Passage(1, 286,
                "English Ayah Themes titles: connect to load, or install private pack", false));
        }
    }

    /** Fetch only on a worker thread; retains source attribution and local private cache. */
    static List<Passage> fetchAndCache(Context context) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(SOURCE_URL).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setRequestProperty("Accept", "text/html");
        try {
            if (connection.getResponseCode() != 200)
                throw new IllegalStateException("Ayah Themes source unavailable");
            String html;
            try (InputStream in = connection.getInputStream()) {
                html = read(in);
            }
            Pattern p = Pattern.compile("<p>\\s*(\\d+)\\s*-\\s*(\\d+)\\s+([^<]+)</p>",
                Pattern.CASE_INSENSITIVE);
            Matcher match = p.matcher(html);
            JSONArray rows = new JSONArray();
            while (match.find()) {
                int a = Integer.parseInt(match.group(1)), b = Integer.parseInt(match.group(2));
                String raw = Html.fromHtml(match.group(3), Html.FROM_HTML_MODE_LEGACY)
                    .toString().replaceAll("\\s+", " ").trim();
                rows.put(new JSONObject().put("start", a).put("end", b).put("title", raw));
            }
            JSONObject obj = new JSONObject().put("surah", 2)
                .put("source", "Ayah Themes").put("passages", rows);
            String packed = obj.toString();
            List<Passage> checked = parse(packed);
            File file = new File(context.getFilesDir(), CACHE);
            try (FileOutputStream stream = new FileOutputStream(file)) {
                stream.write(packed.getBytes(StandardCharsets.UTF_8));
            }
            return checked;
        } finally {
            connection.disconnect();
        }
    }

    private static String read(InputStream in) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count, total = 0;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > LIMIT) throw new IllegalStateException("Unexpected source size");
                out.write(buffer, 0, count);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static List<Passage> parse(String text) throws Exception {
        JSONObject pack = new JSONObject(text);
        if (pack.getInt("surah") != 2 || !"Ayah Themes".equals(pack.getString("source")))
            throw new IllegalStateException("Wrong navigation index");
        JSONArray rows = pack.getJSONArray("passages");
        if (rows.length() < 140 || rows.length() > 160)
            throw new IllegalStateException("Unexpected source coverage");
        ArrayList<Passage> checked = new ArrayList<>();
        int expected = 1;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            int a = row.getInt("start"), b = row.getInt("end");
            String label = row.getString("title").trim();
            if (a < expected || b < a || b > 286 || label.isEmpty() || label.length() > 350)
                throw new IllegalStateException("Overlapping/broken source titles at " + a);
            if (a > expected)
                checked.add(new Passage(expected, a - 1,
                    "Title under source verification", false));
            checked.add(new Passage(a, b, label, true));
            expected = b + 1;
        }
        if (expected <= 286)
            checked.add(new Passage(expected, 286, "Title under source verification", false));
        if (checked.get(0).first != 1 || checked.get(checked.size() - 1).last != 286)
            throw new IllegalStateException("Broken full-surah coverage");
        return Collections.unmodifiableList(checked);
    }
}

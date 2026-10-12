package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * PRIVATE Quran.com English Ibn Kathir h2 audit corpus only.
 *
 * This reader never uses spa5k source-heading candidates as displayed titles.
 * The personal Quran.com corpus is supplied OUTSIDE this PUBLIC repository
 * under assets/ibn_kathir_qurancom_private/chunk-NNN-NNN.json. Missing,
 * unreviewed, corrupt, or unbundled content fails closed: NO TITLE.
 *
 * The dataset is an annotation attached to the immutable 1903 documentary
 * groups, not a source for cue coordinates, Mushaf text, masks or Hifz state.
 *
 * Audit status and Quran Foundation offline-content rights MUST be settled
 * before APK distribution. Do not commit the private dataset to GitHub.
 */
final class IbnKathirQuranComPrivateTitles {
    private static final String SCHEMA = "IK_QURANCOM_PRIVATE_V1";
    private static final Map<Integer, JSONObject> CACHE = new HashMap<>();
    private static final Map<Integer, Boolean> FAILED = new HashMap<>();

    private IbnKathirQuranComPrivateTitles() { }

    static String[] sourceHeadings(Context context, IbnKathirGroupIndex.Group group) {
        if (context == null || group == null || group.surah < 1 || group.surah > 114)
            return new String[0];
        int chunk = (group.surah - 1) / 15;
        JSONObject groups = load(context.getApplicationContext(), chunk);
        if (groups == null) return new String[0];
        JSONArray source = groups.optJSONArray(group.id);
        if (source == null || source.length() == 0 || source.length() > 24)
            return new String[0];
        String[] exact = new String[source.length()];
        for (int i = 0; i < exact.length; i++) {
            String title = source.optString(i, "");
            // Never silently rewrite or truncate Quran.com English titles.
            if (title.isEmpty() || title.length() > 145 ||
                    !title.equals(title.trim()) || title.indexOf('\n') >= 0)
                return new String[0];
            exact[i] = title;
        }
        return exact;
    }

    /**
     * Display the thematic/source heading first, not a chapter preface.
     * All original Quran.com h2 strings remain in their original array and
     * subsequent taps visit every heading, including introductions/virtues.
     * This changes only navigation order, NEVER wording or group boundaries.
     */
    static int firstThematicIndex(String[] headings) {
        if (headings == null || headings.length == 0) return 0;
        for (int i = 0; i < headings.length; i++) {
            String title = headings[i];
            if (title == null || title.trim().isEmpty()) continue;
            String lower = title.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("which was revealed in ") ||
                    lower.startsWith("introduction to ") ||
                    lower.startsWith("the virtues of ") ||
                    lower.startsWith("virtues of ") ||
                    lower.startsWith("the beginning of the ") ||
                    lower.startsWith("the first surah in which ") ||
                    ((lower.startsWith("surat ") || lower.startsWith("surah "))
                     && lower.contains(" was revealed in ")))
                continue;
            return i;
        }
        return 0;
    }

    /**
     * Source-safe initial offsets established by private review. The public
     * code stores only documentary group IDs and array positions: never title
     * text. Every source heading remains reachable in its original order.
     */
    static int preferredFirstTitleIndex(String groupId, String[] headings) {
        if (headings == null || headings.length == 0) return 0;
        int preferred;
        if ("IKEN011_001_004".equals(groupId) ||
                "IKEN018_001_005".equals(groupId)) {
            preferred = 2;
        } else if ("IKEN020_001_008".equals(groupId) ||
                "IKEN079_001_014".equals(groupId)) {
            preferred = 1;
        } else {
            return firstThematicIndex(headings);
        }
        return preferred < headings.length ? preferred : 0;
    }

    private static synchronized JSONObject load(Context context, int chunk) {
        if (CACHE.containsKey(chunk)) return CACHE.get(chunk);
        if (Boolean.TRUE.equals(FAILED.get(chunk))) return null;
        int first = chunk * 15 + 1;
        int last = Math.min(114, first + 14);
        String file = String.format(java.util.Locale.ROOT,
                "ibn_kathir_qurancom_private/chunk-%03d-%03d.json", first, last);
        try (InputStream input = context.getAssets().open(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
                if (output.size() > 400_000)
                    throw new IllegalStateException("Oversized private source data");
            }
            JSONObject root = new JSONObject(new String(
                    output.toByteArray(), StandardCharsets.UTF_8));
            if (!SCHEMA.equals(root.getString("schema")) ||
                    root.getInt("source_resource_id") != 169)
                throw new IllegalStateException("Wrong Quran.com source");
            JSONArray range = root.getJSONArray("range");
            if (range.length() != 2 || range.getInt(0) != first ||
                    range.getInt(1) != last)
                throw new IllegalStateException("Private title chunk range mismatch");
            JSONObject groups = root.getJSONObject("groups");
            CACHE.put(chunk, groups);
            return groups;
        } catch (Exception invalid) {
            // No reading/session crash; a missing title is silent.
            FAILED.put(chunk, true);
            return null;
        }
    }
}

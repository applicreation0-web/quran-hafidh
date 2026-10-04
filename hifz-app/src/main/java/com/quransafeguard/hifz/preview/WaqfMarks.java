package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Waqf (pause-mark) placements derived from Tanzil's Uthmani text (see scripts/build_waqf_marks.py
 * for provenance and a known gap: the لا/mamnu' mark is absent from this source). Best-effort
 * community data used only as a placement signal — it never changes the Quran text itself, which
 * this app renders unmodified from the shipped KFQC SVG corpus.
 */
final class WaqfMarks {
    static final class Mark {
        final int afterWord;
        final String type;

        Mark(int afterWord, String type) {
            this.afterWord = afterWord;
            this.type = type;
        }
    }

    private static volatile WaqfMarks INSTANCE;
    private final Map<String, List<Mark>> marks;

    private WaqfMarks(Context context) throws Exception {
        JSONObject root = new JSONObject(readAsset(context, "reader109/waqf.json"));
        JSONObject entries = root.getJSONObject("marks");
        Map<String, List<Mark>> parsed = new HashMap<>();
        for (java.util.Iterator<String> it = entries.keys(); it.hasNext(); ) {
            String verseKey = it.next();
            JSONArray array = entries.getJSONArray(verseKey);
            ArrayList<Mark> verseMarks = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject mark = array.getJSONObject(i);
                verseMarks.add(new Mark(mark.getInt("afterWord"), mark.getString("type")));
            }
            parsed.put(verseKey, Collections.unmodifiableList(verseMarks));
        }
        marks = Collections.unmodifiableMap(parsed);
    }

    static WaqfMarks get(Context context) {
        WaqfMarks local = INSTANCE;
        if (local != null) return local;
        synchronized (WaqfMarks.class) {
            local = INSTANCE;
            if (local == null) {
                try {
                    local = new WaqfMarks(context.getApplicationContext());
                } catch (Exception error) {
                    throw new IllegalStateException("Waqf mark data unavailable", error);
                }
                INSTANCE = local;
            }
            return local;
        }
    }

    /** Every waqf mark recorded for this verse, in word order; empty when none is recorded. */
    List<Mark> marksFor(VerseRef verse) {
        List<Mark> found = marks.get(verse.getSurah() + ":" + verse.getAyah());
        return found == null ? Collections.emptyList() : found;
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

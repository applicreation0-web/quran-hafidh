package com.quransafeguard.hifz.preview;

import android.content.Context;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Plain Arabic verse text derived from Tanzil's Uthmani text (see scripts/build_verse_text.py
 * for provenance — the same source used for waqf.json). Used only for content verification
 * (InkContentVerifier comparing recognized handwriting to the real word); the Mushaf page itself
 * is always rendered unmodified from the shipped KFQC SVG corpus, never from this text.
 */
final class VerseText {
    private static volatile VerseText INSTANCE;
    private final JSONObject verses;

    private VerseText(Context context) throws Exception {
        JSONObject root = new JSONObject(readAsset(context, "reader109/verses_text.json"));
        verses = root.getJSONObject("verses");
    }

    static VerseText get(Context context) {
        VerseText local = INSTANCE;
        if (local != null) return local;
        synchronized (VerseText.class) {
            local = INSTANCE;
            if (local == null) {
                try {
                    local = new VerseText(context.getApplicationContext());
                } catch (Exception error) {
                    throw new IllegalStateException("Verse text data unavailable", error);
                }
                INSTANCE = local;
            }
            return local;
        }
    }

    /** The verse's plain Arabic text, or null if this verse isn't in the corpus. */
    String textFor(VerseRef verse) {
        String key = verse.getSurah() + ":" + verse.getAyah();
        return verses.has(key) ? verses.optString(key, null) : null;
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

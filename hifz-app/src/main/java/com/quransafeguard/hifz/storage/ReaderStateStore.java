package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.data.MushafRepository;

/** Reader-only persistence. It deliberately does not contain Sabqi/Itqan/Murajaah policy. */
public final class ReaderStateStore {
    private static final String NAME = "quran_hifz_reader_state";
    private static final int SCHEMA = 1;
    private static final String KEY_SCHEMA = "schema";
    private static final String KEY_LAST_PAGE = "last_page";
    private static final String KEY_TAFSIR_TEXT_SP = "tafsir_text_sp";

    private final SharedPreferences prefs;

    public ReaderStateStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        int existing = prefs.getInt(KEY_SCHEMA, SCHEMA);
        if (existing != SCHEMA) {
            throw new IllegalStateException("Unsupported Quran Hifz reader schema: " + existing);
        }
        if (!prefs.contains(KEY_SCHEMA)) prefs.edit().putInt(KEY_SCHEMA, SCHEMA).apply();
    }

    public int lastPage() {
        int page = prefs.getInt(KEY_LAST_PAGE, MushafRepository.FIRST_PAGE);
        return Math.max(MushafRepository.FIRST_PAGE, Math.min(MushafRepository.LAST_PAGE, page));
    }

    public void saveLastPage(int page) {
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604");
        }
        prefs.edit().putInt(KEY_LAST_PAGE, page).apply();
    }

    public float tafsirTextSp() {
        return Math.max(14f, Math.min(30f, prefs.getFloat(KEY_TAFSIR_TEXT_SP, 18f)));
    }

    public void saveTafsirTextSp(float sp) {
        float clamped = Math.max(14f, Math.min(30f, sp));
        prefs.edit().putFloat(KEY_TAFSIR_TEXT_SP, clamped).apply();
    }

    public void clear() {
        prefs.edit().clear().putInt(KEY_SCHEMA, SCHEMA).apply();
    }
}

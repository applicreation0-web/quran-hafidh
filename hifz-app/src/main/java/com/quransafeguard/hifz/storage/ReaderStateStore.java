package com.quransafeguard.hifz.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.quransafeguard.hifz.data.MushafRepository;

/** Reader-only persistence. Deliberately separate from Hifz progress/state. */
public final class ReaderStateStore {
    private static final String NAME = "quran_hifz_reader_state";
    private static final String KEY_PAGE = "last_page";

    private final SharedPreferences prefs;

    public ReaderStateStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public int loadPage() {
        int page = prefs.getInt(KEY_PAGE, MushafRepository.FIRST_PAGE);
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            return MushafRepository.FIRST_PAGE;
        }
        return page;
    }

    public void savePage(int page) {
        if (page < MushafRepository.FIRST_PAGE || page > MushafRepository.LAST_PAGE) {
            throw new IllegalArgumentException("Mushaf page must be 1..604");
        }
        prefs.edit().putInt(KEY_PAGE, page).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}

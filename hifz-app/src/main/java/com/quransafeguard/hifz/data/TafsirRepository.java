package com.quransafeguard.hifz.data;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/** Local-only Tafsir asset boundary. No URL or network fallback is permitted. */
public final class TafsirRepository {
    private static final String ROOT = "tafsir/";
    private final Context appContext;

    public TafsirRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public InputStream open(String assetName) throws IOException {
        if (assetName == null || !assetName.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Invalid Tafsir asset name");
        }
        return appContext.getAssets().open(ROOT + assetName);
    }
}

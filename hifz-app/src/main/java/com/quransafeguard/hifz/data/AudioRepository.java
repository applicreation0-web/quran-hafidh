package com.quransafeguard.hifz.data;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/** Future local audio boundary. Audio remains disabled until its product gate is opened. */
public final class AudioRepository {
    private static final String ROOT = "audio/";
    private final Context appContext;

    public AudioRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public InputStream openLocal(String assetName) throws IOException {
        if (assetName == null || !assetName.matches("[A-Za-z0-9._/-]+") || assetName.contains("..")) {
            throw new IllegalArgumentException("Invalid audio asset name");
        }
        return appContext.getAssets().open(ROOT + assetName);
    }
}

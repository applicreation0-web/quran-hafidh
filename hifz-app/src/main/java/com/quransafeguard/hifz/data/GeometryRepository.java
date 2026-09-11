package com.quransafeguard.hifz.data;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/** Read-only access to the geometry bundled with Quran Hifz. */
public final class GeometryRepository {
    public static final String ASSET_PATH = "geometry/mushaf_geometry.json";

    private final Context appContext;

    public GeometryRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public InputStream open() throws IOException {
        return appContext.getAssets().open(ASSET_PATH);
    }

    public boolean isBundled() {
        try (InputStream ignored = open()) {
            return true;
        } catch (IOException missing) {
            return false;
        }
    }
}

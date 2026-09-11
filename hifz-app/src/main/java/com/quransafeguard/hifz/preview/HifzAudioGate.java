package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Audio availability gate. Production redistribution remains disabled unless approved,
 * while a user-imported private/offline pack can be used without network access.
 */
public final class HifzAudioGate {
    public final String reciter;
    public final boolean redistributionApproved;
    public final String productionHost;
    private final HifzAudioPack localPack;

    public HifzAudioGate(Context context) {
        localPack = new HifzAudioPack(context);
        try {
            JSONObject o = new JSONObject(read(context));
            reciter = o.optString("reciter", "Al-Husary Muʿallim");
            redistributionApproved = o.optBoolean("redistributionApproved", false);
            productionHost = o.optString("productionHost", "");
        } catch (Exception error) {
            throw new IllegalStateException("Invalid audio gate metadata", error);
        }
    }

    /** Only an installed private local pack enables the in-app audio controls. */
    public boolean available() { return localPack.installed(); }
    public String status() {
        if (localPack.installed()) return "Pack audio local disponible (" + localPack.installedFileCount() + " versets)";
        return "Audio non installé : importez un pack local Al-Husary Muʿallim dans Paramètres";
    }

    private static String read(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("reader109/audio.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}

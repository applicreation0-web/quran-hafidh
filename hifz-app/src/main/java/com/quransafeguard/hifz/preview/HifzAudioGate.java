package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Explicit audio gate. The app never invents or hotlinks audio when redistribution/hosting
 * has not been approved. Enabling this later requires approved metadata plus a local/self-hosted source.
 */
public final class HifzAudioGate {
    public final String reciter;
    public final boolean redistributionApproved;
    public final String productionHost;

    public HifzAudioGate(Context context) {
        try {
            JSONObject o = new JSONObject(read(context));
            reciter = o.optString("reciter", "Al-Husary Muʿallim");
            redistributionApproved = o.optBoolean("redistributionApproved", false);
            productionHost = o.optString("productionHost", "");
        } catch (Exception error) {
            throw new IllegalStateException("Invalid audio gate metadata", error);
        }
    }

    public boolean available() { return redistributionApproved && !productionHost.trim().isEmpty(); }
    public String status() {
        return available() ? "Audio disponible" : "Audio non activé : hébergement/redistribution à valider";
    }

    private static String read(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("reader109/audio.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}

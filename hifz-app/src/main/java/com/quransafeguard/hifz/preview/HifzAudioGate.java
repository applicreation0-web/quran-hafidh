package com.quransafeguard.hifz.preview;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Audio availability gate for Quran Hifz only. */
public final class HifzAudioGate {
    public final String reciter;
    public final boolean redistributionApproved;
    public final String baseUrl;
    public final String zipBaseUrl;
    public final boolean personalUseOnly;
    private final HifzAudioPack localPack;

    public HifzAudioGate(Context context) {
        localPack = new HifzAudioPack(context);
        try {
            JSONObject o = new JSONObject(read(context));
            reciter = o.optString("reciter", "Mahmoud Khalil Al-Husary — Muʿallim (Hafṣ)");
            redistributionApproved = o.optBoolean("redistributionApproved", false);
            baseUrl = o.optString("baseUrl", "");
            zipBaseUrl = o.optString("zipBaseUrl", "");
            personalUseOnly = o.optBoolean("personalUseOnly", true);
        } catch (Exception error) {
            throw new IllegalStateException("Invalid Hifz audio source metadata", error);
        }
    }

    public boolean available() { return localPack.installed(); }

    public String status() {
        if (localPack.installed()) {
            return "Audio embarqué disponible · " + localPack.installedFileCount() + " versets · " + localPack.sourceLabel();
        }
        return "Audio non embarqué dans ce build personnel";
    }

    private static String read(Context context) throws Exception {
        try (InputStream in = context.getAssets().open("hifzaudio/source.json"); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }
}

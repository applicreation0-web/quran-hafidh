package com.quransafeguard.hifz.preview;

import android.content.Context;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Offline private Darussalam text adapter. NO commentary is embedded in public
 * source, GitHub Actions or the public staging APK. Each entry must be sourced,
 * individually indexed to one canonical IKEN group and integrity verified.
 * Invalid/missing content remains unavailable, never synthesized.
 */
final class IbnKathirFullCommentaryPrivate {
    static final class Entry {
        final int volume, page;
        final String text;
        Entry(int volume, int page, String text) {
            this.volume=volume; this.page=page; this.text=text;
        }
    }
    private IbnKathirFullCommentaryPrivate() {}

    static Entry load(Context context, IbnKathirGroupIndex.Group group) {
        if (context == null || group == null || group.surah < 1 || group.surah > 114)
            return null;
        String id = group.id;
        if (!id.matches("IKEN[0-9]{3}_[0-9]{3}_[0-9]{3}")) return null;
        String path="ibn_kathir_full_commentary_private/" + id + ".json";
        try (InputStream in=context.getAssets().open(path);
             ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buf=new byte[8192]; int count;
            while ((count=in.read(buf))!=-1) {
                out.write(buf,0,count);
                if (out.size()>750_000) return null;
            }
            return parseVerified(new String(out.toByteArray(), StandardCharsets.UTF_8),id);
        } catch(Exception missingOrInvalid) {
            return null;
        }
    }

    /** Strict private schema; any missing integrity/provenance field fails closed. */
    static Entry parseVerified(String json, String expectedGroupId) {
        if (json==null || expectedGroupId==null) return null;
        try {
            JSONObject obj=new JSONObject(json);
            if (!"IK_DARUSSALAM_PRIVATE_COMMENTARY_V1".equals(obj.getString("schema")))
                return null;
            if (!expectedGroupId.equals(obj.getString("group_id"))) return null;
            if (!"Darussalam".equals(obj.getString("edition"))) return null;
            int volume=obj.getInt("volume"),page=obj.getInt("page");
            if (volume<1 || volume>10 || page<1 || page>1200) return null;
            String text=obj.getString("text_en");
            if (text.isEmpty() || text.length()>600_000) return null;
            String expected=obj.getString("text_sha256");
            byte[] digest=MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder actual=new StringBuilder();
            for(byte b:digest) actual.append(String.format(Locale.ROOT,"%02x",b&255));
            if (!actual.toString().equals(expected)) return null;
            return new Entry(volume,page,text);
        } catch(Exception malformed) {
            return null;
        }
    }
}

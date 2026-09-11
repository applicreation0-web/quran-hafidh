package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Read-only Tafsir al-Jalalayn corpus bundled from the previously audited project asset. */
public final class TafsirRepository {
    private static final String DB_NAME = "al_jalalayn_en.sqlite";
    private static final String EXPECTED_SHA256 = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56";
    private static final int EXPECTED_VERSES = 6236;

    public static final class Entry {
        public final String commentary;
        public final List<String> notes;
        Entry(String commentary, List<String> notes) { this.commentary = commentary; this.notes = notes; }
    }

    private final Context app;
    public TafsirRepository(Context context) { app = context.getApplicationContext(); }

    public Entry load(VerseRef verse) throws Exception {
        File dbFile = materialize();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            int count = -1;
            try (Cursor c = db.rawQuery("SELECT value FROM source_metadata WHERE key='verse_count'", null)) {
                if (c.moveToFirst()) count = Integer.parseInt(c.getString(0));
            }
            if (count != EXPECTED_VERSES) throw new IllegalStateException("Tafsir verse count mismatch: " + count);
            String body = null;
            try (Cursor c = db.rawQuery("SELECT body_json FROM verse_commentary WHERE surah=? AND ayah=?",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())})) {
                if (c.moveToFirst()) body = c.getString(0);
            }
            if (body == null) return null;
            String commentary = flattenRuns(body);
            ArrayList<String> notes = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT label,body_json FROM verse_note WHERE surah=? AND ayah=? ORDER BY ordinal",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())})) {
                while (c.moveToNext()) notes.add(c.getString(0) + ". " + flattenRuns(c.getString(1)));
            }
            return new Entry(commentary, notes);
        } finally {
            db.close();
        }
    }

    private synchronized File materialize() throws Exception {
        File dir = new File(app.getNoBackupFilesDir(), "tafsir");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create Tafsir directory");
        File dest = new File(dir, DB_NAME);
        File verified = new File(dir, DB_NAME + ".verified");
        if (dest.isFile() && verified.isFile() && verificationMarker(dest).equals(readSmallText(verified))) return dest;
        if (dest.isFile() && EXPECTED_SHA256.equals(sha256(dest))) {
            writeSmallText(verified, verificationMarker(dest));
            return dest;
        }
        File tmp = new File(dir, DB_NAME + ".tmp");
        List<InputStream> streams = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) streams.add(app.getAssets().open(String.format(java.util.Locale.ROOT,"tafsir/al_jalalayn_en.sqlite.gz.part%02d", i)));
            try (GZIPInputStream in = new GZIPInputStream(new SequenceInputStream(Collections.enumeration(streams)));
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[64 * 1024]; int n;
                while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
                out.getFD().sync();
            }
            if (!EXPECTED_SHA256.equals(sha256(tmp))) throw new IllegalStateException("Tafsir checksum mismatch");
            if (dest.exists() && !dest.delete()) throw new IllegalStateException("Cannot replace Tafsir database");
            if (!tmp.renameTo(dest)) throw new IllegalStateException("Cannot install Tafsir database");
            writeSmallText(verified, verificationMarker(dest));
            return dest;
        } finally {
            for (InputStream stream : streams) try { stream.close(); } catch (Exception ignored) {}
            if (tmp.exists() && !dest.exists()) tmp.delete();
        }
    }

    private String verificationMarker(File file) { return EXPECTED_SHA256 + ":" + file.length(); }

    private String readSmallText(File file) {
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] bytes = new byte[(int) Math.min(512L, file.length())];
            int n = in.read(bytes);
            return n <= 0 ? "" : new String(bytes, 0, n, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ignored) { return ""; }
    }

    private void writeSmallText(File file, String value) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.getFD().sync();
        }
    }

    private String flattenRuns(String json) throws Exception {
        JSONArray array = new JSONArray(json);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject run = array.getJSONObject(i);
            String text = run.optString("text", "");
            if (!text.isEmpty()) out.append(text);
        }
        return out.toString().trim();
    }

    private String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024]; int n;
            while ((n = in.read(buffer)) >= 0) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(java.util.Locale.ROOT,"%02x", b & 0xff));
        return out.toString();
    }
}

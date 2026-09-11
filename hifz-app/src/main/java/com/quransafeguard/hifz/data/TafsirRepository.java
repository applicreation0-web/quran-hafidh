package com.quransafeguard.hifz.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Base64;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Read-only three-Tafsir repository reused from the previously audited project corpora.
 * Tafsir remains a Reading/Study concern; memorization Activities never depend on this class.
 */
public final class TafsirRepository {
    private static final String JALALAYN_DB_NAME = "al_jalalayn_en.sqlite";
    private static final String JALALAYN_EXPECTED_SHA256 = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56";
    private static final int JALALAYN_EXPECTED_VERSES = 6236;

    public enum Edition {
        JALALAYN("jalalayn", "Jalalayn", 4, 6236, null),
        QURTUBI("qurtubi", "Qurtubi", 4, 432, "0106-qurtubi-hide-verse-labels-v1"),
        QUSHAYRI("qushayri", "Qushayri", 2, 720, "0106-qushayri-source-semantics-v1");

        public final String storageValue;
        public final String displayName;
        final int partCount;
        final int expectedEntries;
        final String presentationRevision;

        Edition(String storageValue, String displayName, int partCount, int expectedEntries,
                String presentationRevision) {
            this.storageValue = storageValue;
            this.displayName = displayName;
            this.partCount = partCount;
            this.expectedEntries = expectedEntries;
            this.presentationRevision = presentationRevision;
        }

        public static Edition fromStorage(String value) {
            if (value != null) {
                for (Edition edition : values()) {
                    if (edition.storageValue.equals(value)) return edition;
                }
            }
            return JALALAYN;
        }
    }

    public static final class Entry {
        public final String commentary;
        public final List<String> notes;
        Entry(String commentary, List<String> notes) {
            this.commentary = commentary;
            this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        }
    }

    private final Context app;
    public TafsirRepository(Context context) { app = context.getApplicationContext(); }

    public Entry load(VerseRef verse) throws Exception {
        return load(verse, Edition.JALALAYN);
    }

    public Entry load(VerseRef verse, Edition edition) throws Exception {
        if (edition == Edition.JALALAYN) return loadJalalayn(verse);
        return loadV2(verse, edition);
    }

    private Entry loadJalalayn(VerseRef verse) throws Exception {
        File dbFile = materializeJalalayn();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
            dbFile.getAbsolutePath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
        );
        try {
            int count = -1;
            try (Cursor c = db.rawQuery("SELECT value FROM source_metadata WHERE key='verse_count'", null)) {
                if (c.moveToFirst()) count = Integer.parseInt(c.getString(0));
            }
            if (count != JALALAYN_EXPECTED_VERSES) {
                throw new IllegalStateException("Tafsir verse count mismatch: " + count);
            }

            String body = null;
            try (Cursor c = db.rawQuery(
                "SELECT body_json FROM verse_commentary WHERE surah=? AND ayah=?",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())}
            )) {
                if (c.moveToFirst()) body = c.getString(0);
            }
            if (body == null) return null;

            String commentary = flattenRuns(body);
            ArrayList<String> notes = new ArrayList<>();
            try (Cursor c = db.rawQuery(
                "SELECT label,body_json FROM verse_note WHERE surah=? AND ayah=? ORDER BY ordinal",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())}
            )) {
                while (c.moveToNext()) notes.add(c.getString(0) + ". " + flattenRuns(c.getString(1)));
            }
            return new Entry(commentary, notes);
        } finally {
            db.close();
        }
    }

    private Entry loadV2(VerseRef verse, Edition edition) throws Exception {
        File file = materializeV2(edition);
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
            file.getAbsolutePath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
        );
        try {
            if (!metadataMatches(db, edition)) {
                throw new IllegalStateException(edition.displayName + " metadata mismatch");
            }
            StringBuilder body = new StringBuilder();
            try (Cursor c = db.rawQuery(
                "SELECT id, verse_translation, commentary FROM tafsir_entry " +
                    "WHERE surah=? AND verse_start<=? AND verse_end>=? " +
                    "ORDER BY verse_start, verse_end, segment_no, id",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah()),
                    Integer.toString(verse.getAyah())}
            )) {
                while (c.moveToNext()) {
                    long rowId = c.getLong(0);
                    String translation = trim(c.getString(1));
                    String commentary = trim(c.getString(2));
                    if (edition == Edition.QUSHAYRI) {
                        verifyQushayriRuns(db, rowId, commentary);
                    }
                    if (body.length() > 0) body.append("\n\n");
                    if (!translation.isEmpty()) {
                        body.append(translation);
                        if (!commentary.isEmpty()) body.append("\n");
                    }
                    body.append(commentary);
                }
            }
            if (body.length() == 0) return null;
            return new Entry(body.toString().trim(), Collections.emptyList());
        } finally {
            db.close();
        }
    }

    private static void verifyQushayriRuns(SQLiteDatabase db, long entryId, String commentary) {
        StringBuilder semantic = new StringBuilder();
        try (Cursor c = db.rawQuery(
            "SELECT style,text FROM tafsir_run WHERE entry_id=? ORDER BY run_no",
            new String[]{Long.toString(entryId)}
        )) {
            while (c.moveToNext()) {
                String style = c.getString(0);
                if (!"REGULAR".equals(style) && !"POETRY".equals(style)) {
                    throw new IllegalStateException("Unsupported Qushayri semantic run style");
                }
                semantic.append(c.getString(1));
            }
        }
        if (semantic.length() == 0 || !semantic.toString().equals(commentary)) {
            throw new IllegalStateException("Qushayri semantic run integrity mismatch");
        }
    }

    public boolean isCorpusBundled() {
        return isCorpusBundled(Edition.JALALAYN);
    }

    public boolean isCorpusBundled(Edition edition) {
        try {
            for (int i = 0; i < edition.partCount; i++) {
                String path = edition == Edition.JALALAYN
                    ? String.format(Locale.ROOT, "tafsir/al_jalalayn_en.sqlite.gz.part%02d", i)
                    : String.format(Locale.ROOT, "tafsir/%s_en.sqlite.gz.b64.part%02d", edition.storageValue, i);
                try (InputStream ignored = app.getAssets().open(path)) { /* presence only */ }
            }
            return true;
        } catch (Exception missing) {
            return false;
        }
    }

    private synchronized File materializeJalalayn() throws Exception {
        File dir = tafsirDirectory();
        File dest = new File(dir, JALALAYN_DB_NAME);
        if (dest.isFile() && JALALAYN_EXPECTED_SHA256.equals(sha256(dest))) return dest;

        File tmp = new File(dir, JALALAYN_DB_NAME + ".tmp");
        List<InputStream> streams = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                streams.add(app.getAssets().open(String.format(Locale.ROOT,
                    "tafsir/al_jalalayn_en.sqlite.gz.part%02d", i)));
            }
            try (GZIPInputStream in = new GZIPInputStream(new SequenceInputStream(Collections.enumeration(streams)));
                 FileOutputStream out = new FileOutputStream(tmp)) {
                copy(in, out);
                out.getFD().sync();
            }
            if (!JALALAYN_EXPECTED_SHA256.equals(sha256(tmp))) {
                throw new IllegalStateException("Jalalayn checksum mismatch");
            }
            replaceAtomically(tmp, dest);
            return dest;
        } finally {
            for (InputStream stream : streams) try { stream.close(); } catch (Exception ignored) { }
            if (tmp.exists() && !dest.exists()) tmp.delete();
        }
    }

    private synchronized File materializeV2(Edition edition) throws Exception {
        if (edition == Edition.JALALAYN) return materializeJalalayn();
        File dir = tafsirDirectory();
        File dest = new File(dir, edition.storageValue + "_en.sqlite");
        if (dest.isFile() && databaseFileMatches(dest, edition)) return dest;

        File tmp = new File(dir, edition.storageValue + "_en.sqlite.tmp");
        try {
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            for (int i = 0; i < edition.partCount; i++) {
                String path = String.format(Locale.ROOT,
                    "tafsir/%s_en.sqlite.gz.b64.part%02d", edition.storageValue, i);
                try (InputStream in = app.getAssets().open(path)) {
                    byte[] buffer = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buffer)) >= 0) encoded.write(buffer, 0, n);
                }
            }
            byte[] compressed = Base64.decode(encoded.toByteArray(), Base64.DEFAULT);
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(compressed));
                 FileOutputStream out = new FileOutputStream(tmp)) {
                copy(in, out);
                out.getFD().sync();
            }
            if (!databaseFileMatches(tmp, edition)) {
                throw new IllegalStateException(edition.displayName + " structural integrity check failed");
            }
            replaceAtomically(tmp, dest);
            return dest;
        } finally {
            if (tmp.exists() && !dest.exists()) tmp.delete();
        }
    }

    private File tafsirDirectory() {
        File dir = new File(app.getNoBackupFilesDir(), "tafsir");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create Tafsir directory");
        return dir;
    }

    private static boolean databaseFileMatches(File file, Edition edition) {
        if (!file.isFile()) return false;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );
            try {
                return metadataMatches(db, edition);
            } finally {
                db.close();
            }
        } catch (Throwable invalid) {
            return false;
        }
    }

    private static boolean metadataMatches(SQLiteDatabase db, Edition edition) {
        if (edition == Edition.JALALAYN) return true;
        try (Cursor quick = db.rawQuery("PRAGMA quick_check", null)) {
            if (!quick.moveToFirst() || !"ok".equals(quick.getString(0))) return false;
        }
        int rows = -1;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM tafsir_entry", null)) {
            if (c.moveToFirst()) rows = c.getInt(0);
        }
        if (rows != edition.expectedEntries) return false;
        if (!"2".equals(metadataValue(db, "schema_version"))) return false;
        if (!edition.storageValue.equals(metadataValue(db, "edition_id"))) return false;
        if (!Integer.toString(edition.expectedEntries).equals(metadataValue(db, "entry_count"))) return false;
        if (!"false".equals(metadataValue(db, "arabic_included"))) return false;
        if (!edition.presentationRevision.equals(metadataValue(db, "presentation_revision"))) return false;
        if (edition == Edition.QUSHAYRI) {
            if (!"tafsir_run".equals(metadataValue(db, "semantic_run_table"))) return false;
            if (!"928".equals(metadataValue(db, "verified_note_call_count"))) return false;
            if (!"121".equals(metadataValue(db, "poetry_index_entry_count"))) return false;
            if (!"126".equals(metadataValue(db, "poetry_index_occurrence_count"))) return false;
            if (!"542".equals(metadataValue(db, "poetry_unique_line_count"))) return false;
            try (Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='tafsir_run'", null)) {
                if (!c.moveToFirst() || c.getInt(0) != 1) return false;
            }
        }
        return true;
    }

    private static String metadataValue(SQLiteDatabase db, String key) {
        try (Cursor c = db.rawQuery("SELECT value FROM source_metadata WHERE key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private static void replaceAtomically(File tmp, File dest) {
        if (dest.exists() && !dest.delete()) throw new IllegalStateException("Cannot replace previous Tafsir database");
        if (!tmp.renameTo(dest)) throw new IllegalStateException("Cannot install Tafsir database atomically");
    }

    private static void copy(InputStream in, FileOutputStream out) throws Exception {
        byte[] buffer = new byte[64 * 1024];
        int n;
        while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }

    private static String flattenRuns(String json) throws Exception {
        JSONArray array = new JSONArray(json);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            JSONObject run = array.getJSONObject(i);
            String text = run.optString("text", "");
            if (!text.isEmpty()) out.append(text);
        }
        return out.toString().trim();
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return out.toString();
    }
}

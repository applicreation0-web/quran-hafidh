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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

/** Read-only, source-backed Tafsir al-Jalalayn corpus from the audited project asset. */
public final class TafsirRepository {
    public static final String EDITION_NAME = "Jalalayn";
    public static final String SOURCE_TITLE = "Tafsir al-Jalalayn (English translation)";
    private static final String DB_NAME = "al_jalalayn_en.sqlite";
    private static final int EXPECTED_VERSES = 6236;

    public enum RunStyle {
        REGULAR,
        ITALIC,
        BOLD,
        BOLD_ITALIC,
        TECHNICAL_TERM,
        TRANSLITERATION,
        POETRY,
        NOTE_REF
    }

    public static final class Run {
        public final RunStyle style;
        public final String text;
        Run(RunStyle style, String text) { this.style = style; this.text = text; }
    }

    public static final class Note {
        public final int number;
        public final List<Run> runs;
        Note(int number, List<Run> runs) { this.number = number; this.runs = Collections.unmodifiableList(runs); }
    }

    public static final class Entry {
        public final String editionName;
        public final String work;
        public final String author;
        public final String translator;
        public final String language;
        public final List<Run> commentaryRuns;
        public final List<Note> notes;

        Entry(String editionName, String work, String author, String translator, String language,
              List<Run> commentaryRuns, List<Note> notes) {
            this.editionName = editionName;
            this.work = work;
            this.author = author;
            this.translator = translator;
            this.language = language;
            this.commentaryRuns = Collections.unmodifiableList(commentaryRuns);
            this.notes = Collections.unmodifiableList(notes);
        }

        public String metadataLine() {
            ArrayList<String> values = new ArrayList<>();
            if (work != null && !work.trim().isEmpty()) values.add(work.trim());
            if (author != null && !author.trim().isEmpty()) values.add(author.trim());
            if (translator != null && !translator.trim().isEmpty()) values.add("tr. " + translator.trim());
            if (language != null && !language.trim().isEmpty()) values.add(language.trim());
            return android.text.TextUtils.join(" · ", values);
        }
    }

    private final Context app;
    public TafsirRepository(Context context) { app = context.getApplicationContext(); }

    public Entry load(VerseRef verse) throws Exception {
        File dbFile = materialize();
        SQLiteDatabase db = SQLiteDatabase.openDatabase(dbFile.getAbsolutePath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        try {
            verifyDatabase(db);
            String body = null;
            try (Cursor c = db.rawQuery("SELECT body_json FROM verse_commentary WHERE surah=? AND ayah=?",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())})) {
                if (c.moveToFirst()) body = c.getString(0);
            }
            if (body == null) return null;
            List<Run> commentary = parseRuns(body);
            if (commentary.isEmpty()) return null;
            ArrayList<Note> notes = new ArrayList<>();
            try (Cursor c = db.rawQuery("SELECT label,body_json FROM verse_note WHERE surah=? AND ayah=? ORDER BY ordinal",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah())})) {
                while (c.moveToNext()) {
                    List<Run> runs = parseRuns(c.getString(1));
                    if (runs.isEmpty()) throw new IllegalStateException("Empty Tafsir note " + c.getInt(0));
                    notes.add(new Note(c.getInt(0), runs));
                }
            }
            return new Entry(EDITION_NAME, SOURCE_TITLE, "", "", "English", commentary, notes);
        } finally {
            db.close();
        }
    }

    private synchronized File materialize() throws Exception {
        File dir = new File(app.getNoBackupFilesDir(), "tafsir");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create Tafsir directory");
        File dest = new File(dir, DB_NAME);
        if (databaseMatches(dest)) return dest;

        File tmp = new File(dir, DB_NAME + ".tmp-" + android.os.Process.myPid());
        List<InputStream> streams = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                streams.add(app.getAssets().open(String.format(java.util.Locale.ROOT,
                    "tafsir/al_jalalayn_en.sqlite.gz.part%02d", i)));
            }
            try (GZIPInputStream in = new GZIPInputStream(new SequenceInputStream(Collections.enumeration(streams)));
                 FileOutputStream out = new FileOutputStream(tmp)) {
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
                out.getFD().sync();
            }
            if (!databaseMatches(tmp)) throw new IllegalStateException("Tafsir corpus integrity check failed");
            if (dest.exists() && !dest.delete()) throw new IllegalStateException("Cannot replace Tafsir database");
            if (!tmp.renameTo(dest)) throw new IllegalStateException("Cannot install Tafsir database");
            return dest;
        } finally {
            for (InputStream stream : streams) try { stream.close(); } catch (Exception ignored) {}
            if (tmp.exists() && !dest.equals(tmp)) tmp.delete();
        }
    }

    private boolean databaseMatches(File file) {
        if (file == null || !file.isFile()) return false;
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            try {
                verifyDatabase(db);
                return true;
            } finally {
                db.close();
            }
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void verifyDatabase(SQLiteDatabase db) {
        try (Cursor check = db.rawQuery("PRAGMA quick_check", null)) {
            if (!check.moveToFirst() || !"ok".equals(check.getString(0))) {
                throw new IllegalStateException("Tafsir SQLite quick_check failed");
            }
        }
        if (!Integer.toString(EXPECTED_VERSES).equals(metadata(db, "verse_count"))) {
            throw new IllegalStateException("Tafsir verse-count metadata mismatch");
        }
        int rows;
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM verse_commentary", null)) {
            if (!c.moveToFirst()) throw new IllegalStateException("Tafsir verse count unavailable");
            rows = c.getInt(0);
        }
        if (rows != EXPECTED_VERSES) throw new IllegalStateException("Tafsir verse count mismatch: " + rows);
        if (!"true".equals(metadata(db, "personal_use_only"))) {
            throw new IllegalStateException("Tafsir personal-use metadata missing");
        }
        if (!"false".equals(metadata(db, "redistribution_approved"))) {
            throw new IllegalStateException("Tafsir redistribution metadata mismatch");
        }
        String rights = metadata(db, "rights_note");
        if (rights == null || rights.trim().isEmpty()) throw new IllegalStateException("Tafsir rights note missing");
    }

    private String metadata(SQLiteDatabase db, String key) {
        try (Cursor c = db.rawQuery("SELECT value FROM source_metadata WHERE key=?", new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : null;
        }
    }

    private List<Run> parseRuns(String json) throws Exception {
        JSONArray array = new JSONArray(json);
        ArrayList<Run> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject run = array.getJSONObject(i);
            RunStyle style = parseStyle(run.optString("style", ""));
            if (style == null) throw new IllegalStateException("Unknown Tafsir run style");
            String text = normalizeJalalaynHonorifics(run.optString("text", ""));
            if (!text.isEmpty()) out.add(new Run(style, text));
        }
        return out;
    }

    private RunStyle parseStyle(String value) {
        switch (value) {
            case "regular": return RunStyle.REGULAR;
            case "italic": return RunStyle.ITALIC;
            case "bold": return RunStyle.BOLD;
            case "bold_italic": return RunStyle.BOLD_ITALIC;
            case "technical_term": return RunStyle.TECHNICAL_TERM;
            case "transliteration": return RunStyle.TRANSLITERATION;
            case "poetry": return RunStyle.POETRY;
            case "note_ref": return RunStyle.NOTE_REF;
            default: return null;
        }
    }

    /** Exact display-only substitutions approved in the historical 0.10.6 Jalalayn presentation. */
    private String normalizeJalalaynHonorifics(String source) {
        return source
            .replace("(ṣʿa)", "ﷺ")
            .replace("(ṣ)", "ﷺ")
            .replace("(ʿa)", "عليه السلام");
    }
}
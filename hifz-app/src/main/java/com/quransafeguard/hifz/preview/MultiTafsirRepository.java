package com.quransafeguard.hifz.preview;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.quransafeguard.hifz.core.VerseRef;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Local audited multi-Tafsir repository. No commentary is synthesized or merged. */
public final class MultiTafsirRepository {
    public enum Edition {
        JALALAYN("jalalayn", "Jalalayn"),
        QURTUBI("qurtubi", "Qurtubi"),
        QUSHAYRI("qushayri", "Qushayri");

        public final String storageValue;
        public final String displayName;
        Edition(String storageValue, String displayName) {
            this.storageValue = storageValue;
            this.displayName = displayName;
        }
        public static Edition fromStorage(String value) {
            for (Edition edition : values()) if (edition.storageValue.equals(value)) return edition;
            return JALALAYN;
        }
    }

    private static final class CorpusSpec {
        final Edition edition;
        final String databaseName;
        final int partCount;
        final int expectedEntries;
        final String presentationRevision;
        CorpusSpec(Edition edition, String databaseName, int partCount, int expectedEntries, String presentationRevision) {
            this.edition = edition;
            this.databaseName = databaseName;
            this.partCount = partCount;
            this.expectedEntries = expectedEntries;
            this.presentationRevision = presentationRevision;
        }
        String assetPart(int index) {
            return String.format(java.util.Locale.ROOT, "tafsir/%s.gz.part%02d", databaseName, index);
        }
    }

    private static final CorpusSpec QURTUBI = new CorpusSpec(
        Edition.QURTUBI, "qurtubi_en.sqlite", 4, 432, "0106-qurtubi-hide-verse-labels-v1"
    );
    private static final CorpusSpec QUSHAYRI = new CorpusSpec(
        Edition.QUSHAYRI, "qushayri_en.sqlite", 2, 720, "0106-qushayri-source-semantics-v1"
    );

    private final Context app;
    private final TafsirRepository jalalayn;

    public MultiTafsirRepository(Context context) {
        app = context.getApplicationContext();
        jalalayn = new TafsirRepository(app);
    }

    public Map<Edition, TafsirRepository.Entry> loadAvailable(VerseRef verse) throws Exception {
        LinkedHashMap<Edition, TafsirRepository.Entry> available = new LinkedHashMap<>();
        TafsirRepository.Entry jal = jalalayn.load(verse);
        if (jal != null) available.put(Edition.JALALAYN, jal);
        TafsirRepository.Entry qurtubi = loadV2(verse, QURTUBI);
        if (qurtubi != null) available.put(Edition.QURTUBI, qurtubi);
        TafsirRepository.Entry qushayri = loadV2(verse, QUSHAYRI);
        if (qushayri != null) available.put(Edition.QUSHAYRI, qushayri);
        return Collections.unmodifiableMap(available);
    }

    private TafsirRepository.Entry loadV2(VerseRef verse, CorpusSpec spec) throws Exception {
        if (!assetExists(spec.assetPart(0))) return null;
        File file = materialize(spec);
        SQLiteDatabase database = SQLiteDatabase.openDatabase(
            file.getAbsolutePath(), null,
            SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
        );
        try {
            verifyMetadata(database, spec);
            ArrayList<Row> rows = new ArrayList<>();
            try (Cursor cursor = database.rawQuery(
                "SELECT id, verse_start, verse_end, segment_no, verse_translation, commentary " +
                    "FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=? " +
                    "ORDER BY verse_start, verse_end, segment_no, id",
                new String[]{Integer.toString(verse.getSurah()), Integer.toString(verse.getAyah()), Integer.toString(verse.getAyah())}
            )) {
                while (cursor.moveToNext()) {
                    rows.add(new Row(cursor.getLong(0), cursor.getInt(1), cursor.getInt(2), cursor.getInt(3),
                        clean(cursor.getString(4)), clean(cursor.getString(5))));
                }
            }
            if (rows.isEmpty()) return null;

            ArrayList<TafsirRepository.Run> runs = new ArrayList<>();
            if (spec.edition == Edition.QUSHAYRI) renderQushayri(database, rows, runs);
            else renderSourceRows(rows, runs);
            if (runs.isEmpty()) return null;

            String display = metadata(database, "display_name");
            String work = metadata(database, "work");
            String author = metadata(database, "author");
            String translator = metadata(database, "translator");
            String language = metadata(database, "language");
            return new TafsirRepository.Entry(
                blankFallback(display, spec.edition.displayName),
                work == null ? "" : work,
                author == null ? "" : author,
                translator == null ? "" : translator,
                language == null ? "" : language,
                runs,
                Collections.emptyList()
            );
        } finally {
            database.close();
        }
    }

    private void renderSourceRows(List<Row> rows, List<TafsirRepository.Run> output) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i > 0) output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.REGULAR, "\n"));
            if (!row.translation.isEmpty()) {
                output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.BOLD_ITALIC, row.translation));
                if (!row.commentary.isEmpty()) output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.REGULAR, "\n"));
            }
            if (!row.commentary.isEmpty()) output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.REGULAR, row.commentary));
        }
    }

    private void renderQushayri(SQLiteDatabase database, List<Row> rows, List<TafsirRepository.Run> output) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i > 0) output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.REGULAR, "\n"));
            if (!row.translation.isEmpty()) {
                output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.BOLD_ITALIC, row.translation));
                if (!row.commentary.isEmpty()) output.add(new TafsirRepository.Run(TafsirRepository.RunStyle.REGULAR, "\n"));
            }
            StringBuilder semanticText = new StringBuilder();
            int semanticCount = 0;
            try (Cursor cursor = database.rawQuery(
                "SELECT style,text FROM tafsir_run WHERE entry_id=? ORDER BY run_no", new String[]{Long.toString(row.id)}
            )) {
                while (cursor.moveToNext()) {
                    String style = cursor.getString(0);
                    String text = cursor.getString(1);
                    TafsirRepository.RunStyle runStyle;
                    if ("REGULAR".equals(style)) runStyle = TafsirRepository.RunStyle.REGULAR;
                    else if ("POETRY".equals(style)) runStyle = TafsirRepository.RunStyle.POETRY;
                    else throw new IllegalStateException("Unsupported Qushayri Tafsir run style: " + style);
                    output.add(new TafsirRepository.Run(runStyle, text));
                    semanticText.append(text);
                    semanticCount++;
                }
            }
            if (semanticCount == 0) throw new IllegalStateException("Qushayri semantic runs missing for row " + row.id);
            if (!semanticText.toString().equals(row.commentary)) {
                throw new IllegalStateException("Qushayri semantic text mismatch for row " + row.id);
            }
        }
    }

    private synchronized File materialize(CorpusSpec spec) throws Exception {
        File directory = new File(app.getNoBackupFilesDir(), "tafsir");
        if (!directory.exists() && !directory.mkdirs()) throw new IllegalStateException("Cannot create Tafsir directory");
        File destination = new File(directory, spec.databaseName);
        if (destination.isFile() && databaseMatches(destination, spec)) return destination;

        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        for (int i = 0; i < spec.partCount; i++) {
            try (InputStream input = app.getAssets().open(spec.assetPart(i))) { copy(input, compressed); }
        }
        File temporary = new File(directory, spec.databaseName + ".tmp-" + android.os.Process.myPid());
        try {
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed.toByteArray()));
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            if (!databaseMatches(temporary, spec)) throw new IllegalStateException(spec.edition.displayName + " corpus integrity check failed");
            if (destination.exists() && !destination.delete()) throw new IllegalStateException("Cannot replace previous Tafsir database");
            if (!temporary.renameTo(destination)) throw new IllegalStateException("Cannot install Tafsir database atomically");
            return destination;
        } finally {
            if (temporary.exists() && !destination.equals(temporary)) temporary.delete();
        }
    }

    private boolean databaseMatches(File file, CorpusSpec spec) {
        if (!file.isFile()) return false;
        try {
            SQLiteDatabase database = SQLiteDatabase.openDatabase(
                file.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS
            );
            try { verifyMetadata(database, spec); return true; }
            finally { database.close(); }
        } catch (Throwable ignored) { return false; }
    }

    private void verifyMetadata(SQLiteDatabase database, CorpusSpec spec) {
        try (Cursor check = database.rawQuery("PRAGMA quick_check", null)) {
            if (!check.moveToFirst() || !"ok".equals(check.getString(0))) {
                throw new IllegalStateException(spec.edition.displayName + " SQLite quick_check failed");
            }
        }
        if (!"2".equals(metadata(database, "schema_version"))) throw new IllegalStateException("Unexpected Tafsir schema");
        if (!spec.edition.storageValue.equals(metadata(database, "edition_id"))) throw new IllegalStateException("Unexpected Tafsir edition");
        if (!Integer.toString(spec.expectedEntries).equals(metadata(database, "entry_count"))) throw new IllegalStateException("Unexpected Tafsir entry count metadata");
        if (!"false".equals(metadata(database, "arabic_included"))) throw new IllegalStateException("Unexpected Tafsir Arabic payload flag");
        if (!spec.presentationRevision.equals(metadata(database, "presentation_revision"))) throw new IllegalStateException("Unexpected Tafsir presentation revision");
        if (!"true".equals(metadata(database, "personal_use_only"))) throw new IllegalStateException("Tafsir personal-use metadata missing");
        if (!"false".equals(metadata(database, "redistribution_approved"))) throw new IllegalStateException("Tafsir redistribution metadata mismatch");
        String rights = metadata(database, "rights_note");
        if (rights == null || rights.trim().isEmpty()) throw new IllegalStateException("Tafsir rights note missing");
        int rowCount;
        try (Cursor count = database.rawQuery("SELECT COUNT(*) FROM tafsir_entry", null)) {
            if (!count.moveToFirst()) throw new IllegalStateException("Tafsir row count unavailable");
            rowCount = count.getInt(0);
        }
        if (rowCount != spec.expectedEntries) throw new IllegalStateException("Unexpected Tafsir database row count: " + rowCount);
        if (spec.edition == Edition.QUSHAYRI) {
            if (!"tafsir_run".equals(metadata(database, "semantic_run_table"))) throw new IllegalStateException("Qushayri semantic table mismatch");
            if (!"928".equals(metadata(database, "verified_note_call_count"))) throw new IllegalStateException("Qushayri note-call audit mismatch");
            if (!"121".equals(metadata(database, "poetry_index_entry_count"))) throw new IllegalStateException("Qushayri poetry index mismatch");
            if (!"126".equals(metadata(database, "poetry_index_occurrence_count"))) throw new IllegalStateException("Qushayri poetry occurrence mismatch");
            if (!"542".equals(metadata(database, "poetry_unique_line_count"))) throw new IllegalStateException("Qushayri poetry line mismatch");
        }
    }

    private String metadata(SQLiteDatabase database, String key) {
        try (Cursor cursor = database.rawQuery("SELECT value FROM source_metadata WHERE key=?", new String[]{key})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    private boolean assetExists(String path) {
        try (InputStream ignored = app.getAssets().open(path)) { return true; }
        catch (IOException missing) { return false; }
    }

    private static void copy(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[32 * 1024];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String blankFallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static final class Row {
        final long id;
        final int verseStart;
        final int verseEnd;
        final int segment;
        final String translation;
        final String commentary;
        Row(long id, int verseStart, int verseEnd, int segment, String translation, String commentary) {
            this.id = id;
            this.verseStart = verseStart;
            this.verseEnd = verseEnd;
            this.segment = segment;
            this.translation = translation;
            this.commentary = commentary;
        }
    }
}
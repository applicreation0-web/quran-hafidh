package com.quransafeguard.hifz.preview;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.quransafeguard.hifz.core.VerseRef;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Personal/offline Al-Husary Muallim pack. No network code and no Hifz state dependency.
 * The app never bundles or downloads audio; a user-selected local ZIP is copied to app-private storage.
 */
final class HifzAudioPack {
    static final int EXPECTED_VERSE_FILES = 6236;
    private static final Pattern VERSE_FILE = Pattern.compile("[0-9]{6}\\.mp3");

    static final class ImportResult {
        final boolean ok;
        final int files;
        final String message;
        ImportResult(boolean ok, int files, String message) { this.ok = ok; this.files = files; this.message = message; }
    }

    private final Context context;
    private final File dir;

    HifzAudioPack(Context context) {
        this.context = context.getApplicationContext();
        this.dir = new File(this.context.getFilesDir(), "audio/husary-muallim");
    }

    boolean installed() {
        return fileNamed("001001.mp3").isFile()
            && fileNamed("114006.mp3").isFile()
            && countVerseFiles(dir) >= EXPECTED_VERSE_FILES;
    }

    int installedFileCount() { return countVerseFiles(dir); }

    File fileFor(VerseRef verse) {
        return fileNamed(String.format(Locale.ROOT, "%03d%03d.mp3", verse.getSurah(), verse.getAyah()));
    }

    ImportResult importZip(Uri uri) {
        File parent = dir.getParentFile();
        if (parent == null) return new ImportResult(false, 0, "Dossier audio interne indisponible.");
        if (!parent.exists() && !parent.mkdirs()) return new ImportResult(false, 0, "Impossible de créer le dossier audio interne.");
        File staging = new File(parent, "husary-muallim.importing");
        deleteRecursive(staging);
        if (!staging.mkdirs()) return new ImportResult(false, 0, "Impossible de préparer l’import audio.");

        int copied = 0;
        ContentResolver resolver = context.getContentResolver();
        try (InputStream raw = resolver.openInputStream(uri)) {
            if (raw == null) throw new IllegalStateException("Fichier sélectionné illisible");
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
                ZipEntry entry;
                byte[] buffer = new byte[64 * 1024];
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) { zip.closeEntry(); continue; }
                    String name = new File(entry.getName()).getName();
                    if (!VERSE_FILE.matcher(name).matches()) { zip.closeEntry(); continue; }
                    File target = new File(staging, name);
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                        int n;
                        while ((n = zip.read(buffer)) >= 0) out.write(buffer, 0, n);
                    }
                    copied++;
                    zip.closeEntry();
                }
            }
            if (copied < EXPECTED_VERSE_FILES
                    || !new File(staging, "001001.mp3").isFile()
                    || !new File(staging, "114006.mp3").isFile()) {
                deleteRecursive(staging);
                return new ImportResult(false, copied,
                    "Pack incomplet : " + copied + " fichiers versets trouvés, " + EXPECTED_VERSE_FILES + " attendus.");
            }
            deleteRecursive(dir);
            if (!staging.renameTo(dir)) {
                if (!dir.mkdirs()) throw new IllegalStateException("Impossible d’activer le pack importé");
                File[] files = staging.listFiles();
                if (files == null) throw new IllegalStateException("Pack importé illisible");
                for (File source : files) copyFile(source, new File(dir, source.getName()));
                deleteRecursive(staging);
            }
            return new ImportResult(true, copied, "Pack Al-Husary Muʿallim installé localement : " + copied + " versets.");
        } catch (Throwable error) {
            deleteRecursive(staging);
            String message = error.getMessage();
            return new ImportResult(false, copied, "Import audio impossible : " + (message == null ? error.getClass().getSimpleName() : message));
        }
    }

    private File fileNamed(String name) { return new File(dir, name); }

    private static int countVerseFiles(File folder) {
        File[] files = folder.listFiles((d, name) -> VERSE_FILE.matcher(name).matches());
        return files == null ? 0 : files.length;
    }

    private static void copyFile(File source, File target) throws Exception {
        try (InputStream in = new java.io.FileInputStream(source);
             BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
            byte[] buffer = new byte[64 * 1024];
            int n;
            while ((n = in.read(buffer)) >= 0) out.write(buffer, 0, n);
        }
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursive(child);
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}

package com.quransafeguard.hifz.preview;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Personal/offline Al-Husary Muallim pack. No runtime network code and no Hifz-state dependency.
 * Preferred delivery is an embedded build-time asset pack. Manual local ZIP import remains as a
 * fallback for test builds. Audio is never allowed to mutate repetitions, promotions or cursors.
 */
final class HifzAudioPack {
    static final int EXPECTED_VERSE_FILES = 6236;
    private static final Pattern VERSE_FILE = Pattern.compile("[0-9]{6}\\.mp3");
    private static final String EMBEDDED_ROOT = "audio/husary-muallim";
    private static final String EMBEDDED_MANIFEST = EMBEDDED_ROOT + "/source.json";

    static final class ImportResult {
        final boolean ok;
        final int files;
        final String message;
        ImportResult(boolean ok, int files, String message) { this.ok = ok; this.files = files; this.message = message; }
    }

    private final Context context;
    private final File dir;
    private volatile EmbeddedInfo embeddedInfo;

    private static final class EmbeddedInfo {
        final int count;
        final String source;
        final boolean valid;
        EmbeddedInfo(int count, String source, boolean valid) {
            this.count = count;
            this.source = source;
            this.valid = valid;
        }
    }

    HifzAudioPack(Context context) {
        this.context = context.getApplicationContext();
        this.dir = new File(this.context.getFilesDir(), "audio/husary-muallim");
    }

    boolean installed() { return embeddedInstalled() || localInstalled(); }

    boolean embeddedInstalled() { return embedded().valid; }

    int installedFileCount() {
        EmbeddedInfo info = embedded();
        return info.valid ? info.count : countVerseFiles(dir);
    }

    String sourceLabel() {
        EmbeddedInfo info = embedded();
        if (info.valid) return info.source;
        if (localInstalled()) return "Pack local importé";
        return "Aucun pack audio valide";
    }

    /** Configure the player from the embedded asset first, then the legacy private file fallback. */
    void setDataSource(MediaPlayer player, VerseRef verse) throws Exception {
        String name = fileNameFor(verse);
        if (embeddedInstalled()) {
            try (AssetFileDescriptor afd = context.getAssets().openFd(EMBEDDED_ROOT + "/" + name)) {
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return;
            }
        }
        File local = fileNamed(name);
        if (!local.isFile()) throw new IllegalStateException("Audio manquant pour " + verse);
        player.setDataSource(local.getAbsolutePath());
    }

    boolean hasVerse(VerseRef verse) {
        String name = fileNameFor(verse);
        if (embeddedInstalled()) {
            try (AssetFileDescriptor ignored = context.getAssets().openFd(EMBEDDED_ROOT + "/" + name)) {
                return true;
            } catch (Exception ignored) { return false; }
        }
        return fileNamed(name).isFile();
    }

    File fileFor(VerseRef verse) { return fileNamed(fileNameFor(verse)); }

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

    private EmbeddedInfo embedded() {
        EmbeddedInfo cached = embeddedInfo;
        if (cached != null) return cached;
        synchronized (this) {
            cached = embeddedInfo;
            if (cached != null) return cached;
            cached = inspectEmbedded();
            embeddedInfo = cached;
            return cached;
        }
    }

    private EmbeddedInfo inspectEmbedded() {
        try (InputStream in = context.getAssets().open(EMBEDDED_MANIFEST)) {
            String json = readUtf8(in);
            JSONObject root = new JSONObject(json);
            int count = root.optInt("fileCount", root.optInt("expectedVerseFiles", 0));
            String source = root.optString("source", root.optString("baseUrl", "EveryAyah · Husary Muallim"));
            if (count != EXPECTED_VERSE_FILES) return new EmbeddedInfo(count, source, false);
            try (AssetFileDescriptor first = context.getAssets().openFd(EMBEDDED_ROOT + "/001001.mp3");
                 AssetFileDescriptor last = context.getAssets().openFd(EMBEDDED_ROOT + "/114006.mp3")) {
                boolean valid = first.getLength() > 0 && last.getLength() > 0;
                return new EmbeddedInfo(count, source, valid);
            }
        } catch (Exception absent) {
            return new EmbeddedInfo(0, "", false);
        }
    }

    private boolean localInstalled() {
        return fileNamed("001001.mp3").isFile()
            && fileNamed("114006.mp3").isFile()
            && countVerseFiles(dir) >= EXPECTED_VERSE_FILES;
    }

    private static String fileNameFor(VerseRef verse) {
        return String.format(Locale.ROOT, "%03d%03d.mp3", verse.getSurah(), verse.getAyah());
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

    private static String readUtf8(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) >= 0) out.write(buffer, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
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

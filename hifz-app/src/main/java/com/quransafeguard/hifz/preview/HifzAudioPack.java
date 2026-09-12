package com.quransafeguard.hifz.preview;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;

import com.quransafeguard.hifz.core.QuranCanon;
import com.quransafeguard.hifz.core.VerseRef;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Personal/offline Al-Husary Muallim pack. Import is staged, verified, then activated atomically.
 * Runtime Android code never downloads audio and audio state never mutates Hifz progress.
 */
final class HifzAudioPack {
    static final int EXPECTED_VERSE_FILES = 6236;
    static final String PACK_FILE_NAME = "Quran-Hifz-Husary-Muallim.zip";
    static final String RECOMMENDED_FOLDER = "Téléchargements/QuranHifz/";

    private static final Pattern VERSE_FILE = Pattern.compile("[0-9]{6}\\.mp3");
    private static final String SOURCE_JSON = "source.json";
    private static final String SHA256_MANIFEST = "sha256.txt";
    private static final String UPSTREAM_CHECKSUM = "upstream_checksum.md5";
    private static final String VERIFIED_MARKER = ".verified-6236-v1";
    private static final String EMBEDDED_ROOT = "audio/husary-muallim";
    private static final String EMBEDDED_MANIFEST = EMBEDDED_ROOT + "/source.json";
    private static final String BACKUP_DIR = "husary-muallim.previous";
    private static final String IMPORT_PREFIX = "husary-muallim.importing-";

    // Import needs room for the verified extracted pack while the downloaded ZIP remains in Downloads.
    private static final long MIN_IMPORT_FREE_BYTES = 3_200_000_000L;
    private static final long MAX_VERSE_BYTES = 15L * 1024L * 1024L;
    private static final long MAX_METADATA_BYTES = 16L * 1024L * 1024L;
    private static final long MAX_TOTAL_EXTRACTED_BYTES = 4_000_000_000L;

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
        recoverInterruptedActivation();
    }

    boolean installed() { return localInstalled() || embeddedInstalled(); }
    boolean embeddedInstalled() { return embedded().valid; }

    int installedFileCount() {
        if (localInstalled()) return EXPECTED_VERSE_FILES;
        EmbeddedInfo info = embedded();
        return info.valid ? info.count : 0;
    }

    String sourceLabel() {
        if (localInstalled()) return "Pack local · Al-Husary Muʿallim";
        EmbeddedInfo info = embedded();
        if (info.valid) return info.source;
        return "Aucun pack audio valide";
    }

    /** Configure the player from the durable local pack first, then a legacy embedded fallback. */
    void setDataSource(MediaPlayer player, VerseRef verse) throws Exception {
        String name = fileNameFor(verse);
        if (localInstalled()) {
            File local = fileNamed(name);
            if (!local.isFile()) throw new IllegalStateException("Audio local manquant pour " + verse);
            player.setDataSource(local.getAbsolutePath());
            return;
        }
        if (embeddedInstalled()) {
            try (AssetFileDescriptor afd = context.getAssets().openFd(EMBEDDED_ROOT + "/" + name)) {
                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                return;
            }
        }
        throw new IllegalStateException("Pack audio Al-Husary Muʿallim non installé");
    }

    boolean hasVerse(VerseRef verse) {
        String name = fileNameFor(verse);
        if (localInstalled()) return fileNamed(name).isFile();
        if (embeddedInstalled()) {
            try (AssetFileDescriptor ignored = context.getAssets().openFd(EMBEDDED_ROOT + "/" + name)) {
                return true;
            } catch (Exception ignored) { return false; }
        }
        return false;
    }

    File fileFor(VerseRef verse) { return fileNamed(fileNameFor(verse)); }

    ImportResult importZip(Uri uri) {
        File parent = dir.getParentFile();
        if (parent == null) return new ImportResult(false, 0, "Dossier audio interne indisponible.");
        if (!parent.exists() && !parent.mkdirs()) return new ImportResult(false, 0, "Impossible de créer le dossier audio interne.");
        if (!parent.isDirectory() || !parent.canWrite()) return new ImportResult(false, 0, "Dossier audio interne non accessible en écriture.");

        recoverInterruptedActivation();
        cleanupStaleImports(parent);
        long usable = parent.getUsableSpace();
        if (usable > 0L && usable < MIN_IMPORT_FREE_BYTES) {
            return new ImportResult(false, 0,
                "Espace insuffisant pour installer l’audio. Libérez au moins 3,2 Go puis réessayez.");
        }

        File staging = new File(parent, IMPORT_PREFIX + System.currentTimeMillis());
        if (!staging.mkdirs() || !staging.isDirectory()) {
            return new ImportResult(false, 0, "Impossible de préparer l’import audio.");
        }

        Set<String> copiedNames = new HashSet<>();
        ContentResolver resolver = context.getContentResolver();
        long totalExtracted = 0L;
        try (InputStream raw = resolver.openInputStream(uri)) {
            if (raw == null) throw new IllegalStateException("Fichier sélectionné illisible");
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(raw))) {
                ZipEntry entry;
                byte[] buffer = new byte[64 * 1024];
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) { zip.closeEntry(); continue; }
                    String name = new File(entry.getName()).getName();
                    boolean verseFile = VERSE_FILE.matcher(name).matches();
                    boolean metadata = SOURCE_JSON.equals(name) || SHA256_MANIFEST.equals(name) || UPSTREAM_CHECKSUM.equals(name);
                    if (!verseFile && !metadata) { zip.closeEntry(); continue; }
                    if (verseFile) {
                        if (!isCanonicalVerseFile(name)) throw new IllegalStateException("Fichier verset hors canon : " + name);
                        if (!copiedNames.add(name)) throw new IllegalStateException("Verset dupliqué dans le pack : " + name);
                    }
                    long declared = entry.getSize();
                    long maxEntry = verseFile ? MAX_VERSE_BYTES : MAX_METADATA_BYTES;
                    if (declared > maxEntry) throw new IllegalStateException("Entrée audio anormalement volumineuse : " + name);

                    File target = new File(staging, name);
                    long entryBytes = 0L;
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            if (n <= 0) continue;
                            entryBytes += n;
                            totalExtracted += n;
                            if (entryBytes > maxEntry || totalExtracted > MAX_TOTAL_EXTRACTED_BYTES) {
                                throw new IllegalStateException("Pack audio trop volumineux ou invalide");
                            }
                            out.write(buffer, 0, n);
                        }
                    }
                    if (target.length() <= 0) throw new IllegalStateException("Fichier vide : " + name);
                    zip.closeEntry();
                }
            }

            if (copiedNames.size() != EXPECTED_VERSE_FILES || !hasAllCanonicalFiles(staging)) {
                throw new IllegalStateException(
                    "Pack incomplet : " + copiedNames.size() + " / " + EXPECTED_VERSE_FILES + " versets canoniques trouvés");
            }

            verifySourceMetadata(staging);
            verifySha256Manifest(staging);
            writeVerifiedMarker(staging);
            activateVerifiedPack(staging);
            return new ImportResult(true, EXPECTED_VERSE_FILES,
                "✓ 6 236 / 6 236 versets vérifiés · Al-Husary Muʿallim · audio hors ligne prêt.");
        } catch (Throwable error) {
            deleteRecursive(staging);
            String message = error.getMessage();
            return new ImportResult(false, copiedNames.size(),
                "Import audio impossible : " + (message == null ? error.getClass().getSimpleName() : message));
        }
    }

    private void activateVerifiedPack(File staging) throws Exception {
        File parent = dir.getParentFile();
        if (parent == null) throw new IllegalStateException("Dossier audio interne indisponible");
        File backup = new File(parent, BACKUP_DIR);
        deleteRecursive(backup);

        boolean hadCurrent = dir.exists();
        if (hadCurrent && !dir.renameTo(backup)) {
            throw new IllegalStateException("Impossible de sécuriser le pack audio existant");
        }
        if (!staging.renameTo(dir)) {
            if (hadCurrent && backup.exists()) backup.renameTo(dir);
            throw new IllegalStateException("Impossible d’activer le pack audio vérifié");
        }
        if (!isVerifiedLocalDirectory(dir)) {
            deleteRecursive(dir);
            if (hadCurrent && backup.exists()) backup.renameTo(dir);
            throw new IllegalStateException("Vérification finale du pack audio échouée");
        }
        deleteRecursive(backup);
    }

    /** Recover the last known-good pack if Android stopped the app between the two atomic renames. */
    private void recoverInterruptedActivation() {
        File parent = dir.getParentFile();
        if (parent == null || !parent.isDirectory()) return;
        File backup = new File(parent, BACKUP_DIR);
        if (dir.exists()) {
            if (isVerifiedLocalDirectory(dir)) deleteRecursive(backup);
            return;
        }
        if (isVerifiedLocalDirectory(backup)) backup.renameTo(dir);
    }

    private static void cleanupStaleImports(File parent) {
        File[] stale = parent.listFiles((folder, name) -> name.startsWith(IMPORT_PREFIX));
        if (stale == null) return;
        for (File file : stale) deleteRecursive(file);
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

    private boolean localInstalled() { return isVerifiedLocalDirectory(dir); }

    private static boolean isVerifiedLocalDirectory(File folder) {
        return folder != null && folder.isDirectory()
            && new File(folder, VERIFIED_MARKER).isFile()
            && new File(folder, SOURCE_JSON).isFile()
            && new File(folder, SHA256_MANIFEST).isFile()
            && new File(folder, "001001.mp3").isFile()
            && new File(folder, "114006.mp3").isFile()
            && countVerseFiles(folder) == EXPECTED_VERSE_FILES;
    }

    private static boolean hasAllCanonicalFiles(File folder) {
        for (int surah = 1; surah <= 114; surah++) {
            int max = QuranCanon.INSTANCE.ayahCount(surah);
            for (int ayah = 1; ayah <= max; ayah++) {
                File file = new File(folder, String.format(Locale.ROOT, "%03d%03d.mp3", surah, ayah));
                if (!file.isFile() || file.length() <= 0) return false;
            }
        }
        return true;
    }

    private static boolean isCanonicalVerseFile(String name) {
        if (!VERSE_FILE.matcher(name).matches()) return false;
        try {
            int surah = Integer.parseInt(name.substring(0, 3));
            int ayah = Integer.parseInt(name.substring(3, 6));
            return surah >= 1 && surah <= 114 && ayah >= 1 && ayah <= QuranCanon.INSTANCE.ayahCount(surah);
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    private static void verifySourceMetadata(File folder) throws Exception {
        File sourceFile = new File(folder, SOURCE_JSON);
        if (!sourceFile.isFile()) throw new IllegalStateException("source.json absent du pack audio");
        JSONObject root;
        try (InputStream in = new FileInputStream(sourceFile)) {
            root = new JSONObject(readUtf8(in));
        }
        if (root.optInt("fileCount", 0) != EXPECTED_VERSE_FILES) {
            throw new IllegalStateException("source.json annonce un nombre de versets incorrect");
        }
        String identity = (root.optString("source", "") + " " + root.optString("reciter", "") + " " + root.optString("baseUrl", ""))
            .toLowerCase(Locale.ROOT);
        if (!identity.contains("husary")) throw new IllegalStateException("source.json ne correspond pas à Al-Husary Muʿallim");
    }

    private static void verifySha256Manifest(File folder) throws Exception {
        File manifest = new File(folder, SHA256_MANIFEST);
        if (!manifest.isFile()) throw new IllegalStateException("sha256.txt absent du pack audio");
        Map<String,String> expected = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(manifest), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                int split = line.indexOf("  ");
                if (split <= 0) throw new IllegalStateException("Ligne SHA-256 invalide");
                String hash = line.substring(0, split).trim().toLowerCase(Locale.ROOT);
                String name = line.substring(split + 2).trim();
                if (hash.length() != 64 || !isCanonicalVerseFile(name)) throw new IllegalStateException("Entrée SHA-256 invalide : " + name);
                if (expected.put(name, hash) != null) throw new IllegalStateException("Doublon SHA-256 : " + name);
            }
        }
        if (expected.size() != EXPECTED_VERSE_FILES) {
            throw new IllegalStateException("Manifest SHA-256 incomplet : " + expected.size() + " / " + EXPECTED_VERSE_FILES);
        }
        for (Map.Entry<String,String> entry : expected.entrySet()) {
            String actual = sha256(new File(folder, entry.getKey()));
            if (!actual.equals(entry.getValue())) throw new IllegalStateException("SHA-256 incorrect : " + entry.getKey());
        }
    }

    private static void writeVerifiedMarker(File folder) throws Exception {
        File marker = new File(folder, VERIFIED_MARKER);
        try (FileOutputStream out = new FileOutputStream(marker)) {
            out.write("Quran Hifz audio pack verified: 6236 canonical ayahs + SHA-256\n".getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[128 * 1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                if (n > 0) digest.update(buffer, 0, n);
            }
        }
        StringBuilder hex = new StringBuilder(64);
        for (byte b : digest.digest()) hex.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return hex.toString();
    }

    private static String fileNameFor(VerseRef verse) {
        return String.format(Locale.ROOT, "%03d%03d.mp3", verse.getSurah(), verse.getAyah());
    }

    private File fileNamed(String name) { return new File(dir, name); }

    private static int countVerseFiles(File folder) {
        File[] files = folder.listFiles((d, name) -> VERSE_FILE.matcher(name).matches());
        return files == null ? 0 : files.length;
    }

    private static String readUtf8(InputStream input) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) {
            if (n > 0) out.write(buffer, 0, n);
        }
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

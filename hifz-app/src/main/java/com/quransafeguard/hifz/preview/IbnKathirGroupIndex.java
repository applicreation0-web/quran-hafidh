package com.quransafeguard.hifz.preview;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.zip.InflaterInputStream;

/**
 * Read-only documentary interval index, NOT a memorization/progress engine.
 *
 * Source: spa5k/tafsir_api, Darussalam abridged English Ibn Kathir,
 * commit eb82bb6294efe30ad5c135c03b1864afaa70e855; WORK 05 audit.
 * Intervals are maximal runs of equal digital commentary, not certified
 * editorial headings. No copyrighted commentary, title or introduction here.
 */
final class IbnKathirGroupIndex {
    static final String SOURCE_COMMIT = "eb82bb6294efe30ad5c135c03b1864afaa70e855";
    static final int EXPECTED_GROUPS = 1903;
    static final int EXPECTED_VERSES = 6236;
    private static final String ENDPOINTS_SHA256 = "deffec6e775965d7b1f717a63fc4d0d60c1933540aacefc13e1333d30c49be62";

    // Compact lossless representation of all 1,903 end-ayah boundaries by surah.
    // Decoded format: one line "surah:end1,end2,..." for every surah 1..114.
    // It deliberately contains no English tafsir text and no legacy passages.
    private static final String BOUNDARIES_ZLIB_BASE64 = "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";

    static final class Group {
        final String id;
        final int surah;
        final int startAyah;
        final int endAyah;
        Group(int surah, int startAyah, int endAyah) {
            this.surah = surah;
            this.startAyah = startAyah;
            this.endAyah = endAyah;
            this.id = String.format(Locale.ROOT, "IKEN%03d_%03d_%03d", surah, startAyah, endAyah);
        }
        String navigationRange() { return surah + ":" + startAyah + "–" + endAyah; }
    }

    private final List<List<Group>> bySurah;
    private final int totalGroups;
    private static volatile IbnKathirGroupIndex shared;

    static IbnKathirGroupIndex shared() {
        IbnKathirGroupIndex found = shared;
        if (found != null) return found;
        synchronized (IbnKathirGroupIndex.class) {
            if (shared == null) shared = new IbnKathirGroupIndex(decodeEndpoints());
            return shared;
        }
    }

    static String decodeEndpoints() {
        try {
            byte[] compressed = Base64.getDecoder().decode(BOUNDARIES_ZLIB_BASE64);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(compressed))) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                    if (out.size() > 20000) throw new IllegalStateException("Unexpected size");
                }
            }
            byte[] raw = out.toByteArray();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b & 255));
            if (!ENDPOINTS_SHA256.equals(hex.toString())) throw new IllegalStateException("Ibn Kathir source hash mismatch");
            return new String(raw, StandardCharsets.UTF_8);
        } catch (Exception invalid) {
            throw new IllegalStateException("Verified Ibn Kathir boundaries unavailable", invalid);
        }
    }

    /** Same pure Java parser in JVM tests and Android. Strict, fail-closed. */
    static IbnKathirGroupIndex parseForTest(String text) {
        return new IbnKathirGroupIndex(text);
    }

    private IbnKathirGroupIndex(String csv) {
        String[] lines = csv.trim().split("\\n");
        if (lines.length != 114) throw new IllegalStateException("114 surahs required");
        List<List<Group>> complete = new ArrayList<>();
        complete.add(Collections.emptyList()); // 1-indexed
        int groups = 0;
        int verses = 0;
        for (int i = 0; i < lines.length; i++) {
            String[] parts = lines[i].split(":", -1);
            if (parts.length != 2 || Integer.parseInt(parts[0]) != i + 1)
                throw new IllegalStateException("Bad surah order at " + (i + 1));
            String[] ends = parts[1].split(",", -1);
            List<Group> segment = new ArrayList<>();
            int start = 1;
            for (String item : ends) {
                int end = Integer.parseInt(item);
                if (end < start || end > 286) throw new IllegalStateException("Bad interval " + (i + 1) + ":" + start);
                segment.add(new Group(i + 1, start, end));
                groups++;
                verses += end - start + 1;
                start = end + 1;
            }
            complete.add(Collections.unmodifiableList(segment));
        }
        if (groups != EXPECTED_GROUPS || verses != EXPECTED_VERSES)
            throw new IllegalStateException("Incompletely indexed Quran: " + groups + "/" + verses);
        if (complete.get(50).size() != 8 || complete.get(51).size() != 7 || complete.get(53).size() != 8
                || complete.get(114).get(complete.get(114).size()-1).endAyah != 6)
            throw new IllegalStateException("Work 05 control samples disagree");
        bySurah = Collections.unmodifiableList(complete);
        totalGroups = groups;
    }

    int count() { return totalGroups; }

    List<Group> groupsForSurah(int surah) {
        if (surah < 1 || surah > 114) throw new IllegalArgumentException("Surah " + surah);
        return bySurah.get(surah);
    }

    Group containing(int surah, int ayah) {
        if (ayah < 1) return null;
        List<Group> groups = groupsForSurah(surah);
        int lo = 0, hi = groups.size()-1;
        while (lo <= hi) {
            int mid = (lo+hi)>>>1;
            Group g = groups.get(mid);
            if (ayah < g.startAyah) hi = mid-1;
            else if (ayah > g.endAyah) lo = mid+1;
            else return g;
        }
        return null;
    }
}

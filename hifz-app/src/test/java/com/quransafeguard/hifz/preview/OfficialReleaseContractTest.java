package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Regression contract for the official post-audit Quran Hifz 0.7.x release. */
public final class OfficialReleaseContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void readerBootMaskAndRevealUseOfficialContract() throws Exception {
        String reader = read("hifz-app/src/main/assets/hifzreader/reader.js");
        String index = read("hifz-app/src/main/assets/hifzreader/index.html");

        assertTrue("prepare must complete before native ready", reader.indexOf("prepare();") < reader.indexOf("N?.ready();"));
        assertTrue("runtime E-Ink setter required", reader.contains("setEink(value)"));
        assertTrue("line ids must be normalized", reader.contains("new Set") && reader.contains("String("));
        assertFalse("mask must not be based on cell count", reader.contains("Math.ceil(n*percent/100)"));
        assertFalse("mask must not slice N cells", reader.contains("cells.slice(n-take)"));
        assertFalse("non-scrollable reader must not call window.scrollBy", reader.contains("window.scrollBy"));
        assertTrue("reveal must use an explicit Mushaf translation", reader.contains("--reveal-shift") || reader.contains("translateY"));
        assertFalse("opening Tafsir must not pre-shift the whole centered page using reveal padding", reader.contains("padding-bottom:var(--reveal-pad)"));
        assertTrue("CSP must explicitly allow the local boot nonce", index.contains("'nonce-hifz-local'"));
        assertTrue("reader must force light color scheme", index.contains("color-scheme:light") || index.contains("color-scheme: light"));
    }

    @Test public void tafsirUiAndPackagingAreCompactAndUnified() throws Exception {
        String study = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        String multi = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/MultiTafsirRepository.java");
        String gradle = read("hifz-app/build.gradle.kts");

        assertTrue("Tafsir identity must stay on one line", study.contains("title.setSingleLine(true)"));
        assertTrue("A-/A+ must use compact controls", study.contains("tafsirTextControl") || study.contains("tafsirCompactButton"));
        assertTrue("edition tabs must use compact controls", study.contains("tafsirEditionButton") || study.contains("tafsirCompactTab"));
        assertFalse("Quran Hifz must not copy divergent reader109/audio.json", gradle.contains("reader109/audio.json"));
        assertFalse("Tafsir loader must not retain Base64 decoding", multi.contains("android.util.Base64") || multi.contains("Base64.decode"));
        assertFalse("Tafsir asset paths must not use .gz.b64.part", multi.contains(".gz.b64.part"));
        assertTrue("Tafsir rights metadata must be enforced", multi.contains("personal_use_only") && multi.contains("redistribution_approved") && multi.contains("rights_note"));
    }

    @Test public void officialVersionIsIncremented() throws Exception {
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("official update must increment versionCode", gradle.contains("versionCode = 8"));
        assertTrue("official update must identify the 0.7.1 BOOX release", gradle.contains("versionName = \"0.7.1-boox\""));
    }
}

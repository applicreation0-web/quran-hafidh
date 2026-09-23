package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * The one deliberate exception to Quran Hifz's offline-first boundary: a settings-gated
 * (off by default), one-time ML Kit model download to check handwritten-content correctness.
 * Every assertion here mirrors what verifyHifzProductBoundary itself enforces at build time —
 * this test catches the same regression sooner, in a normal JVM test run.
 */
public final class InkContentVerifierSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void internetPermissionIsPairedWithTheGatedVerifierOnly() throws Exception {
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue("INTERNET must be declared for the one gated path",
            manifest.contains("android.permission.INTERNET"));
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("the boundary check must confine network APIs to InkContentVerifier.java",
            gradle.contains("val allowedNetworkFile = \"InkContentVerifier.java\""));
        assertTrue("the boundary check must require the opt-in gate inside that file",
            gradle.contains("advancedWritingVerificationEnabled()"));
        assertTrue("must declare the ML Kit dependency the verifier needs",
            gradle.contains("com.google.mlkit:digital-ink-recognition"));
    }

    @Test public void verifierChecksTheOptInGateBeforeAnyNetworkUse() throws Exception {
        String verifier = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/InkContentVerifier.java");
        assertTrue("must check the gate as its very first action in verify()",
            verifier.contains("if (!prefs.advancedWritingVerificationEnabled()) {\n            callback.onDisabled();\n            return;\n        }"));
        assertTrue("must compare against the real verse text, not a placeholder",
            verifier.contains("VerseText.get(context).textFor(verse)"));
        assertTrue("must use the tashkil/alef-insensitive comparison, not raw string equality",
            verifier.contains("ArabicTextComparison.anyMatches"));
    }

    @Test public void settingDefaultsOffAndIsExposedInSettings() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must default to disabled on fresh install",
            prefs.contains(".putBoolean(\"advancedWritingVerificationEnabled\", false)"));
        assertTrue("must expose a getter Settings and InkContentVerifier both rely on",
            prefs.contains("public boolean advancedWritingVerificationEnabled()"));
        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("must be a visible, explicit toggle in Settings, not a hidden flag",
            settings.contains("\"Vérification avancée de l’écriture\""));
    }

    @Test public void verseTextComesFromTheRealTanzilDerivedCorpus() throws Exception {
        String verseText = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/VerseText.java");
        assertTrue("must load the generated per-verse asset, not invent text at runtime",
            verseText.contains("\"reader109/verses_text.json\""));
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("the asset must be synced into the app's own assets like waqf.json already is",
            gradle.contains("reader109/verses_text.json")
                && gradle.contains("into(\"reader109\")"));
    }
}

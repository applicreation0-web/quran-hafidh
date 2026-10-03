package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported: nothing in Diagnostic shows which build is actually installed, which matters once
 * updates ship regularly (0.7.6 onward) and a learner needs to confirm which version they're
 * running. BuildConfig requires buildFeatures.buildConfig=true (off by default on modern AGP),
 * so both the Gradle flag and the Diagnostic text are checked together — one without the other
 * either fails to compile or silently shows nothing.
 */
public final class DiagnosticShowsInstalledVersionSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void gradleGeneratesBuildConfigAndDiagnosticReadsItsVersion() throws Exception {
        String gradle = read("hifz-app/build.gradle.kts");
        assertTrue("BuildConfig generation must be explicitly enabled (AGP defaults it off)",
            gradle.contains("buildFeatures {\n        buildConfig = true\n    }"));

        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("Diagnostic must show the installed version name",
            settings.contains("BuildConfig.VERSION_NAME"));
        assertTrue("Diagnostic must show the installed version code, so two builds sharing a "
                + "version name (e.g. two debug builds of the same release) stay distinguishable",
            settings.contains("BuildConfig.VERSION_CODE"));
    }
}

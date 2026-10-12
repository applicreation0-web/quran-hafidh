package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Historical name retained so the source/JVM test inventory stays stable. */
public final class IbnKathirHeadingSelectionTest {
    private static String read(String path) throws Exception {
        Path file=Paths.get(path);
        if(!Files.exists(file))file=Paths.get("..",path);
        return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
    }

    @Test public void legacyHeadingAssetsAreAuditOnlyAndExcludedFromPackaging() throws Exception {
        String gradle=read("hifz-app/build.gradle.kts");
        assertTrue(Files.isDirectory(Paths.get("hifz-app/src/main/assets/ibn_kathir_headings")) ||
            Files.isDirectory(Paths.get("../hifz-app/src/main/assets/ibn_kathir_headings")));
        assertTrue(gradle.contains("assets.exclude(\"ibn_kathir_headings/**\")"));
    }

    @Test public void supersededTitleClassesAreAuditOnlyAndExcludedFromMainCompilation() throws Exception {
        String gradle=read("hifz-app/build.gradle.kts");
        assertTrue(gradle.contains("java.exclude(\"**/IbnKathirOfflineTitles.java\")"));
        assertTrue(gradle.contains("java.exclude(\"**/IbnKathirTitlePack.java\")"));
    }

    @Test public void readerUsesOnlyThePrivateQuranComTitleProvider() throws Exception {
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(reader.contains("IbnKathirQuranComPrivateTitles.sourceHeadings(this,group)"));
        assertFalse(reader.contains("IbnKathirOfflineTitles.verifiedHeadings(group)"));
        assertFalse(reader.contains("IbnKathirTitlePack.sourceHeadings(this,group)"));
    }
}

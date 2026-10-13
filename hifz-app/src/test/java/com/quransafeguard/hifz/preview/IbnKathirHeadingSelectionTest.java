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
        Path main=Paths.get("hifz-app/src/main/assets/ibn_kathir_headings");
        if(!Files.exists(Paths.get("hifz-app")))main=Paths.get("..").resolve(main);
        Path audit=main.getParent().getParent().getParent().resolve(
            "audit/legacy-ibn-kathir/assets/ibn_kathir_headings");
        assertFalse(Files.exists(main));
        assertEquals(8,Files.list(audit).filter(p -> p.getFileName().toString()
            .startsWith("chunk-")).count());
    }

    @Test public void supersededTitleClassesAreAuditOnlyAndExcludedFromMainCompilation() throws Exception {
        Path main=Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview");
        if(!Files.exists(Paths.get("hifz-app")))main=Paths.get("..").resolve(main);
        Path audit=main.getParent().getParent().getParent().getParent().getParent().getParent()
            .resolve("audit/legacy-ibn-kathir/java");
        assertFalse(Files.exists(main.resolve("IbnKathirOfflineTitles.java")));
        assertFalse(Files.exists(main.resolve("IbnKathirTitlePack.java")));
        assertTrue(Files.isRegularFile(audit.resolve("IbnKathirOfflineTitles.java")));
        assertTrue(Files.isRegularFile(audit.resolve("IbnKathirTitlePack.java")));
    }

    @Test public void readerUsesOnlyThePrivateQuranComTitleProvider() throws Exception {
        String reader=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/StudyReaderActivity.java");
        assertTrue(reader.contains("IbnKathirQuranComPrivateTitles.approvedTitle(this,group)"));
        assertFalse(reader.contains("IbnKathirOfflineTitles.verifiedHeadings(group)"));
        assertFalse(reader.contains("IbnKathirTitlePack.sourceHeadings(this,group)"));
    }
}

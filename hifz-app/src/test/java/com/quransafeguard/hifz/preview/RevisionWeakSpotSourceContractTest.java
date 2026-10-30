package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
/** Legacy weak-spot tests repurposed: flags are no longer part of revision UX. */
public final class RevisionWeakSpotSourceContractTest {
    private static String read(String path)throws Exception{
        java.nio.file.Path p=java.nio.file.Paths.get(path);
        if(!java.nio.file.Files.exists(p))p=java.nio.file.Paths.get("..",path);
        return new String(java.nio.file.Files.readAllBytes(p),java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test public void noManualFlagOrWeakHighlightInAnyRevision()throws Exception{
        String src=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(src.contains("weakMarkMode"));
        assertFalse(src.contains("toggleWeakVerse("));
        assertFalse(src.contains("\"Marquer\""));
        assertFalse(src.contains("setHighlightVerses(prefs.murajaahWeakVerses())"));
        assertTrue(src.contains("correctionStore.recordReveal("));
        assertTrue(src.contains("renderActiveCorrection()"));
    }
    @Test public void previouslySavedFlagDataIsNotDestroyed()throws Exception{
        String prefs=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("public List<VerseRef> murajaahWeakVerses()"));
        assertFalse(read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java").contains("clearCorrectedMurajaahVerse"));
    }
}

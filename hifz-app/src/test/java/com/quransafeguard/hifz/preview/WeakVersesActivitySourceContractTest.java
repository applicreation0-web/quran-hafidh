package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** The legacy global marker list stays reachable; no fictitious 3-pass score remains. */
public final class WeakVersesActivitySourceContractTest {
    private static String read(String path) throws Exception {
        Path file=Paths.get(path);
        if(!Files.exists(file))file=Paths.get("..",path);
        return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
    }
    @Test public void listIsAvailableAndOnlyShowsPendingCorrection() throws Exception {
        String activity=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeakVersesActivity.java");
        String prefs=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String settings=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String manifest=read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue(manifest.contains("<activity android:name=\".WeakVersesActivity\" android:exported=\"false\" />"));
        assertTrue(settings.contains("weakVersesSetting=Ui.settingRow(this,\"Repères faibles\""));
        assertTrue(activity.contains("new ArrayList<>(prefs.murajaahWeakVerses())"));
        assertTrue(activity.contains("À corriger"));
        assertFalse(activity.contains("WEAK_VERSE_CLEAN_STREAK_TO_CLEAR"));
        assertFalse(prefs.contains("WEAK_VERSE_CLEAN_STREAK_TO_CLEAR"));
        assertTrue(activity.contains("StudyReaderActivity.EXTRA_JUMP_VERSE"));
        assertTrue(activity.contains("prefs.toggleMurajaahWeakVerse(verse)"));
    }
}

package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
/** Retire flag entry points without erasing pre-existing stored user data. */
public final class WeakVersesActivitySourceContractTest {
    private static String read(String path)throws Exception{
        java.nio.file.Path p=java.nio.file.Paths.get(path);
        if(!java.nio.file.Files.exists(p))p=java.nio.file.Paths.get("..",path);
        return new String(java.nio.file.Files.readAllBytes(p),java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test public void settingsAndManifestHideRetiredFlagScreen()throws Exception{
        String settings=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        String manifest=read("hifz-app/src/main/AndroidManifest.xml");
        assertFalse(settings.contains("Repères faibles"));
        assertFalse(settings.contains("weakVersesSetting"));
        assertFalse(settings.contains("new Intent(this,WeakVersesActivity.class)"));
        assertFalse(manifest.contains("<activity android:name=\".WeakVersesActivity\""));
    }
    @Test public void oldPreferencesRemainAvailableForRestoreCompatibility()throws Exception{
        String prefs=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("public List<VerseRef> murajaahWeakVerses()"));
    }
}

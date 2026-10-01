package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * The standing list of weak-spot verses (task follow-up to #33/#34): every currently-flagged verse
 * from prefs.murajaahWeakVerses(), each showing its clean-recall streak toward the automatic
 * WEAK_VERSE_CLEAN_STREAK_TO_CLEAR-in-a-row clear, with a tap-through to its exact location in
 * Lecture (via StudyReaderActivity's EXTRA_JUMP_PAGE/EXTRA_JUMP_VERSE) and a direct "Démarquer"
 * action so un-flagging doesn't require re-arming "Marquer" and finding the verse again on the
 * Mushaf.
 */
public final class WeakVersesActivitySourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void activityIsRegisteredAndReachableFromSettings() throws Exception {
        String manifest = read("hifz-app/src/main/AndroidManifest.xml");
        assertTrue("the new screen must actually be declared, or Android refuses to launch it",
            manifest.contains("<activity android:name=\".WeakVersesActivity\" android:exported=\"false\" />"));

        String settings = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/SettingsActivity.java");
        assertTrue("Settings must offer a way in, next to Diagnostic",
            settings.contains("weakVersesSetting=Ui.settingRow(this,\"Repères faibles\",\"\",v->startActivity(new Intent(this,WeakVersesActivity.class)));"));
        assertTrue("the row's value must reflect the live count, not a static label",
            settings.contains("weakValue.setText(weakCount==0?\"Aucun\":weakCount+\" verset(s)\");"));
    }

    @Test public void listShowsStreakProgressAndLinksIntoTheMushaf() throws Exception {
        String activity = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/WeakVersesActivity.java");
        assertTrue("must read the actual flagged list, not some other verse source",
            activity.contains("List<VerseRef> weak = new ArrayList<>(prefs.murajaahWeakVerses());"));
        assertTrue("must show each verse's real progress toward the auto-clear threshold",
            activity.contains("int streak = prefs.weakVerseStreak(verse);"));
        assertTrue("must reuse the same threshold constant Phase 3's decay logic actually clears at "
                + "(never a second hardcoded copy of the number 3)",
            activity.contains("streak + \"/\" + HifzPrefs.WEAK_VERSE_CLEAN_STREAK_TO_CLEAR"));
        assertTrue("tapping a verse must jump Lecture to its exact page",
            activity.contains("intent.putExtra(StudyReaderActivity.EXTRA_JUMP_PAGE, page);"));
        assertTrue("and highlight the exact verse once there, not just land on the right page",
            activity.contains("intent.putExtra(StudyReaderActivity.EXTRA_JUMP_VERSE, verse.toString());"));
        assertTrue("must offer a direct way to un-flag from the list itself",
            activity.contains("prefs.toggleMurajaahWeakVerse(verse);"));
    }

    @Test public void prefsExposesTheStreakAndItsThresholdPublicly() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the threshold must be public so the UI never hardcodes its own copy of it",
            prefs.contains("public static final int WEAK_VERSE_CLEAN_STREAK_TO_CLEAR = 3;"));
        assertTrue("must expose a per-verse streak reader",
            prefs.contains("public int weakVerseStreak(VerseRef verse) {"));
    }
}

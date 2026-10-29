package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Corrected contract: the old automatic 3-pass decay is completely inactive. */
public final class RevisionWeakSpotDecaySourceContractTest {
    private String read(String name) throws Exception {
        String path="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"+name;
        java.nio.file.Path file=Paths.get(path);
        if (!Files.exists(file)) file=Paths.get("..",path);
        return new String(Files.readAllBytes(file),StandardCharsets.UTF_8);
    }

    @Test public void noAutomaticThreePassClearAndExplicitConfirmedRemoval() throws Exception {
        String prefs=read("HifzPrefs.java");
        String session=read("HifzSessionActivity.java");
        assertFalse(prefs.contains("WEAK_VERSE_CLEAN_STREAK_TO_CLEAR"));
        assertFalse(prefs.contains("advanceWeakVerseStreaks("));
        assertFalse(session.contains("advanceWeakVerseStreaksForActiveSession"));
        assertTrue(prefs.contains("public boolean clearCorrectedMurajaahVerse(VerseRef verse)"));
        assertTrue(session.contains("if (correctionState.assisted)"));
        assertTrue(session.contains("prefs.clearCorrectedMurajaahVerse(corrected)"));
        assertTrue(session.contains("correctionStore.revealDuringControl(correctionState)"));
    }

    @Test public void normalPassiveAndFinaleNeverRemoveCorrectiveFlags() throws Exception {
        String session=read("HifzSessionActivity.java");
        assertTrue(session.contains("if (active && correctionState == null)"));
        assertTrue(session.contains("eligibleActiveCorrectionPages()"));
        assertTrue(session.contains("if (active && correctionState != null && !\"DONE\".equals(correctionState.stage))"));
        assertTrue(session.contains("if (!prefs.completeActiveMurajaah(next, sessionDate.toString(), label))"));
    }
}

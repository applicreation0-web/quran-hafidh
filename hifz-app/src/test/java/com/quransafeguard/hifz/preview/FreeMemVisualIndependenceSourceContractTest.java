package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** CP-04: portrait-safe free memorisation controls and strict Hifz independence. */
public final class FreeMemVisualIndependenceSourceContractTest {
    private static String source() throws Exception {
        Path path = Paths.get("hifz-app/src/main/java/com/quransafeguard/hifz/preview/FreeMemActivity.java");
        if (!Files.exists(path)) path = Paths.get("..", path.toString());
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    @Test public void passageCaptionCanWrapWithoutPortraitTruncation() throws Exception {
        String free = source();
        assertTrue(free.contains("selection.setMaxLines(2)"));
        assertFalse(free.contains("selection.setSingleLine(true)"));
    }

    @Test public void everyMaskTargetMatchesTheFortyEightDpPrimaryTouchGrammar() throws Exception {
        String free = source();
        assertTrue(free.contains("b.setMinWidth(Ui.dp(this,48))"));
        assertTrue(free.contains("b.setMinHeight(Ui.dp(this,48))"));
        assertTrue(free.contains("new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,Ui.dp(this,48))"));
        assertTrue(free.contains("int[] levels={0,25,50,75,100};"));
    }

    @Test public void freeMemPersistsItsOwnStateButNeverCreditsStructuredHifz() throws Exception {
        String free = source();
        assertTrue(free.contains("getSharedPreferences(\"free_mem_preview\""));
        for (String key : new String[]{"page", "count", "mask", "start", "end"})
            assertTrue("Missing independent state key " + key,
                free.contains("putInt(\"" + key + "\"") || free.contains("putString(\"" + key + "\""));
        for (String forbidden : new String[]{
                "setSabqiProgress", "completeSabqi", "completeStabilization",
                "completeSnowball", "validateLearningFinalReview",
                "validateConsolidationFinalReview", "activeMurajaahCorpus"})
            assertFalse("Free memorisation must never credit Hifz: " + forbidden,
                free.contains(forbidden));
    }

    @Test public void selectionCounterAudioNavigationAndResumeRemainPresent() throws Exception {
        String free = source();
        for (String required : new String[]{
                "onVerseTap(VerseRef verse)", "Retirer une répétition",
                "Ajouter une répétition", "Remettre à zéro", "HifzAudioDialog",
                "Page suivante", "Page précédente", "Sourate", "Hizb",
                "mushaf.show(page,refs,lines,mask)", "updateMaskButtons()", "save()"})
            assertTrue("Missing free memorisation contract: " + required,
                free.contains(required));
    }
}

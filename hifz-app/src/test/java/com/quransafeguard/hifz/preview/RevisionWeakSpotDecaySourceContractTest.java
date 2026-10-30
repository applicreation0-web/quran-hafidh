package com.quransafeguard.hifz.preview;
import org.junit.Test;
import static org.junit.Assert.*;
/** Migration safety: do not advance obsolete weak-verse streaks on active completion. */
public final class RevisionWeakSpotDecaySourceContractTest {
    private static String read(String path)throws Exception{
        java.nio.file.Path p=java.nio.file.Paths.get(path);
        if(!java.nio.file.Files.exists(p))p=java.nio.file.Paths.get("..",path);
        return new String(java.nio.file.Files.readAllBytes(p),java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test public void activeCompletionIsIndependentFromRetiredFlagDecay()throws Exception{
        String session=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse(session.contains("advanceWeakVerseStreaksForActiveSession"));
        assertFalse(session.contains("prefs.advanceWeakVerseStreaks"));
        assertTrue(session.contains("ActiveReviewCorrectionPlan.select("));
        assertTrue(session.contains("correctionStore.pageRecalled("));
        assertTrue(session.contains("correctionStore.clear()"));
    }
    @Test public void noOtherHifzProgramsDependOnCorrection()throws Exception{
        String plan=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ActiveReviewCorrectionPlan.java");
        String store=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ActiveReviewCorrectionStore.java");
        assertFalse(plan.contains("HifzPrefs"));
        assertFalse(store.contains("completeActiveMurajaah"));
        assertFalse(store.contains("setSabqiProgress"));
        assertFalse(store.contains("setItqanProgress"));
    }
}

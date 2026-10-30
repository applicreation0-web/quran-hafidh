package com.quransafeguard.hifz.preview;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;

/** Every revealed page is automatically scheduled for visible then masked replay. */
public final class RevisionRevealDoesNotMarkHintSourceContractTest {
    private static String read(String repoPath)throws Exception {
        Path path=Paths.get(repoPath);
        if(!Files.exists(path))path=Paths.get("..",repoPath);
        return new String(Files.readAllBytes(path),StandardCharsets.UTF_8);
    }
    @Test public void revealAutomaticallySchedulesWholePageWithoutManualFlags()throws Exception {
        String session=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("correctionStore.recordReveal("));
        assertTrue(session.contains("ActiveReviewCorrectionPlan.select("));
        assertTrue(session.contains("correctionStore.pageReread("));
        assertTrue(session.contains("correctionStore.pageRecalled("));
        assertFalse(session.contains("revealDoesNotMarkHintShown"));
        assertFalse(session.contains("weakMarkMode"));
        assertFalse(session.contains("Révéler n’enregistre rien"));
    }
    @Test public void saveUsesActualPageNotFlaggedVerse()throws Exception {
        String store=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ActiveReviewCorrectionStore.java");
        String plan=read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ActiveReviewCorrectionPlan.java");
        assertTrue(store.contains("activeReviewRevealedPagesV1"));
        assertTrue(store.contains("originPage"));
        assertTrue(plan.contains("select(Set<Integer> revealedPages)"));
        assertFalse(plan.contains("flagged"));
    }
}

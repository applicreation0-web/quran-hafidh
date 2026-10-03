package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source contract for the revised spatial active-recall UI. */
public final class RevisionActiveLandmarkLineSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void activeSessionKeepsOfficialAmorcesInTheirRealMushafPositions() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertFalse("the duplicated Arabic prompt above the Mushaf is retired", session.contains("activeCuePrompt"));
        assertTrue("all page cues are applied in place", session.contains("applyActiveRevisionPageCues()"));
        assertTrue(session.contains("semanticPassages.readerCuesForPage(currentPage)"));
        assertTrue("active recall remains fully masked outside explicit visible ranges", session.contains("currentMask = 100"));
        assertTrue("no old half-line landmark fallback is used", session.contains("mushaf.setLandmarkLines(null, null)"));
    }

    @Test public void activeActionsAreOnlyRevealNextAndFinish() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        int method = session.indexOf("private void updateMurajaahActions()");
        int active = session.indexOf("if (active) {", method);
        int passive = session.indexOf("List<MurajaahSegment> segments", active);
        assertTrue("active action block must be locatable", method >= 0 && active > method && passive > active);
        String activeBlock = session.substring(active, passive);
        assertTrue(activeBlock.contains("\"Révéler\""));
        assertTrue(activeBlock.contains("\"Suivant\""));
        assertTrue(activeBlock.contains("\"Terminer\""));
        assertFalse(activeBlock.contains("Amorce suivante"));
        assertFalse(activeBlock.contains("Valider jusqu’ici"));
    }

    @Test public void earlyFinishRequiresExplicitConfirmation() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("Temps effectué : "));
        assertTrue(session.contains("La séance n’a pas encore atteint sa durée cible."));
        assertTrue(session.contains("Terminer quand même"));
    }
}

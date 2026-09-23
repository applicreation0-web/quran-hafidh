package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Reported as confusing: an apprenant can press "Révéler" in Révision active without ever pressing
 * "Marquer", and nothing records that the verse was difficult — Révéler is a whole-page reveal with
 * no memory of which verse prompted it, while "Marquer" is the one deliberate, verse-precise gesture
 * that actually flags a weak spot for passive review. Since the two can't be merged (a page reveal
 * can't identify a single verse), the fix is a one-time reminder the first time Révéler is touched
 * in a session, not a change to what either button records.
 */
public final class RevisionRevealDoesNotMarkHintSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    private static String method(String source, String start, String end) {
        int a = source.indexOf(start);
        int b = source.indexOf(end, a + start.length());
        if (a < 0 || b < 0 || b <= a) throw new IllegalStateException("Method boundary missing: " + start);
        return source.substring(a, b);
    }

    @Test public void revealExplainsItRecordsNothingTheFirstTimePerSessionOnlyInActiveMode() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("must track whether the hint was already shown this session, so it fires once, not on every touch",
            session.contains("private boolean revealDoesNotMarkHintShown;"));

        String configure = method(session,
            "private void configureRevealButton(Button button) {", "private boolean consumeReveal()");
        assertTrue("the hint must live inside the active-only branch, never fire for Sabqi/Itqan's own Révéler",
            configure.contains("if(MURAJAAH_ACTIVE.equals(mode)){\n                    activeRevealedPages.add(currentPage);\n"
                + "                    if(!revealDoesNotMarkHintShown){"));
        assertTrue("must flip the flag before showing it, so a second touch never repeats the toast",
            configure.contains("revealDoesNotMarkHintShown=true;"));
        assertTrue("the message must name both actions so the distinction is explicit, not just say 'nothing happened'",
            configure.contains("Révéler n’enregistre rien : utilisez « Marquer » pour signaler un verset difficile."));
    }
}

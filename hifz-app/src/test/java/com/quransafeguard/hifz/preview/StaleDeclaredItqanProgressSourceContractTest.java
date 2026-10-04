package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Defense-in-depth alongside the real fix in HifzSessionActivity (see
 * StaleItqanUnitMismatchSourceContractTest for the bug this was originally, incorrectly, thought
 * to already cover): if inProgressAnchoringEntry() ever does find a legacy-anchoringQueue match
 * for a unit that sits entirely inside a declared Plage Acquise, currentAnchoringEntry must not
 * hand it back as something to build from scratch — the learner has explicitly said this material
 * is already known. Genuine reinforcement-lap material is deliberately left alone:
 * physicalUnitsInLeg revisits every unit forever regardless of Stabilisation/Acquis status, so
 * only this one-time declared-material case needs the discard.
 */
public final class StaleDeclaredItqanProgressSourceContractTest {
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

    @Test public void currentAnchoringEntryDiscardsInProgressWorkOnDeclaredMaterial() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "public AnchoringQueue.Entry currentAnchoringEntry(GeometryRepository geometry) {",
            "\n    }\n\n    /** True only when every verse");
        assertTrue("must check the in-progress entry against the declared corpus before trusting it",
            m.contains("fullyDeclaredAcquired(inProgress.start, inProgress.end, geometry)"));
        assertTrue("a declared-material in-progress session must be discarded, not silently kept",
            m.contains("discardStaleItqanProgress()"));
        assertTrue("a genuinely non-declared in-progress unit must still resume normally",
            m.contains("if (!fullyDeclaredAcquired(inProgress.start, inProgress.end, geometry)) return inProgress;"));
    }

    @Test public void fullyDeclaredAcquiredChecksEveryVerseAgainstItqanRanges() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "private boolean fullyDeclaredAcquired(", "\n    }");
        assertTrue("must read the learner's own declared corpus, not the auto-grown eligible one",
            m.contains("List<VerseRange> declared = itqanRanges();"));
        assertTrue("every verse in the range must be covered, not just the first/last",
            m.contains("for (VerseRef verse : geometry.versesForRange(start, end)) {"));
        assertTrue("a single uncovered verse must fail the whole check",
            m.contains("if (!covered) return false;"));
    }

    @Test public void discardResetsExactlyTheInProgressSessionFields() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        String m = method(prefs, "boolean discardStaleItqanProgress() {", "\n    }");
        assertTrue("must clear the rep counter the in-progress check gates on",
            m.contains("\"itqanRep\", 0"));
        assertTrue("must clear the block counter the in-progress check gates on",
            m.contains("\"itqanBlockIndex\", 0"));
        assertTrue("must clear the saved unit identity so a fresh pick isn't mistaken for a resume",
            m.contains("\"itqanUnitStart\", \"\""));
    }
}

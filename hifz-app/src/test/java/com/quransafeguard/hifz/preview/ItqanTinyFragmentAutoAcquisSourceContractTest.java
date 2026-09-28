package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Requested directly: when today's Itqān sub-block is reduced to just 1 or 2 physical lines (a
 * leg-boundary or Sabqi-frontier remnant — see StabilizationHalfPagePolicy.appendSegment, which
 * only ever returns such a tiny block as a whole, un-split segment, never as a byproduct of
 * splitting a larger one), running a full repeated session on it is pure overhead. Instead it is
 * credited straight to Acquis — skipping both the repetition protocol and the whole Consolidation
 * snowball stage a normal block would go through — and the same render immediately chains into the
 * next real (7-8 line) portion, same day, rather than making the learner wait until tomorrow.
 */
public final class ItqanTinyFragmentAutoAcquisSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void hifzPrefsCreditsATinyFragmentDirectlyToAcquiredNeverStabilized() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("must guard the fragment size to exactly 1 or 2 lines",
            prefs.contains("if (lineIds == null || lineIds.isEmpty() || lineIds.size() > 2)"));
        assertTrue("a line not already Acquired must move straight into the acquired set, never "
                + "the stabilized set (that would still need a later Consolidation pass)",
            prefs.contains("if (state != ProgressState.ACQUIRED) {\n"
                + "                    learned.remove(lineId);\n"
                + "                    stabilized.remove(lineId);\n"
                + "                    acquired.add(lineId);\n"
                + "                }"));
        String tinyBlockMethod = methodBody(prefs, "boolean completeItqanTinyBlockV6(");
        assertFalse("must never enroll the fragment in the weekly Consolidation snowball",
            tinyBlockMethod.contains("weeklySnowballAppendEntries"));
        assertFalse("must never set lastItqanDate/lastItqanLabel — doing so would lock the day and "
                + "block the same-day chain into the next real portion",
            tinyBlockMethod.contains("lastItqanDate"));
    }

    @Test public void renderItqanInterceptsATinyWorkingBlockBeforeTheBonusDialogLogic() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("must be checked right after the working block/currentLineIds are resolved, "
                + "before the P3 finish-the-page bonus logic runs",
            session.contains("if (rep == 0 && workingUnit.lineIds.size() <= 2 && autoChainDepth < MAX_ITQAN_AUTO_CHAIN\n"
                + "                && !prefs.entryIsFullyStabilizedOrAcquired(anchoringEntry, geometry)) {\n"
                + "            creditTinyItqanBlockAndChain(autoChainDepth);\n"
                + "            return;\n"
                + "        }"));
        assertTrue("a reinforcement lap (already fully Acquired material) must keep the existing "
                + "full-protocol reinforcement pass, never the tiny-fragment fast path",
            session.contains("!prefs.entryIsFullyStabilizedOrAcquired(anchoringEntry, geometry)"));
        assertTrue("the chain must be bounded so no pathological run of tiny fragments can recurse "
                + "unboundedly",
            session.contains("private static final int MAX_ITQAN_AUTO_CHAIN = 5;"));
    }

    @Test public void creditTinyItqanBlockAndChainMirrorsValidateItqansOwnBookkeepingThenRenders() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        String method = methodBody(session, "private void creditTinyItqanBlockAndChain(int autoChainDepth) {");
        assertTrue("must compute nextBlock/finalBlock exactly like validateItqan",
            method.contains("int nextBlock = itqanBlockIndex + 1;")
                && method.contains("boolean finalBlock = nextBlock >= itqanBlockCount;"));
        assertTrue("must advance the perpetual rotation past this unit once it's the final block, "
                + "exactly like a normal validation would",
            method.contains("if (finalBlock && !prefs.advanceItqanRotationPast(itqanUnit.end)) {"));
        assertTrue("must chain into the next render with an incremented depth counter",
            method.contains("renderItqan(autoChainDepth + 1);"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new IllegalStateException("Method not found: " + signature);
        int braceDepth = 0;
        int i = source.indexOf('{', start);
        int bodyStart = i;
        for (; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') braceDepth++;
            else if (c == '}') {
                braceDepth--;
                if (braceDepth == 0) return source.substring(bodyStart, i + 1);
            }
        }
        throw new IllegalStateException("Unbalanced braces for method: " + signature);
    }
}

package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * P3's persistence plumbing: completeStabilizationBlockV6 now also folds the +1/+2 bonus
 * bookkeeping into its single existing commit (never a second commit for the bonus part), and a
 * pending bonus decision can only be saved while itqanRep is still 0 — before that, corrupting or
 * losing the bookkeeping must only ever cost a future offer, never a wrong progression commit, so
 * both readers degrade to empty/null instead of throwing.
 */
public final class ItqanBonusPersistenceSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void completeStabilizationBlockV6FoldsBonusBookkeepingIntoItsOneCommit() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the bonus subset must be an explicit parameter, not re-derived",
            prefs.contains("boolean completeStabilizationBlockV6(List<String> lineIds, int nextBlockIndex, boolean finalBlock,\n"
                + "                                         VerseRef unitStart, VerseRef unitEnd, VerseRef nextCursor,\n"
                + "                                         String date, String label, List<String> bonusLineIds) {"));
        assertTrue("consumed-bonus tracking must be read from the same commit's own snapshot of prior state",
            prefs.contains("LinkedHashSet<String> consumedBonus = optionalLineIdSet(\"itqanConsumedBonusLineIds\");"));
        assertTrue("finishing the parent (finalBlock) must reset the consumed-bonus tracking",
            prefs.contains(".putString(\"itqanConsumedBonusLineIds\",\n"
                + "                    finalBlock ? \"[]\" : lineIdsJson(consumedBonus))"));
        assertTrue("a committed block's own pending decision snapshot must always be cleared, "
                + "never left to be misread by a later block",
            prefs.contains(".putString(\"itqanBonusSnapshotV1\", \"\");"));
        assertTrue("still exactly one editor built from the frozen pre-commit state",
            prefs.contains("SharedPreferences.Editor e = p.edit()"));
        assertTrue("and still exactly one return e.commit() closing out the whole method",
            prefs.contains("return e.commit();\n        }\n    }\n\n    /** Tolerant counterpart to v6LineIdSet"));
    }

    @Test public void bonusReadersDegradeSilentlyRatherThanThrow() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("a missing or corrupt consumed-bonus key must default to empty, not throw like "
                + "the strict schema6 v6LineIdSet does",
            prefs.contains("private LinkedHashSet<String> optionalLineIdSet(String key) {"));
        assertTrue(prefs.contains("} catch (Exception corruptOrMissing) {\n"
            + "            return new LinkedHashSet<>();\n        }"));
        assertTrue("a missing or corrupt bonus snapshot must resolve to null, never throw",
            prefs.contains("ItqanPlanSnapshot itqanBonusSnapshot() {"));
        assertTrue(prefs.contains("} catch (Exception corrupt) {\n            return null;\n        }"));
    }

    @Test public void bonusDecisionCanOnlyBeSavedBeforeTheFirstRepetition() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue(prefs.contains("boolean saveItqanBonusDecision(ItqanPlanSnapshot snapshot) {\n"
            + "        if (p.getInt(\"itqanRep\", 0) != 0)\n"
            + "            throw new IllegalStateException(\"Itqān bonus decision must be settled before rep 1\");"));
    }

    @Test public void theOnlyCallSiteNowPassesAnExplicitBonusArgument() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue("the call site must pass a real bonus-lines argument, computed from the "
                + "settled decision (see ItqanBonusDialogSourceContractTest for the full offer/"
                + "dialog wiring this now derives from)",
            session.contains("sessionDate.toString(), label, bonusLineIds);"));
        assertFalse("the old 8-argument call must be gone, not left alongside a second call site",
            session.contains("sessionDate.toString(), label);"));
    }
}

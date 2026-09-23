package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Follow-up audit of the promotion API surface after removing markPromotedConsolidated (#28):
 * confirmed the exact same dead-wrapper pattern on lowerBound/promotedFrontier/upperTailStart/
 * setPromotedFrontier and isForcedPromoted — zero callers anywhere in the repo, main or test.
 *
 * lowerBound/promotedFrontier/upperTailStart carried a comment claiming they were "retained for
 * migration visibility", but nothing anywhere actually read them for display — the real migration
 * code (migrateV1ToV2) reads the raw "promotedFrontier" SharedPreferences key directly
 * (safeRef(p.getString("promotedFrontier", ...))) rather than through this wrapper, so the
 * wrapper's own justification never held. isForcedPromoted was a convenience predicate over
 * forcedPromotedRanges() with no caller at all, unlike its siblings isPromoted/
 * isUnconsolidatedPromoted which are actually used.
 *
 * addPromotedVerses and isUnconsolidatedPromoted are deliberately left alone: both are exercised
 * by androidTest instrumented tests (HifzPrefsV4MigrationInstrumentedTest, AstraFixInstrumentedTest,
 * AnchoringProtocolContinuityInstrumentedTest), the same bar that kept completeItqanUnitAndConsolidate
 * around — "no main-app caller" alone isn't enough to call something dead if a real test still
 * exercises it; "no caller anywhere, including tests" is the actual bar this repo applies.
 */
public final class PromotionApiDeadWrappersRemovedSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void deadPromotionWrappersAreGoneAndLiveOnesRemain() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");

        assertFalse("lowerBound() had zero callers anywhere, despite its own comment", prefs.contains("public VerseRef lowerBound()"));
        assertFalse("promotedFrontier() had zero callers anywhere", prefs.contains("public VerseRef promotedFrontier()"));
        assertFalse("upperTailStart() had zero callers anywhere", prefs.contains("public VerseRef upperTailStart()"));
        assertFalse("setPromotedFrontier() had zero callers anywhere", prefs.contains("public void setPromotedFrontier("));
        assertFalse("isForcedPromoted() had zero callers anywhere, unlike isPromoted/isUnconsolidatedPromoted",
            prefs.contains("public boolean isForcedPromoted("));

        assertTrue("the raw migration-time read of the old promotedFrontier key must still work "
                + "(this is what actually reads it, not the removed wrapper)",
            prefs.contains("safeRef(p.getString(\"promotedFrontier\", \"2:74\"), new VerseRef(2,74));"));
        assertTrue("isPromoted must remain (used by the real completeConsolidationSessionV6 path)",
            prefs.contains("public boolean isPromoted(VerseRef verse) {"));
        assertTrue("isUnconsolidatedPromoted must remain (exercised by androidTest)",
            prefs.contains("public boolean isUnconsolidatedPromoted(VerseRef verse) {"));
        assertTrue("addPromotedVerses must remain (exercised by androidTest)",
            prefs.contains("public boolean addPromotedVerses(List<VerseRef> verses, boolean forcedPromotion) {"));
        assertTrue("forcedPromotedRanges must remain (still consulted directly by the real V6 completion paths)",
            prefs.contains("public List<VerseRange> forcedPromotedRanges() {"));
    }
}

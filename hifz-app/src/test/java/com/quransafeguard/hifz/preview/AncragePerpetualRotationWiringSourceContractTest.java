package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * P4/A03: the Ancrage/Stabilisation queue's own selection (HifzPrefs.currentAnchoringEntry) never
 * stops once every entry is Stabilisé/Acquis — it walks ItqanRotationPolicy's perpetual
 * TAIL(Al-Hujurāt→An-Nās)/FRONT(Al-Baqara→wherever Sabqi has actually reached) leg+cursor forever,
 * visiting every physical unit in bounds on its turn regardless of status. Status only decides,
 * once a unit is reached, whether HifzSessionActivity builds it or gives it another reinforcement
 * pass — never whether or when it is reached.
 */
public final class AncragePerpetualRotationWiringSourceContractTest {
    private static String read(String repoPath) throws Exception {
        Path direct = Paths.get(repoPath);
        if (Files.exists(direct)) return new String(Files.readAllBytes(direct), StandardCharsets.UTF_8);
        Path parent = Paths.get("..", repoPath);
        if (Files.exists(parent)) return new String(Files.readAllBytes(parent), StandardCharsets.UTF_8);
        throw new IllegalStateException("Missing repository file: " + repoPath);
    }

    @Test public void rotationStateIsPersistedWithATailStartingDefault() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("HifzPrefs must delegate rotation persistence to ItqanRegimeStore",
            prefs.contains("return ItqanRegimeStore.readRotationState(p);"));
        String store = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanRegimeStore.java");
        assertTrue(store.contains("ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, ItqanRotationPolicy.TAIL_START, everCompleted);"));
        assertTrue("an install upgrading from the older leg-only rotation must be migrated, not reset blind",
            store.contains("if (p.contains(LEGACY_LEG)) {") && store.contains("LEGACY_LEG = \"p4AncrageLeg\""));
    }

    @Test public void currentAnchoringEntryNoLongerFiltersByStabilisationStatus() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("the historical cyclic list-index scan must stay gone",
            !prefs.contains("int start = anchoringQueueIndex(queue.size());"));
        assertTrue("selection must no longer stop once every entry is Stabilisé/Acquis",
            !prefs.contains("List<AnchoringQueue.Entry> notDone = new ArrayList<>();"));
        assertTrue("selection must walk ItqanRotationPolicy's own leg+cursor state",
            prefs.contains("ItqanRotationPolicy.State original = itqanRotationState();"));
        assertTrue("selection must go through the pure rotation pick",
            prefs.contains("ItqanRotationPolicy.pick(original,"));
        String policy = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/ItqanRotationPolicy.java");
        assertTrue("a leg boundary (not an empty not-done pool) is what triggers the flip",
            policy.contains("state = onLegExhausted(state);"));
    }

    @Test public void aLegFlipIsPersistedButAStableLegIsNotRewrittenEveryCall() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        assertTrue("only an actual state change (leg, cursor or the post-An-Nās latch) should trigger a write",
            prefs.contains("boolean moved = state.leg != original.leg || !state.cursor.equals(original.cursor)\n"
                + "            || state.initialTailCompleted != original.initialTailCompleted;\n"
                + "        if (moved && !saveItqanRotationState(state)) {"));
    }

    @Test public void aUnitAlreadyAcquiredGetsAReinforcementPassInsteadOfARegularCredit() throws Exception {
        String session = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java");
        assertTrue(session.contains("boolean reinforcementLap = prefs.entryIsFullyStabilizedOrAcquired(anchoringEntry, geometry);"));
        assertTrue(session.contains("? prefs.completeItqanReinforcementBlock("));
        assertTrue("the rotation cursor must advance past every completed unit, build or reinforcement alike",
            session.contains("if (finalBlock && !prefs.advanceItqanRotationPast(itqanUnit.end)) {"));
    }

    @Test public void reinforcementNeverTouchesProgressionStateOrTheConsolidationSnowball() throws Exception {
        String prefs = read("hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzPrefs.java");
        int start = prefs.indexOf("public boolean completeItqanReinforcementBlock(");
        assertTrue("completeItqanReinforcementBlock must exist", start >= 0);
        int end = prefs.indexOf("\n    }", start);
        String body = prefs.substring(start, end);
        assertTrue(!body.contains("v6StabilizedLineIds"));
        assertTrue(!body.contains("v6AcquiredCreditLineIds"));
        assertTrue(!body.contains("weeklySnowballAppendEntries"));
    }
}

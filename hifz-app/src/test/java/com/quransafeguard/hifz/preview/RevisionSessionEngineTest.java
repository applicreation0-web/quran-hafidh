package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Pure schema6 Révision contract: physical traversal, laps, exact credit and cursor semantics. */
public final class RevisionSessionEngineTest {
    private static RevisionSessionEngine engine() {
        return new RevisionSessionEngine(
            Arrays.asList("L1", "L2", "L3"),
            new LinkedHashSet<>(Arrays.asList("L1", "L2")));
    }

    @Test public void zeroVisitCommitKeepsCursorAndCreditsNothing() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s0", "L2");
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.emptySet());

        assertTrue(result.state.committed());
        assertEquals("L2", result.state.startCursor());
        assertEquals("L2", result.state.endCursor());
        assertEquals("L2", result.nextCursor);
        assertTrue(result.creditedLineIds.isEmpty());
        assertEquals(0, result.state.occurrences());
        assertEquals(0, result.state.laps());
        assertEquals(0L, result.state.elapsedMs());
    }

    @Test public void duplicatePhysicalVisitsCountOccurrencesButCreditUniqueEligibleOnly() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s1", "L1");
        s = e.visit(s, "L1", 1_000L);
        s = e.visit(s, "L2", 2_000L);
        s = e.visit(s, "L3", 3_000L);
        s = e.visit(s, "L1", 4_000L);
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.emptySet());

        assertEquals(4, result.state.occurrences());
        assertEquals(new LinkedHashSet<>(Arrays.asList("L1", "L2", "L3")), result.state.visitedUniqueLineIds());
        assertEquals(new LinkedHashSet<>(Arrays.asList("L1", "L2")), result.creditedLineIds);
        assertEquals(1, result.state.laps());
        assertEquals(10_000L, result.state.elapsedMs());
        assertEquals("L2", result.nextCursor);
    }

    @Test public void lapsCountOnlyActualLastToFirstWrapAndCanExceedOne() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s2", "L2");
        String[] visits = {"L2", "L3", "L1", "L2", "L3", "L1"};
        for (String id : visits) s = e.visit(s, id, 100L);
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.emptySet());

        assertEquals(6, result.state.occurrences());
        assertEquals(2, result.state.laps());
        assertEquals("L1", result.state.endCursor());
        assertEquals("L2", result.nextCursor);
    }

    @Test public void earlyStopAdvancesOnlyFromLastActuallyVisitedLine() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s3", "L1");
        s = e.visit(s, "L1", 500L);
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.emptySet());

        assertEquals(1, result.state.occurrences());
        assertEquals(0, result.state.laps());
        assertEquals("L1", result.state.endCursor());
        assertEquals("L2", result.nextCursor);
        assertEquals(Collections.singleton("L1"), result.creditedLineIds);
    }

    @Test public void alreadyCreditedTodayIsNeverCreditedTwice() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s4", "L1");
        s = e.visit(s, "L1", 100L);
        s = e.visit(s, "L2", 100L);
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.singleton("L1"));

        assertEquals(Collections.singleton("L2"), result.creditedLineIds);
    }

    @Test public void visitedButIneligibleLineNeverGetsCredit() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s5", "L3");
        s = e.visit(s, "L3", 100L);
        RevisionSessionEngine.CommitResult result = e.commit(s, Collections.emptySet());

        assertEquals(1, result.state.occurrences());
        assertTrue(result.creditedLineIds.isEmpty());
        assertEquals("L1", result.nextCursor);
    }

    @Test public void traversalMustFollowRotationAndCommittedSessionCannotMutate() {
        RevisionSessionEngine e = engine();
        RevisionSessionState s = e.start("s6", "L1");
        s = e.visit(s, "L1", 100L);
        final RevisionSessionState open = s;
        expectIllegalState(() -> e.visit(open, "L3", 100L));

        RevisionSessionState committed = e.commit(open, Collections.emptySet()).state;
        assertTrue(committed.committed());
        expectIllegalState(() -> e.visit(committed, "L2", 100L));
    }

    @Test public void invalidStartCursorAndNegativeElapsedFailClosed() {
        RevisionSessionEngine e = engine();
        expectIllegalArgument(() -> e.start("bad", "L9"));
        RevisionSessionState s = e.start("s7", "L1");
        expectIllegalArgument(() -> e.visit(s, "L1", -1L));
        assertFalse(s.committed());
    }

    private static void expectIllegalState(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}

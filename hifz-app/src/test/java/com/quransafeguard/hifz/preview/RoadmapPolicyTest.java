package com.quransafeguard.hifz.preview;

import org.junit.Test;

import java.time.LocalDate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class RoadmapPolicyTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 28);

    private static RoadmapPolicy.Input in(boolean allNewSabqiComplete, LocalDate completedOn,
            boolean baqaraSabqiComplete, boolean initialTailComplete, LocalDate today,
            RoadmapPolicy.Phase currentPhase, boolean closedWeek, int currentLag, int previousLag,
            Integer recoveryReference, RoadmapPolicy.CruiseWeek nextCruiseWeek, RoadmapPolicy.Regulator regulator) {
        return new RoadmapPolicy.Input(allNewSabqiComplete, completedOn, baqaraSabqiComplete,
            initialTailComplete, today, currentPhase, closedWeek, currentLag, previousLag,
            recoveryReference, nextCruiseWeek, regulator);
    }

    /**
     * The user's own real installed state at the time this was written: Sabqi at 2:77 (Al-Baqara
     * not finished), Itqān already partway through the TAIL leg (49:1-49:8 acquired, 49:9-114:6 to
     * stabilize) but nowhere near a first full lap. The Roadmap must recommend exactly the 3/3
     * ratio the user's own Settings already show — never surprise them on day one.
     */
    @Test public void matchesTheUsersOwnRealCurrentState() {
        RoadmapPolicy.Decision d = RoadmapPolicy.decide(in(
            false, null, false, false, MONDAY,
            RoadmapPolicy.Phase.CURRENT, true, 0, 0, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.CURRENT, d.phase);
        assertEquals(3, d.learningDays);
        assertEquals(3, d.itqanDays);
    }

    @Test public void bridgeIsZeroSixOnceBaqaraDoneButFirstTailNotYet() {
        RoadmapPolicy.Decision d = RoadmapPolicy.decide(in(
            false, null, true, false, MONDAY,
            RoadmapPolicy.Phase.BRIDGE, true, 5, 5, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.BRIDGE, d.phase);
        assertEquals(0, d.learningDays);
        assertEquals(6, d.itqanDays);
    }

    @Test public void catchupStaysTwoFourUntilLagReachesZeroAtAClosedWeek() {
        RoadmapPolicy.Decision stillLagging = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CATCHUP, true, 4, 6, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.CATCHUP, stillLagging.phase);
        assertEquals(2, stillLagging.learningDays);
    }

    @Test public void catchupExitsToCruiseOnlyWhenLagHitsZeroAtAClosedWeek() {
        RoadmapPolicy.Decision midWeekZero = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CATCHUP, false, 0, 2, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals("lag==0 mid-week is not a closure signal — must not exit early",
            RoadmapPolicy.Phase.CATCHUP, midWeekZero.phase);

        RoadmapPolicy.Decision closedZero = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CATCHUP, true, 0, 2, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.CRUISE, closedZero.phase);
        assertEquals(4, closedZero.learningDays);
        assertEquals(RoadmapPolicy.CruiseWeek.A_4_2, closedZero.nextCruiseWeek);
        assertEquals(RoadmapPolicy.Regulator.NORMAL, closedZero.nextRegulator);
    }

    @Test public void cruiseAlternatesFourTwoAndThreeThreeWhenLagNeverIncreases() {
        RoadmapPolicy.Decision weekA = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CRUISE, true, 3, 3, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(4, weekA.learningDays);
        assertEquals(RoadmapPolicy.CruiseWeek.B_3_3, weekA.nextCruiseWeek);

        RoadmapPolicy.Decision weekB = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY.plusWeeks(1),
            RoadmapPolicy.Phase.CRUISE, true, 3, 3, null,
            weekA.nextCruiseWeek, weekA.nextRegulator));
        assertEquals(3, weekB.learningDays);
        assertEquals(RoadmapPolicy.CruiseWeek.A_4_2, weekB.nextCruiseWeek);
    }

    @Test public void cruiseMidWeekHoldsTheAlreadyCommittedRatioSteady() {
        RoadmapPolicy.Decision midWeek = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CRUISE, false, 3, 3, null,
            RoadmapPolicy.CruiseWeek.B_3_3, RoadmapPolicy.Regulator.RECOVERY_2_4));
        assertEquals(RoadmapPolicy.Phase.CRUISE, midWeek.phase);
        assertEquals("mid-week must not change the regulator's own committed ratio", 2, midWeek.learningDays);
        assertEquals(RoadmapPolicy.Regulator.RECOVERY_2_4, midWeek.nextRegulator);
    }

    /**
     * The full escalation ladder: a lag increase enters RECOVERY_3_3 (reference = the lag before
     * it rose); a further rise past that reference escalates to RECOVERY_2_4; RECOVERY_2_4 holds
     * until the lag falls back to the reference; then exactly one EXIT_3_3 week; a stable lag
     * after EXIT_3_3 returns to plain NORMAL alternation.
     */
    @Test public void fullRecoveryEscalationAndExitLadder() {
        RoadmapPolicy.Decision d1 = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CRUISE, true, 5, 3, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Regulator.RECOVERY_3_3, d1.nextRegulator);
        assertEquals(3, d1.learningDays);
        assertEquals(Integer.valueOf(3), d1.recoveryReferenceLagLines);

        RoadmapPolicy.Decision d2 = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY.plusWeeks(1),
            RoadmapPolicy.Phase.CRUISE, true, 7, 5, d1.recoveryReferenceLagLines,
            d1.nextCruiseWeek, d1.nextRegulator));
        assertEquals("lag rose past the recovery reference — escalate",
            RoadmapPolicy.Regulator.RECOVERY_2_4, d2.nextRegulator);
        assertEquals(2, d2.learningDays);

        RoadmapPolicy.Decision d3 = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY.plusWeeks(2),
            RoadmapPolicy.Phase.CRUISE, true, 6, 7, d2.recoveryReferenceLagLines,
            d2.nextCruiseWeek, d2.nextRegulator));
        assertEquals("still above reference — stay in 2/4", RoadmapPolicy.Regulator.RECOVERY_2_4, d3.nextRegulator);
        assertEquals(2, d3.learningDays);

        RoadmapPolicy.Decision d4 = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY.plusWeeks(3),
            RoadmapPolicy.Phase.CRUISE, true, 3, 6, d3.recoveryReferenceLagLines,
            d3.nextCruiseWeek, d3.nextRegulator));
        assertEquals("back at/under reference — exactly one EXIT_3_3 week",
            RoadmapPolicy.Regulator.EXIT_3_3, d4.nextRegulator);
        assertEquals(3, d4.learningDays);

        RoadmapPolicy.Decision d5 = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY.plusWeeks(4),
            RoadmapPolicy.Phase.CRUISE, true, 3, 3, d4.recoveryReferenceLagLines,
            d4.nextCruiseWeek, d4.nextRegulator));
        assertEquals("stable after EXIT — back to plain alternation",
            RoadmapPolicy.Regulator.NORMAL, d5.nextRegulator);
        assertNull(d5.recoveryReferenceLagLines);
    }

    @Test public void exitWeekRelapsesBackToRecoveryIfLagRisesAgain() {
        RoadmapPolicy.Decision relapse = RoadmapPolicy.decide(in(
            false, null, true, true, MONDAY,
            RoadmapPolicy.Phase.CRUISE, true, 8, 4, 4,
            RoadmapPolicy.CruiseWeek.B_3_3, RoadmapPolicy.Regulator.EXIT_3_3));
        assertEquals(RoadmapPolicy.Regulator.RECOVERY_3_3, relapse.nextRegulator);
        assertEquals(3, relapse.learningDays);
        assertEquals(Integer.valueOf(4), relapse.recoveryReferenceLagLines);
    }

    @Test public void postSabqiOnlyForTheSameIsoWeekSabqiFinishedIn() {
        LocalDate finishedOn = LocalDate.of(2026, 9, 24); // Thursday
        RoadmapPolicy.Decision sameWeek = RoadmapPolicy.decide(in(
            true, finishedOn, true, true, LocalDate.of(2026, 9, 26),
            RoadmapPolicy.Phase.CRUISE, true, 0, 0, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.POST_SABQI, sameWeek.phase);
        assertEquals(0, sameWeek.learningDays);

        RoadmapPolicy.Decision nextWeek = RoadmapPolicy.decide(in(
            true, finishedOn, true, true, LocalDate.of(2026, 9, 28),
            RoadmapPolicy.Phase.POST_SABQI, true, 0, 0, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
        assertEquals(RoadmapPolicy.Phase.PERMANENT, nextWeek.phase);
        assertEquals(0, nextWeek.learningDays);
    }

    @Test(expected = IllegalArgumentException.class)
    public void requiresCompletionDateOnceAllSabqiIsComplete() {
        RoadmapPolicy.decide(in(true, null, true, true, MONDAY,
            RoadmapPolicy.Phase.PERMANENT, true, 0, 0, null,
            RoadmapPolicy.CruiseWeek.A_4_2, RoadmapPolicy.Regulator.NORMAL));
    }
}

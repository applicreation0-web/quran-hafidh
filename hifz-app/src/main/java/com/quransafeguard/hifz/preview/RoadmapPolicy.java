package com.quransafeguard.hifz.preview;

import java.time.LocalDate;
import java.time.temporal.WeekFields;

/**
 * Pure Roadmap state machine (P4): decides the weekly Apprentissage/Stabilisation ratio and phase
 * label from a snapshot of real progress signals. Never touches persistence, the Itqān engine, or
 * any UI — HifzPrefs/a future RoadmapStore own reading whatever Input this needs and persisting
 * the Decision it returns (including feeding {@code nextCruiseWeek}/{@code nextRegulator}/
 * {@code recoveryReferenceLagLines} back in as next week's Input, and {@code phase} back in as
 * next week's currentPhase — this class carries no state of its own between calls).
 *
 * The Roadmap pilots frequency only, never the Itqān engine's own visiting order.
 */
final class RoadmapPolicy {
    enum Phase { CURRENT, BRIDGE, CATCHUP, CRUISE, POST_SABQI, PERMANENT }
    enum CruiseWeek { A_4_2, B_3_3 }
    enum Regulator { NORMAL, RECOVERY_3_3, RECOVERY_2_4, EXIT_3_3 }

    private RoadmapPolicy() {}

    static final class Input {
        final boolean allNewSabqiComplete;
        /** Null unless allNewSabqiComplete — the date it first became true, never recomputed. */
        final LocalDate allNewSabqiCompletedOn;
        final boolean baqaraSabqiComplete;
        final boolean initialTailComplete;
        final LocalDate today;
        /** The phase last week's Decision left us in — CATCHUP only ends via this feedback loop. */
        final Phase currentPhase;
        /** True only for the one decision made as a week closes; mid-week calls hold steady. */
        final boolean closedWeek;
        final int currentLagLines;
        final int previousClosedWeekLagLines;
        /** Null unless a CRUISE recovery is in progress. */
        final Integer recoveryReferenceLagLines;
        final CruiseWeek nextCruiseWeek;
        final Regulator regulator;

        Input(boolean allNewSabqiComplete, LocalDate allNewSabqiCompletedOn,
              boolean baqaraSabqiComplete, boolean initialTailComplete, LocalDate today,
              Phase currentPhase, boolean closedWeek, int currentLagLines,
              int previousClosedWeekLagLines, Integer recoveryReferenceLagLines,
              CruiseWeek nextCruiseWeek, Regulator regulator) {
            if (allNewSabqiComplete && allNewSabqiCompletedOn == null)
                throw new IllegalArgumentException("allNewSabqiCompletedOn required once Sabqi is complete");
            this.allNewSabqiComplete = allNewSabqiComplete;
            this.allNewSabqiCompletedOn = allNewSabqiCompletedOn;
            this.baqaraSabqiComplete = baqaraSabqiComplete;
            this.initialTailComplete = initialTailComplete;
            this.today = today;
            this.currentPhase = currentPhase == null ? Phase.CURRENT : currentPhase;
            this.closedWeek = closedWeek;
            this.currentLagLines = currentLagLines;
            this.previousClosedWeekLagLines = previousClosedWeekLagLines;
            this.recoveryReferenceLagLines = recoveryReferenceLagLines;
            this.nextCruiseWeek = nextCruiseWeek == null ? CruiseWeek.A_4_2 : nextCruiseWeek;
            this.regulator = regulator == null ? Regulator.NORMAL : regulator;
        }
    }

    static final class Decision {
        final Phase phase;
        final int learningDays;
        final int itqanDays;
        final CruiseWeek nextCruiseWeek;
        final Regulator nextRegulator;
        final Integer recoveryReferenceLagLines;

        private Decision(Phase phase, int learningDays, CruiseWeek nextCruiseWeek,
                          Regulator nextRegulator, Integer recoveryReferenceLagLines) {
            this.phase = phase;
            this.learningDays = learningDays;
            this.itqanDays = 6 - learningDays;
            this.nextCruiseWeek = nextCruiseWeek;
            this.nextRegulator = nextRegulator;
            this.recoveryReferenceLagLines = recoveryReferenceLagLines;
        }
    }

    static Decision decide(Input in) {
        if (in.allNewSabqiComplete) {
            Phase phase = sameIsoWeek(in.today, in.allNewSabqiCompletedOn) ? Phase.POST_SABQI : Phase.PERMANENT;
            return new Decision(phase, 0, in.nextCruiseWeek, in.regulator, in.recoveryReferenceLagLines);
        }
        if (!in.baqaraSabqiComplete) {
            // Even if the Itqān rotation's first TAIL lap already finished, the ratio stays 3/3
            // until Baqara Sabqi itself is done — the rotation simply carries on into FRONT.
            return new Decision(Phase.CURRENT, 3, in.nextCruiseWeek, in.regulator, in.recoveryReferenceLagLines);
        }
        if (!in.initialTailComplete) {
            return new Decision(Phase.BRIDGE, 0, in.nextCruiseWeek, in.regulator, in.recoveryReferenceLagLines);
        }
        if (in.currentPhase == Phase.CATCHUP) {
            if (in.closedWeek && in.currentLagLines == 0) {
                return new Decision(Phase.CRUISE, learningDaysFor(in.nextCruiseWeek),
                    in.nextCruiseWeek, Regulator.NORMAL, null);
            }
            return new Decision(Phase.CATCHUP, 2, in.nextCruiseWeek, in.regulator, in.recoveryReferenceLagLines);
        }
        return decideCruise(in);
    }

    private static Decision decideCruise(Input in) {
        if (!in.closedWeek) {
            // Regulation only ever changes at a closed week's boundary; a mid-week check just
            // reports the ratio already committed for the week in progress.
            return new Decision(Phase.CRUISE, learningDaysForRegulator(in.regulator, in.nextCruiseWeek),
                in.nextCruiseWeek, in.regulator, in.recoveryReferenceLagLines);
        }
        boolean lagIncreased = in.currentLagLines > in.previousClosedWeekLagLines;
        switch (in.regulator) {
            case NORMAL: {
                if (lagIncreased) {
                    return new Decision(Phase.CRUISE, 3, in.nextCruiseWeek,
                        Regulator.RECOVERY_3_3, in.previousClosedWeekLagLines);
                }
                return new Decision(Phase.CRUISE, learningDaysFor(in.nextCruiseWeek),
                    flip(in.nextCruiseWeek), Regulator.NORMAL, null);
            }
            case RECOVERY_3_3: {
                int reference = referenceOf(in);
                if (in.currentLagLines > reference) {
                    return new Decision(Phase.CRUISE, 2, in.nextCruiseWeek, Regulator.RECOVERY_2_4, reference);
                }
                return new Decision(Phase.CRUISE, 3, in.nextCruiseWeek, Regulator.RECOVERY_3_3, reference);
            }
            case RECOVERY_2_4: {
                int reference = referenceOf(in);
                if (in.currentLagLines <= reference) {
                    return new Decision(Phase.CRUISE, 3, in.nextCruiseWeek, Regulator.EXIT_3_3, reference);
                }
                return new Decision(Phase.CRUISE, 2, in.nextCruiseWeek, Regulator.RECOVERY_2_4, reference);
            }
            case EXIT_3_3: {
                if (lagIncreased) {
                    return new Decision(Phase.CRUISE, 3, in.nextCruiseWeek,
                        Regulator.RECOVERY_3_3, in.previousClosedWeekLagLines);
                }
                return new Decision(Phase.CRUISE, learningDaysFor(in.nextCruiseWeek),
                    flip(in.nextCruiseWeek), Regulator.NORMAL, null);
            }
            default:
                throw new IllegalStateException("Unknown regulator: " + in.regulator);
        }
    }

    private static int referenceOf(Input in) {
        return in.recoveryReferenceLagLines != null ? in.recoveryReferenceLagLines : in.previousClosedWeekLagLines;
    }

    private static int learningDaysForRegulator(Regulator regulator, CruiseWeek nextCruiseWeek) {
        switch (regulator) {
            case RECOVERY_3_3:
            case EXIT_3_3:
                return 3;
            case RECOVERY_2_4:
                return 2;
            default:
                return learningDaysFor(nextCruiseWeek);
        }
    }

    private static int learningDaysFor(CruiseWeek week) {
        return week == CruiseWeek.A_4_2 ? 4 : 3;
    }

    private static CruiseWeek flip(CruiseWeek week) {
        return week == CruiseWeek.A_4_2 ? CruiseWeek.B_3_3 : CruiseWeek.A_4_2;
    }

    private static boolean sameIsoWeek(LocalDate a, LocalDate b) {
        WeekFields wf = WeekFields.ISO;
        return a.get(wf.weekBasedYear()) == b.get(wf.weekBasedYear())
            && a.get(wf.weekOfWeekBasedYear()) == b.get(wf.weekOfWeekBasedYear());
    }
}

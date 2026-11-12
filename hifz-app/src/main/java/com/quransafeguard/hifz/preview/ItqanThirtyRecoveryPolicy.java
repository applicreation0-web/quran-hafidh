package com.quransafeguard.hifz.preview;

/**
 * First-pass Itqan ONLY: 10 visible, 10 masked at 50%, 10 masked at 100%.
 * An unsuccessful block is followed by as many *earned* 5 visible + 5 fully
 * masked recovery cycles as necessary. No failed attempt gives V6 credits.
 *
 * The caller owns the dated, frozen physical proof and must atomically persist
 * the counter/target/final-mask reveal state. This class has no preferences.
 */
final class ItqanThirtyRecoveryPolicy {
    static final int BASE = 30;
    static final int RECOVERY = 10;
    static final int MAX_REPS = 1030; // fail closed on corrupt or unbounded state

    private ItqanThirtyRecoveryPolicy() {}

    static boolean eligible(ItqanRegimeStore.UnitPlan plan) {
        return plan != null && plan.regime == ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS
            && plan.protocol == AnchoringQueue.ItqanProtocol.FULL
            && plan.maskProfile == ItqanRegimeStore.MaskProfile.THREE_STAGE_10_10_10;
    }

    static boolean validTarget(int target) {
        return target >= BASE && target <= MAX_REPS
            && (target - BASE) % RECOVERY == 0;
    }

    static int maskForNextRep(int completed) {
        if (completed < 0 || completed >= MAX_REPS) return 0;
        if (completed < 10) return 0;
        if (completed < 20) return 50;
        if (completed < BASE) return 100;
        return (completed - BASE) % RECOVERY < 5 ? 0 : 100;
    }

    static boolean finalRecallRep(int completed) {
        return completed >= 20 && completed < 30
            || completed >= 30 && (completed - 30) % RECOVERY >= 5;
    }

    static boolean shouldExtendAfterRep(int newCompleted, int target, int failedRecallReveals) {
        if (!validTarget(target) || newCompleted != target || failedRecallReveals < 0)
            throw new IllegalArgumentException("Invalid Itqan checkpoint");
        return failedRecallReveals > 0;
    }

    static int nextRecoveryTarget(int target) {
        if (!validTarget(target) || target > MAX_REPS - RECOVERY)
            throw new IllegalStateException("Invalid/overlong Itqan recovery");
        return target + RECOVERY;
    }
}

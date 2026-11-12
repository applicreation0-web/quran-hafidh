package com.quransafeguard.hifz.preview;

/**
 * New Sabqi physical lessons only (frozen version 2).
 * 25 visible + 5 at 25% + 5 at 50% + 5 at 75% + 5 at 100% = 45.
 * Failure adds 5 visible + 5 fully masked; earlier physical receipts stay intact.
 * Legacy frozen version 1 keeps its original 37-repetition protocol.
 */
final class SabqiFortyFiveRecoveryPolicy {
    static final int BASE=45;
    static final int RECOVERY=10;
    static final int MAX_REPS=1045;
    static final int VERSION=2;
    private SabqiFortyFiveRecoveryPolicy() {}

    static boolean validTarget(int target) {
        return target>=BASE && target<=MAX_REPS && (target-BASE)%RECOVERY==0;
    }

    static int maskForNextRep(int completed) {
        if(completed<0 || completed>=MAX_REPS)return 0;
        if(completed<25)return 0;
        if(completed<30)return 25;
        if(completed<35)return 50;
        if(completed<40)return 75;
        if(completed<BASE)return 100;
        return (completed-BASE)%RECOVERY<5 ? 0 : 100;
    }

    static boolean maskedFinalRep(int completed) {
        return (completed>=40 && completed<BASE)
            || (completed>=BASE && (completed-BASE)%RECOVERY>=5);
    }

    static int nextRecoveryTarget(int current) {
        if(!validTarget(current) || current>MAX_REPS-RECOVERY)
            throw new IllegalStateException("Sabqi recovery target invalid");
        return current+RECOVERY;
    }
}

package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import java.time.LocalDate;

/**
 * Crash-safe staging for the pre-An-Nas 2-page / 4-date plan.
 *
 * A frozen plan is never recomputed after a process restart. Corrupt data
 * fails closed, not silently reset. A current legacy Itqan block always wins
 * until explicitly completed. Full V6 credit + rotation integration must
 * append snapshot advancement to the SAME editor, not call recordRep().
 */
final class ItqanFourSessionStore {
    static final String KEY="itqanFourDatePlanV1";
    static final String WEEKLY_KEY="itqanWeeklyPagePlanV2";
    static final String WEEKLY_DATE="itqanWeeklyPageDateV2";
    private ItqanFourSessionStore(){}

    static boolean hasPlan(SharedPreferences p) {
        return p.contains(KEY)||p.contains(WEEKLY_KEY);
    }

    static String keyFor(ItqanFourSessionSnapshot snapshot) {
        return snapshot.weeklyPage()?WEEKLY_KEY:KEY;
    }

    static boolean isCurrentWeeklyDate(SharedPreferences p, ItqanFourSessionSnapshot snapshot, LocalDate date) {
        return !snapshot.weeklyPage() || (date.equals(HifzClock.today())
            && date.toString().equals(p.getString(WEEKLY_DATE,"")));
    }

    static ItqanFourSessionSnapshot read(SharedPreferences p,GeometryRepository geo) {
        if(p==null||geo==null)throw new IllegalArgumentException("Itqan store needs QCF");
        if(p.contains(WEEKLY_KEY)){
            if(p.contains(KEY))throw new IllegalStateException("Conflicting Itqan snapshots");
            String date=p.getString(WEEKLY_DATE,"");
            ItqanFourSessionSnapshot snapshot=ItqanFourSessionSnapshot.decodeWeeklyPage(
                p.getString(WEEKLY_KEY,""),geo);
            int assisted=p.getInt("itqanAssisted",0);
            int finalReveals=p.getInt("itqanFinalReveals",0);
            if(snapshot.sessionIndex>=snapshot.plan.sessions.size()
                    ||snapshot.reps!=p.getInt("itqanRep",0)
                    ||assisted<0||assisted>snapshot.reps||finalReveals<0||finalReveals>assisted
                    ||(date.isEmpty()&&(snapshot.reps!=0||snapshot.blockIndex!=0))
                    ||(!date.isEmpty()&&LocalDate.parse(date).isBefore(LocalDate.parse(snapshot.scheduledWeek))))
                throw new IllegalStateException("Invalid saved Itqan slot date");
            return snapshot;
        }
        String raw=p.getString(KEY,"");
        return raw==null||raw.isEmpty()?null:ItqanFourSessionSnapshot.decode(raw,geo);
    }

    /** Create only on a clean first-pass boundary, never during old OPEN work. */
    static boolean open(SharedPreferences p,ItqanFourSessionSnapshot snapshot,
                        GeometryRepository geo) {
        if(p==null||snapshot==null||geo==null)throw new IllegalArgumentException("Frozen window required");
        synchronized(p) {
            if(hasPlan(p))return false; // never replace a completed or unfinished plan
            if(p.getInt("itqanRep",0)!=0||p.getInt("itqanBlockIndex",0)!=0
                ||!p.getString("itqanUnitStart","").isEmpty()
                ||!p.getString("itqanUnitEnd","").isEmpty())
                throw new IllegalStateException("Finish existing legacy Itqan before new window");
            if(snapshot.sessionIndex!=0||snapshot.blockIndex!=0||snapshot.reps!=0)
                throw new IllegalStateException("Only clean first slot can be opened");
            // Serialize and decode once before writing: verifies 604-page QCF
            // continuity, same-surah subblocks, size and SHA-256 fingerprint.
            ItqanFourSessionSnapshot checked=ItqanFourSessionSnapshot.decode(snapshot.encode(),geo);
            ItqanRotationPolicy.State rotation=ItqanRegimeStore.readRotationState(p);
            if(rotation.initialTailCompleted||ItqanRegimeStore.postNasMaintenance(p)
                ||!rotation.leg.name().equals(checked.originLeg)
                ||!rotation.cursor.toString().equals(checked.originCursor))
                throw new IllegalStateException("Stale or post-An-Nas Itqan window");
            return p.edit().putString(KEY,checked.encode()).commit();
        }
    }

    /** Persist a *single* successful ×40 repetition, protecting against
     * duplicate taps, stale screen references and process termination. */
    static boolean recordRep(SharedPreferences p,ItqanFourSessionSnapshot expected,
                             GeometryRepository geo) {
        return recordRep(p,expected,geo,false);
    }

    /** Assistance is written with the SAME repetition transaction, never
     * forgotten on process death. Only the final ×40 mask stages count toward
     * final-reveal limits, mirroring the legacy FULL deep-first-pass engine. */
    static boolean recordRep(SharedPreferences p,ItqanFourSessionSnapshot expected,
                             GeometryRepository geo,boolean revealed) {
        return recordRep(p,expected,geo,revealed,HifzClock.today());
    }

    static boolean recordRep(SharedPreferences p, ItqanFourSessionSnapshot expected,
            GeometryRepository geo, boolean revealed, LocalDate sessionDate) {
        if(p==null||expected==null||geo==null)throw new IllegalArgumentException("Itqan rep required");
        synchronized(p) {
            ItqanFourSessionSnapshot stored=read(p,geo);
            if(stored==null||!stored.encode().equals(expected.encode()))return false;
            if(!isCurrentWeeklyDate(p,stored,sessionDate))return false;
            if(stored.sessionIndex>=stored.plan.sessions.size()||stored.reps>=stored.targetReps()
               ||stored.blockIndex>=stored.plan.sessions.get(stored.sessionIndex).blocks.size())
                throw new IllegalStateException("No incomplete current Itqan block");
            int rep=stored.reps+1;
            int target=stored.recoveryTarget;
            int assisted=p.getInt("itqanAssisted",0)+(revealed?1:0);
            int finalReveals=p.getInt("itqanFinalReveals",0);
            boolean recall=stored.usesRecovery()
                ? ItqanThirtyRecoveryPolicy.finalRecallRep(stored.reps)
                : ItqanMaintenancePolicy.isValidationRep(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS,
                    AnchoringQueue.ItqanProtocol.FULL,stored.reps);
            if(revealed && recall) finalReveals++;
            if(stored.usesRecovery() && rep==target && finalReveals>0){
                target=ItqanThirtyRecoveryPolicy.nextRecoveryTarget(target);
                finalReveals=0;
            }
            ItqanFourSessionSnapshot next=stored.withProgress(stored.sessionIndex,
                stored.blockIndex,rep,target,geo);
            return p.edit().putString(keyFor(stored),next.encode())
                .putInt("itqanRep",next.reps)
                .putInt("itqanAssisted",assisted)
                .putInt("itqanFinalReveals",finalReveals).commit();
        }
    }

    /** Retry ONLY this in-flight ×40 block after excessive assistance.
     * The date slot, all earlier validated blocks and its frozen geometry stay put. */
    static boolean restartBlock(SharedPreferences p,ItqanFourSessionSnapshot expected,
                                GeometryRepository geo) {
        return restartBlock(p,expected,geo,HifzClock.today());
    }

    static boolean restartBlock(SharedPreferences p, ItqanFourSessionSnapshot expected,
            GeometryRepository geo, LocalDate sessionDate) {
        if(p==null||expected==null||geo==null)throw new IllegalArgumentException("Frozen retry required");
        synchronized(p) {
            ItqanFourSessionSnapshot stored=read(p,geo);
            if(stored==null||!stored.encode().equals(expected.encode()))return false;
            if(!isCurrentWeeklyDate(p,stored,sessionDate))return false;
            if(stored.sessionIndex>=stored.plan.sessions.size()||
                    stored.blockIndex>=stored.plan.sessions.get(stored.sessionIndex).blocks.size())
                throw new IllegalStateException("No active Itqan block to repeat");
            if(stored.usesRecovery() && stored.reps!=stored.targetReps()) return false;
            ItqanFourSessionSnapshot reset=stored.usesRecovery()
                ? stored.withProgress(stored.sessionIndex,stored.blockIndex,stored.reps,
                    ItqanThirtyRecoveryPolicy.nextRecoveryTarget(stored.targetReps()),geo)
                : stored.withProgress(stored.sessionIndex,stored.blockIndex,0,geo);
            return p.edit().putString(keyFor(stored),reset.encode())
                .putInt("itqanRep",reset.reps).putInt("itqanAssisted",0)
                .putInt("itqanFinalReveals",0)
                .putLong("itqanElapsedMs",0L).commit();
        }
    }

    static boolean ready(SharedPreferences p,ItqanFourSessionSnapshot snapshot) {
        return snapshot.reps==snapshot.targetReps() && (snapshot.usesRecovery()
            ? p.getInt("itqanFinalReveals",0)==0
            : ItqanMaintenancePolicy.validationAllowed(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS,
                p.getInt("itqanAssisted",0)));
    }

    /** Pure next-state calculation. This is NOT a persistence or credit API:
     * only the final Hifz V6 completion transaction may append this result. */
    static ItqanFourSessionSnapshot nextAfterValidatedBlock(
             ItqanFourSessionSnapshot stored,GeometryRepository geo) {
        if(stored==null||geo==null||stored.sessionIndex>=stored.plan.sessions.size()||stored.reps!=stored.targetReps())
            throw new IllegalStateException("Cannot advance an incomplete block");
        ItqanFourSessionPolicy.Session slot=stored.plan.sessions.get(stored.sessionIndex);
        if(stored.blockIndex>=slot.blocks.size())
            throw new IllegalStateException("Invalid Itqan block cursor");
        int nextBlock=stored.blockIndex+1,nextSession=stored.sessionIndex;
        if(nextBlock==slot.blocks.size()){
            nextSession++;nextBlock=0;
            // Empty future date slots occur only after a short partial final
            // window. They carry no repetitions and must not create sessions.
            while(nextSession<stored.plan.sessions.size() && stored.plan.sessions.get(nextSession).blocks.isEmpty())
                nextSession++;
        }
        return stored.withProgress(nextSession,nextBlock,0,geo);
    }
}

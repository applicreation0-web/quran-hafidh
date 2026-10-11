package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;
import com.quransafeguard.hifz.core.VerseRef;
import java.util.*;

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
    private ItqanFourSessionStore(){}

    static ItqanFourSessionSnapshot read(SharedPreferences p,GeometryRepository geo) {
        if(p==null||geo==null)throw new IllegalArgumentException("Itqan store needs QCF");
        String raw=p.getString(KEY,"");
        return raw==null||raw.isEmpty()?null:ItqanFourSessionSnapshot.decode(raw,geo);
    }

    /** Create only on a clean first-pass boundary, never during old OPEN work. */
    static boolean open(SharedPreferences p,ItqanFourSessionSnapshot snapshot,
                        GeometryRepository geo) {
        if(p==null||snapshot==null||geo==null)throw new IllegalArgumentException("Frozen window required");
        synchronized(p) {
            if(p.contains(KEY))return false; // never replace a completed or unfinished plan
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
        if(p==null||expected==null||geo==null)throw new IllegalArgumentException("Itqan rep required");
        synchronized(p) {
            ItqanFourSessionSnapshot stored=read(p,geo);
            if(stored==null||!stored.encode().equals(expected.encode()))return false;
            if(stored.sessionIndex>=4||stored.reps>=40
               ||stored.blockIndex>=stored.plan.sessions.get(stored.sessionIndex).blocks.size())
                throw new IllegalStateException("No incomplete current Itqan block");
            ItqanFourSessionSnapshot next=stored.withProgress(stored.sessionIndex,
                stored.blockIndex,stored.reps+1,geo);
            return p.edit().putString(KEY,next.encode()).putInt("itqanRep",next.reps).commit();
        }
    }

    /** Pure next-state calculation. This is NOT a persistence or credit API:
     * only the final Hifz V6 completion transaction may append this result. */
    static ItqanFourSessionSnapshot nextAfterValidatedBlock(
             ItqanFourSessionSnapshot stored,GeometryRepository geo) {
        if(stored==null||geo==null||stored.sessionIndex>=4||stored.reps!=40)
            throw new IllegalStateException("Cannot advance an incomplete block");
        ItqanFourSessionPolicy.Session slot=stored.plan.sessions.get(stored.sessionIndex);
        if(stored.blockIndex>=slot.blocks.size())
            throw new IllegalStateException("Invalid Itqan block cursor");
        int nextBlock=stored.blockIndex+1,nextSession=stored.sessionIndex;
        if(nextBlock==slot.blocks.size()){
            nextSession++;nextBlock=0;
            // Empty future date slots occur only after a short partial final
            // window. They carry no repetitions and must not create sessions.
            while(nextSession<4 && stored.plan.sessions.get(nextSession).blocks.isEmpty())
                nextSession++;
        }
        return stored.withProgress(nextSession,nextBlock,0,geo);
    }
}

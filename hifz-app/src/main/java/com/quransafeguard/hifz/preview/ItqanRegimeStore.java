package com.quransafeguard.hifz.preview;

import android.content.SharedPreferences;

import com.quransafeguard.hifz.core.VerseRef;

/**
 * Persistence of the perpetual Itqān rotation and of the regime/protocol an in-progress Itqān unit
 * was started with. Works on a bare SharedPreferences (an interface) so the exact commit/restart
 * behaviour is testable on the JVM with an in-memory implementation — HifzPrefs only delegates.
 *
 * <p>Keys: p4ItqanRotationLeg/Cursor/InitialTailCompleted (unchanged since P4; the legacy 0.7.13
 * p4AncrageLeg/p4AncrageInitialTailCompleted pair is still read), plus itqanUnitPlanV1, a
 * "start|end|REGIME|PROTOCOL" stamp written when a unit is rendered. The stamp only counts while
 * that exact unit is the one mid-repetition (itqanUnitStart/End + itqanRep/itqanBlockIndex), so a
 * stale stamp can never leak into a later lap over the same range.
 */
final class ItqanRegimeStore {
    static final String LEG = "p4ItqanRotationLeg";
    static final String CURSOR = "p4ItqanRotationCursor";
    static final String INITIAL_TAIL_COMPLETED = "p4ItqanRotationInitialTailCompleted";
    static final String LEGACY_LEG = "p4AncrageLeg";
    static final String LEGACY_INITIAL_TAIL_COMPLETED = "p4AncrageInitialTailCompleted";
    static final String UNIT_PLAN = "itqanUnitPlanV1";
    enum MaskProfile { LEGACY_15_5_5_5_10, BALANCED_20_5_5_5_5 }

    private ItqanRegimeStore() {}

    /** A fresh install starts at TAIL_START; an install upgrading from the leg-only (0.7.13)
     *  rotation restarts that same leg from its own start — nothing is skipped by revisiting it. */
    static ItqanRotationPolicy.State readRotationState(SharedPreferences p) {
        boolean everCompleted = p.getBoolean(INITIAL_TAIL_COMPLETED, false)
            || p.getBoolean(LEGACY_INITIAL_TAIL_COMPLETED, false);
        String cursorRaw = p.getString(CURSOR, "");
        if (cursorRaw != null && !cursorRaw.isEmpty()) {
            ItqanRotationPolicy.Leg leg;
            try {
                leg = ItqanRotationPolicy.Leg.valueOf(p.getString(LEG, ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS.name()));
            } catch (RuntimeException malformed) {
                leg = ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS;
            }
            VerseRef cursor;
            try {
                cursor = GeometryRepository.parseVerse(cursorRaw);
            } catch (RuntimeException malformed) {
                cursor = leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS
                    ? ItqanRotationPolicy.TAIL_START : ItqanRotationPolicy.FRONT_START;
            }
            return new ItqanRotationPolicy.State(leg, cursor, everCompleted);
        }
        if (p.contains(LEGACY_LEG)) {
            ItqanRotationPolicy.Leg leg = "FRONT_BAQARA_HUJURAT".equals(p.getString(LEGACY_LEG, ""))
                ? ItqanRotationPolicy.Leg.FRONT_BAQARA_HUJURAT : ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS;
            VerseRef cursor = leg == ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS
                ? ItqanRotationPolicy.TAIL_START : ItqanRotationPolicy.FRONT_START;
            return new ItqanRotationPolicy.State(leg, cursor, everCompleted);
        }
        return new ItqanRotationPolicy.State(
            ItqanRotationPolicy.Leg.TAIL_HUJURAT_NAS, ItqanRotationPolicy.TAIL_START, everCompleted);
    }

    /** Persists the rotation synchronously. The post-An-Nās latch is one-way: a write carrying
     *  initialTailCompleted=false never clears a true already on disk. */
    static boolean saveRotationState(SharedPreferences p, ItqanRotationPolicy.State state) {
        if (state == null) throw new IllegalArgumentException("state required");
        return appendRotationState(p,p.edit(),state).commit();
    }

    /** Add leg, cursor and irreversible An-Nas latch to an existing V6 editor.
     * Never commit these keys after crediting a block in a separate edit. */
    static SharedPreferences.Editor appendRotationState(SharedPreferences p,
            SharedPreferences.Editor e,ItqanRotationPolicy.State state) {
        if(p==null||e==null||state==null)throw new IllegalArgumentException("rotation required");
        boolean everCompleted=p.getBoolean(INITIAL_TAIL_COMPLETED,false)
            ||p.getBoolean(LEGACY_INITIAL_TAIL_COMPLETED,false);
        ItqanRotationPolicy.State latched=ItqanRotationPolicy.latched(state,everCompleted);
        return e.putString(LEG,latched.leg.name())
            .putString(CURSOR,latched.cursor.toString())
            .putBoolean(INITIAL_TAIL_COMPLETED,latched.initialTailCompleted);
    }

    static boolean postNasMaintenance(SharedPreferences p) {
        return readRotationState(p).initialTailCompleted;
    }

    /** Regime and protocol frozen for one Itqān unit. */
    static final class UnitPlan {
        final ItqanMaintenancePolicy.Regime regime;
        final AnchoringQueue.ItqanProtocol protocol;
        final MaskProfile maskProfile;

        UnitPlan(ItqanMaintenancePolicy.Regime regime, AnchoringQueue.ItqanProtocol protocol) {
            this(regime,protocol,MaskProfile.LEGACY_15_5_5_5_10);
        }

        UnitPlan(ItqanMaintenancePolicy.Regime regime, AnchoringQueue.ItqanProtocol protocol,
                 MaskProfile maskProfile) {
            if (regime == null || protocol == null || maskProfile == null)
                throw new IllegalArgumentException("plan required");
            this.regime = regime;
            this.protocol = protocol;
            this.maskProfile = maskProfile;
        }
    }

    private static boolean unitInProgress(SharedPreferences p, String start, String end) {
        if (!start.equals(p.getString("itqanUnitStart", ""))
                || !end.equals(p.getString("itqanUnitEnd", ""))) return false;
        // After an assistance restart the repetition counter is deliberately reset
        // to zero, but the same persisted unit and stamped plan must survive.
        // On successful FINAL completion HifzPrefs clears both unit bounds.
        return p.getInt("itqanRep", 0)>0 || p.getInt("itqanBlockIndex", 0)>0
            || parsePlan(p.getString(UNIT_PLAN, ""),start,end)!=null;
    }

    /**
     * The plan for the Itqān unit [start, end] about to be rendered:
     * <ul>
     *   <li>mid-repetition with a matching stamp → exactly the stamped plan;</li>
     *   <li>mid-repetition from before this upgrade (no stamp) → the deep first-pass plan with the
     *       protocol the old code had given it ({@code legacyProtocol}, LIGHT included), so the
     *       persisted repetitions finish under the rules they were started with;</li>
     *   <li>otherwise a new session → FULL, deep before the first An-Nās arrival, maintenance
     *       after it. LIGHT is unreachable here.</li>
     * </ul>
     */
    static UnitPlan resolvePlan(SharedPreferences p, String start, String end,
                                AnchoringQueue.ItqanProtocol legacyProtocol) {
        if (start == null || end == null) throw new IllegalArgumentException("unit range required");
        if (unitInProgress(p, start, end)) {
            UnitPlan stamped = parsePlan(p.getString(UNIT_PLAN, ""), start, end);
            if (stamped != null) return stamped;
            return new UnitPlan(ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS,
                ItqanMaintenancePolicy.protocolFor(true, legacyProtocol));
        }
        return new UnitPlan(ItqanMaintenancePolicy.regimeFor(postNasMaintenance(p)),
            ItqanMaintenancePolicy.protocolForNewSession(),MaskProfile.BALANCED_20_5_5_5_5);
    }

    /** Regime a new selection should be sized with, honouring a unit already mid-repetition. */
    static ItqanMaintenancePolicy.Regime selectionRegime(SharedPreferences p) {
        String start = p.getString("itqanUnitStart", "");
        String end = p.getString("itqanUnitEnd", "");
        if (start != null && end != null && !start.isEmpty() && !end.isEmpty() && unitInProgress(p, start, end)) {
            UnitPlan stamped = parsePlan(p.getString(UNIT_PLAN, ""), start, end);
            return stamped != null ? stamped.regime : ItqanMaintenancePolicy.Regime.DEEP_FIRST_PASS;
        }
        return ItqanMaintenancePolicy.regimeFor(postNasMaintenance(p));
    }

    static boolean stampPlan(SharedPreferences p, String start, String end, UnitPlan plan) {
        String value = start + "|" + end + "|" + plan.regime.name() + "|"
            + plan.protocol.name() + "|" + plan.maskProfile.name();
        if (value.equals(p.getString(UNIT_PLAN, ""))) return true;
        return p.edit().putString(UNIT_PLAN, value).commit();
    }

    private static UnitPlan parsePlan(String raw, String start, String end) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split("\\|", -1);
        if ((parts.length != 4 && parts.length != 5)
                || !start.equals(parts[0]) || !end.equals(parts[1])) return null;
        try {
            // The existing four-field plans are already-started sessions:
            // they remain on the exact legacy 15/5/5/5/10 schedule.
            MaskProfile profile=parts.length==4
                ?MaskProfile.LEGACY_15_5_5_5_10:MaskProfile.valueOf(parts[4]);
            return new UnitPlan(ItqanMaintenancePolicy.Regime.valueOf(parts[2]),
                AnchoringQueue.ItqanProtocol.valueOf(parts[3]),profile);
        } catch (RuntimeException malformed) {
            return null;
        }
    }
}

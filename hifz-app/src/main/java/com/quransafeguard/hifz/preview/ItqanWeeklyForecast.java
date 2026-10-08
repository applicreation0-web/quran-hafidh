package com.quransafeguard.hifz.preview;

import com.quransafeguard.hifz.core.VerseRef;

import java.util.List;

/**
 * Read-only cursor for the dashboard's Itqān forecast.
 *
 * Uses the live rotation's pick/advance rules, never the legacy anchoring queue.
 * Advancing this object never writes SharedPreferences or credits any repetitions.
 */
final class ItqanWeeklyForecast {
    interface UnitSource {
        List<AnchoringQueue.Entry> units(ItqanRotationPolicy.Leg leg, boolean postNas);
    }

    private final UnitSource source;
    private ItqanRotationPolicy.State state;
    private AnchoringQueue.Entry entry;
    private int blockIndex;
    private boolean initialUnit;

    ItqanWeeklyForecast(ItqanRotationPolicy.State state, AnchoringQueue.Entry liveEntry,
                        int liveBlockIndex, UnitSource source) {
        if (state == null || source == null) throw new IllegalArgumentException("Forecast state required");
        this.state = state;
        this.source = source;
        this.entry = liveEntry;
        this.initialUnit = liveEntry != null;
        this.blockIndex = liveEntry == null ? 0 : Math.max(0, liveBlockIndex);
        if (this.entry == null) pickNext();
    }

    AnchoringQueue.Entry entry() { return entry; }
    int blockIndex() { return blockIndex; }
    boolean isInitialUnit() { return initialUnit; }

    ItqanMaintenancePolicy.Regime regime() {
        return ItqanMaintenancePolicy.regimeFor(state.initialTailCompleted);
    }

    void completedBlock(int totalBlocks) {
        if (entry == null) throw new IllegalStateException("No Itqān unit to advance");
        if (++blockIndex < Math.max(1, totalBlocks)) return;

        VerseRef end = GeometryRepository.parseVerse(entry.end);
        state = ItqanRotationPolicy.advancedPast(state, end);
        blockIndex = 0;
        initialUnit = false;
        pickNext();
    }

    private void pickNext() {
        boolean postNas = state.initialTailCompleted;
        ItqanRotationPolicy.Pick pick = ItqanRotationPolicy.pick(state,
            leg -> source.units(leg, postNas));
        state = pick.state;
        entry = pick.unit;
    }
}

package com.quransafeguard.hifz.next;

import java.util.Collections;
import java.util.List;

/**
 * Evening snowball chain. The morning lesson is not cumulative;
 * cumulative reading happens only here.
 */
public final class HifzSnowballStep {
    public final HifzCycleType type;
    public final int week;
    public final int dayIndex;
    public final List<String> blocks;
    public final int repetitions;

    public HifzSnowballStep(HifzCycleType type, int week, int dayIndex, List<String> blocks) {
        this.type = type;
        this.week = week;
        this.dayIndex = dayIndex;
        this.blocks = Collections.unmodifiableList(blocks);
        this.repetitions = 5;
    }
}

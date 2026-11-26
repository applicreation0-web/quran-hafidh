package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Ordered, unique page queue. A page consuming its full budget moves to the back. */
final class FragilityQueuePolicy {
    private FragilityQueuePolicy() {}

    static List<Integer> merge(Collection<Integer> queued, Collection<Integer> revealed) {
        LinkedHashSet<Integer> result = new LinkedHashSet<>(queued);
        result.addAll(revealed);
        return new ArrayList<>(result);
    }

    static List<Integer> remaining(List<Integer> queued, List<Integer> batch, int index, boolean done) {
        if (batch.isEmpty() || index < 0 || index >= batch.size())
            throw new IllegalArgumentException("Invalid fragility batch");
        LinkedHashSet<Integer> result = new LinkedHashSet<>(queued);
        result.removeAll(batch);
        if (!done) {
            result.addAll(batch.subList(index + 1, batch.size()));
            result.add(batch.get(index));
        }
        return new ArrayList<>(result);
    }
}

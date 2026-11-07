package com.quransafeguard.hifz.preview;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Exact physical-line units handed from Stabilisation to Consolidation. */
final class ConsolidationPhysicalUnitPolicy {
    private ConsolidationPhysicalUnitPolicy() {}

    static List<StabilizationHalfPagePolicy.Unit> readyUnits(
            List<StabilizationHalfPagePolicy.Unit> planned,
            Set<String> stabilized,
            Set<String> acquired,
            int maxUnits) {
        if (maxUnits < 1 || maxUnits > 3) {
            throw new IllegalArgumentException("Consolidation group size must be 1..3");
        }
        if (planned == null || stabilized == null || acquired == null) {
            throw new IllegalArgumentException("Consolidation physical state required");
        }
        ArrayList<StabilizationHalfPagePolicy.Unit> out = new ArrayList<>();
        for (StabilizationHalfPagePolicy.Unit unit : planned) {
            if (unit == null || unit.lineIds.isEmpty()) {
                throw new IllegalStateException("Consolidation physical unit must contain lines");
            }
            boolean anyStabilized = false;
            boolean allStabilized = true;
            boolean anyAcquired = false;
            boolean allAcquired = true;
            for (String id : unit.lineIds) {
                boolean s = stabilized.contains(id);
                boolean a = acquired.contains(id);
                anyStabilized |= s;
                allStabilized &= s;
                anyAcquired |= a;
                allAcquired &= a;
            }
            if (allAcquired) continue;
            // A physical unit is eligible only when every line is Stabilized and none are
            // already Acquired. Partial states are normal at migration/manual boundaries;
            // they must not block earlier ready units or the rest of the queue.
            if (anyAcquired) continue;
            if (anyStabilized && !allStabilized) continue;
            if (!allStabilized) continue;
            out.add(unit);
            if (out.size() == maxUnits) break;
        }
        return Collections.unmodifiableList(out);
    }

    static String encodeLineUnit(List<String> lineIds) {
        if (lineIds == null || lineIds.isEmpty()) {
            throw new IllegalArgumentException("Consolidation line ids required");
        }
        StringBuilder raw = new StringBuilder();
        for (String id : lineIds) {
            if (id == null || id.isEmpty()) throw new IllegalArgumentException("Empty physical line id");
            if (raw.length() > 0) raw.append('\n');
            raw.append(id);
        }
        return "L:" + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The weekly snowball's extra "continuous" pass: every already-accumulated physical unit's
     * lines, concatenated in accumulation order, as one combined unit — on top of (not instead
     * of) each block's own individual repetitions.
     */
    /** Sort physical QCF blocks by their first 0-based printed line, not by the
     * date or repetition status. Historical synthetic ids retain insertion order. */
    static long firstPhysicalLineOrder(String encoded) {
        String first = decodeLineUnit(encoded).get(0);
        int colon = first.indexOf(':');
        if (colon <= 0 || colon == first.length() - 1) return Long.MAX_VALUE;
        try {
            int page = Integer.parseInt(first.substring(0, colon));
            int line = Integer.parseInt(first.substring(colon + 1));
            if (page < 1 || page > 604 || line < 0 || line > 30) return Long.MAX_VALUE;
            return page * 1000L + line;
        } catch (NumberFormatException synthetic) {
            return Long.MAX_VALUE;
        }
    }

    static String encodeContinuousUnit(List<String> encodedUnits) {
        if (encodedUnits == null || encodedUnits.size() < 2) {
            throw new IllegalArgumentException("Continuous unit requires at least two physical units");
        }
        ArrayList<String> combined = new ArrayList<>();
        for (String encoded : encodedUnits) combined.addAll(decodeLineUnit(encoded));
        return encodeLineUnit(combined);
    }

    static List<String> decodeLineUnit(String encoded) {
        if (encoded == null || !encoded.startsWith("L:") || encoded.length() <= 2) {
            throw new IllegalArgumentException("Invalid Consolidation physical unit id");
        }
        final String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(encoded.substring(2)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid Consolidation physical unit encoding", invalid);
        }
        String[] split = raw.split("\\n", -1);
        ArrayList<String> ids = new ArrayList<>(split.length);
        for (String id : split) {
            if (id.isEmpty()) throw new IllegalArgumentException("Invalid empty physical line id");
            ids.add(id);
        }
        return Collections.unmodifiableList(ids);
    }
}

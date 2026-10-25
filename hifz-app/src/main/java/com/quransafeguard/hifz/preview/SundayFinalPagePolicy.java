package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Pure page-boundary projection for the revised Sunday finals.
 * It never changes the receipt-bearing weekday physical blocks, and never
 * manufactures/rounds lines to fill a Mushaf page.
 */
final class SundayFinalPagePolicy {
    private SundayFinalPagePolicy() {}

    static final class LinePosition {
        final int page;
        final int globalIndex;

        LinePosition(int page, int globalIndex) {
            if (page < 1 || page > 604 || globalIndex < 0)
                throw new IllegalArgumentException("Invalid exact Mushaf line position");
            this.page = page;
            this.globalIndex = globalIndex;
        }
    }

    /**
     * Input: encoded weekday receipt units. Output: page-exact units only;
     * no invented text, duplicate lines, and no combined continuous pass.
     * Original input order need not coincide with physical Mushaf line order.
     */
    static List<String> physicalPageUnits(List<String> encoded, Map<String, LinePosition> positions) {
        if (encoded == null || positions == null)
            throw new IllegalArgumentException("Finale sources and QCF geometry required");
        LinkedHashMap<Integer, ArrayList<String>> pages = new LinkedHashMap<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        for (String source : encoded) {
            for (String id : ConsolidationPhysicalUnitPolicy.decodeLineUnit(source)) {
                LinePosition pos = positions.get(id);
                if (pos == null) throw new IllegalStateException("Missing verified QCF line: " + id);
                if (seen.add(id)) pages.computeIfAbsent(pos.page, k -> new ArrayList<>()).add(id);
            }
        }
        ArrayList<Integer> pageOrder = new ArrayList<>(pages.keySet());
        Collections.sort(pageOrder);
        ArrayList<String> out = new ArrayList<>();
        for (int page : pageOrder) {
            ArrayList<String> lines = pages.get(page);
            lines.sort(Comparator.comparingInt(id -> positions.get(id).globalIndex));
            if (lines.isEmpty() || lines.size() > 15)
                throw new IllegalStateException("Invalid number of QCF lines in a Sunday final page");
            int last = -1;
            for (String id : lines) {
                int next = positions.get(id).globalIndex;
                if (next <= last) throw new IllegalStateException("Duplicate or unordered physical QCF lines");
                last = next;
            }
            out.add(ConsolidationPhysicalUnitPolicy.encodeLineUnit(lines));
        }
        return Collections.unmodifiableList(out);
    }

    /** A Sunday page has been earned only if EACH constituent physical line has a
     * completed weekday receipt, or was already acquired. Splitting/merging must
     * never forge receipt credit merely because a page happens to contain it. */
    static boolean earnedEveryLine(List<String> pageLines, List<String> completedReceipts,
                                   java.util.Set<String> alreadyAcquired) {
        if (pageLines == null || completedReceipts == null || alreadyAcquired == null)
            throw new IllegalArgumentException("Finale credit sources required");
        LinkedHashSet<String> covered = new LinkedHashSet<>(alreadyAcquired);
        for (String unit : completedReceipts)
            covered.addAll(ConsolidationPhysicalUnitPolicy.decodeLineUnit(unit));
        return !pageLines.isEmpty() && covered.containsAll(pageLines);
    }
}

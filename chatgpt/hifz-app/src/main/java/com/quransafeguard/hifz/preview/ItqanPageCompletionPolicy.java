package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Itqān-only "finish the page" bonus (P3): when a normal Stabilisation sub-block ends exactly 1 or
 * 2 physical lines short of its own page, offers to fold those trailing lines into the block
 * before repetitions start. Sabqi never uses this, and this class never decides on its own — it
 * only evaluates whether an offer is legal; the caller still requires an explicit user choice.
 *
 * Pure and Android-free: it never re-derives a source range from just a start/end VerseRef the
 * way the ordinary planner does, since that can silently pull in lines from a different physical
 * owner than intended. The caller resolves candidateTrailingLines itself (contiguous, unclaimed,
 * same page, correct ownership) and this class only re-validates the merged result the same way
 * completeConsolidationSessionV6 will later re-verify it: candidate = base + bonus must itself come
 * back out of StabilizationHalfPagePolicy.planPage as a single, identical block. That single check
 * is what keeps this feature from ever handing Consolidation an un-reproducible 12/13-line unit —
 * a legitimate 11-line base plus any bonus fails it and is correctly refused, never merged anyway.
 */
final class ItqanPageCompletionPolicy {
    enum Choice { NONE, PLUS_ONE, PLUS_TWO }

    static final class Offer {
        final Choice choice;
        final List<String> bonusLineIds;
        final List<String> actualLineIds;

        private Offer(Choice choice, List<String> bonusLineIds, List<String> actualLineIds) {
            this.choice = choice;
            this.bonusLineIds = Collections.unmodifiableList(bonusLineIds);
            this.actualLineIds = Collections.unmodifiableList(actualLineIds);
        }

        private static Offer none(List<String> baseLineIds) {
            return new Offer(Choice.NONE, Collections.emptyList(), new ArrayList<>(baseLineIds));
        }
    }

    private ItqanPageCompletionPolicy() {}

    /**
     * @param baseLineIds the just-planned sub-block's own lines, in order.
     * @param baseLines   the same lines resolved to geometry, in the same order as baseLineIds.
     * @param candidateTrailingLines every canonical line that could extend the block, already
     *        restricted by the caller to: the same page as the base block's last line, physically
     *        contiguous immediately after it, not yet committed/reserved/quarantined, and drawn
     *        only from a parent this offer is allowed to borrow from. 0, 1 or 2 lines only — a
     *        longer list (a caller bug) is treated the same as "no offer" rather than trusted.
     */
    static Offer evaluate(List<String> baseLineIds,
                          List<GeometryRepository.LineMeta> baseLines,
                          List<GeometryRepository.LineMeta> candidateTrailingLines) {
        if (baseLineIds == null || baseLineIds.isEmpty() || baseLines == null || baseLines.isEmpty())
            throw new IllegalArgumentException("base block required");
        if (candidateTrailingLines == null || candidateTrailingLines.isEmpty()
                || candidateTrailingLines.size() > 2)
            return Offer.none(baseLineIds);

        List<GeometryRepository.LineMeta> candidateLines = new ArrayList<>(baseLines);
        candidateLines.addAll(candidateTrailingLines);
        List<String> candidateIds = new ArrayList<>(baseLineIds);
        for (GeometryRepository.LineMeta l : candidateTrailingLines) candidateIds.add(l.id);

        List<StabilizationHalfPagePolicy.Unit> verified;
        try {
            verified = StabilizationHalfPagePolicy.planPage(candidateLines);
        } catch (RuntimeException notEligible) {
            return Offer.none(baseLineIds);
        }
        if (verified.size() != 1 || !verified.get(0).lineIds.equals(candidateIds))
            return Offer.none(baseLineIds);

        List<String> bonusIds = new ArrayList<>();
        for (GeometryRepository.LineMeta l : candidateTrailingLines) bonusIds.add(l.id);
        Choice choice = candidateTrailingLines.size() == 1 ? Choice.PLUS_ONE : Choice.PLUS_TWO;
        return new Offer(choice, bonusIds, candidateIds);
    }
}

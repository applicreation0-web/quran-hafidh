package com.quransafeguard.hifz.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Latest user decision (ITQĀN FULL ONLY / POST-AN-NĀS), pure and Android-free.
 *
 * <p>Phase 1 — the first Itqān pass 49:1 → 114:6 keeps the deep FULL protocol unchanged: ×40,
 * progressive eraser 25/50/75/100, 8/7/7 half-page sub-blocks.
 *
 * <p>Phase 2 — once the first arrival at An-Nās is recorded (ItqanRotationPolicy's one-way
 * initialTailCompleted latch), every later Itqān session is a FULL maintenance session: at most
 * {@link #MAX_LINES} physical lines (never past the real Sabqi frontier, never a fabricated
 * cross-surah unit), FULL ×40 halved to ×{@link #TOTAL_REPS}, frozen as {@link #VISIBLE_REPS}
 * repetitions with the Mushaf fully visible then {@link #ANCHOR_REPS} recall repetitions with the
 * project's existing validated anchors (the unit's Al-Munīr amorces as exact word holes in the
 * paper mask, plus the permanent page cues). No 25/50/75 eraser in phase 2.
 *
 * <p>LIGHT is never chosen for a new session. The enum value survives only so a legacy queue
 * entry or a unit already mid-repetition under LIGHT before this upgrade can be read and
 * finished without losing its persisted repetitions.
 */
final class ItqanMaintenancePolicy {
    enum Regime { DEEP_FIRST_PASS, POST_NAS_MAINTENANCE }

    static final int MAX_LINES = 15;
    static final int VISIBLE_REPS = 10;
    static final int ANCHOR_REPS = 10;
    static final int TOTAL_REPS = 20;

    private ItqanMaintenancePolicy() {}

    static Regime regimeFor(boolean postNasMaintenance) {
        return postNasMaintenance ? Regime.POST_NAS_MAINTENANCE : Regime.DEEP_FIRST_PASS;
    }

    /** The only protocol a new Itqān session may ever be planned with. */
    static AnchoringQueue.ItqanProtocol protocolForNewSession() {
        return AnchoringQueue.ItqanProtocol.FULL;
    }

    static int totalReps(Regime regime, AnchoringQueue.ItqanProtocol protocol) {
        if (regime == Regime.POST_NAS_MAINTENANCE) return TOTAL_REPS;
        return PreviewConfig.itqanTotalReps(protocol);
    }

    /** Mask for the next repetition: maintenance is 0% (visible) then 100% with anchors kept. */
    static int maskForNextRep(Regime regime, AnchoringQueue.ItqanProtocol protocol, int completed) {
        if (regime != Regime.POST_NAS_MAINTENANCE) return PreviewConfig.itqanMaskForNextRep(completed, protocol);
        if (completed < 0 || completed >= TOTAL_REPS) return 0;
        return completed < VISIBLE_REPS ? 0 : 100;
    }

    /** True while the next repetition is one of the ten anchored recall repetitions. */
    static boolean anchoredRecallRep(Regime regime, int completed) {
        return regime == Regime.POST_NAS_MAINTENANCE && completed >= VISIBLE_REPS && completed < TOTAL_REPS;
    }

    static boolean isValidationRep(Regime regime, AnchoringQueue.ItqanProtocol protocol, int completedBefore) {
        if (regime != Regime.POST_NAS_MAINTENANCE) return PreviewConfig.isItqanValidationRep(completedBefore, protocol);
        int next = completedBefore + 1;
        return next >= TOTAL_REPS - 1 && next <= TOTAL_REPS;
    }

    /**
     * Protocol for the Itqān unit about to be rendered. A unit already mid-repetition keeps the
     * protocol it was started with (a legacy LIGHT unit finishes as LIGHT, so no persisted
     * repetition is lost or silently re-scaled); anything else is FULL.
     */
    static AnchoringQueue.ItqanProtocol protocolFor(boolean inProgress, AnchoringQueue.ItqanProtocol startedWith) {
        if (inProgress && startedWith != null) return startedWith;
        return protocolForNewSession();
    }

    /**
     * Lines a maintenance session may newly credit to Stabilisé: only lines with no progression
     * state at all (genuine reconstruction material such as 2:1–2:74). Already Stabilisé/Acquis
     * lines are reinforcement only, and Appris lines stay in their own Apprentissage →
     * Renforcement → Acquis chain — Itqān never takes Sabqi material away from it.
     */
    static List<String> newlyCreditableLines(List<String> lineIds, Set<String> learned,
                                             Set<String> stabilized, Set<String> acquired) {
        if (lineIds == null) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>();
        for (String id : lineIds) {
            if (id == null || out.contains(id)) continue;
            if (learned.contains(id) || stabilized.contains(id) || acquired.contains(id)) continue;
            out.add(id);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Consolidation only accepts a frozen unit that re-plans as exactly one whole half-page block
     * (≤11 lines, see StabilizationHalfPagePolicy / completeConsolidationSessionV6). A 15-line
     * maintenance unit can exceed that, so its newly credited lines are enrolled as the same
     * half-page blocks StabilizationHalfPagePolicy itself would plan — never as one oversized unit.
     */
    static List<List<String>> consolidationEnrollment(List<GeometryRepository.LineMeta> newlyCreditedLines) {
        if (newlyCreditedLines == null || newlyCreditedLines.isEmpty()) return Collections.emptyList();
        ArrayList<List<String>> out = new ArrayList<>();
        for (StabilizationHalfPagePolicy.Unit unit : StabilizationHalfPagePolicy.planPage(newlyCreditedLines)) {
            out.add(unit.lineIds);
        }
        return Collections.unmodifiableList(out);
    }

    /** An Al-Munīr amorce belongs to a maintenance unit when its unit starts inside it. */
    static boolean amorceInsideUnit(com.quransafeguard.hifz.core.VerseRef amorceStart,
                                    com.quransafeguard.hifz.core.VerseRef unitStart,
                                    com.quransafeguard.hifz.core.VerseRef unitEnd) {
        if (amorceStart == null || unitStart == null || unitEnd == null) return false;
        return amorceStart.compareTo(unitStart) >= 0 && amorceStart.compareTo(unitEnd) <= 0;
    }

    /** Owned-line lookup for a candidate unit ending at the given verse index. */
    interface OwnedLines {
        int count(int lastVerseIndex);
    }

    /**
     * Largest prefix of a unit's ordered verses whose owned physical lines fit in maxLines. A
     * verse is never split: the first verse is always kept even if, alone, it owns more lines
     * (no Madani verse does at 15), so a unit is never empty and never cut mid-verse.
     */
    static int lastVerseIndexWithinBudget(int verseCount, int maxLines, OwnedLines owned) {
        if (verseCount <= 0) throw new IllegalArgumentException("unit requires verses");
        int last = verseCount - 1;
        while (last > 0 && owned.count(last) > maxLines) last--;
        return last;
    }
}

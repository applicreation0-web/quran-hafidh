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
 * initialTailCompleted latch), every later Itqān session is one maintenance session per HIZB
 * (user decision, 2026-10-04: wider coverage, fewer passes): the hizb's real boundaries
 * (QuranRubBoundaries), clipped to the rotation leg and never past the real Sabqi frontier,
 * recited {@link #TOTAL_REPS} times — {@link #VISIBLE_REPS} passes with the Mushaf fully visible
 * then {@link #ANCHOR_REPS} recall passes with the project's existing validated anchors (the
 * hizb's Al-Munīr amorces as exact word holes in the paper mask, plus the permanent page cues).
 * No 25/50/75 eraser and no early-review policy: reveals are recorded, never a reason to
 * restart or reschedule the hizb.
 *
 * <p>LIGHT is never chosen for a new session. The enum value survives only so a legacy queue
 * entry or a unit already mid-repetition under LIGHT before this upgrade can be read and
 * finished without losing its persisted repetitions.
 */
final class ItqanMaintenancePolicy {
    enum Regime { DEEP_FIRST_PASS, POST_NAS_MAINTENANCE }

    static final int VISIBLE_REPS = 2;
    static final int ANCHOR_REPS = 2;
    static final int TOTAL_REPS = 4;
    /** Sanity bound for one maintenance unit (the longest hizb is ~11 pages ≈ 165 lines). */
    static final int MAX_UNIT_LINES = 260;

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

    /** Maintenance validation never depends on the reveal count (no restart, no rescheduling). */
    static boolean validationAllowed(Regime regime, int reveals) {
        return regime == Regime.POST_NAS_MAINTENANCE || StructuredSessionPolicy.assistancePasses(reveals);
    }

    /**
     * Consolidation only accepts a frozen unit that re-plans as exactly one whole half-page block
     * (≤11 lines, see StabilizationHalfPagePolicy / completeConsolidationSessionV6). A hizb's
     * newly credited lines are therefore cut into consecutive same-surah runs, each run into
     * weekly-sized pieces, and each piece into the half-page blocks StabilizationHalfPagePolicy
     * itself plans — never one oversized unit.
     */
    static List<List<String>> consolidationEnrollment(List<GeometryRepository.LineMeta> newlyCreditedLines) {
        if (newlyCreditedLines == null || newlyCreditedLines.isEmpty()) return Collections.emptyList();
        ArrayList<List<String>> out = new ArrayList<>();
        ArrayList<GeometryRepository.LineMeta> piece = new ArrayList<>();
        int surah = -1;
        for (GeometryRepository.LineMeta line : newlyCreditedLines) {
            int lineSurah = line.verses.get(0).getSurah();
            boolean contiguous = piece.isEmpty() || line.globalIndex == piece.get(piece.size() - 1).globalIndex + 1;
            if (!piece.isEmpty() && (lineSurah != surah || !contiguous
                    || piece.size() >= PreviewConfig.STABILIZATION_WEEKLY_LINES)) {
                for (StabilizationHalfPagePolicy.Unit unit : StabilizationHalfPagePolicy.planPage(piece)) out.add(unit.lineIds);
                piece = new ArrayList<>();
            }
            surah = lineSurah;
            piece.add(line);
        }
        for (StabilizationHalfPagePolicy.Unit unit : StabilizationHalfPagePolicy.planPage(piece)) out.add(unit.lineIds);
        return Collections.unmodifiableList(out);
    }

    /** Start verse of each of the 60 hizb (QuranRubBoundaries rows at position 0), in order. */
    static List<com.quransafeguard.hifz.core.VerseRef> hizbStarts() {
        ArrayList<com.quransafeguard.hifz.core.VerseRef> out = new ArrayList<>();
        for (int[] row : QuranRubBoundaries.TABLE) {
            if (row[5] == 0) out.add(new com.quransafeguard.hifz.core.VerseRef(row[1], row[2]));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * The hizb-sized maintenance units covering [legStart, legEnd], as [start, end] verse pairs:
     * each hizb's real range clipped to the leg (49:1 falls inside hizb 52; FRONT stops before
     * Sabqi's current block). Never crosses a hizb boundary, never fabricates one.
     */
    static List<com.quransafeguard.hifz.core.VerseRef[]> hizbUnits(com.quransafeguard.hifz.core.VerseRef legStart,
                                                                  com.quransafeguard.hifz.core.VerseRef legEnd) {
        ArrayList<com.quransafeguard.hifz.core.VerseRef[]> out = new ArrayList<>();
        if (legStart == null || legEnd == null || legStart.compareTo(legEnd) > 0) return out;
        List<com.quransafeguard.hifz.core.VerseRef> starts = hizbStarts();
        for (int h = 0; h < starts.size(); h++) {
            com.quransafeguard.hifz.core.VerseRef hizbStart = starts.get(h);
            com.quransafeguard.hifz.core.VerseRef hizbEnd = h + 1 < starts.size()
                ? GeometryRepository.previous(starts.get(h + 1)) : new com.quransafeguard.hifz.core.VerseRef(114, 6);
            com.quransafeguard.hifz.core.VerseRef start = hizbStart.compareTo(legStart) < 0 ? legStart : hizbStart;
            com.quransafeguard.hifz.core.VerseRef end = hizbEnd.compareTo(legEnd) > 0 ? legEnd : hizbEnd;
            if (start.compareTo(end) <= 0) out.add(new com.quransafeguard.hifz.core.VerseRef[]{start, end});
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
}

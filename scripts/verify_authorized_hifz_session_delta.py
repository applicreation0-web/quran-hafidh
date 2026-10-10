#!/usr/bin/env python3
"""Only the three explicitly authorized QCF fail-open HifzSessionActivity changes.

Compare byte-for-byte to the immutable official 1.17.1 source after applying
three exact substitutions. A future unrelated Hifz engine change fails CI.
"""
from pathlib import Path
import subprocess

BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
PATH="hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
baseline=subprocess.check_output(["git","show",f"{BASE}:{PATH}"]).decode("utf-8")
replacements=[
(
"""        mushaf.clearPageLandmarkBoxes();
        if (itqanAnchorFailOpen) { mushaf.clearSemanticCues(); return; }
        mushaf.setSemanticCues(cues, true, false);""",
"""        mushaf.clearPageLandmarkBoxes();
        // A unit that has no Ibn Kathir beginning on the displayed page must
        // remain visible; a totally covered page is not a valid recall aid.
        if (cues.length() == 0) itqanAnchorFailOpen = true;
        if (itqanAnchorFailOpen) { mushaf.clearSemanticCues(); return; }
        mushaf.setSemanticCues(cues, true, false);"""
),
(
"""    /** The mask actually drawn: an anchored page without exact anchors stays readable. */
    private int displayedMask() {
        return ITQAN.equals(mode) && itqanAnchorFailOpen ? 0 : currentMask;
    }""",
"""    /** Fail open on active-recall pages with incomplete QCF landmark/cue geometry too. */
    private int displayedMask() {
        if (ITQAN.equals(mode) && itqanAnchorFailOpen) return 0;
        if (MURAJAAH_ACTIVE.equals(mode) && currentMask > 0
                && (semanticPassages == null || !semanticPassages.isAvailable()
                    || !semanticPassages.hasCompleteExactGeometryForPage(currentPage)))
            return 0;
        return currentMask;
    }"""
),
(
"""view.setPressed(false);mushaf.setMask(currentMask);if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;""",
"""view.setPressed(false);mushaf.setMask(displayedMask());if(action==android.view.MotionEvent.ACTION_UP)view.performClick();return true;"""
),
]

# J2 explicitly authorized: frozen per-unit mask profile and strictly physical
# page/surah completion check; any other Hifz session delta still fails.
replacements.extend([
    ("    private AnchoringQueue.ItqanProtocol itqanSessionProtocol = AnchoringQueue.ItqanProtocol.FULL;", "    private AnchoringQueue.ItqanProtocol itqanSessionProtocol = AnchoringQueue.ItqanProtocol.FULL;\n    private ItqanRegimeStore.UnitPlan itqanUnitPlan;"),
    ("        itqanSessionProtocol = plan.protocol;\n        itqanTargetReps =", "        itqanSessionProtocol = plan.protocol;\n        itqanUnitPlan = plan;\n        itqanTargetReps ="),
    ("currentMask=ItqanMaintenancePolicy.maskForNextRep(itqanRegime, itqanSessionProtocol, rep);", "currentMask=ItqanBalancedMaskSchedule.maskForNextRep(itqanUnitPlan, rep);"),
    ("    private ItqanPageCompletionPolicy.Offer computeItqanBonusOffer(\n            List<StabilizationHalfPagePolicy.Unit> plannedUnits, List<String> ownBlockLineIds) {\n        List<GeometryRepository.LineMeta> ownBlockLines = geometry.linesForExactIds(ownBlockLineIds);\n        List<GeometryRepository.LineMeta> candidates = new ArrayList<>();\n        if (itqanBlockIndex + 1 < itqanBlockCount) {\n            StabilizationHalfPagePolicy.Unit nextBlock = plannedUnits.get(itqanBlockIndex + 1);\n            List<GeometryRepository.LineMeta> nextBlockLines = geometry.linesForExactIds(nextBlock.lineIds);\n            int page = ownBlockLines.get(ownBlockLines.size() - 1).page;\n            int maxTakeable = Math.min(2, nextBlockLines.size() - 1);\n            for (int i = 0; i < maxTakeable; i++) {\n                GeometryRepository.LineMeta l = nextBlockLines.get(i);\n                if (l.page != page) break;\n                candidates.add(l);\n            }\n        }\n        return ItqanPageCompletionPolicy.evaluate(ownBlockLineIds, ownBlockLines, candidates);\n    }\n\n", "    private ItqanPageCompletionPolicy.Offer computeItqanBonusOffer(\n            List<StabilizationHalfPagePolicy.Unit> plannedUnits, List<String> ownBlockLineIds) {\n        List<String> nextLineIds=itqanBlockIndex + 1 < itqanBlockCount\n            ? plannedUnits.get(itqanBlockIndex + 1).lineIds : Collections.emptyList();\n        // Physically inspect the next one/two lines against the actual Mushaf:\n        // an extension is legal only if it closes this page or this surah.\n        return ItqanCompletionBoundaryPolicy.offer(geometry,ownBlockLineIds,nextLineIds);\n    }\n\n"),
])
# J5: prevent replay and reject cross-date session snapshot assignment.
replacements.extend([
    ("        consolidationSession = prefs.restoreConsolidationSession(consolidationEngine, family);\n","        consolidationSession = prefs.restoreConsolidationSession(consolidationEngine, family);\n        if (consolidationSession != null\n                && !consolidationSession.cycle().cycleId().equals(cycleIdPrefix+today)) {\n            // Cross-midnight or another final/evening session: NEVER attribute\n            // repetitions from a different date to today's credits.\n            throw new IllegalStateException(\"Séance antérieure encore ouverte : \"\n                +consolidationSession.cycle().cycleId()\n                +\". Reprendre l'ancienne séance avant une nouvelle validation.\");\n        }\n"),
    ("if (today.equals(prefs.lastStabilizationSnowballEveningDate())) {","if (!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.STABILIZATION,sessionDate)) {"),
    ("if (today.equals(prefs.lastLearningSnowballEveningDate())) {","if (!prefs.snowballEveningPending(ConsolidationCycleEngine.Family.LEARNING,sessionDate)) {"),
])
for before,after in replacements:
    expected=2 if before.startswith("currentMask=ItqanMaintenancePolicy.maskForNextRep(") else 1
    assert baseline.count(before)==expected,"Official 1.17.1 source no longer matches expected Hifz code."
    baseline=baseline.replace(before,after)
actual=Path(PATH).read_text(encoding="utf-8")
assert baseline == actual, ("UNAUTHORIZED HifzSessionActivity delta: "
    "only the three verified Ibn Kathir fail-open substitutions are allowed")
print("PASS: HifzSessionActivity matches official v1.17.1 except three exact QCF fail-open deltas")

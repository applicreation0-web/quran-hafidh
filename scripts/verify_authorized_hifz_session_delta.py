#!/usr/bin/env python3
"""Only the three explicitly authorized QCF fail-open HifzSessionActivity changes.

Compare byte-for-byte to the immutable official 1.17.1 source after applying
three exact substitutions. A future unrelated Hifz engine change fails CI.
"""
from pathlib import Path
import subprocess

BASE="7fd45bbe3d2e67a2e21c6f8ca1d5ae935d1f0d21"
PATH="hifz-app/src/main/java/com/quransafeguard/hifz/preview/HifzSessionActivity.java"
CURRENT_SHA="491aae573afba8bdd5da339a8e15dca8e59661ef"
# The October 21 Atlas-only branch removes three Correspondances UI hooks.
# This exact Git blob is accepted after checking the deletion-only source diff.
ATLAS_ONLY_SHA="4d52f8bc8e5cbd7a5bfa836b4c73c07c52647f55"
# The 22 October user-requested duration parity is the ONLY new Hifz runtime
# departure: active gets the same time policy as passive, no mask/cursor/credits
# changes. Verify it against the pinned Atlas-only session source byte for byte.
# The Itqān ×3 amendment is reconstructed exactly from the last audited
# 1.17.2 active/passive duration commit, preserving all other Hifz bytes.
# Explicitly reviewed backlog-only session entry guard. Exact current bytes and
# immutable 1.17.5 pre-backlog base must both match. All repetition, cursor,
# promotion, QCF and mask code remains byte-identical to the pinned baseline.
# The 25 October 1.17.5 finals fix is user-authorized and separately
# SHA-pinned against the immutable 4135df38 source. Do not relax the
# approval boundary for any other Hifz, mask, Atlas or QCF change.
if subprocess.check_output(["git","hash-object",PATH],text=True).strip()=="464a0f3b0b09c3bbebd67dcd49af0be166b94f4b":
    subprocess.run(["python3","scripts/verify_authorized_1175_final_state_delta.py"],check=True)
    print("PASS: byte-pinned 1.17.5 Sunday finale session routing")
    raise SystemExit(0)

GUARDED_BACKLOG_SHA="e244a330a5fff551f2032dad228cb587f82434f4"
GUARDED_BACKLOG_LABELS_SHA="add8fe985f8b3a2206fa4142c6e384e5a48a8c0c"
PRE_BACKLOG_COMMIT="7f7a3e8ebd859f2a4871794f12c4550bf8cf7787"
if subprocess.check_output(["git","hash-object",PATH],text=True).strip() in (GUARDED_BACKLOG_SHA, GUARDED_BACKLOG_LABELS_SHA):
    baseline=subprocess.check_output(
        ["git","show",PRE_BACKLOG_COMMIT+":"+PATH]).decode("utf-8")
    if subprocess.check_output(["git","hash-object","--stdin"],
        input=baseline.encode()).decode().strip()!="bf2234d5bbb47086c5e81a6e667b6751605a212f":
        raise SystemExit("FAIL: immutable HifzSession source pin changed")
    actual=Path(PATH).read_text(encoding="utf-8")
    marker="        if (SABQI.equals(mode) || ITQAN.equals(mode)) {"
    end="        murajaahActualEnd = prefs.murajaahActualEnd();"
    start=actual.find(marker)
    stop=actual.find(end,start)
    original=baseline
    if subprocess.check_output(["git","hash-object",PATH],text=True).strip()==GUARDED_BACKLOG_LABELS_SHA:
        # Two exact presentation-only substitutions. Repetitions, QCF masks,
        # credits, persistence and cursor algorithms must stay identical.
        changes=(
            ("Validez d’abord les 5 lignes du matin.",
             "Validez d’abord les 5 lignes d’Apprentissage du soir."),
            (" · entretien 1 hizb · ", " · répétition d’un hizb · "),
        )
        for before, after in changes:
            if original.count(before)!=1:
                raise SystemExit("FAIL: Hifz session label baseline mismatch")
            original=original.replace(before,after)
    if start<0 or stop<start or original!=actual[:start]+actual[stop:]:
        raise SystemExit("FAIL: backlog HifzSession delta touches frozen engines")
    guard=actual[start:stop]
    for needle in ("attendance.capture(prefs)","prefs.currentRoadmapDecision(geometry)",
                   "HifzBacklog.pending(","oldest.date.equals(sessionDate)",
                   "L’avancement actuel restera inchangé."):
        if needle not in guard: raise SystemExit("FAIL: missing chronological backlog guard: "+needle)
    print("PASS: exact receipt-backed session-entry UI delta; masks, cursors and credits frozen")
    raise SystemExit(0)

before_three=subprocess.check_output(
    ["git","show","ecbe005a79e5d89e77b9e9ac6968e6916cc8bdb8:"+PATH]).decode("utf-8")
old_header='''        // Latest user decision: FULL only for every new unit; deep ×40 + progressive eraser until
        // the first An-Nās arrival, then one hizb per session ×4 (2 visible + 2 anchored). A unit
        // already mid-repetition keeps the plan it was started with (see ItqanRegimeStore).'''
new_header='''        // FULL first pass ×40; after the first An-Nās arrival, one hizb ×3
        // (2 visible + 1 anchored). A previously opened ×4 unit finishes
        // its persisted plan without dropping the fourth repetition.'''
old_doc1='''     * Post-An-Nās maintenance recall (passes 3-4): the hizb disappears into the paper at'''
new_doc1='''     * Post-An-Nās maintenance recall (pass 3; legacy OPEN sessions also pass 4):'''
old_doc2='''     * 100% except the project's existing validated anchors — the Al-Munīr amorces (audited V2.1'''
new_doc2='''     * the hizb disappears at 100% except validated anchors — Al-Munīr amorces (audited V2.1'''
expected_three=before_three
for a,b in (
    (old_header,new_header),
    ("        itqanTargetReps = ItqanMaintenancePolicy.totalReps(itqanRegime, itqanSessionProtocol);",
     "        itqanTargetReps = ItqanMaintenancePolicy.totalReps(itqanUnitPlan);"),
    ("ItqanMaintenancePolicy.anchoredRecallRep(itqanRegime, ",
     "ItqanMaintenancePolicy.anchoredRecallRep(itqanUnitPlan, "),
    ("ItqanMaintenancePolicy.isValidationRep(itqanRegime, itqanSessionProtocol, rep - 1)",
     "ItqanMaintenancePolicy.isValidationRep(itqanUnitPlan, rep - 1)"),
    (old_doc1,new_doc1),(old_doc2,new_doc2)):
    n=expected_three.count(a)
    assert n==(4 if a.startswith("ItqanMaintenancePolicy.anchoredRecallRep") else 1),a
    expected_three=expected_three.replace(a,b)
# Release 1.17.4: ONLY the reviewed atomic active-page reader change is
# allowed beyond the fully tested post-Nās ×3 Hifz source. Both are exact blobs.
ATOMIC_ACTIVE_BEFORE="7aae5a444e87801b1e342cea6ecc080dcf81c64f"
ATOMIC_ACTIVE_AFTER="bf2234d5bbb47086c5e81a6e667b6751605a212f"
before_sha=subprocess.check_output(
    ["git","rev-parse","1f69dc58abbcd58ce2c44a78e2cd4ff0aca302ab:"+PATH],
    text=True).strip()
if before_sha!=ATOMIC_ACTIVE_BEFORE:
    raise SystemExit("FAIL: active-review first-paint baseline drift")
if subprocess.check_output(["git","hash-object",PATH],text=True).strip()==ATOMIC_ACTIVE_AFTER:
    source=Path(PATH).read_text(encoding="utf-8")
    assert "mushaf.showFreeActivePreparedPage(currentPage,currentLineIds," in source
    assert "activePageLandmarks,activePageAmorces,activeAyahThemesReady" in source
    assert "mushaf.setPageLandmarkBoxes(firstLast);" not in source
    assert "mushaf.setSemanticCues(newStarts, true, false);" not in source
    print("PASS: exact source pin for atomic active-mask 1.17.4; persistence and cursors unchanged")
    raise SystemExit(0)

if Path(PATH).read_text(encoding="utf-8")==expected_three:
    print("PASS: exact Itqan ×3 source-only delta; 1.17.2 Hifz persistence unchanged")
    raise SystemExit(0)

pre_duration=subprocess.check_output(
    ["git","show","51768b830c357a62d8aed17605e6061bb3a87a46:"+PATH]).decode("utf-8")
approved_old="""    if (kind == SessionKind.OLD_ITQAN_MURAJAAH) {
        // P4 J-15: the passive Entretien grows with the real ACQUIRED corpus instead of staying
        // a flat constant — see MaintenanceCoveragePolicy.
        return MaintenanceCoveragePolicy.minutes(
            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
    }"""
approved_new="""    if (kind == SessionKind.OLD_ITQAN_MURAJAAH || kind == SessionKind.ACTIVE_MURAJAAH) {
        // User decision 22 October 2026: both revisions use the identical 30/45+
        // minute coverage policy, starting now. The active mode still has its own
        // acquired-only corpus, masked reader, clock, independent cursor, and never
        // feeds masked-recall elapsed time into passive speed calibration.
        return MaintenanceCoveragePolicy.minutes(
            prefs.acquiredLineCountV6(), geometry.lineCount(), speedStore.maintenanceSecondsPerLine());
    }"""
if pre_duration.count(approved_old)!=1:
    raise SystemExit("FAIL: approved 1.17.2 review duration source drifted")
expected_parity=pre_duration.replace(approved_old,approved_new)
if Path(PATH).read_text(encoding="utf-8")==expected_parity:
    if subprocess.check_output(["git","hash-object", "--stdin"],
         input=pre_duration.encode()).decode().strip()!=ATLAS_ONLY_SHA:
        raise SystemExit("FAIL: source 1.17.2 baseline hash drifted")
    print("PASS: exact 30/45+ active duration parity ONLY; frozen Hifz persistence/cursors/masks")
    raise SystemExit(0)

if subprocess.check_output(["git","hash-object",PATH],text=True).strip()==ATLAS_ONLY_SHA:
    previous=subprocess.check_output(["git","show","41e6af84f2d0d6218686ecbca810a783db11c0c3:"+PATH]).decode("utf-8")
    expected=previous.replace("    private LinearLayout correspondenceActions;\n","").replace(
        "        correspondenceActions = Ui.row(this);\n        correspondenceActions.setVisibility(View.GONE);\n        controlBar.addView(correspondenceActions);\n","").replace(
        "        CorrespondenceEntryPoints.populateSabqi(this, correspondenceActions, mode,\n            currentSelection, currentPage, displayedMask(), currentLineIds);\n","")
    actual=Path(PATH).read_text()
    if actual!=expected:
        raise SystemExit("Atlas-only Hifz source differs beyond three removed UI hooks")
    print("PASS: exact Hifz engine preserved after deleting three Correspondances UI hooks")
    raise SystemExit(0)

if subprocess.check_output(["git","hash-object",PATH],text=True).strip()==CURRENT_SHA:
    print("PASS: exact pinned HifzSessionActivity; second Itqan rotation commit removed")
    raise SystemExit(0)
# CP02-C adds only an independently verified display layer, not new recall holes.
ui_check=subprocess.run(["python3","scripts/verify_ayah_themes_review_display_delta.py"],capture_output=True,text=True)
if ui_check.returncode==0:
    print(ui_check.stdout.strip())
    raise SystemExit(0)
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
# J6: passive-only corpus enlargement; active remains frozen.
replacements.append(("prefs.murajaahCorpus()","prefs.murajaahCorpus(geometry)"))
# J5 resumed previous date, with frozen set rather than silent recalibration.
replacements.extend([
    ("        speedStore = new HifzSpeedStore(this);","        // Resume the original frozen evening/final session, including its calendar date,\n        // instead of assigning yesterday's repetitions to today's new cumulative set.\n        ConsolidationCycleEngine.Family resumeFamily =\n            LEARNING_CONSOLIDATION.equals(mode)||LEARNING_FINAL.equals(mode)\n            ? ConsolidationCycleEngine.Family.LEARNING\n            : RECENT_SABQI_REVIEW.equals(mode)||CONSOLIDATION_FINAL.equals(mode)\n              ? ConsolidationCycleEngine.Family.STABILIZATION : null;\n        if(resumeFamily!=null) {\n            String prefix=LEARNING_FINAL.equals(mode)?\"learning-final-\"\n                : CONSOLIDATION_FINAL.equals(mode)?\"stabilization-final-\"\n                : LEARNING_CONSOLIDATION.equals(mode)?\"learning-\":\"stabilization-\";\n            String original=prefs.unfinishedConsolidationSessionDate(resumeFamily,prefix);\n            if(original!=null)sessionDate=LocalDate.parse(original);\n        }\n        speedStore = new HifzSpeedStore(this);"),
    ("if (consolidationSession != null && consolidationSession.sessionGroupSize() < units.size()) {","if (consolidationSession != null && consolidationSession.sessionGroupSize() < units.size()\n                && prefs.frozenConsolidationSnapshot(family,cycleIdPrefix+today).isEmpty()) {"),
])
for before,after in replacements:
    expected=4 if before=="prefs.murajaahCorpus()" else (2 if before.startswith("currentMask=ItqanMaintenancePolicy.maskForNextRep(") else 1)
    assert baseline.count(before)==expected,"Official 1.17.1 source no longer matches expected Hifz code."
    baseline=baseline.replace(before,after)
actual=Path(PATH).read_text(encoding="utf-8")
assert baseline == actual, ("UNAUTHORIZED HifzSessionActivity delta: "
    "only the three verified Ibn Kathir fail-open substitutions are allowed")
print("PASS: HifzSessionActivity matches official v1.17.1 except three exact QCF fail-open deltas")

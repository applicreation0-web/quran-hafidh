#!/usr/bin/env python3
"""Fail-closed explicit authorization for 1.17.5 Sunday finale slot isolation.

The changed source is pinned by Git object ID. All other Hifz contracts remain
subject to the earlier freeze. Updating either file requires a separate review
and regeneration of this gate, never a blanket freeze bypass.
"""
from pathlib import Path
import subprocess

BASE = "4135df3863e17da72adbd06457f46f2966ddcb6a"
PREFIX = "hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
FILES = {
    "HifzPrefs.java": ("066d2685a920b50877a6cd62cb6ad023337e16f4", "3c8a9b63c3f8835b57ea80a2059539d50469d1fe"),
    "HifzSessionActivity.java": ("add8fe985f8b3a2206fa4142c6e384e5a48a8c0c", "521ba4db317b18e696e34a340dbdf11147682ea6"),
}
def git(*args):
    return subprocess.check_output(["git", *args], text=True).strip()

for name, (baseline, approved) in FILES.items():
    path = PREFIX + name
    assert Path(path).is_file(), "Missing source: " + path
    assert git("rev-parse", BASE + ":" + path) == baseline, "Untrusted 4135 baseline: " + name
    assert git("hash-object", path) == approved, "Unreviewed 1.17.5 final change: " + name

prefs = Path(PREFIX + "HifzPrefs.java").read_text(encoding="utf-8")
session = Path(PREFIX + "HifzSessionActivity.java").read_text(encoding="utf-8")
for needle in (
    "migrateLegacyFinalSlot(family)",
    "consolidationFinalStateKey(ConsolidationCycleEngine.Family.LEARNING)",
    "consolidationFinalStateKey(ConsolidationCycleEngine.Family.STABILIZATION)",
    "restoreConsolidationSession(engine,family,null)",
    "SundayFinalPagePolicy.physicalPageUnits(inputs, positions)",
    "SundayFinalPagePolicy.earnedEveryLine(due, new ArrayList<>(paid), acquired)",
):
    assert needle in prefs, "Missing 1.17.5 migration invariant: " + needle
assert "restoreConsolidationSession(consolidationEngine, family,cycleIdPrefix+today)" in session
subprocess.run(["python3", "scripts/verify_1175_open_session_safety_delta.py"], check=True)
assert "Another frozen OPEN Consolidation session must be resumed first" in prefs
assert "Stale Consolidation repetition cannot roll back persisted progress" in prefs
assert "mushaf.showFreeActivePreparedPage(currentPage,currentLineIds," in session
assert "showSundayFinalLectureMaskPage()" in session
assert "anchors = ayahThemesTitles.forPage(currentPage);" in session
assert "mushaf.showFreeActivePreparedPage(currentPage, pageLines, landmarks, anchors, ready);" in session
assert "semanticPassages.readerCuesForPage(currentPage)" in session  # Only unchanged post-Nas Itqan paths
assert "mushaf.showFreeActivePreparedPage(currentPage,currentLineIds," in session
assert "sundayFinalAnchorUnavailable" in session
print("PASS: final and weekday states isolated, legacy payload migrated; QCF, Atlas, masks and post-Nas protocols retain prior independent gates")

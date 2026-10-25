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
    "HifzPrefs.java": ("066d2685a920b50877a6cd62cb6ad023337e16f4", "b3faeb478bc4b3f940380901d69ec530244ea0e5"),
    "HifzSessionActivity.java": ("add8fe985f8b3a2206fa4142c6e384e5a48a8c0c", "4684858ed1ef1884be48169592539d639b78f7e5"),
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
):
    assert needle in prefs, "Missing 1.17.5 migration invariant: " + needle
assert "restoreConsolidationSession(consolidationEngine, family,cycleIdPrefix+today)" in session
assert "discardConsolidationSession(family,cycleIdPrefix+today)" in session
assert "mushaf.showFreeActivePreparedPage(currentPage,currentLineIds," in session
print("PASS: final and weekday states isolated, legacy payload migrated; QCF, Atlas, masks and post-Nas protocols retain prior independent gates")

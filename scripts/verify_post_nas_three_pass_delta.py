#!/usr/bin/env python3
"""Fail-closed checkpoint for the 2026-10-22 user-requested post-An-Nas ×3.

No original deep ×40 schedule, persisted progression, or legacy open ×4 session
may be silently changed. This gate is independent from JVM behavioral tests.
"""
from pathlib import Path
import subprocess

BASE="ecbe005a79e5d89e77b9e9ac6968e6916cc8bdb8"
PREFIX="hifz-app/src/main/java/com/quransafeguard/hifz/preview/"
PINS={
    "ItqanMaintenancePolicy.java":"05c9dfaec00b7d67b3ab3aa8763337294da62935",
    "ItqanRegimeStore.java":"bf9f44a95fdbaeccdb80a7239372743f4eaf7225",
    "ItqanBalancedMaskSchedule.java":"4ed14f4f2559977405a7d23d61cac080302d8b06",
}
def git(*args):
    return subprocess.check_output(["git",*args],text=True).strip()
for filename,pin in PINS.items():
    target=PREFIX+filename
    assert Path(target).is_file(),f"Missing Itqan policy: {target}"
    assert git("hash-object",target)==pin,f"UNREVIEWED post-An-Nas change: {target}"

# Under no circumstances may the user request alter the storage writer or
# any acquired/Sabqi, Consolidation, Murajaah, calendar or QCF geometry state.
prefs=PREFIX+"HifzPrefs.java"
if git("hash-object",prefs)!=git("rev-parse",BASE+":"+prefs):
    # User-authorized 1.17.5 Sunday-final persistence bug fix; exact byte pins.
    subprocess.run(["python3","scripts/verify_authorized_1175_final_state_delta.py"],check=True)
core=PREFIX+"ItqanMaintenancePolicy.java"
policy=Path(core).read_text(encoding="utf-8")
assert "static final int VISIBLE_REPS = 2;" in policy
assert "static final int ANCHOR_REPS = 1;" in policy
assert "static final int LEGACY_OPEN_TOTAL_REPS = 4;" in policy
assert "return next == TOTAL_REPS;" in policy
assert "POST_NAS_THREE" in policy
regime=Path(PREFIX+"ItqanRegimeStore.java").read_text(encoding="utf-8")
assert "MaskProfile.POST_NAS_THREE" in regime
assert "MaskProfile.BALANCED_20_5_5_5_5" in regime
assert "return stamped;" in regime and "parts.length != 4 && parts.length != 5" in regime
masked=Path(PREFIX+"ItqanBalancedMaskSchedule.java").read_text(encoding="utf-8")
assert "ItqanMaintenancePolicy.maskForNextRep(plan,completed)" in masked
assert "plan.protocol==AnchoringQueue.ItqanProtocol.LIGHT" in masked
print("PASS: post-An-Nas one hizb ×3 (visible, visible, 100% anchored); old OPEN ×4 intact")
print("PASS: exact HifzPrefs progression writer and source-pinned Itqan policy")

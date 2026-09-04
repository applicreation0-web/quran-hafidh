#!/usr/bin/env python3
"""Release gate for the Al-Hikam layer in 0.10.4.

The 264/264 matn and its current French translation stay byte-for-byte frozen to
0.10.3. A deeper textual re-audit is not represented as complete here.

The historical sharh research is different: its own extractor marks candidates as
research-only. Because no independently verified display asset exists, 0.10.4 must
fail closed and keep the Plus sharh UI disabled instead of implying availability.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "1118119c4b4ac5f306cb10870935499659414d62"

FROZEN_GIT_BLOBS = {
    "app/src/main/assets/hikam/al_hikam_verified.json": "cf2453963ee1091f4f6c2a8d3a0e4a93c78b37c6",
    "app/src/main/assets/hikam/verification_report.json": "62c6eae40b37b5f97dd64d80ebfbae1a64c4fb36",
    "app/src/main/java/com/quranunlock/guard/HikamRepository.kt": "b83bd21cd92d26b560756ce0c6e46ca6ffcf3e34",
    "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt": "71e24d3de1e195dc6523e99504ce0f57cebab42c",
}

SHARH_EDITION = ROOT / "app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt"
SHARH_ASSET = ROOT / "app/src/plus/assets/hikam/hikam_sharh_dual.json"


def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


failures: list[str] = []
for rel, expected in FROZEN_GIT_BLOBS.items():
    path = ROOT / rel
    if not path.is_file():
        failures.append(f"missing frozen Hikam file: {rel}")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        failures.append(
            f"Hikam matn freeze violation: {rel} has blob {actual}, expected {expected} from {BASELINE}"
        )

if not SHARH_EDITION.is_file():
    failures.append("missing Plus HikamSharhEdition.kt")
else:
    sharh = SHARH_EDITION.read_text(encoding="utf-8")
    if "const val isEnabled: Boolean = false" not in sharh:
        failures.append("unverified Hikam sharh must be disabled in 0.10.4 Plus")
    if "if (!isEnabled) return emptyList()" not in sharh:
        failures.append("Hikam sharh API must fail closed while disabled")
    if "research-only" not in sharh:
        failures.append("Hikam sharh source status must remain explicit in code")

if SHARH_ASSET.exists():
    failures.append(
        "hikam_sharh_dual.json unexpectedly present: no commentary asset may ship until independently verified"
    )

if failures:
    raise SystemExit("0.10.4 HIKAM GATE FAILURE:\n- " + "\n- ".join(failures))

print("0.10.4 Hikam gate: PASS")
print(f"- baseline release: {BASELINE}")
print(f"- {len(FROZEN_GIT_BLOBS)} matn/translation/runtime files are byte-for-byte unchanged")
print("- Plus sharh is fail-closed; no unverified commentary asset is packaged")
print("- 0.10.4 makes no new claim that the separate 264/264 textual re-audit or commentary corpus is complete")

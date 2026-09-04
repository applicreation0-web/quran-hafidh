#!/usr/bin/env python3
"""0.10.4 Hikam integrity gate.

This gate deliberately distinguishes two questions:
1) has the shipped 0.10.3 Hikam layer been accidentally changed? (must be no),
2) has the stronger 0.10.4 matn-only, two-witness 264/264 documentary re-audit been completed? (not yet).

A release may use this script as a regression guard, but it must not use its PASS
message as proof that the stronger editorial release contract is complete.
"""
from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "1118119c4b4ac5f306cb10870935499659414d62"

EXPECTED_GIT_BLOBS = {
    "app/src/main/assets/hikam/al_hikam_verified.json": "cf2453963ee1091f4f6c2a8d3a0e4a93c78b37c6",
    "app/src/main/assets/hikam/verification_report.json": "62c6eae40b37b5f97dd64d80ebfbae1a64c4fb36",
    "app/src/main/java/com/quranunlock/guard/HikamRepository.kt": "b83bd21cd92d26b560756ce0c6e46ca6ffcf3e34",
    "app/src/main/java/com/quranunlock/guard/HikamDetailActivity.kt": "71e24d3de1e195dc6523e99504ce0f57cebab42c",
    "app/src/plus/java/com/quranunlock/guard/HikamSharhEdition.kt": "a3c3c03a68948deb9a2989369315932c02892640",
}


def git_blob_sha(path: Path) -> str:
    data = path.read_bytes()
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


failures: list[str] = []
for rel, expected in EXPECTED_GIT_BLOBS.items():
    path = ROOT / rel
    if not path.is_file():
        failures.append(f"missing frozen Hikam file: {rel}")
        continue
    actual = git_blob_sha(path)
    if actual != expected:
        failures.append(
            f"Hikam freeze violation: {rel} has blob {actual}, expected {expected} from {BASELINE}"
        )

if failures:
    raise SystemExit("0.10.4 HIKAM FREEZE FAILURE:\n- " + "\n- ".join(failures))

print("0.10.4 Hikam regression freeze: PASS")
print(f"- baseline release: {BASELINE}")
print(f"- {len(EXPECTED_GIT_BLOBS)} critical Hikam corpus/runtime files are byte-for-byte unchanged")
print("- IMPORTANT: stronger 264/264 matn-only + two-independent-witness documentary audit remains a separate release gate")

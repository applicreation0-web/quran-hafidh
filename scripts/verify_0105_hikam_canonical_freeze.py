#!/usr/bin/env python3
"""0.10.5 Hikam canonical-corpus freeze.

0.10.5 is explicitly allowed to change the Plus Sharh loader and its documentary
contract. It is NOT allowed to drift the canonical 264 Hikam, their verification
report or the canonical repository parser. This gate freezes only those immutable
objects and leaves the Sharh implementation to the dedicated 0.10.5 audits.
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
            f"Hikam canonical integrity violation: {rel} has blob {actual}, "
            f"expected {expected} from {BASELINE}"
        )

if failures:
    raise SystemExit(
        "0.10.5 HIKAM CANONICAL CORPUS INTEGRITY FAILURE:\n- " +
        "\n- ".join(failures)
    )

print("0.10.5 Hikam canonical corpus integrity: PASS")
print(f"- baseline release: {BASELINE}")
print("- canonical 264-entry corpus, verification report and repository parser are byte-for-byte frozen")
print("- Plus Sharh loader changes are intentionally outside this freeze and are governed by the dedicated 0.10.5 documentary/runtime gates")

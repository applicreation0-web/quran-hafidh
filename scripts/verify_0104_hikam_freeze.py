#!/usr/bin/env python3
"""Release gate: 0.10.4 must not alter the validated 0.10.3 Hikam layer.

The 0.10.4 release is scoped to multi-tafsir. A deeper 264/264 textual-criticism
project is intentionally not represented as completed by this release. Instead,
this gate proves that the existing Hikam corpus and its runtime presentation are
byte-for-byte identical to the audited 0.10.3 release baseline.
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

print("0.10.4 Hikam freeze: PASS")
print(f"- baseline release: {BASELINE}")
print(f"- {len(EXPECTED_GIT_BLOBS)} critical Hikam corpus/runtime files are byte-for-byte unchanged")
print("- 0.10.4 makes no new claim that the separate 264/264 textual re-audit is complete")

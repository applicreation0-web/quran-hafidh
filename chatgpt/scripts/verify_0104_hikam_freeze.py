#!/usr/bin/env python3
"""0.10.4 Hikam corpus integrity gate.

The 0.10.4 work is allowed to improve presentation, terminology aids and the
strict visual separation between matn / French translation / classical sharh.
What must never drift accidentally is the canonical 264-entry corpus itself,
its verification report, the repository parser, or the Plus sharh loader.

Translation changes, when intentionally reviewed, must go through a dedicated
translation audit rather than being hidden inside an unrelated feature commit.
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
            f"Hikam corpus integrity violation: {rel} has blob {actual}, expected {expected} from {BASELINE}"
        )

if failures:
    raise SystemExit("0.10.4 HIKAM CORPUS INTEGRITY FAILURE:\n- " + "\n- ".join(failures))

print("0.10.4 Hikam corpus integrity: PASS")
print(f"- baseline release: {BASELINE}")
print(f"- {len(EXPECTED_GIT_BLOBS)} canonical Hikam data/runtime files are byte-for-byte unchanged")
print("- HikamDetailActivity and terminology UI may evolve under dedicated UI/translation audits")

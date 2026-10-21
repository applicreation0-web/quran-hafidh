#!/usr/bin/env python3
"""Fail-closed visual freeze of Atlas/Mushaf/Jalalayn BOOX screenshot-approved sources.

Only Correspondances-specific adapters may evolve in this phase. No copied
assets, credentials, bibliographic text or private material are stored here.
"""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "hifz-app/src/main/java/com/quransafeguard/hifz/preview"
PINNED = {
    "MawdudiMapScreen.java": "a925f2be39b0ea0755bb3a552b16ff7bd987aa9d",
    "StudyReaderActivity.java": "30cf1f1fd118f6ef1f65a5da0a2fcd2ce4d752d8",
    "MapJalalaynDialog.java": "e2f82338c1feb0673b51e9a6f9375e4be37861b9",
    "MapQcfThumbnails.java": "750013d368854082d6dc6d1094d9c472ed962cc7",
    "MapUnitMarkers.java": "deaf4f97a08a771c88ba21048a60419323b613ab",
    "MushafView.java": "4473cf97fcfd080af08b0bd9790883bedb6901c4"
}

def git_blob(raw: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()

def main() -> None:
    failed = []
    for filename, expected in PINNED.items():
        p = SOURCE / filename
        if not p.is_file():
            failed.append(filename + ": missing")
            continue
        actual = git_blob(p.read_bytes())
        if actual != expected:
            failed.append(filename + ": unexpected source delta " + actual)
    if failed:
        raise SystemExit("ATLAS BOOX VISUAL FREEZE FAILED\n" + "\n".join(failed))
    print("PASS: Atlas dual rail, reader/Jalalayn, QCF and markers exact Git blobs (" +
          str(len(PINNED)) + " files)")

if __name__ == "__main__":
    main()

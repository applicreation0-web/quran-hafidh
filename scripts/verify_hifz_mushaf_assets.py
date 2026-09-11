#!/usr/bin/env python3
"""Offline integrity check for the vendored Quran Hifz Mushaf assets."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path

ROOT = Path("hifz-app/src/main/assets/mushaf/hafs/kfqc/svg-br")
MANIFEST = ROOT.parent / "manifest.json"


def git_blob_sha(data: bytes) -> str:
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    expected = [f"{page:03d}.svg.br" for page in range(1, 605)]
    actual = sorted(path.name for path in ROOT.glob("*.svg.br"))
    if actual != expected:
        raise SystemExit(f"Expected 604 canonical pages, found {len(actual)}")
    files = manifest.get("files", {})
    if sorted(files) != expected:
        raise SystemExit("Manifest does not cover exactly 604 pages")
    for name in expected:
        data = (ROOT / name).read_bytes()
        sha = git_blob_sha(data)
        if sha != files[name]["git_blob_sha1"]:
            raise SystemExit(f"Integrity mismatch: {name}")
    print("PASS Quran Hifz Mushaf assets: 604/604, hashes verified")


if __name__ == "__main__":
    main()

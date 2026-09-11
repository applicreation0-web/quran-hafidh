#!/usr/bin/env python3
"""Vendor the pinned local KFQC Hafs SVG-Brotli corpus into Quran Hifz.

The source is the pinned git submodule under third_party/quran-svg. This script
never downloads Quran data and is intended for local developer/release prep only.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile

PINNED_COMMIT = "1b427fab77aae1403fe7e1f0b8c794a5384d5605"
SOURCE_REPOSITORY = "quranpedia/quran-svg"
SUBMODULE = Path("third_party/quran-svg")
SOURCE = SUBMODULE / "mushafs/hafs/kfqc/svg-br"
DEST = Path("hifz-app/src/main/assets/mushaf/hafs/kfqc/svg-br")
MANIFEST = DEST.parent / "manifest.json"


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(SUBMODULE), *args],
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )
    return result.stdout.strip()


def git_blob_sha(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=path.name + ".", dir=str(path.parent))
    try:
        with os.fdopen(fd, "wb") as out:
            out.write(data)
            out.flush()
            os.fsync(out.fileno())
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp):
            os.unlink(tmp)


def verify_source() -> list[str]:
    if not SOURCE.is_dir():
        raise SystemExit(
            "Pinned quran-svg submodule is missing. Run: git submodule update --init --recursive"
        )

    head = git("rev-parse", "HEAD")
    if head != PINNED_COMMIT:
        raise SystemExit(f"Unexpected quran-svg commit: {head} != {PINNED_COMMIT}")

    dirty = git("status", "--porcelain", "--untracked-files=no")
    if dirty:
        raise SystemExit("quran-svg submodule has tracked local modifications; refusing to vendor")

    expected = [f"{page:03d}.svg.br" for page in range(1, 605)]
    actual = sorted(path.name for path in SOURCE.glob("*.svg.br"))
    if actual != expected:
        raise SystemExit(f"Expected the canonical 604-page set, found {len(actual)} pages")
    return expected


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--smoke", action="store_true", help="vendor only pages 001 and 002")
    args = parser.parse_args()

    expected = verify_source()
    wanted = expected[:2] if args.smoke else expected

    DEST.mkdir(parents=True, exist_ok=True)
    wanted_set = set(wanted)
    for existing in DEST.glob("*.svg.br"):
        if existing.name not in wanted_set:
            existing.unlink()

    manifest = {
        "source_repository": SOURCE_REPOSITORY,
        "pinned_commit": PINNED_COMMIT,
        "canonical_page_count": 604,
        "vendored_page_count": len(wanted),
        "mode": "smoke" if args.smoke else "full",
        "files": {},
    }

    for name in wanted:
        data = (SOURCE / name).read_bytes()
        atomic_write(DEST / name, data)
        manifest["files"][name] = {
            "git_blob_sha1": git_blob_sha(data),
            "sha256": hashlib.sha256(data).hexdigest(),
            "bytes": len(data),
        }
        print(f"OK {name} {len(data)} bytes")

    atomic_write(MANIFEST, (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode("utf-8"))
    print(f"Vendored {len(wanted)}/604 canonical pages from pinned local source")


if __name__ == "__main__":
    main()

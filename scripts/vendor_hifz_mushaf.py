#!/usr/bin/env python3
"""Vendor the pinned 604-page KFQC Hafs SVG-Brotli corpus into Quran Hifz.

This is a developer/local setup tool, never an app runtime dependency and never a
GitHub Actions debugging step.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import tempfile
import urllib.request

PINNED_COMMIT = "1b427fab77aae1403fe7e1f0b8c794a5384d5605"
SVG_BR_TREE = "fe267b844a17750e57fbe2dd5e9c8ca81d59c8f9"
REPO = "quranpedia/quran-svg"
DEST = Path("hifz-app/src/main/assets/mushaf/hafs/kfqc/svg-br")
MANIFEST = DEST.parent / "manifest.json"


def github_blob_sha(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def get_json(url: str) -> object:
    req = urllib.request.Request(url, headers={"User-Agent": "Quran-Hifz-local-vendor"})
    with urllib.request.urlopen(req, timeout=30) as response:
        return json.load(response)


def get_bytes(url: str) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "Quran-Hifz-local-vendor"})
    with urllib.request.urlopen(req, timeout=60) as response:
        return response.read()


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


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--smoke", action="store_true", help="vendor only pages 001 and 002")
    args = parser.parse_args()

    tree_url = f"https://api.github.com/repos/{REPO}/git/trees/{SVG_BR_TREE}"
    tree = get_json(tree_url)
    entries = {item["path"]: item for item in tree["tree"] if item.get("type") == "blob"}
    expected_names = [f"{page:03d}.svg.br" for page in range(1, 605)]
    if sorted(entries) != expected_names:
        raise SystemExit("Pinned source tree is not exactly the canonical 604-page set")

    wanted = expected_names[:2] if args.smoke else expected_names
    manifest = {
        "source_repository": REPO,
        "pinned_commit": PINNED_COMMIT,
        "svg_br_tree": SVG_BR_TREE,
        "page_count": 604,
        "files": {},
    }

    for name in wanted:
        expected_sha = entries[name]["sha"]
        raw_url = (
            f"https://raw.githubusercontent.com/{REPO}/{PINNED_COMMIT}/"
            f"mushafs/hafs/kfqc/svg-br/{name}"
        )
        data = get_bytes(raw_url)
        actual_sha = github_blob_sha(data)
        if actual_sha != expected_sha:
            raise SystemExit(f"Integrity mismatch for {name}: {actual_sha} != {expected_sha}")
        atomic_write(DEST / name, data)
        manifest["files"][name] = {"git_blob_sha1": actual_sha, "bytes": len(data)}
        print(f"OK {name} {len(data)} bytes {actual_sha}")

    atomic_write(MANIFEST, (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode("utf-8"))
    if args.smoke:
        print("Smoke corpus ready: 001.svg.br and 002.svg.br")
    else:
        print("Canonical local Mushaf ready: 604/604 pages")


if __name__ == "__main__":
    main()

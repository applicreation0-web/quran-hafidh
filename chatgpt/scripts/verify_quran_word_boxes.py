#!/usr/bin/env python3
"""Fail-closed integrity gate for the exact Quran word-box sidecar used by Quran Haafidh."""
from __future__ import annotations

import json
import sys
from pathlib import Path

EXPECTED_SCHEMA = "quran-haafidh-word-boxes-v1"
EXPECTED_RELEASE = "v1.1.2"
EXPECTED_BOX_SPACE = "viewBox 0 0 345 550"
EXPECTED_PAGES = 604
EXPECTED_WORDS = 77432
EXPECTED_FILES = (
    "word-boxes-001-150.json",
    "word-boxes-151-300.json",
    "word-boxes-301-450.json",
    "word-boxes-451-604.json",
)


def fail(message: str) -> None:
    raise SystemExit("Quran word geometry rejected: " + message)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: verify_quran_word_boxes.py SOURCE_DIR")
    root = Path(sys.argv[1])
    actual = sorted(p.name for p in root.glob("*.json"))
    if actual != sorted(EXPECTED_FILES):
        fail(f"unexpected source file set: {actual}")

    pages: dict[int, list] = {}
    seen_words: set[str] = set()
    total = 0
    for name in EXPECTED_FILES:
        data = json.loads((root / name).read_text(encoding="utf-8"))
        if data.get("schema") != EXPECTED_SCHEMA:
            fail(f"{name}: schema mismatch")
        if data.get("source") != "quran-ws/quran-svg-elements":
            fail(f"{name}: source mismatch")
        if data.get("source_release") != EXPECTED_RELEASE:
            fail(f"{name}: source release mismatch")
        if data.get("box_space") != EXPECTED_BOX_SPACE:
            fail(f"{name}: coordinate-space mismatch")
        for page_text, rows in data.get("pages", {}).items():
            page = int(page_text)
            if page in pages or not 1 <= page <= EXPECTED_PAGES:
                fail(f"duplicate/out-of-range page {page}")
            if not isinstance(rows, list) or not rows:
                fail(f"page {page}: no Quran words")
            last_key = None
            for row in rows:
                if not isinstance(row, list) or len(row) != 5:
                    fail(f"page {page}: invalid word row")
                key = str(row[0])
                try:
                    surah, ayah, word = (int(part) for part in key.split(":"))
                except Exception:
                    fail(f"page {page}: invalid word key {key!r}")
                if surah < 1 or surah > 114 or ayah < 1 or word < 1:
                    fail(f"page {page}: invalid word key {key}")
                if key in seen_words:
                    fail(f"duplicate Quran word key {key}")
                seen_words.add(key)
                x0, y0, x1, y1 = (float(v) for v in row[1:])
                if not (0 <= x0 < x1 <= 345 and 0 <= y0 < y1 <= 550):
                    fail(f"page {page}: invalid box for {key}")
                last_key = key
                total += 1
            pages[page] = rows

    if set(pages) != set(range(1, EXPECTED_PAGES + 1)):
        fail(f"page coverage is {len(pages)}/{EXPECTED_PAGES}")
    if total != EXPECTED_WORDS:
        fail(f"word count is {total}, expected {EXPECTED_WORDS}")
    if pages[1][0][0] != "1:1:1":
        fail("first Quran word key changed")
    if pages[604][-1][0] != "114:6:3":
        fail("last Quran word key changed")

    print(
        f"Quran word geometry OK: release={EXPECTED_RELEASE} "
        f"pages={len(pages)} words={total} box_space=345x550"
    )


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""0.10.6 presentation-only wrapper for the pinned multi-tafsir build.

Qurtubi's 0.10.5 parser uses INLINE_NUM both for source indexing and for display
cleanup. 0.10.6 must not change indexing. This wrapper therefore injects a
separate Unicode-aware DISPLAY_NUM matcher only while the approved assets are
rebuilt, then restores the canonical builder byte-for-byte.

The patch is fail-closed: every expected source fragment must occur exactly once.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUILDER = ROOT / "scripts" / "build_qurtubi.py"
PREPARE = ROOT / "scripts" / "prepare_multitafsir_assets.py"

INLINE_LINE = "INLINE_NUM=re.compile(r'\\b(\\d{1,3})\\.?\\s+(?=[A-Za-z\\u2018\\u201c])')"
DISPLAY_LINE = (
    "DISPLAY_NUM=re.compile(r'\\b(\\d{1,3})\\.?\\s+"
    "(?=(?:[^\\W\\d_]|[\"\\u2018\\u201c]))')"
)
LOOP_OLD = "for match in INLINE_NUM.finditer(text):"
LOOP_NEW = "for match in DISPLAY_NUM.finditer(text):"
META_OLD = (
    "'verse_marker_display_policy':'indexed-source-verse-labels-hidden-in-translation'}"
)
META_NEW = (
    "'verse_marker_display_policy':'indexed-source-verse-labels-hidden-in-translation',"
    "'presentation_revision':'0106-qurtubi-hide-verse-labels-v1'}"
)


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Qurtubi 0.10.6 patch drift: {label} count={count}")
    return text.replace(old, new, 1)


def main() -> None:
    original = BUILDER.read_text(encoding="utf-8")
    patched = replace_once(
        original,
        INLINE_LINE,
        INLINE_LINE + "\n" + DISPLAY_LINE,
        "INLINE_NUM declaration",
    )
    patched = replace_once(
        patched,
        LOOP_OLD,
        LOOP_NEW,
        "display-only marker loop",
    )
    patched = replace_once(
        patched,
        META_OLD,
        META_NEW,
        "presentation metadata",
    )
    if patched == original:
        raise SystemExit("Qurtubi 0.10.6 presentation patch made no change")

    BUILDER.write_text(patched, encoding="utf-8")
    try:
        subprocess.run(
            [sys.executable, str(PREPARE), *sys.argv[1:]],
            cwd=ROOT,
            check=True,
        )
    finally:
        BUILDER.write_text(original, encoding="utf-8")

    restored = BUILDER.read_text(encoding="utf-8")
    if restored != original:
        raise SystemExit("Qurtubi canonical builder was not restored after 0.10.6 build")
    print(
        "Qurtubi 0.10.6 display cleanup applied without changing source-indexing regex"
    )


if __name__ == "__main__":
    main()

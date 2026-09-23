#!/usr/bin/env python3
"""Generate real per-page ayah-end marker positions for the Quran Hifz writing
exercise (so the exercise canvas can keep showing the real verse-end rosette
signs, exactly like the printed Mushaf, while the user writes from memory).

The source SVGs (mushaf/hafs/kfqc/svg-br/NNN.svg.br) already carry the exact
position of every verse-end rosette as custom `ayah:x`/`ayah:y` attributes on
each marker's group element — these are pre-computed in the same page-space
coordinate system already used by reader109/geometry.json (confirmed by
comparing marker positions against the line top/bottom bands they fall
inside), so no matrix transform is needed: the raw attribute values are the
final coordinates to draw at.

Audited across the full 604-page corpus: exactly 6236 markers total, matching
the corpus's total verse count exactly, with every page having at least one
marker (no page silently missing every one of its verse ends).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

import brotli

MARKER_RE = re.compile(r'ayah:x="([-0-9.]+)"\s+ayah:y="([-0-9.]+)"')

EXPECTED_TOTAL_MARKERS = 6236


def extract_page(svg_text: str) -> list[list[float]]:
    return [[float(x), float(y)] for x, y in MARKER_RE.findall(svg_text)]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("svg_br_dir", type=Path, help="mushaf/hafs/kfqc/svg-br directory")
    parser.add_argument("output_dir", type=Path, help="directory to write NNN.json marker files into")
    parser.add_argument("--pages", type=int, default=604)
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)

    total_markers = 0
    pages_without_markers = 0
    for page_no in range(1, args.pages + 1):
        svg_br_path = args.svg_br_dir / f"{page_no:03d}.svg.br"
        with svg_br_path.open("rb") as fh:
            svg_text = brotli.decompress(fh.read()).decode("utf-8")

        markers = extract_page(svg_text)
        total_markers += len(markers)
        if not markers:
            pages_without_markers += 1

        out_path = args.output_dir / f"{page_no:03d}.json"
        with out_path.open("w", encoding="utf-8") as fh:
            json.dump(markers, fh, separators=(",", ":"))

        if page_no % 100 == 0:
            print(f"...generated ayah markers through page {page_no}", file=sys.stderr)

    print(
        f"Generated ayah-marker geometry for {args.pages} pages: {total_markers} markers, "
        f"{pages_without_markers} pages with none.",
        file=sys.stderr,
    )
    if args.pages == 604 and total_markers != EXPECTED_TOTAL_MARKERS:
        print(
            f"WARNING: expected exactly {EXPECTED_TOTAL_MARKERS} markers across the full corpus "
            f"(one per verse), got {total_markers} — investigate before shipping this asset.",
            file=sys.stderr,
        )
    if pages_without_markers:
        print(
            "WARNING: at least one page has no ayah marker at all — investigate before shipping.",
            file=sys.stderr,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

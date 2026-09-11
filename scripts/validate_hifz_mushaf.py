#!/usr/bin/env python3
"""Validate the pinned local 604-page Quran Hifz Mushaf source.

This is a build-time/audit tool only. It does not alter Mushaf assets.
"""
from __future__ import annotations

import argparse
import hashlib
import re
import statistics
import sys
from pathlib import Path

try:
    import brotli
except ImportError as exc:  # pragma: no cover - environment gate
    raise SystemExit("Missing Python dependency: brotli (pip install brotli)") from exc

VIEWBOX_RE = re.compile(rb"viewBox\s*=\s*['\"]\s*([-+0-9.eE]+)\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)\s+([-+0-9.eE]+)\s*['\"]")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "root",
        nargs="?",
        default="third_party/quran-svg/mushafs/hafs/kfqc/svg-br",
        help="Directory containing 001.svg.br .. 604.svg.br",
    )
    parser.add_argument(
        "--ratio-tolerance",
        type=float,
        default=0.03,
        help="Maximum relative aspect-ratio deviation from median before failure",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    root = Path(args.root)
    if not root.is_dir():
        print(f"ERROR: Mushaf directory not found: {root}", file=sys.stderr)
        return 2

    expected = [f"{page:03d}.svg.br" for page in range(1, 605)]
    actual = sorted(path.name for path in root.glob("*.svg.br"))
    if actual != expected:
        missing = sorted(set(expected) - set(actual))
        extra = sorted(set(actual) - set(expected))
        print(f"ERROR: expected exactly 604 canonical pages; found {len(actual)}", file=sys.stderr)
        if missing:
            print("Missing: " + ", ".join(missing[:20]), file=sys.stderr)
        if extra:
            print("Extra: " + ", ".join(extra[:20]), file=sys.stderr)
        return 3

    ratios: list[float] = []
    digests: dict[str, str] = {}
    page_ratios: list[tuple[int, float]] = []

    for page in range(1, 605):
        path = root / f"{page:03d}.svg.br"
        compressed = path.read_bytes()
        if not compressed:
            print(f"ERROR: empty page asset: {path.name}", file=sys.stderr)
            return 4
        digest = hashlib.sha256(compressed).hexdigest()
        if digest in digests:
            print(
                f"ERROR: duplicate compressed page content: {path.name} == {digests[digest]}",
                file=sys.stderr,
            )
            return 5
        digests[digest] = path.name

        try:
            svg = brotli.decompress(compressed)
        except brotli.error as exc:
            print(f"ERROR: Brotli decode failed for {path.name}: {exc}", file=sys.stderr)
            return 6

        if b"<svg" not in svg[:4096]:
            print(f"ERROR: decoded asset is not an SVG: {path.name}", file=sys.stderr)
            return 7

        match = VIEWBOX_RE.search(svg[:16384])
        if not match:
            print(f"ERROR: missing SVG viewBox: {path.name}", file=sys.stderr)
            return 8

        _, _, width_raw, height_raw = match.groups()
        width = float(width_raw)
        height = float(height_raw)
        if width <= 0 or height <= 0:
            print(f"ERROR: invalid viewBox dimensions in {path.name}: {width}x{height}", file=sys.stderr)
            return 9

        ratio = width / height
        ratios.append(ratio)
        page_ratios.append((page, ratio))

    median = statistics.median(ratios)
    tolerance = args.ratio_tolerance
    outliers = [
        (page, ratio)
        for page, ratio in page_ratios
        if abs(ratio - median) / median > tolerance
    ]
    if outliers:
        print(
            f"ERROR: {len(outliers)} page(s) have aspect ratio > {tolerance:.1%} away from median {median:.6f}",
            file=sys.stderr,
        )
        for page, ratio in outliers[:20]:
            print(f"  page {page:03d}: ratio={ratio:.6f}", file=sys.stderr)
        return 10

    print("OK: 604/604 Mushaf pages validated")
    print(f"Median viewBox ratio: {median:.6f}")
    print("Checks: exact filenames, non-empty, unique SHA-256, Brotli decode, SVG/viewBox, positive dimensions, ratio consistency")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

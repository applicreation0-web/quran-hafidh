#!/usr/bin/env python3
"""Generate compact per-word ink-shape geometry for the Quran Hifz writing exercise
(Palier 2: does the user's handwriting cover the real letterform?).

For every page of the bundled Mushaf (mushaf/hafs/kfqc/svg-br/NNN.svg.br), this
reads the real vector glyph paths already shipped in the app, groups their
subpaths into the same line/cell geometry the masking feature already uses
(reader109/geometry.json), and writes one compact JSON file per page: for each
line, for each cell (word/segment), the list of subpaths that belong to it, each
subpath simplified to a short polyline (not the original bezier data — a
handwriting-coverage check does not need pixel-exact curves, only the real
shape and position).

Tolerance for assigning a subpath to a cell is deliberately generous (matches
what a full 604-page audit found to be the best general default: about 1.0%
of cells end up with no matched ink, almost entirely genuine gaps rather than
false negatives). A stricter tolerance was tried and rejected: it more than
doubled the empty-cell rate because Arabic ascenders/descenders legitimately
extend past a cell's nominal line band.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

import brotli
from svgpathtools import parse_path

MATRIX_RE = re.compile(
    r"matrix\(([-\d.]+)[ ,]([-\d.]+)[ ,]([-\d.]+)[ ,]([-\d.]+)[ ,]([-\d.]+)[ ,]([-\d.]+)\)"
)
GROUP_RE = re.compile(r'<g transform="translate\(([-\d.]+)[ ,]([-\d.]+)\)"><path\s+([^>]*?)/?>')
ATTR_D_RE = re.compile(r'\bd="([^"]+)"')
ATTR_CLASS_RE = re.compile(r'\bclass="([^"]+)"')

Y_TOLERANCE = 1.0
X_TOLERANCE = 2.0
MIN_SAMPLES = 4
MAX_SAMPLES = 14
SAMPLES_PER_UNIT_LENGTH = 1.0 / 1.2


def parse_matrix(s: str) -> tuple[float, float, float, float, float, float]:
    m = MATRIX_RE.match(s)
    if not m:
        raise ValueError(f"unrecognized matrix: {s!r}")
    return tuple(float(x) for x in m.groups())  # type: ignore[return-value]


def sample_subpath(subpath, a, b, c, d, e, f, tx, ty) -> list[list[float]]:
    length = subpath.length()
    n = max(MIN_SAMPLES, min(MAX_SAMPLES, round(length * SAMPLES_PER_UNIT_LENGTH)))
    points = []
    for i in range(n):
        t = i / (n - 1) if n > 1 else 0.0
        z = subpath.point(t)
        x2, y2 = z.real + tx, z.imag + ty
        px = a * x2 + c * y2 + e
        py = b * x2 + d * y2 + f
        points.append([round(px, 2), round(py, 2)])
    return points


def extract_page(svg_text: str, page_lines: list[dict]) -> tuple[list, int, int]:
    m_outer = MATRIX_RE.search(svg_text)
    if not m_outer:
        raise ValueError("no outer matrix found")
    a, b, c, d, e, f = parse_matrix(m_outer.group(0))

    idx = svg_text.find('<g id="content">')
    tail = svg_text[idx:] if idx >= 0 else svg_text

    cell_shapes: list[list[list[list[float]]]] = [
        [[] for _ in line["cells"]] for line in page_lines
    ]
    total_cells = sum(len(line["cells"]) for line in page_lines)

    for gm in GROUP_RE.finditer(tail):
        tx, ty = float(gm.group(1)), float(gm.group(2))
        attrs = gm.group(3)
        cm = ATTR_CLASS_RE.search(attrs)
        if cm and "ayahPolygon" in cm.group(1):
            continue
        dm = ATTR_D_RE.search(attrs)
        if not dm:
            continue
        try:
            path = parse_path(dm.group(1))
        except Exception:
            continue
        for sp in path.continuous_subpaths():
            try:
                xmin, xmax, ymin, ymax = sp.bbox()
            except Exception:
                continue
            cx, cy = (xmin + xmax) / 2, (ymin + ymax) / 2
            x2, y2 = cx + tx, cy + ty
            px = a * x2 + c * y2 + e
            py = b * x2 + d * y2 + f
            for li, line in enumerate(page_lines):
                top, bottom = line["top"], line["bottom"]
                if not (top - Y_TOLERANCE <= py <= bottom + Y_TOLERANCE):
                    continue
                for ci, (x0, x1) in enumerate(line["cells"]):
                    if x0 - X_TOLERANCE <= px <= x1 + X_TOLERANCE:
                        cell_shapes[li][ci].append(
                            sample_subpath(sp, a, b, c, d, e, f, tx, ty)
                        )
                        break
                break

    empty = sum(1 for line in cell_shapes for cell in line if not cell)
    return cell_shapes, total_cells, empty


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("svg_br_dir", type=Path, help="mushaf/hafs/kfqc/svg-br directory")
    parser.add_argument("geometry_json", type=Path, help="reader109/geometry.json path")
    parser.add_argument("output_dir", type=Path, help="directory to write NNN.json shape files into")
    parser.add_argument("--pages", type=int, default=604)
    args = parser.parse_args()

    with args.geometry_json.open(encoding="utf-8") as fh:
        geometry = json.load(fh)

    args.output_dir.mkdir(parents=True, exist_ok=True)

    total_cells_all = 0
    total_empty_all = 0
    for page_no in range(1, args.pages + 1):
        svg_br_path = args.svg_br_dir / f"{page_no:03d}.svg.br"
        with svg_br_path.open("rb") as fh:
            svg_text = brotli.decompress(fh.read()).decode("utf-8")

        page_lines = geometry["pages"][str(page_no)]["lines"]
        shapes, total_cells, empty = extract_page(svg_text, page_lines)
        total_cells_all += total_cells
        total_empty_all += empty

        out_path = args.output_dir / f"{page_no:03d}.json"
        with out_path.open("w", encoding="utf-8") as fh:
            json.dump(shapes, fh, separators=(",", ":"))

        if page_no % 100 == 0:
            print(f"...generated shapes through page {page_no}", file=sys.stderr)

    empty_pct = 100 * total_empty_all / total_cells_all if total_cells_all else 0
    print(
        f"Generated word-shape geometry for {args.pages} pages: "
        f"{total_cells_all} cells, {total_empty_all} with no matched ink ({empty_pct:.2f}%).",
        file=sys.stderr,
    )
    if empty_pct > 2.0:
        print(
            "WARNING: empty-cell rate exceeds the 2% sanity ceiling — "
            "investigate before shipping this asset.",
            file=sys.stderr,
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

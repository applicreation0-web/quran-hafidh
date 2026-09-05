#!/usr/bin/env python3
"""Render small source crops around special Tafsir glyphs for visual mapping.

Research-only. The images are diagnostic evidence; no font files are extracted or
published and no corpus text is modified.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import fitz

QURTUBI_SAMPLES = [
    (1, 7, "g", "prophet_g"),
    (1, 299, "f", "prophet_f"),
    (2, 18, "c", "god_c"),
    (1, 149, "n", "adam_n"),
    (1, 58, "h", "umar_h"),
    (1, 291, "p", "muhammad_p"),
    (1, 364, "i", "aisha_i"),
    (1, 46, "k", "umar_k"),
]


def render_span(page: fitz.Page, bbox, out: Path) -> None:
    x0, y0, x1, y1 = [float(v) for v in bbox]
    clip = fitz.Rect(max(0, x0 - 155), max(0, y0 - 18), min(page.rect.width, x1 + 155), min(page.rect.height, y1 + 18))
    pix = page.get_pixmap(matrix=fitz.Matrix(3, 3), clip=clip, alpha=False)
    pix.save(out)


def qurtubi(source_dir: Path, out_dir: Path) -> None:
    docs = {n: fitz.open(source_dir / f"qurtubi-v{n}.pdf") for n in range(1, 5)}
    for volume, page_number, wanted, label in QURTUBI_SAMPLES:
        page = docs[volume][page_number - 1]
        found = None
        for block in page.get_text("dict").get("blocks", []):
            for line in block.get("lines", []):
                for span in line.get("spans", []):
                    if span.get("font") == "KFGQPCArabicSymbols01" and span.get("text", "").strip() == wanted:
                        found = span
                        break
                if found:
                    break
            if found:
                break
        if not found:
            raise RuntimeError(f"missing Qurtubi sample {label} on v{volume} p{page_number}")
        render_span(page, found["bbox"], out_dir / f"qurtubi_{label}.png")


def qushayri(pdf: Path, out_dir: Path) -> None:
    doc = fitz.open(pdf)
    first_by_cp = {}
    for page_index in range(37, 506):
        page = doc[page_index]
        for block in page.get_text("dict").get("blocks", []):
            for line in block.get("lines", []):
                for span in line.get("spans", []):
                    for ch in span.get("text", ""):
                        if 0xE000 <= ord(ch) <= 0xF8FF and ord(ch) not in first_by_cp:
                            first_by_cp[ord(ch)] = (page_index, span)
    for cp, (page_index, span) in sorted(first_by_cp.items()):
        render_span(doc[page_index], span["bbox"], out_dir / f"qushayri_U{cp:04X}.png")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    args = parser.parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)
    qurtubi(args.source_dir, args.out_dir)
    qushayri(args.source_dir / "lataif.pdf", args.out_dir)
    print("rendered", len(list(args.out_dir.glob("*.png"))), "glyph samples")


if __name__ == "__main__":
    main()

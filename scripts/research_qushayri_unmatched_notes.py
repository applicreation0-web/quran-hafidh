#!/usr/bin/env python3
"""Research-only diagnosis for residual Qushayri footnote-body keys.

Prints every occurrence of the target number/codepoints on the same source page,
with span font/size/bbox. No production data is modified.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

import fitz

TARGETS = [
    (61, 30), (87, 90), (170, 246), (241, 374), (339, 133),
    (344, 149), (349, 167), (354, 176), (366, 191), (370, 201), (448, 72),
]
SUPERSCRIPT_MAP = str.maketrans("⁰¹²³⁴⁵⁶⁷⁸⁹", "0123456789")
SUBSCRIPT_MAP = str.maketrans("₀₁₂₃₄₅₆₇₈₉", "0123456789")


def normalized_digits(text: str) -> str:
    return text.translate(SUPERSCRIPT_MAP).translate(SUBSCRIPT_MAP)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    doc = fitz.open(args.pdf)
    output = []
    for page_number, target in TARGETS:
        page = doc[page_number - 1]
        page_height = float(page.rect.height)
        records = []
        for block_index, block in enumerate(page.get_text("dict").get("blocks", [])):
            for line_index, line in enumerate(block.get("lines", [])):
                spans = [s for s in line.get("spans", []) if s.get("text", "")]
                if not spans:
                    continue
                text = "".join(s.get("text", "") for s in spans)
                normalized = normalized_digits(text)
                target_pattern = re.compile(rf"(?<!\d){target}(?!\d)")
                if not target_pattern.search(normalized):
                    continue
                records.append({
                    "block": block_index,
                    "line": line_index,
                    "text": text,
                    "normalized": normalized,
                    "bbox": [round(float(x), 2) for x in line.get("bbox", (0, 0, 0, 0))],
                    "relative_y": round(float(line.get("bbox", (0, 0, 0, 0))[1]) / page_height, 4),
                    "spans": [
                        {
                            "text": s.get("text", ""),
                            "codepoints": [f"U+{ord(c):04X}" for c in s.get("text", "")],
                            "font": s.get("font", ""),
                            "size": round(float(s.get("size", 0)), 3),
                            "flags": int(s.get("flags", 0)),
                            "bbox": [round(float(x), 2) for x in s.get("bbox", (0, 0, 0, 0))],
                        }
                        for s in spans
                    ],
                })
        output.append({"page": page_number, "target": target, "occurrences": records})

    payload = {
        "status": "research_only",
        "production_eligible": False,
        "targets": output,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(payload, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

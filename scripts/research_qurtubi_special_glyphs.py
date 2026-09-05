#!/usr/bin/env python3
"""Research-only inventory of Qurtubi PDF special-glyph spans.

The English Qurtubi PDFs use one or more embedded/symbol fonts for honorific
marks. Generic PDF text extraction can expose these as ASCII letters (notably
`g`) or private-use characters. This tool records the *span font + text + local
context* so production code can map only proven source glyphs. It never edits
Tafsir text.
"""
from __future__ import annotations

import argparse
import collections
import json
import re
from pathlib import Path

import fitz

PUA = re.compile(r"[\ue000-\uf8ff]")
HONORIFIC_CONTEXT = re.compile(
    r"(?:Muhammad|Muḥammad|Prophet|Messenger of Allah|Messenger|Abu Bakr|Abū Bakr|"
    r"Umar|ʿUmar|Uthman|ʿUthmān|Ali|ʿAlī|Allah|Ibrahim|Abraham|Musa|Moses|Isa|Jesus|"
    r"Jibril|Gabriel)",
    re.IGNORECASE,
)


def line_text(line: dict) -> str:
    return "".join(span.get("text", "") for span in line.get("spans", []))


def analyze_pdf(path: Path, label: str) -> dict:
    doc = fitz.open(path)
    candidates = []
    font_text_counts = collections.Counter()
    pua_counts = collections.Counter()
    all_fonts = collections.Counter()

    for page_index, page in enumerate(doc):
        data = page.get_text("dict")
        for block in data.get("blocks", []):
            for line in block.get("lines", []):
                spans = line.get("spans", [])
                full = line_text(line)
                context_hit = bool(HONORIFIC_CONTEXT.search(full))
                for index, span in enumerate(spans):
                    text = span.get("text", "")
                    font = span.get("font", "")
                    size = round(float(span.get("size", 0.0)), 3)
                    flags = int(span.get("flags", 0))
                    all_fonts[font] += len(text)
                    if PUA.search(text):
                        for char in text:
                            if PUA.match(char):
                                pua_counts[(font, f"U+{ord(char):04X}")] += 1
                    # A suspicious ASCII g is only a candidate here; final mapping
                    # must use the font and visual/source evidence.
                    suspicious_g = text.strip() == "g" or (
                        "g" in text and context_hit and len(text.strip()) <= 3
                    )
                    special_font = bool(PUA.search(text)) or any(
                        token in font.lower()
                        for token in ("symbol", "arab", "islam", "wing", "icon", "glyph", "ornament")
                    )
                    if suspicious_g or special_font or (context_hit and len(text.strip()) <= 3):
                        previous = spans[index - 1].get("text", "") if index > 0 else ""
                        following = spans[index + 1].get("text", "") if index + 1 < len(spans) else ""
                        record = {
                            "volume": label,
                            "pdf_page": page_index + 1,
                            "text": text,
                            "codepoints": [f"U+{ord(c):04X}" for c in text],
                            "font": font,
                            "size": size,
                            "flags": flags,
                            "previous_span": previous[-80:],
                            "next_span": following[:80],
                            "line": full[:500],
                            "bbox": [round(float(x), 2) for x in span.get("bbox", (0, 0, 0, 0))],
                        }
                        candidates.append(record)
                        font_text_counts[(font, text)] += 1

    return {
        "volume": label,
        "path": str(path),
        "page_count": doc.page_count,
        "fonts_by_character_count": [
            {"font": font, "characters": count}
            for font, count in all_fonts.most_common()
        ],
        "candidate_font_text_counts": [
            {"font": font, "text": text, "count": count}
            for (font, text), count in font_text_counts.most_common()
        ],
        "private_use_counts": [
            {"font": font, "codepoint": codepoint, "count": count}
            for (font, codepoint), count in pua_counts.most_common()
        ],
        "candidates": candidates,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    reports = []
    for number in range(1, 5):
        path = args.source_dir / f"qurtubi-v{number}.pdf"
        if not path.is_file():
            raise SystemExit(f"missing source: {path}")
        reports.append(analyze_pdf(path, f"v{number}"))

    aggregate = collections.Counter()
    for report in reports:
        for item in report["candidate_font_text_counts"]:
            aggregate[(item["font"], item["text"])] += item["count"]

    payload = {
        "status": "research_only",
        "production_eligible": False,
        "rule": "map only a glyph proven by source span/font evidence; never insert by person name",
        "aggregate_candidate_font_text_counts": [
            {"font": font, "text": text, "count": count}
            for (font, text), count in aggregate.most_common()
        ],
        "volumes": reports,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "candidate_count": sum(len(r["candidates"]) for r in reports),
        "top_font_text": payload["aggregate_candidate_font_text_counts"][:30],
        "pua": [item for r in reports for item in r["private_use_counts"]][:30],
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

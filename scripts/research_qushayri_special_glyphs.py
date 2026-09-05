#!/usr/bin/env python3
"""Research-only inventory of Qushayri private-use/source glyphs.

Records each PUA glyph with embedded font, neighboring spans and line context so
production can restore only source-attested honorific symbols. No text is edited.
"""
from __future__ import annotations

import argparse
import collections
import json
from pathlib import Path

import fitz


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    doc = fitz.open(args.pdf)
    occurrences = []
    counts = collections.Counter()
    for page_index, page in enumerate(doc):
        for block in page.get_text("dict").get("blocks", []):
            for line in block.get("lines", []):
                spans = line.get("spans", [])
                full = "".join(s.get("text", "") for s in spans)
                for i, span in enumerate(spans):
                    text = span.get("text", "")
                    pua_chars = [c for c in text if 0xE000 <= ord(c) <= 0xF8FF]
                    if not pua_chars:
                        continue
                    for char in pua_chars:
                        key = (span.get("font", ""), f"U+{ord(char):04X}", char)
                        counts[key] += 1
                        occurrences.append({
                            "pdf_page": page_index + 1,
                            "codepoint": f"U+{ord(char):04X}",
                            "glyph_text": char,
                            "font": span.get("font", ""),
                            "size": round(float(span.get("size", 0.0)), 3),
                            "previous_span": spans[i-1].get("text", "")[-120:] if i else "",
                            "next_span": spans[i+1].get("text", "")[:120] if i+1 < len(spans) else "",
                            "line": full[:600],
                        })
    payload = {
        "status": "research_only",
        "production_eligible": False,
        "counts": [
            {"font": font, "codepoint": cp, "glyph_text": char, "count": count}
            for (font, cp, char), count in counts.most_common()
        ],
        "occurrences": occurrences,
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"counts": payload["counts"], "sample": occurrences[:80]}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

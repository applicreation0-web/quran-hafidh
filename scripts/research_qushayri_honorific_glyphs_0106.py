#!/usr/bin/env python3
"""Inventory every private-use honorific/decorative glyph in pinned Qushayri PDF.

This is a source-research gate only. It does not modify the corpus. It records
codepoint counts and contexts so the release mapping can be derived from the
edition's own abbreviation legend rather than guessed from names.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
from pathlib import Path

import fitz

EXPECTED_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
KNOWN = {
    0xF023, 0xF063, 0xF067, 0xF068, 0xF069, 0xF06E, 0xF070, 0xF072,
    0xF081, 0xF082, 0xF085, 0xF094, 0xF096,
}


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf", type=Path)
    ap.add_argument("--json", type=Path)
    args = ap.parse_args()
    if sha256(args.pdf) != EXPECTED_SHA256:
        raise SystemExit("Pinned Qushayri PDF SHA-256 mismatch")

    doc = fitz.open(args.pdf)
    counts = collections.Counter()
    payload_counts = collections.Counter()
    contexts: dict[int, list[dict]] = collections.defaultdict(list)

    # The edition itself defines the symbol legend on PDF page 26 (index 25).
    legend_text = doc[25].get_text("text")
    print("=== EDITION HONORIFIC LEGEND PAGE ===")
    print(legend_text)

    for pi, page in enumerate(doc):
        for bi, block in enumerate(page.get_text("dict").get("blocks", [])):
            if "lines" not in block:
                continue
            for li, line in enumerate(block["lines"]):
                spans = line.get("spans", [])
                raw = "".join(span.get("text", "") for span in spans)
                pua = [ch for ch in raw if 0xE000 <= ord(ch) <= 0xF8FF]
                if not pua:
                    continue
                max_size = max((float(s.get("size", 0.0)) for s in spans), default=0.0)
                fonts = [s.get("font", "") for s in spans if s.get("text", "").strip()]
                # Match the production extraction window closely enough to separate
                # commentary honorifics from contents/ornaments and footnotes.
                in_payload_window = 37 <= pi < 506 and max_size > 9.4
                if any("KFGQPC" in f or "Arabic" in f for f in fonts):
                    in_payload_window = False
                for ch in pua:
                    cp = ord(ch)
                    counts[cp] += 1
                    if in_payload_window:
                        payload_counts[cp] += 1
                    if len(contexts[cp]) < 12:
                        contexts[cp].append({
                            "pdf_page": pi + 1,
                            "block": bi,
                            "line": li,
                            "payload_window": in_payload_window,
                            "text": raw.strip(),
                        })

    unknown = set(counts) - KNOWN
    if unknown:
        raise SystemExit("Unknown Qushayri PUA codepoints: " + ", ".join(f"U+{cp:04X}" for cp in sorted(unknown)))

    report = {
        "source_sha256": EXPECTED_SHA256,
        "all_pdf_counts": {f"U+{cp:04X}": counts[cp] for cp in sorted(counts)},
        "payload_window_counts": {f"U+{cp:04X}": payload_counts[cp] for cp in sorted(payload_counts)},
        "contexts": {f"U+{cp:04X}": contexts[cp] for cp in sorted(contexts)},
    }
    print("=== PUA INVENTORY ===")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    if args.json:
        args.json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Independent, research-only cross-check of Qushayri footnote calls.

The primary inventory identifies source calls textually and note bodies by two
layout strategies. This script approaches the same question from the opposite
direction: it looks for *typographic superscript numeric spans* inside main
English commentary lines and only accepts a call when a note-body candidate
with the same number exists on the same PDF page.

No output from this script is production eligible. Its purpose is to prove that
the textual call detector did not silently miss genuine printed note calls.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
from pathlib import Path

import fitz

EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
FIRST_COMMENTARY_PAGE_INDEX = 37
LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE = 506
SMALL_TEXT_MAX = 9.4
NOTE_START_RE = re.compile(r"^(\d{1,3})[\s\u2009\u200a]+")
INLINE_CALL_RE = re.compile(r"(?<!\d\.)(?<=[\.,;!?…\)\]”’\"])(\d{1,3})(?![\d:])")
ARABIC_RE = re.compile(
    r"[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]"
)
HEADER_RE = re.compile(
    r"^(Subtle Allusions\s+\[|Laṭāʾif al-ishārāt\s+\[|\d+\s*\|\s*•|•\s*Laṭāʾif)",
    re.I,
)


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def nonempty_spans(line: dict) -> list[dict]:
    return [s for s in line.get("spans", []) if s.get("text", "").strip()]


def line_text(line: dict) -> str:
    return "".join(s.get("text", "") for s in line.get("spans", [])).replace("\u00a0", " ").strip()


def mostly_arabic(text: str) -> bool:
    letters = [c for c in text if c.isalpha()]
    return bool(letters) and sum(bool(ARABIC_RE.match(c)) for c in letters) / len(letters) > 0.45


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    source_sha = sha256_file(args.pdf)
    if source_sha != EXPECTED_SOURCE_SHA256:
        raise SystemExit(f"Qushayri source SHA-256 mismatch: {source_sha}")

    doc = fitz.open(args.pdf)
    apparatus: dict[int, list[str]] = collections.defaultdict(list)
    layout_records: dict[int, list[dict]] = collections.defaultdict(list)
    textual_calls: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    superscript_candidates: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)

    for page_index in range(FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE):
        page_number = page_index + 1
        page_dict = doc[page_index].get_text("dict")
        for block_index, block in enumerate(page_dict.get("blocks", [])):
            for line_index, line in enumerate(block.get("lines", [])):
                spans = nonempty_spans(line)
                if not spans:
                    continue
                text = line_text(line)
                if not text or HEADER_RE.match(text) or mostly_arabic(text):
                    continue
                sizes = [float(s.get("size", 0.0)) for s in spans]
                max_size = max(sizes)
                bbox = tuple(float(v) for v in line.get("bbox", (0, 0, 0, 0)))
                layout_records[page_number].append(
                    {
                        "text": text,
                        "bbox": bbox,
                        "block": block_index,
                        "line": line_index,
                    }
                )

                if max_size <= SMALL_TEXT_MAX:
                    apparatus[page_number].append(text)
                    continue

                for match in INLINE_CALL_RE.finditer(text):
                    textual_calls[(page_number, int(match.group(1)))].append(
                        {"text": text, "offset": match.start(1)}
                    )

                # Independent call detector: a genuine printed footnote call is
                # commonly a smaller numeric span raised relative to the prose.
                # Requiring a substantial size drop makes this fail closed.
                for span in spans:
                    raw = span.get("text", "").strip()
                    if not re.fullmatch(r"\d{1,3}", raw):
                        continue
                    size = float(span.get("size", 0.0))
                    if size > max_size * 0.82:
                        continue
                    superscript_candidates[(page_number, int(raw))].append(
                        {
                            "text": text,
                            "span_text": raw,
                            "span_size": round(size, 3),
                            "line_max_size": round(max_size, 3),
                            "span_bbox": [round(float(v), 2) for v in span.get("bbox", (0, 0, 0, 0))],
                        }
                    )

    # Note body detector 1: numbered starts in the conservative small-text apparatus.
    strict_note_keys: set[tuple[int, int]] = set()
    for page_number, lines in apparatus.items():
        for text in lines:
            match = NOTE_START_RE.match(text)
            if match:
                strict_note_keys.add((page_number, int(match.group(1))))

    # Note body detector 2: a standalone bottom-page number followed on the
    # same baseline by one English body line.
    layout_note_keys: set[tuple[int, int]] = set()
    for page_number, records in layout_records.items():
        page = doc[page_number - 1]
        width = float(page.rect.width)
        height = float(page.rect.height)
        for record in records:
            text = record["text"].strip()
            if not re.fullmatch(r"\d{1,3}", text):
                continue
            x0, y0, x1, y1 = record["bbox"]
            if y0 < height * 0.55 or x0 > width * 0.30:
                continue
            cy = (y0 + y1) / 2.0
            neighbors = []
            for other in records:
                if other is record:
                    continue
                other_text = other["text"].strip()
                if not other_text or re.fullmatch(r"\d{1,3}", other_text) or mostly_arabic(other_text):
                    continue
                ox0, oy0, ox1, oy1 = other["bbox"]
                ocy = (oy0 + oy1) / 2.0
                if ox0 > x1 and ox0 <= width * 0.55 and abs(ocy - cy) <= 5.0:
                    neighbors.append(other)
            if len(neighbors) == 1:
                layout_note_keys.add((page_number, int(text)))

    body_keys = strict_note_keys | layout_note_keys
    textual_keys = set(textual_calls)
    superscript_keys = set(superscript_candidates)
    textual_matched = textual_keys & body_keys
    superscript_matched = superscript_keys & body_keys
    union_matched = textual_matched | superscript_matched

    report = {
        "schema": "quran-safeguard-qushayri-note-superscript-crosscheck-v1",
        "production_eligible": False,
        "source_sha256": source_sha,
        "note_body_union_keys": len(body_keys),
        "textual_call_keys_after_decimal_exclusion": len(textual_keys),
        "textual_calls_matched_to_body": len(textual_matched),
        "superscript_candidate_keys": len(superscript_keys),
        "superscript_calls_matched_to_body": len(superscript_matched),
        "matched_by_text_or_superscript": len(union_matched),
        "superscript_only_matched_keys": [list(k) for k in sorted(superscript_matched - textual_matched)],
        "textual_only_matched_keys": [list(k) for k in sorted(textual_matched - superscript_matched)],
        "body_keys_without_any_call_detector": [list(k) for k in sorted(body_keys - union_matched)],
        "superscript_candidates_without_body": [list(k) for k in sorted(superscript_keys - body_keys)],
        "duplicate_superscript_candidate_keys": [
            list(k) for k, values in sorted(superscript_candidates.items()) if len(values) != 1
        ],
        "superscript_samples": [
            {"page": k[0], "number": k[1], **superscript_candidates[k][0]}
            for k in sorted(superscript_matched)[:100]
        ],
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "superscript_samples"}, ensure_ascii=False, indent=2))
    print(f"Research-only report written to {args.report}")


if __name__ == "__main__":
    main()

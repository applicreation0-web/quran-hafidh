#!/usr/bin/env python3
"""Research-only inventory of editorial footnotes in the pinned Qushayri PDF.

This script deliberately does NOT build an application asset. It measures the
printed call/body relationship so production note extraction can be frozen only
after the source structure is understood.

Two independent note-body detectors are compared:
1. strict small-text lines (the original conservative detector);
2. numbered text blocks whose first meaningful English line begins with the same
   note number as a source call on that page.

The second detector exists because some genuine printed footnote blocks use a
number glyph or mixed line whose maximum text size exceeds the strict threshold.
It never promotes data to production: every mismatch remains explicit.
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
INLINE_CALL_RE = re.compile(r"(?<=[\.,;!?…\)\]”’\"])(\d{1,3})(?![\d:])")
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


def spans(line: dict) -> list[dict]:
    return [span for span in line.get("spans", []) if span.get("text", "").strip()]


def line_text(line: dict) -> str:
    return "".join(span.get("text", "") for span in line.get("spans", [])).replace("\u00a0", " ").strip()


def mostly_arabic(text: str) -> bool:
    chars = [char for char in text if char.isalpha()]
    return bool(chars) and sum(bool(ARABIC_RE.match(char)) for char in chars) / len(chars) > 0.45


def normalize_lines(lines: list[str]) -> str:
    value = " ".join(part.strip() for part in lines if part.strip())
    value = value.replace("\u00ad", "").replace("\u00a0", " ")
    value = re.sub(r"([A-Za-zÀ-ÖØ-öø-ÿ])-\s+([a-zà-öø-ÿ])", r"\1\2", value)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def inventory(pdf: Path) -> dict:
    actual_sha = sha256_file(pdf)
    if actual_sha != EXPECTED_SOURCE_SHA256:
        raise SystemExit(f"Qushayri source SHA-256 mismatch: {actual_sha}")

    doc = fitz.open(pdf)
    calls: list[dict] = []
    apparatus_by_page: dict[int, list[str]] = collections.defaultdict(list)
    numbered_blocks: list[dict] = []
    layout_lines_by_page: dict[int, list[dict]] = collections.defaultdict(list)

    for page_index in range(FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE):
        page_number = page_index + 1
        page_dict = doc[page_index].get_text("dict")
        for block_index, block in enumerate(page_dict.get("blocks", [])):
            if "lines" not in block:
                continue

            block_lines: list[str] = []
            for line_index, line in enumerate(block["lines"]):
                line_spans = spans(line)
                if not line_spans:
                    continue
                text = line_text(line)
                if not text or HEADER_RE.match(text) or mostly_arabic(text):
                    continue
                block_lines.append(text)

                sizes = [float(span.get("size", 0.0)) for span in line_spans]
                x0, y0, x1, y1 = [float(v) for v in line.get("bbox", (0, 0, 0, 0))]
                layout_lines_by_page[page_number].append(
                    {
                        "block_index": block_index,
                        "line_index": line_index,
                        "text": text,
                        "bbox": (x0, y0, x1, y1),
                        "sizes": sizes,
                    }
                )
                max_size = max(sizes)
                if max_size <= SMALL_TEXT_MAX:
                    apparatus_by_page[page_number].append(text)
                else:
                    for match in INLINE_CALL_RE.finditer(text):
                        calls.append(
                            {
                                "page": page_number,
                                "number": int(match.group(1)),
                                "call_text": text,
                                "call_offset": match.start(1),
                            }
                        )

            if block_lines:
                first = block_lines[0]
                start = NOTE_START_RE.match(first)
                if start:
                    numbered_blocks.append(
                        {
                            "page": page_number,
                            "number": int(start.group(1)),
                            "block_index": block_index,
                            "body": normalize_lines([first[start.end():], *block_lines[1:]]),
                            "first_line": first,
                        }
                    )

    calls_by_key: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for call in calls:
        calls_by_key[(call["page"], call["number"])].append(call)
    call_keys = set(calls_by_key)

    # Detector A: strict small-text apparatus.
    strict_notes: list[dict] = []
    strict_start_candidates = 0
    for page_number, lines in sorted(apparatus_by_page.items()):
        starts: list[tuple[int, int, int]] = []
        for index, text in enumerate(lines):
            match = NOTE_START_RE.match(text)
            if not match:
                continue
            strict_start_candidates += 1
            starts.append((index, int(match.group(1)), match.end()))

        for position, (start_index, number, prefix_end) in enumerate(starts):
            end_index = starts[position + 1][0] if position + 1 < len(starts) else len(lines)
            first = lines[start_index][prefix_end:]
            body = normalize_lines([first, *lines[start_index + 1 : end_index]])
            strict_notes.append({"page": page_number, "number": number, "body": body})

    strict_by_key: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for note in strict_notes:
        strict_by_key[(note["page"], note["number"])].append(note)
    strict_keys = set(strict_by_key)

    # Detector B: full text blocks. A block only becomes a pairing candidate if
    # its printed number is also a source call on that exact page.
    blocks_by_key: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for note in numbered_blocks:
        blocks_by_key[(note["page"], note["number"])].append(note)
    block_keys = set(blocks_by_key)

    # Detector C: layout-row starts. In some source pages the printed note
    # number is its own narrow text line/block, while the body begins in a
    # separate block on the same baseline. Detect only bottom-page, left-margin
    # standalone numbers that have one horizontally adjacent English text line.
    row_starts: list[dict] = []
    for page_number, records in sorted(layout_lines_by_page.items()):
        page = doc[page_number - 1]
        page_width = float(page.rect.width)
        page_height = float(page.rect.height)
        for record in records:
            text = record["text"].strip()
            if not re.fullmatch(r"\d{1,3}", text):
                continue
            number = int(text)
            x0, y0, x1, y1 = record["bbox"]
            if y0 < page_height * 0.55 or x0 > page_width * 0.30:
                continue
            cy = (y0 + y1) / 2.0
            neighbors = []
            for other in records:
                if other is record:
                    continue
                other_text = other["text"].strip()
                if (
                    not other_text
                    or re.fullmatch(r"\d{1,3}", other_text)
                    or HEADER_RE.match(other_text)
                    or mostly_arabic(other_text)
                ):
                    continue
                ox0, oy0, ox1, oy1 = other["bbox"]
                ocy = (oy0 + oy1) / 2.0
                if ox0 <= x1 or ox0 > page_width * 0.55:
                    continue
                if abs(ocy - cy) <= 5.0:
                    neighbors.append(other)
            if len(neighbors) == 1:
                body_start = neighbors[0]
                row_starts.append(
                    {
                        "page": page_number,
                        "number": number,
                        "number_bbox": [round(v, 2) for v in record["bbox"]],
                        "body_bbox": [round(v, 2) for v in body_start["bbox"]],
                        "body_start": body_start["text"],
                    }
                )

    rows_by_key: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for note in row_starts:
        rows_by_key[(note["page"], note["number"])].append(note)
    row_keys = set(rows_by_key)

    strict_paired = call_keys & strict_keys
    block_paired = call_keys & block_keys
    row_paired = call_keys & row_keys
    union_paired = strict_paired | block_paired | row_paired
    both_paired = strict_paired & block_paired

    duplicate_call_keys = sorted(key for key, value in calls_by_key.items() if len(value) != 1)
    duplicate_strict_note_keys = sorted(key for key, value in strict_by_key.items() if len(value) != 1)
    duplicate_block_note_keys = sorted(key for key, value in blocks_by_key.items() if len(value) != 1)

    samples = []
    for key in sorted(union_paired)[:80]:
        call = calls_by_key[key][0]
        strict_note = strict_by_key.get(key, [None])[0]
        block_note = blocks_by_key.get(key, [None])[0]
        row_note = rows_by_key.get(key, [None])[0]
        samples.append(
            {
                "page": key[0],
                "number": key[1],
                "call_text": call["call_text"],
                "strict_note_body": strict_note["body"] if strict_note else None,
                "block_note_body": block_note["body"] if block_note else None,
                "layout_row_body_start": row_note["body_start"] if row_note else None,
                "detectors": [
                    name
                    for name, present in (
                        ("strict_small_text", key in strict_paired),
                        ("numbered_block", key in block_paired),
                        ("layout_row", key in row_paired),
                    )
                    if present
                ],
            }
        )

    return {
        "schema": "quran-safeguard-qushayri-note-research-v5",
        "production_eligible": False,
        "source_sha256": actual_sha,
        "pdf_page_index_window": [FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE - 1],
        "candidate_call_occurrences": len(calls),
        "candidate_call_keys": len(call_keys),
        "strict_apparatus_numeric_start_candidates": strict_start_candidates,
        "strict_candidate_note_keys": len(strict_keys),
        "strict_matched_page_number_pairs": len(strict_paired),
        "numbered_block_start_candidates": len(numbered_blocks),
        "numbered_block_candidate_keys": len(block_keys),
        "numbered_block_matched_page_number_pairs": len(block_paired),
        "matched_by_both_original_detectors": len(both_paired),
        "layout_row_start_candidates": len(row_starts),
        "layout_row_candidate_keys": len(row_keys),
        "layout_row_matched_page_number_pairs": len(row_paired),
        "matched_by_any_detector": len(union_paired),
        "strict_only_pairs": [list(key) for key in sorted(strict_paired - block_paired)],
        "block_only_pairs": [list(key) for key in sorted(block_paired - strict_paired)],
        "layout_row_only_pairs": [list(key) for key in sorted(row_paired - strict_paired - block_paired)],
        "still_unpaired_call_keys": [list(key) for key in sorted(call_keys - union_paired)],
        "duplicate_call_keys": [list(key) for key in duplicate_call_keys],
        "duplicate_strict_note_keys": [list(key) for key in duplicate_strict_note_keys],
        "duplicate_block_note_keys": [list(key) for key in duplicate_block_note_keys],
        "duplicate_layout_row_keys": [
            list(key) for key, value in sorted(rows_by_key.items()) if len(value) != 1
        ],
        "strict_uncalled_note_keys": [list(key) for key in sorted(strict_keys - call_keys)],
        "block_uncalled_note_keys": [list(key) for key in sorted(block_keys - call_keys)],
        "layout_row_uncalled_note_keys": [list(key) for key in sorted(row_keys - call_keys)],
        "layout_row_samples": row_starts[:80],
        "sample_pairs": samples,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    report = inventory(args.pdf)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({key: value for key, value in report.items() if key != "sample_pairs"}, ensure_ascii=False, indent=2))
    print(f"Research-only report written to {args.report}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Research-only inventory of editorial footnotes in the pinned Qushayri PDF.

This script deliberately does NOT build an application asset. It measures the
printed call/body relationship so production note extraction can be frozen only
after the source structure is understood. A candidate call is a small numeric
span embedded inside an otherwise normal commentary line. A candidate note
body begins in the page's small-text apparatus with the same number.

The report is intentionally fail-open for research ambiguity: it records
unpaired/duplicate candidates rather than guessing a correspondence.
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


def normalize_small_lines(lines: list[str]) -> str:
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
    digit_sizes = collections.Counter()

    for page_index in range(FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE):
        page_number = page_index + 1
        page_dict = doc[page_index].get_text("dict")
        for block in page_dict.get("blocks", []):
            if "lines" not in block:
                continue
            for line in block["lines"]:
                line_spans = spans(line)
                if not line_spans:
                    continue
                text = line_text(line)
                if not text or HEADER_RE.match(text) or mostly_arabic(text):
                    continue
                sizes = [float(span.get("size", 0.0)) for span in line_spans]
                max_size = max(sizes)

                if max_size <= SMALL_TEXT_MAX:
                    apparatus_by_page[page_number].append(text)
                    continue

                # A true printed footnote call is a smaller numeric span inside
                # a normal-size prose line. We do not accept ordinary-size numbers.
                for span in line_spans:
                    token = span.get("text", "").strip()
                    size = float(span.get("size", 0.0))
                    if token.isdigit() and size <= SMALL_TEXT_MAX and size < max_size:
                        number = int(token)
                        digit_sizes[f"{size:.2f}"] += 1
                        calls.append(
                            {
                                "page": page_number,
                                "number": number,
                                "call_text": text,
                                "call_font": span.get("font", ""),
                                "call_size": round(size, 3),
                            }
                        )

    calls_by_page_number: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for call in calls:
        calls_by_page_number[(call["page"], call["number"])].append(call)

    notes: list[dict] = []
    apparatus_start_candidates = 0
    for page_number, lines in sorted(apparatus_by_page.items()):
        starts: list[tuple[int, int, int]] = []
        for index, text in enumerate(lines):
            match = NOTE_START_RE.match(text)
            if not match:
                continue
            apparatus_start_candidates += 1
            number = int(match.group(1))
            if (page_number, number) in calls_by_page_number:
                starts.append((index, number, match.end()))

        for position, (start_index, number, prefix_end) in enumerate(starts):
            end_index = starts[position + 1][0] if position + 1 < len(starts) else len(lines)
            first = lines[start_index][prefix_end:]
            body = normalize_small_lines([first, *lines[start_index + 1 : end_index]])
            notes.append(
                {
                    "page": page_number,
                    "number": number,
                    "body": body,
                }
            )

    notes_by_page_number: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for note in notes:
        notes_by_page_number[(note["page"], note["number"])].append(note)

    call_keys = set(calls_by_page_number)
    note_keys = set(notes_by_page_number)
    paired_keys = sorted(call_keys & note_keys)
    duplicate_call_keys = sorted(key for key, value in calls_by_page_number.items() if len(value) != 1)
    duplicate_note_keys = sorted(key for key, value in notes_by_page_number.items() if len(value) != 1)
    unpaired_calls = sorted(call_keys - note_keys)
    uncalled_notes = sorted(note_keys - call_keys)

    pair_samples = []
    for key in paired_keys[:40]:
        call = calls_by_page_number[key][0]
        note = notes_by_page_number[key][0]
        pair_samples.append(
            {
                "page": key[0],
                "number": key[1],
                "call_text": call["call_text"],
                "note_body": note["body"],
            }
        )

    return {
        "schema": "quran-safeguard-qushayri-note-research-v1",
        "production_eligible": False,
        "source_sha256": actual_sha,
        "pdf_page_index_window": [
            FIRST_COMMENTARY_PAGE_INDEX,
            LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE - 1,
        ],
        "candidate_call_occurrences": len(calls),
        "candidate_call_keys": len(call_keys),
        "matched_page_number_pairs": len(paired_keys),
        "apparatus_numeric_start_candidates": apparatus_start_candidates,
        "duplicate_call_keys": [list(key) for key in duplicate_call_keys],
        "duplicate_note_keys": [list(key) for key in duplicate_note_keys],
        "unpaired_call_keys": [list(key) for key in unpaired_calls],
        "uncalled_note_keys": [list(key) for key in uncalled_notes],
        "candidate_call_size_distribution": dict(sorted(digit_sizes.items())),
        "sample_pairs": pair_samples,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    report = inventory(args.pdf)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps({key: value for key, value in report.items() if key != "sample_pairs"}, ensure_ascii=False, indent=2))
    print(f"Research-only report written to {args.report}")


if __name__ == "__main__":
    main()

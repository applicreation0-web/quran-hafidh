#!/usr/bin/env python3
"""Research-only inventory of editorial footnotes in the pinned Qushayri PDF.

This script deliberately does NOT build an application asset. It measures the
printed call/body relationship so production note extraction can be frozen only
after the source structure is understood.

The PDF text layer flattens superscript calls into the surrounding prose, e.g.
`imām)19`, `servant,”20`, `implication.21`. We therefore recognize only a
1–3 digit token glued immediately after punctuation typical of a printed
footnote call, then require a same-page small-text note body with the identical
number. Ordinary chapter:verse notation is excluded by construction.

The report remains research-only and records every ambiguity instead of guessing.
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
# Footnote calls in this edition are printed after punctuation and are flattened
# into the prose text layer. Colon is deliberately absent, so Qur'an 2:255 is
# never considered a footnote call.
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

                for match in INLINE_CALL_RE.finditer(text):
                    calls.append(
                        {
                            "page": page_number,
                            "number": int(match.group(1)),
                            "call_text": text,
                            "call_offset": match.start(1),
                        }
                    )

    calls_by_page_number: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for call in calls:
        calls_by_page_number[(call["page"], call["number"])].append(call)

    # Build every small-text numbered note candidate independently from calls.
    # This makes uncalled apparatus entries visible in the report.
    notes: list[dict] = []
    apparatus_start_candidates = 0
    for page_number, lines in sorted(apparatus_by_page.items()):
        starts: list[tuple[int, int, int]] = []
        for index, text in enumerate(lines):
            match = NOTE_START_RE.match(text)
            if not match:
                continue
            apparatus_start_candidates += 1
            starts.append((index, int(match.group(1)), match.end()))

        for position, (start_index, number, prefix_end) in enumerate(starts):
            end_index = starts[position + 1][0] if position + 1 < len(starts) else len(lines)
            first = lines[start_index][prefix_end:]
            body = normalize_small_lines([first, *lines[start_index + 1 : end_index]])
            notes.append({"page": page_number, "number": number, "body": body})

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
    for key in paired_keys[:60]:
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
        "schema": "quran-safeguard-qushayri-note-research-v2",
        "production_eligible": False,
        "source_sha256": actual_sha,
        "pdf_page_index_window": [FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE - 1],
        "candidate_call_occurrences": len(calls),
        "candidate_call_keys": len(call_keys),
        "apparatus_numeric_start_candidates": apparatus_start_candidates,
        "candidate_note_keys": len(note_keys),
        "matched_page_number_pairs": len(paired_keys),
        "duplicate_call_keys": [list(key) for key in duplicate_call_keys],
        "duplicate_note_keys": [list(key) for key in duplicate_note_keys],
        "unpaired_call_keys": [list(key) for key in unpaired_calls],
        "uncalled_note_keys": [list(key) for key in uncalled_notes],
        "sample_pairs": pair_samples,
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

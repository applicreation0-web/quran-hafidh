#!/usr/bin/env python3
"""Independent, research-only reverse cross-check of Qushayri footnote calls.

PyMuPDF flattens many printed superscript calls into their surrounding text, so
span size alone cannot enumerate them reliably. This script therefore starts
from independently detected note bodies, finds the top edge of the printed
footnote apparatus on each page, and searches only the main commentary area for
a same-page call with the same note number.

The primary detector accepts calls attached to a word (for example ``not25``)
as well as calls after punctuation. A second, deliberately narrow geometry
fallback handles a note number isolated in its own superscript span. It is used
only for note-body keys that the primary detector did not match, and only when
there is exactly one raised/smaller numeric span candidate on that page.
Decimals, percentages and Qur'an-style chapter:verse numbers remain excluded.
Every result remains research-only and is never promoted to an application
asset by this script.
"""
from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
import statistics
from pathlib import Path

import fitz

EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
FIRST_COMMENTARY_PAGE_INDEX = 37
LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE = 506
SMALL_TEXT_MAX = 9.4
NOTE_START_RE = re.compile(r"^(\d{1,3})[\s\u2009\u200a]+")
# Printed calls are glued to the preceding prose after PDF text flattening.
# - require an immediately preceding Latin letter or punctuation;
# - reject the second digit of a decimal such as 2.5;
# - reject continuations, Qur'an references and percentages.
ATTACHED_CALL_RE = re.compile(
    r"(?<!\d\.)(?<=[A-Za-zÀ-ÖØ-öø-ÿ\.,;!?…\)\]”’\"])(\d{1,3})(?![\d:%])"
)
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
    return [s for s in line.get("spans", []) if s.get("text", "").strip()]


def line_text(line: dict) -> str:
    return "".join(s.get("text", "") for s in line.get("spans", [])).replace("\u00a0", " ").strip()


def mostly_arabic(text: str) -> bool:
    letters = [c for c in text if c.isalpha()]
    return bool(letters) and sum(bool(ARABIC_RE.match(c)) for c in letters) / len(letters) > 0.45


def span_snapshot(span: dict) -> dict:
    return {
        "text": span.get("text", ""),
        "size": round(float(span.get("size", 0.0)), 2),
        "bbox": [round(float(v), 2) for v in span.get("bbox", (0, 0, 0, 0))],
        "font": span.get("font", ""),
    }


def isolated_superscript_candidates(record: dict, number: int) -> list[dict]:
    """Return only visually credible isolated numeric superscripts for one key.

    This is intentionally stricter than a text regex. The candidate must be a
    span containing exactly the desired number and must be either materially
    smaller than neighboring prose or visibly raised relative to the prose
    baseline. Ordinary verse/page numbers therefore do not qualify.
    """
    wanted = str(number)
    raw_spans = record.get("spans", [])
    if len(raw_spans) < 2:
        return []

    prose = [
        s for s in raw_spans
        if s.get("text", "").strip()
        and not re.fullmatch(r"\d{1,3}", s.get("text", "").strip())
    ]
    if not prose:
        return []

    prose_sizes = [float(s.get("size", 0.0)) for s in prose if float(s.get("size", 0.0)) > 0]
    prose_bottoms = [float(s.get("bbox", (0, 0, 0, 0))[3]) for s in prose]
    if not prose_sizes or not prose_bottoms:
        return []
    prose_size = statistics.median(prose_sizes)
    prose_bottom = statistics.median(prose_bottoms)

    found = []
    for span in raw_spans:
        text = span.get("text", "")
        if text.strip() != wanted:
            continue
        size = float(span.get("size", 0.0))
        bbox = span.get("bbox", (0, 0, 0, 0))
        bottom = float(bbox[3])
        smaller = size > 0 and size <= prose_size * 0.86
        raised = bottom <= prose_bottom - 1.2
        if not (smaller or raised):
            continue
        found.append(
            {
                "page": record["page"],
                "number": number,
                "text": record["text"],
                "offset": None,
                "bbox": [round(float(v), 2) for v in record["bbox"]],
                "span": span_snapshot(span),
                "evidence": "isolated-superscript-span",
            }
        )
    return found


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pdf", type=Path)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    source_sha = sha256_file(args.pdf)
    if source_sha != EXPECTED_SOURCE_SHA256:
        raise SystemExit(f"Qushayri source SHA-256 mismatch: {source_sha}")

    doc = fitz.open(args.pdf)
    page_records: dict[int, list[dict]] = collections.defaultdict(list)
    strict_note_starts: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    layout_note_starts: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)

    for page_index in range(FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE):
        page_number = page_index + 1
        page_dict = doc[page_index].get_text("dict")
        for block_index, block in enumerate(page_dict.get("blocks", [])):
            for line_index, line in enumerate(block.get("lines", [])):
                line_spans = spans(line)
                if not line_spans:
                    continue
                text = line_text(line)
                if not text or HEADER_RE.match(text) or mostly_arabic(text):
                    continue
                bbox = tuple(float(v) for v in line.get("bbox", (0, 0, 0, 0)))
                sizes = [float(s.get("size", 0.0)) for s in line_spans]
                record = {
                    "page": page_number,
                    "text": text,
                    "bbox": bbox,
                    "max_size": max(sizes),
                    "block": block_index,
                    "line": line_index,
                    "spans": line_spans,
                }
                page_records[page_number].append(record)

                if record["max_size"] <= SMALL_TEXT_MAX:
                    match = NOTE_START_RE.match(text)
                    if match:
                        strict_note_starts[(page_number, int(match.group(1)))].append(record)

    # Independent layout detector for source pages where the note number and its
    # first body line are split into adjacent blocks.
    for page_number, records in sorted(page_records.items()):
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
                if (
                    not other_text
                    or re.fullmatch(r"\d{1,3}", other_text)
                    or HEADER_RE.match(other_text)
                    or mostly_arabic(other_text)
                ):
                    continue
                ox0, oy0, ox1, oy1 = other["bbox"]
                ocy = (oy0 + oy1) / 2.0
                if ox0 > x1 and ox0 <= width * 0.55 and abs(ocy - cy) <= 5.0:
                    neighbors.append(other)
            if len(neighbors) == 1:
                layout_note_starts[(page_number, int(text))].append(
                    {"number": record, "body": neighbors[0]}
                )

    strict_keys = set(strict_note_starts)
    layout_keys = set(layout_note_starts)
    body_keys = strict_keys | layout_keys

    # Establish the top edge of the actual footnote apparatus from note starts.
    apparatus_top: dict[int, float] = {}
    for page_number, number in body_keys:
        y_candidates: list[float] = []
        for record in strict_note_starts.get((page_number, number), []):
            y_candidates.append(float(record["bbox"][1]))
        for pair in layout_note_starts.get((page_number, number), []):
            y_candidates.append(float(pair["number"]["bbox"][1]))
            y_candidates.append(float(pair["body"]["bbox"][1]))
        if y_candidates:
            current = apparatus_top.get(page_number)
            found = min(y_candidates)
            apparatus_top[page_number] = found if current is None else min(current, found)

    attached_calls: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    all_attached_candidates: list[dict] = []
    for page_number, records in sorted(page_records.items()):
        top = apparatus_top.get(page_number)
        if top is None:
            continue
        for record in records:
            # Search only the commentary region above the first printed note.
            if float(record["bbox"][3]) >= top - 1.0:
                continue
            text = record["text"]
            for match in ATTACHED_CALL_RE.finditer(text):
                number = int(match.group(1))
                candidate = {
                    "page": page_number,
                    "number": number,
                    "text": text,
                    "offset": match.start(1),
                    "bbox": [round(float(v), 2) for v in record["bbox"]],
                    "evidence": "attached-text-call",
                }
                all_attached_candidates.append(candidate)
                key = (page_number, number)
                if key in body_keys:
                    attached_calls[key].append(candidate)

    primary_call_keys = set(attached_calls)
    primary_missing = body_keys - primary_call_keys

    # Narrow geometry fallback: only unresolved note-body keys are considered,
    # and a key is accepted only when there is exactly one credible isolated
    # superscript span above the apparatus. Ambiguous pages stay unresolved.
    geometry_calls: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    for page_number, number in sorted(primary_missing):
        top = apparatus_top.get(page_number)
        if top is None:
            continue
        candidates = []
        for record in page_records[page_number]:
            if float(record["bbox"][3]) >= top - 1.0:
                continue
            candidates.extend(isolated_superscript_candidates(record, number))
        if len(candidates) == 1:
            geometry_calls[(page_number, number)] = candidates
            attached_calls[(page_number, number)] = candidates

    call_keys = set(attached_calls)
    missing = body_keys - call_keys
    extra_candidate_keys = {
        (item["page"], item["number"]) for item in all_attached_candidates
    } - body_keys

    unmatched_diagnostics = []
    for page_number, number in sorted(missing):
        top = apparatus_top.get(page_number)
        note_records = []
        for record in strict_note_starts.get((page_number, number), []):
            note_records.append(
                {
                    "kind": "strict-body",
                    "text": record["text"],
                    "bbox": [round(float(v), 2) for v in record["bbox"]],
                }
            )
        for pair in layout_note_starts.get((page_number, number), []):
            note_records.append(
                {
                    "kind": "layout-body",
                    "number_bbox": [round(float(v), 2) for v in pair["number"]["bbox"]],
                    "body_text": pair["body"]["text"],
                    "body_bbox": [round(float(v), 2) for v in pair["body"]["bbox"]],
                }
            )
        numeric_context = []
        for record in page_records[page_number]:
            if top is not None and float(record["bbox"][3]) >= top - 1.0:
                continue
            if str(number) not in record["text"]:
                continue
            numeric_context.append(
                {
                    "text": record["text"],
                    "bbox": [round(float(v), 2) for v in record["bbox"]],
                    "spans": [span_snapshot(s) for s in record.get("spans", [])],
                }
            )
        unmatched_diagnostics.append(
            {
                "page": page_number,
                "number": number,
                "note_bodies": note_records,
                "main_text_candidates_containing_number": numeric_context[:20],
            }
        )

    report = {
        "schema": "quran-safeguard-qushayri-note-reverse-crosscheck-v3",
        "production_eligible": False,
        "source_sha256": source_sha,
        "strict_note_body_keys": len(strict_keys),
        "layout_note_body_keys": len(layout_keys),
        "note_body_union_keys": len(body_keys),
        "matched_note_body_call_keys": len(call_keys),
        "matched_by_primary_attached_detector": len(primary_call_keys),
        "matched_by_geometry_fallback": len(geometry_calls),
        "unmatched_note_body_keys": [list(k) for k in sorted(missing)],
        "duplicate_matched_call_keys": [
            list(k) for k, values in sorted(attached_calls.items()) if len(values) != 1
        ],
        "matched_by_strict_body_detector": len(call_keys & strict_keys),
        "matched_by_layout_body_detector": len(call_keys & layout_keys),
        "attached_candidates_without_same_page_note_body": len(extra_candidate_keys),
        "candidate_keys_without_body_sample": [list(k) for k in sorted(extra_candidate_keys)[:100]],
        "geometry_fallback_matches": [
            {"page": k[0], "number": k[1], **geometry_calls[k][0]}
            for k in sorted(geometry_calls)
        ],
        "unmatched_diagnostics": unmatched_diagnostics,
        "matched_samples": [
            {"page": k[0], "number": k[1], **attached_calls[k][0]}
            for k in sorted(call_keys)[:120]
        ],
    }

    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(
        json.dumps(
            {
                k: v
                for k, v in report.items()
                if k not in {"matched_samples", "unmatched_diagnostics"}
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    if unmatched_diagnostics:
        print("Unmatched diagnostics:")
        print(json.dumps(unmatched_diagnostics, ensure_ascii=False, indent=2))
    print(f"Research-only report written to {args.report}")


if __name__ == "__main__":
    main()

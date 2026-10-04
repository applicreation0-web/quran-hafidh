#!/usr/bin/env python3
"""Verified Qushayri 0.10.6 presentation semantics.

Extends the pinned-source 0.10.6 semantic inventory with one crucial Poetry
Index rule: when two independently indexed poems begin on the same source page,
the first poem stops at the next indexed incipit. This prevents one indexed
poem from swallowing a following independently indexed poem and gives a unique,
source-authoritative poetry-line map.
"""
from __future__ import annotations

import collections

import qushayri_source_semantics_0106 as base

EXPECTED_UNIQUE_POETRY_LINES = 542


def _build_verified_poetry_lines(doc):
    entries = base._index_entries(doc)
    occurrences = []

    for incipit, printed_pages, raw_entry in entries:
        for printed_page in printed_pages:
            page_number = printed_page + base.PRINTED_TO_PDF_OFFSET
            if not (1 <= page_number <= len(doc)):
                raise RuntimeError(
                    f"Qushayri Poetry Index page out of range: {incipit!r} -> {page_number}"
                )
            records = base._page_records(doc, page_number)
            scored = [
                (base._similarity(record["text"], incipit), index)
                for index, record in enumerate(records)
            ]
            score, start_index = max(scored, default=(0.0, -1))
            if start_index < 0 or score < 0.55:
                raise RuntimeError(
                    "Qushayri Poetry Index incipit not found on source page: "
                    f"{incipit!r} printed={printed_page} pdf={page_number} score={score:.3f}"
                )
            occurrences.append(
                {
                    "incipit": incipit,
                    "printed_page": printed_page,
                    "page": page_number,
                    "records": records,
                    "start": start_index,
                    "score": score,
                }
            )

    if len(occurrences) != base.EXPECTED_POETRY_OCCURRENCES:
        raise RuntimeError(
            "Qushayri Poetry Index occurrence count changed: "
            f"{len(occurrences)} != {base.EXPECTED_POETRY_OCCURRENCES}"
        )

    by_page = collections.defaultdict(list)
    for occurrence in occurrences:
        by_page[occurrence["page"]].append(occurrence)

    poetry = {}
    for page_number, page_occurrences in sorted(by_page.items()):
        starts = sorted({item["start"] for item in page_occurrences})
        for occurrence in sorted(page_occurrences, key=lambda item: item["start"]):
            records = occurrence["records"]
            start_index = occurrence["start"]
            later_starts = [value for value in starts if value > start_index]
            next_indexed_start = min(later_starts) if later_starts else len(records)

            poem_records = []
            index = start_index
            while index < min(len(records), next_indexed_start):
                record = records[index]
                if (
                    record["x0"] < base.POETRY_X_MIN
                    or record["max_size"] < base.POETRY_BODY_SIZE_MIN
                    or base._mostly_arabic(record["text"])
                    or (poem_records and record["y0"] < poem_records[-1]["y0"] - 2.0)
                ):
                    break
                poem_records.append(record)
                index += 1

            if len(poem_records) < 2:
                raise RuntimeError(
                    "Qushayri indexed poetry occurrence has <2 source lines: "
                    f"{occurrence['incipit']!r} on printed page {occurrence['printed_page']}"
                )

            poem_id = (
                f"{occurrence['printed_page']}:"
                f"{base._norm_index_text(occurrence['incipit'])}"
            )
            previous = None
            for line_index, record in enumerate(poem_records):
                key = (page_number, record["block"], record["line"])
                stanza = False
                if previous is not None:
                    stanza = (
                        record["y0"] - previous["y0"]
                    ) > base.POETRY_STANZA_GAP_MIN
                value = base.PoetryLine(
                    page=page_number,
                    block=record["block"],
                    line=record["line"],
                    poem_id=poem_id,
                    line_index=line_index,
                    stanza_break_before=stanza,
                )
                existing = poetry.get(key)
                if existing is not None:
                    raise RuntimeError(
                        "Qushayri Poetry Index still overlaps after next-incipit boundary: "
                        f"{key}: {existing} vs {value}"
                    )
                poetry[key] = value
                previous = record

    if len(poetry) != EXPECTED_UNIQUE_POETRY_LINES:
        raise RuntimeError(
            "Qushayri unique indexed poetry-line count changed: "
            f"{len(poetry)} != {EXPECTED_UNIQUE_POETRY_LINES}"
        )
    return poetry, base.EXPECTED_POETRY_INDEX_ENTRIES, len(occurrences)


def build_qushayri_source_semantics(doc):
    note_calls = base._build_note_calls(doc)
    poetry, entries, occurrences = _build_verified_poetry_lines(doc)
    note_total = sum(len(values) for values in note_calls.values())
    if note_total != base.EXPECTED_NOTE_RELATIONS:
        raise RuntimeError(
            f"Qushayri verified note-call line map changed: "
            f"{note_total} != {base.EXPECTED_NOTE_RELATIONS}"
        )
    return base.QushayriSourceSemantics(
        note_calls_by_line=note_calls,
        poetry_lines=poetry,
        poetry_entry_count=entries,
        poetry_occurrence_count=occurrences,
        poetry_line_count=len(poetry),
        verified_note_relation_count=note_total,
    )

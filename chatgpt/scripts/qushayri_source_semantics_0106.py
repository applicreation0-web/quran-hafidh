#!/usr/bin/env python3
"""Pinned-source presentation semantics for Qushayri 0.10.6.

This module does not interpret al-Qushayri. It derives two presentation-only
facts from the approved English PDF:
- source footnote calls that are proven by a same-page printed note body;
- poetry lines whose incipits are listed in the book's own Poetry Index.

Everything fails closed on the pinned source and frozen inventory counts.
"""
from __future__ import annotations

import collections
import re
from dataclasses import dataclass

SOFT_HYPHEN_MARKER = "__QSH_SOFT_HYPHEN__"
EXPECTED_SOURCE_SHA256 = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
EXPECTED_NOTE_RELATIONS = 928
EXPECTED_PRIMARY_NOTE_RELATIONS = 917
EXPECTED_POETRY_INDEX_ENTRIES = 121
EXPECTED_POETRY_OCCURRENCES = 126
EXPECTED_POETRY_LINES = 546
POETRY_INDEX_PDF_PAGES = range(517, 521)
PRINTED_TO_PDF_OFFSET = 32
FIRST_COMMENTARY_PAGE_INDEX = 37
LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE = 506
SMALL_TEXT_MAX = 9.4
POETRY_X_MIN = 80.0
POETRY_BODY_SIZE_MIN = 9.8
POETRY_STANZA_GAP_MIN = 15.0

NOTE_START_RE = re.compile(r"^(\d{1,3})[\s\u2009\u200a]+")
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

EXPECTED_SPECIAL_NOTE_RULES = {
    (61, 30): "honorific",
    (87, 90): "honorific",
    (170, 246): "colon",
    (241, 374): "spaced-punctuation",
    (339, 133): "spaced-punctuation",
    (344, 149): "honorific",
    (349, 167): "honorific",
    (354, 176): "honorific",
    (366, 191): "spaced-punctuation",
    (370, 201): "honorific",
    (448, 72): "honorific",
}
EXPECTED_STANDALONE_NUMERIC_CANDIDATES = {(173, 101), (173, 117)}


@dataclass(frozen=True)
class VerifiedNoteCall:
    page: int
    block: int
    line: int
    number: int
    start: int
    end: int
    rule: str


@dataclass(frozen=True)
class PoetryLine:
    page: int
    block: int
    line: int
    poem_id: str
    line_index: int
    stanza_break_before: bool


@dataclass
class QushayriSourceSemantics:
    note_calls_by_line: dict[tuple[int, int, int], tuple[VerifiedNoteCall, ...]]
    poetry_lines: dict[tuple[int, int, int], PoetryLine]
    poetry_entry_count: int
    poetry_occurrence_count: int
    poetry_line_count: int
    verified_note_relation_count: int
    stripped_note_calls: int = 0

    def strip_verified_note_calls(self, page: int, block: int, line: int, text: str) -> str:
        calls = self.note_calls_by_line.get((page, block, line), ())
        if not calls:
            return text
        value = text
        for call in sorted(calls, key=lambda item: item.start, reverse=True):
            if value[call.start:call.end] != str(call.number):
                raise RuntimeError(
                    "Qushayri verified note call drifted before removal: "
                    f"page={page} block={block} line={line} number={call.number} "
                    f"slice={value[call.start:call.end]!r}"
                )
            value = value[:call.start] + value[call.end:]
            self.stripped_note_calls += 1
        return value

    def poetry_line(self, page: int, block: int, line: int) -> PoetryLine | None:
        return self.poetry_lines.get((page, block, line))

    def assert_complete_note_stripping(self) -> None:
        if self.stripped_note_calls != self.verified_note_relation_count:
            raise RuntimeError(
                "Qushayri verified source note-call removal incomplete: "
                f"{self.stripped_note_calls} != {self.verified_note_relation_count}"
            )


def _span_text(span: dict) -> str:
    return str(span.get("text", "")).replace("\u00ad", SOFT_HYPHEN_MARKER)


def _line_record(page_number: int, block_index: int, line_index: int, line: dict) -> dict | None:
    spans = [span for span in line.get("spans", []) if str(span.get("text", ""))]
    if not spans:
        return None
    text = "".join(_span_text(span) for span in spans)
    if not text.strip():
        return None
    bbox = tuple(float(v) for v in line.get("bbox", (0, 0, 0, 0)))
    nonblank = [span for span in spans if str(span.get("text", "")).strip()]
    sizes = [float(span.get("size", 0.0)) for span in nonblank]
    return {
        "page": page_number,
        "block": block_index,
        "line": line_index,
        "text": text.strip(),
        "raw_text": text,
        "bbox": bbox,
        "x0": bbox[0],
        "y0": bbox[1],
        "y1": bbox[3],
        "max_size": max(sizes) if sizes else 0.0,
        "spans": spans,
    }


def _page_records(doc, page_number: int) -> list[dict]:
    page = doc[page_number - 1]
    records: list[dict] = []
    for block_index, block in enumerate(page.get_text("dict").get("blocks", [])):
        for line_index, line in enumerate(block.get("lines", [])):
            record = _line_record(page_number, block_index, line_index, line)
            if record is not None:
                records.append(record)
    return records


def _mostly_arabic(text: str) -> bool:
    letters = [c for c in text if c.isalpha()]
    return bool(letters) and sum(bool(ARABIC_RE.match(c)) for c in letters) / len(letters) > 0.45


def _starts_with_number(text: str, number: int) -> tuple[int, int] | None:
    leading = len(text) - len(text.lstrip(" \t\u00a0"))
    stripped = text[leading:]
    wanted = str(number)
    if not stripped.startswith(wanted):
        return None
    tail = stripped[len(wanted):len(wanted) + 1]
    if tail and tail in "0123456789:%":
        return None
    return leading, leading + len(wanted)


def _special_call_span(record: dict, number: int, rule: str) -> tuple[int, int] | None:
    text = record["raw_text"]
    if rule == "colon":
        pattern = re.compile(rf"(?<!\d):\s*({number})(?![\d:%])")
        matches = list(pattern.finditer(text))
        if len(matches) == 1:
            return matches[0].start(1), matches[0].end(1)
        return None
    if rule == "spaced-punctuation":
        pattern = re.compile(rf"[\.\?\!\)\]”’\"]\s+({number})(?![\d:%])")
        matches = list(pattern.finditer(text))
        if len(matches) == 1:
            return matches[0].start(1), matches[0].end(1)
        return None
    if rule == "honorific":
        spans = record["spans"]
        cursor = 0
        found: list[tuple[int, int]] = []
        for index, span in enumerate(spans):
            transformed = _span_text(span)
            if index < len(spans) - 1 and span.get("font") == "Honorifics":
                raw = str(span.get("text", ""))
                if any("\ue000" <= ch <= "\uf8ff" for ch in raw):
                    next_text = _span_text(spans[index + 1])
                    local = _starts_with_number(next_text, number)
                    if local is not None:
                        next_start = cursor + len(transformed)
                        found.append((next_start + local[0], next_start + local[1]))
            cursor += len(transformed)
        if len(found) == 1:
            return found[0]
        return None
    raise RuntimeError(f"Unknown Qushayri note-call verification rule: {rule}")


def _build_note_calls(doc) -> dict[tuple[int, int, int], tuple[VerifiedNoteCall, ...]]:
    page_records: dict[int, list[dict]] = collections.defaultdict(list)
    strict_note_starts: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)
    layout_note_starts: dict[tuple[int, int], list[dict]] = collections.defaultdict(list)

    for page_index in range(FIRST_COMMENTARY_PAGE_INDEX, LAST_COMMENTARY_PAGE_INDEX_EXCLUSIVE):
        page_number = page_index + 1
        records = _page_records(doc, page_number)
        page_records[page_number] = records
        for record in records:
            text = record["text"].replace("\u00a0", " ")
            if HEADER_RE.match(text) or _mostly_arabic(text):
                continue
            if record["max_size"] <= SMALL_TEXT_MAX:
                match = NOTE_START_RE.match(text)
                if match:
                    strict_note_starts[(page_number, int(match.group(1)))].append(record)

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
                    or _mostly_arabic(other_text)
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

    body_keys = set(strict_note_starts) | set(layout_note_starts)
    if len(body_keys) != EXPECTED_NOTE_RELATIONS:
        raise RuntimeError(
            f"Qushayri note-body inventory changed: {len(body_keys)} != {EXPECTED_NOTE_RELATIONS}"
        )

    apparatus_top: dict[int, float] = {}
    for page_number, number in body_keys:
        ys = [float(r["bbox"][1]) for r in strict_note_starts.get((page_number, number), [])]
        for pair in layout_note_starts.get((page_number, number), []):
            ys.extend([float(pair["number"]["bbox"][1]), float(pair["body"]["bbox"][1])])
        if ys:
            found = min(ys)
            apparatus_top[page_number] = min(apparatus_top.get(page_number, found), found)

    primary: dict[tuple[int, int], list[VerifiedNoteCall]] = collections.defaultdict(list)
    all_candidate_keys: set[tuple[int, int]] = set()
    for page_number, records in sorted(page_records.items()):
        top = apparatus_top.get(page_number)
        if top is None:
            continue
        for record in records:
            if float(record["bbox"][3]) >= top - 1.0:
                continue
            detection_text = record["raw_text"].replace("\u00a0", " ")
            for match in ATTACHED_CALL_RE.finditer(detection_text):
                number = int(match.group(1))
                key = (page_number, number)
                all_candidate_keys.add(key)
                if key in body_keys:
                    primary[key].append(
                        VerifiedNoteCall(
                            page=page_number,
                            block=record["block"],
                            line=record["line"],
                            number=number,
                            start=match.start(1),
                            end=match.end(1),
                            rule="attached",
                        )
                    )

    duplicate = {key: values for key, values in primary.items() if len(values) != 1}
    if duplicate:
        raise RuntimeError(f"Qushayri duplicate primary note calls: {sorted(duplicate)[:20]}")
    primary_keys = set(primary)
    if len(primary_keys) != EXPECTED_PRIMARY_NOTE_RELATIONS:
        raise RuntimeError(
            "Qushayri primary source note-call inventory changed: "
            f"{len(primary_keys)} != {EXPECTED_PRIMARY_NOTE_RELATIONS}"
        )
    unresolved = body_keys - primary_keys
    if unresolved != set(EXPECTED_SPECIAL_NOTE_RULES):
        raise RuntimeError(
            "Qushayri unresolved note-call key set changed: "
            f"{sorted(unresolved)}"
        )
    extras = all_candidate_keys - body_keys
    if extras != EXPECTED_STANDALONE_NUMERIC_CANDIDATES:
        raise RuntimeError(
            "Qushayri standalone numeric candidate set changed: "
            f"{sorted(extras)}"
        )

    resolved: list[VerifiedNoteCall] = [values[0] for values in primary.values()]
    for key in sorted(unresolved):
        page_number, number = key
        rule = EXPECTED_SPECIAL_NOTE_RULES[key]
        top = apparatus_top[page_number]
        candidates: list[VerifiedNoteCall] = []
        for record in page_records[page_number]:
            if float(record["bbox"][3]) >= top - 1.0:
                continue
            span = _special_call_span(record, number, rule)
            if span is None:
                continue
            candidates.append(
                VerifiedNoteCall(
                    page=page_number,
                    block=record["block"],
                    line=record["line"],
                    number=number,
                    start=span[0],
                    end=span[1],
                    rule=rule,
                )
            )
        if len(candidates) != 1:
            raise RuntimeError(
                f"Qushayri special note call {page_number}:{number} ({rule}) "
                f"resolved to {len(candidates)} candidates"
            )
        resolved.append(candidates[0])

    if len(resolved) != EXPECTED_NOTE_RELATIONS:
        raise RuntimeError(
            f"Qushayri verified note-call total changed: {len(resolved)} != {EXPECTED_NOTE_RELATIONS}"
        )
    by_line: dict[tuple[int, int, int], list[VerifiedNoteCall]] = collections.defaultdict(list)
    for call in resolved:
        by_line[(call.page, call.block, call.line)].append(call)
    return {key: tuple(sorted(values, key=lambda c: c.start)) for key, values in by_line.items()}


def _norm_index_text(value: str) -> str:
    value = value.replace("\u00a0", " ").replace("–", "-").replace("—", "-")
    value = re.sub(r"[“”‘’]", "'", value)
    return re.sub(r"\s+", " ", value).strip()


def _index_entries(doc) -> list[tuple[str, list[int], str]]:
    raw: list[str] = []
    for page_number in POETRY_INDEX_PDF_PAGES:
        lines = [_norm_index_text(line) for line in doc[page_number - 1].get_text("text").splitlines()]
        lines = [
            line for line in lines
            if line
            and "Poetry Index" not in line
            and not re.match(r"^\d+ \|", line)
            and line != ""
        ]
        buffer = ""
        for line in lines:
            buffer = (buffer + " " + line).strip() if buffer else line
            if re.search(
                r"(?:\b\d+(?:-\d+)?(?: n\.\d+)?|\b[ivxlcdm]+)"
                r"(?:,\s*\d+(?:-\d+)?(?: n\.\d+)?)*$",
                buffer,
                re.I,
            ):
                raw.append(buffer)
                buffer = ""
        if buffer:
            raw.append(buffer)

    entries: list[tuple[str, list[int], str]] = []
    for text in raw:
        match = re.search(
            r"(?P<refs>(?:\b(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))"
            r"(?:,\s*(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))*)$",
            text,
            re.I,
        )
        if not match:
            continue
        lead = text[:match.start()].rstrip(" ,")
        incipit = lead.split(" - ", 1)[0].strip()
        pages: list[int] = []
        for token in [part.strip() for part in match.group("refs").split(",")]:
            if " n." in token:
                continue
            numeric = re.fullmatch(r"(\d+)(?:-(\d+))?", token)
            if numeric:
                first = int(numeric.group(1))
                last = int(numeric.group(2) or first)
                pages.extend(range(first, last + 1))
        entries.append((incipit, pages, text))
    if len(entries) != EXPECTED_POETRY_INDEX_ENTRIES:
        raise RuntimeError(
            "Qushayri Poetry Index entry count changed: "
            f"{len(entries)} != {EXPECTED_POETRY_INDEX_ENTRIES}"
        )
    occurrence_count = sum(len(pages) for _, pages, _ in entries)
    if occurrence_count != EXPECTED_POETRY_OCCURRENCES:
        raise RuntimeError(
            "Qushayri Poetry Index numeric occurrence count changed: "
            f"{occurrence_count} != {EXPECTED_POETRY_OCCURRENCES}"
        )
    return entries


def _similarity(left: str, right: str) -> float:
    left = _norm_index_text(left).lower()
    right = _norm_index_text(right).lower()
    if left.startswith(right) or right.startswith(left):
        return 1.0
    a = left.split()
    b = right.split()
    count = min(len(a), len(b), 8)
    if not count:
        return 0.0
    return sum(
        1 for index in range(count)
        if a[index].strip(".,;:?!") == b[index].strip(".,;:?!")
    ) / count


def _build_poetry_lines(doc) -> tuple[dict[tuple[int, int, int], PoetryLine], int, int]:
    poetry: dict[tuple[int, int, int], PoetryLine] = {}
    occurrences = 0
    for incipit, printed_pages, raw_entry in _index_entries(doc):
        for printed_page in printed_pages:
            occurrences += 1
            page_number = printed_page + PRINTED_TO_PDF_OFFSET
            if not (1 <= page_number <= len(doc)):
                raise RuntimeError(
                    f"Qushayri Poetry Index page out of range: {incipit!r} -> {page_number}"
                )
            records = _page_records(doc, page_number)
            scored = [(_similarity(record["text"], incipit), index) for index, record in enumerate(records)]
            score, start_index = max(scored, default=(0.0, -1))
            if start_index < 0 or score < 0.55:
                raise RuntimeError(
                    "Qushayri Poetry Index incipit not found on source page: "
                    f"{incipit!r} printed={printed_page} pdf={page_number} score={score:.3f}"
                )
            poem_records: list[dict] = []
            index = start_index
            while index < len(records):
                record = records[index]
                if (
                    record["x0"] < POETRY_X_MIN
                    or record["max_size"] < POETRY_BODY_SIZE_MIN
                    or _mostly_arabic(record["text"])
                    or (poem_records and record["y0"] < poem_records[-1]["y0"] - 2.0)
                ):
                    break
                poem_records.append(record)
                index += 1
            if len(poem_records) < 2:
                raise RuntimeError(
                    f"Qushayri indexed poetry occurrence has <2 source lines: {incipit!r}"
                )
            poem_id = f"{printed_page}:{_norm_index_text(incipit)}"
            previous: dict | None = None
            for line_index, record in enumerate(poem_records):
                key = (page_number, record["block"], record["line"])
                stanza = False
                if previous is not None:
                    stanza = (record["y0"] - previous["y0"]) > POETRY_STANZA_GAP_MIN
                value = PoetryLine(
                    page=page_number,
                    block=record["block"],
                    line=record["line"],
                    poem_id=poem_id,
                    line_index=line_index,
                    stanza_break_before=stanza,
                )
                existing = poetry.get(key)
                if existing is not None and existing != value:
                    raise RuntimeError(
                        f"Qushayri overlapping Poetry Index semantics at {key}: {existing} vs {value}"
                    )
                poetry[key] = value
                previous = record
    if occurrences != EXPECTED_POETRY_OCCURRENCES:
        raise RuntimeError(
            f"Qushayri poetry occurrence count changed: {occurrences} != {EXPECTED_POETRY_OCCURRENCES}"
        )
    if len(poetry) != EXPECTED_POETRY_LINES:
        raise RuntimeError(
            f"Qushayri indexed poetry line count changed: {len(poetry)} != {EXPECTED_POETRY_LINES}"
        )
    return poetry, EXPECTED_POETRY_INDEX_ENTRIES, occurrences


def build_qushayri_source_semantics(doc) -> QushayriSourceSemantics:
    note_calls = _build_note_calls(doc)
    poetry, entries, occurrences = _build_poetry_lines(doc)
    note_total = sum(len(values) for values in note_calls.values())
    if note_total != EXPECTED_NOTE_RELATIONS:
        raise RuntimeError(
            f"Qushayri verified note-call line map changed: {note_total} != {EXPECTED_NOTE_RELATIONS}"
        )
    return QushayriSourceSemantics(
        note_calls_by_line=note_calls,
        poetry_lines=poetry,
        poetry_entry_count=entries,
        poetry_occurrence_count=occurrences,
        poetry_line_count=len(poetry),
        verified_note_relation_count=note_total,
    )

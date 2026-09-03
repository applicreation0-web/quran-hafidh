#!/usr/bin/env python3
"""Deterministic, build-time Al-Jalalayn PDF -> read-only SQLite corpus.

The PDF is never shipped or parsed at runtime. PyMuPDF is required because this
specific InDesign PDF exposes discretionary line-break hyphens as U+00AD while
genuine hyphens remain U+002D.
"""
from __future__ import annotations

import argparse
import collections
import concurrent.futures
import hashlib
import json
import os
import re
import sqlite3
import unicodedata
from dataclasses import dataclass
from pathlib import Path

import fitz


VERSE_RE = re.compile(r"^\s*\[(\d{1,3}):(\d{1,3})\]\s*")
SURAH_RE = re.compile(r"^\s*\[(\d{1,3})\](?:\s|$)")
NOTE_START_RE = re.compile(r"^(\d{1,3})[\s\u2009\u200a]+")
VERSE_COUNTS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,
    98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,
    75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,
    13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,
    29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,
    9,5,4,7,3,6,3,5,4,5,6,
]
ALLOWED_STYLES = {"regular", "italic", "bold", "bold_italic", "note_ref"}


@dataclass
class Run:
    style: str
    text: str


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def block_spans(block: dict) -> list[dict]:
    return [s for line in block.get("lines", []) for s in line.get("spans", [])]


def block_text(block: dict) -> str:
    return "".join(s["text"] for s in block_spans(block))


def style_of(span: dict, *, note_ref: bool = False) -> str:
    if note_ref:
        return "note_ref"
    font = span.get("font", "")
    flags = int(span.get("flags", 0))
    bold = "Bold" in font or bool(flags & 16)
    italic = "It" in font or "Italic" in font or bool(flags & 2)
    if bold and italic:
        return "bold_italic"
    if bold:
        return "bold"
    if italic:
        return "italic"
    return "regular"


def append_run(runs: list[Run], style: str, text: str) -> None:
    if not text:
        return
    text = unicodedata.normalize("NFC", text)
    text = re.sub(r"[\t\u2009\u200a ]+", " ", text)
    if runs and runs[-1].style == style:
        runs[-1].text += text
    else:
        runs.append(Run(style, text))


def strip_prefix(runs: list[Run], count: int) -> list[Run]:
    out: list[Run] = []
    for r in runs:
        if count >= len(r.text):
            count -= len(r.text)
            continue
        append_run(out, r.style, r.text[count:])
        count = 0
    if count:
        raise AssertionError("prefix crossed run content")
    return out


def clean_soft_hyphens(runs: list[Run], stats: collections.Counter) -> None:
    for r in runs:
        stats["soft_hyphens_removed"] += r.text.count("\u00ad")
        r.text = r.text.replace("\u00ad", "")


def append_physical_line(target: list[Run], source: list[Run]) -> None:
    if not source:
        return
    previous = "".join(r.text for r in target)
    if target and previous and not previous.endswith(("\u00ad", "-", " ")):
        append_run(target, target[-1].style, " ")
    for r in source:
        append_run(target, r.style, r.text)


def line_runs(line: dict, *, note_mode: bool = False) -> list[Run]:
    out: list[Run] = []
    for s in line.get("spans", []):
        is_ref = (not note_mode and float(s["size"]) < 9.0 and s["text"].strip().isdigit())
        append_run(out, style_of(s, note_ref=is_ref), s["text"])
    return out


def expected_keys() -> set[tuple[int, int]]:
    return {(s, a) for s, n in enumerate(VERSE_COUNTS, 1) for a in range(1, n + 1)}


def plain(runs: list[Run]) -> str:
    return "".join(r.text for r in runs).strip()


def serialise(runs: list[Run]) -> str:
    return json.dumps(
        [{"style": r.style, "text": r.text} for r in runs if r.text],
        ensure_ascii=False,
        separators=(",", ":"),
    )


def digest_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def extract_page_chunk(args):
    pdf_text, start, end = args
    doc = fitz.open(pdf_text)
    result = []
    for page_index in range(start, end):
        result.append((page_index, doc[page_index].get_text("dict")))
    doc.close()
    return result


def load_page_dicts(pdf: Path):
    start, end = 30, 672
    workers = min(6, os.cpu_count() or 1)
    step = (end - start + workers - 1) // workers
    jobs = [(str(pdf), i, min(i + step, end)) for i in range(start, end, step)]
    if workers == 1:
        chunks = [extract_page_chunk(job) for job in jobs]
    else:
        with concurrent.futures.ProcessPoolExecutor(max_workers=workers) as pool:
            chunks = list(pool.map(extract_page_chunk, jobs))
    pages = [item for chunk in chunks for item in chunk]
    pages.sort(key=lambda item: item[0])
    return pages


def extract(pdf: Path):
    verses: dict[tuple[int, int], dict] = {}
    current: tuple[int, int] | None = None
    current_surah: int | None = None
    note_lines_by_page: dict[int, list[list[Run]]] = collections.defaultdict(list)
    calls = []
    stats = collections.Counter()

    # PDF pages 31..672 contain the numbered commentary. Zero-based indices below.
    for page_index, page_dict in load_page_dicts(pdf):
        page_number = page_index + 1
        blocks = [b for b in page_dict["blocks"] if b.get("lines")]
        blocks.sort(key=lambda b: (round(b["bbox"][1], 1), round(b["bbox"][0], 1)))
        for block in blocks:
            spans = block_spans(block)
            if not spans:
                continue
            y0, y1 = block["bbox"][1], block["bbox"][3]
            if y1 < 88 or y0 > 756:
                continue
            text = block_text(block)
            max_size = max(float(s["size"]) for s in spans)

            header = SURAH_RE.match(text)
            if max_size >= 13.5 and header:
                current_surah = int(header.group(1))
                current = None
                continue

            if max_size <= 8.1:
                for line in block["lines"]:
                    note_lines_by_page[page_number].append(line_runs(line, note_mode=True))
                continue

            if max_size < 9.5:
                continue
            if text.strip().casefold() in {"the end", "bibliography", "primary sources", "secondary sources"}:
                current = None
                continue

            for line in block["lines"]:
                raw = "".join(s["text"] for s in line.get("spans", []))
                marker = VERSE_RE.match(raw)
                if marker:
                    key = (int(marker.group(1)), int(marker.group(2)))
                    if key in verses:
                        raise AssertionError(f"duplicate marker {key}")
                    if current_surah != key[0]:
                        raise AssertionError(f"marker {key} under surah header {current_surah}")
                    current = key
                    verses[key] = {"runs": [], "page": page_number, "notes": []}
                if current is None:
                    continue
                runs = line_runs(line)
                if marker:
                    normalized_raw = "".join(r.text for r in runs)
                    normalized_marker = VERSE_RE.match(normalized_raw)
                    if not normalized_marker:
                        raise AssertionError(f"marker lost during normalization: {key}")
                    runs = strip_prefix(runs, normalized_marker.end())
                for s in line.get("spans", []):
                    if float(s["size"]) < 9.0 and s["text"].strip().isdigit():
                        calls.append({
                            "key": current,
                            "number": int(s["text"].strip()),
                            "page": page_number,
                        })
                append_physical_line(verses[current]["runs"], runs)

    # Only a note call inside a numbered verse can make a footnote eligible.
    calls_by_page_number = collections.defaultdict(list)
    for call in calls:
        calls_by_page_number[(call["page"], call["number"])].append(call)
    if any(len(v) != 1 for v in calls_by_page_number.values()):
        raise AssertionError("duplicate same-page note-call number")

    found_notes = {}
    for page_number, lines in note_lines_by_page.items():
        true_starts = []
        for idx, runs in enumerate(lines):
            value = plain(runs)
            match = NOTE_START_RE.match(value)
            if match and (page_number, int(match.group(1))) in calls_by_page_number:
                true_starts.append((idx, int(match.group(1)), match.end()))
        for pos, (start_idx, number, prefix_len) in enumerate(true_starts):
            end_idx = true_starts[pos + 1][0] if pos + 1 < len(true_starts) else len(lines)
            body = strip_prefix(lines[start_idx], prefix_len)
            for continuation in lines[start_idx + 1:end_idx]:
                append_physical_line(body, continuation)
            key = (page_number, number)
            if key in found_notes:
                raise AssertionError(f"duplicate note footer {key}")
            found_notes[key] = body

    for ordinal, call in enumerate(calls):
        note_key = (call["page"], call["number"])
        if note_key not in found_notes:
            raise AssertionError(f"missing note for call {call}")
        verses[call["key"]]["notes"].append({
            "number": call["number"],
            "page": call["page"],
            "runs": found_notes[note_key],
        })

    # U+00AD is a proven InDesign discretionary hyphen in this PDF. U+002D stays.
    for verse in verses.values():
        clean_soft_hyphens(verse["runs"], stats)
        for note in verse["notes"]:
            clean_soft_hyphens(note["runs"], stats)

    return verses, calls, stats


def validate(verses, calls, stats):
    expected = expected_keys()
    actual = set(verses)
    assert len(VERSE_COUNTS) == 114 and sum(VERSE_COUNTS) == 6236
    assert actual == expected, (sorted(expected - actual)[:10], sorted(actual - expected)[:10])
    assert len(calls) == 427, len(calls)
    assert sum(len(v["notes"]) for v in verses.values()) == len(calls)
    forbidden_chars = {"\u00ad", "\ufffd"}
    for key, verse in verses.items():
        assert plain(verse["runs"]), f"empty verse {key}"
        for payload in [verse["runs"], *(n["runs"] for n in verse["notes"])]:
            value = plain(payload)
            assert not any(ch in value for ch in forbidden_chars), key
            assert not any(unicodedata.category(ch) == "Co" for ch in value), key
            assert all(r.style in ALLOWED_STYLES for r in payload), key
    # Corpus-scoped count: 1,985 discretionary U+00AD characters are removed.
    # The broader 2,146 visual line-end-hyphen population also contains 161
    # genuine U+002D hyphens, which must remain intact.
    assert stats["soft_hyphens_removed"] == 1985, stats


def write_sqlite(path: Path, pdf: Path, verses, stats):
    if path.exists():
        path.unlink()
    con = sqlite3.connect(path)
    con.executescript("""
        PRAGMA page_size=4096;
        PRAGMA journal_mode=OFF;
        PRAGMA synchronous=OFF;
        PRAGMA user_version=1;
        CREATE TABLE source_metadata (
            key TEXT PRIMARY KEY NOT NULL,
            value TEXT NOT NULL
        ) WITHOUT ROWID;
        CREATE TABLE verse_commentary (
            surah INTEGER NOT NULL CHECK(surah BETWEEN 1 AND 114),
            ayah INTEGER NOT NULL CHECK(ayah > 0),
            body_json TEXT NOT NULL,
            plain_text TEXT NOT NULL,
            source_page INTEGER NOT NULL,
            content_sha256 TEXT NOT NULL,
            PRIMARY KEY (surah, ayah)
        ) WITHOUT ROWID;
        CREATE TABLE verse_note (
            surah INTEGER NOT NULL,
            ayah INTEGER NOT NULL,
            ordinal INTEGER NOT NULL,
            label INTEGER NOT NULL,
            body_json TEXT NOT NULL,
            plain_text TEXT NOT NULL,
            source_page INTEGER NOT NULL,
            content_sha256 TEXT NOT NULL,
            PRIMARY KEY (surah, ayah, ordinal),
            FOREIGN KEY (surah, ayah) REFERENCES verse_commentary(surah, ayah)
        ) WITHOUT ROWID;
    """)
    metadata = {
        "source_title": "Tafsir al-Jalalayn (English translation)",
        "source_file_sha256": sha256_file(pdf),
        "extractor_schema": "jalalayn-styled-runs-v1",
        "verse_count": "6236",
        "note_count": "427",
        "soft_hyphens_removed": str(stats["soft_hyphens_removed"]),
    }
    con.executemany("INSERT INTO source_metadata VALUES (?,?)", sorted(metadata.items()))
    for (surah, ayah), verse in sorted(verses.items()):
        body_json = serialise(verse["runs"])
        body_plain = plain(verse["runs"])
        con.execute(
            "INSERT INTO verse_commentary VALUES (?,?,?,?,?,?)",
            (surah, ayah, body_json, body_plain, verse["page"], digest_text(body_json)),
        )
        for ordinal, note in enumerate(verse["notes"], 1):
            note_json = serialise(note["runs"])
            note_plain = plain(note["runs"])
            con.execute(
                "INSERT INTO verse_note VALUES (?,?,?,?,?,?,?,?)",
                (surah, ayah, ordinal, note["number"], note_json, note_plain,
                 note["page"], digest_text(note_json)),
            )
    con.commit()
    con.execute("VACUUM")
    assert con.execute("PRAGMA quick_check").fetchone()[0] == "ok"
    assert con.execute("SELECT count(*) FROM verse_commentary").fetchone()[0] == 6236
    assert con.execute("SELECT count(*) FROM verse_note").fetchone()[0] == 427
    con.close()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("pdf", type=Path)
    ap.add_argument("output", type=Path)
    args = ap.parse_args()
    verses, calls, stats = extract(args.pdf)
    validate(verses, calls, stats)
    write_sqlite(args.output, args.pdf, verses, stats)
    print(json.dumps({
        "output": str(args.output),
        "output_sha256": sha256_file(args.output),
        "output_bytes": args.output.stat().st_size,
        "source_sha256": sha256_file(args.pdf),
        "verses": len(verses),
        "notes": len(calls),
        "soft_hyphens_removed": stats["soft_hyphens_removed"],
    }, indent=2))


if __name__ == "__main__":
    main()


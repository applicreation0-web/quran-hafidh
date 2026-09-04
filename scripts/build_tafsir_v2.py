#!/usr/bin/env python3
"""Build a reviewed tafsir-v2 SQLite database from a source-specific JSON export.

The builder is deliberately strict. It never extracts, translates, summarizes, or repairs
religious content. Source-specific extraction and review happen upstream. By default this
command only builds a distribution-eligible database; --allow-unreleased may be used for
local audit databases while rights/content gates are still pending.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sqlite3
from pathlib import Path

SCHEMA_VERSION = "tafsir-v2"
ALLOWED_EDITIONS = {"qurtubi_en_bewley", "qushayri_en_sands"}
ALLOWED_RIGHTS = {"licensed", "public_domain", "permission_documented"}
ALLOWED_STYLES = {"regular", "italic", "bold", "bold_italic", "note_ref"}
ALLOWED_TYPES = {
    "SURA_INTRODUCTION",
    "BASMALA_COMMENTARY",
    "VERSE_COMMENTARY",
    "VERSE_RANGE_COMMENTARY",
}
ALLOWED_COVERAGE_STATUS = {"available", "partial"}
ARABIC_SCRIPT = re.compile(r"[\u0600-\u06ff\u0750-\u077f\u08a0-\u08ff]")
FORBIDDEN_PAYLOAD_MARKERS = (
    "sunniconnect.com",
    "Downloaded via sunniconnect",
)
REQUIRED_METADATA = {
    "schema_version",
    "edition_id",
    "source_title",
    "source_sha256",
    "translator",
    "rights_status",
    "source_audit_status",
    "content_audit_status",
    "expected_mapped_verse_count",
    "content_language",
    "arabic_source_text_included",
}

SCHEMA = """
PRAGMA foreign_keys=ON;
CREATE TABLE source_metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE tafsir_entry(
  entry_id INTEGER PRIMARY KEY,
  source_entry_id TEXT NOT NULL UNIQUE,
  entry_type TEXT NOT NULL,
  surah INTEGER NOT NULL CHECK(surah BETWEEN 1 AND 114),
  verse_start INTEGER,
  verse_end INTEGER,
  segment_no INTEGER NOT NULL CHECK(segment_no > 0),
  section_title TEXT,
  body_json TEXT NOT NULL,
  plain_text TEXT NOT NULL,
  source_page_start INTEGER,
  source_page_end INTEGER,
  content_sha256 TEXT NOT NULL
);
CREATE TABLE entry_verse_map(
  surah INTEGER NOT NULL CHECK(surah BETWEEN 1 AND 114),
  ayah INTEGER NOT NULL CHECK(ayah > 0),
  entry_id INTEGER NOT NULL,
  ordinal INTEGER NOT NULL CHECK(ordinal > 0),
  PRIMARY KEY(surah, ayah, entry_id),
  FOREIGN KEY(entry_id) REFERENCES tafsir_entry(entry_id) ON DELETE CASCADE
);
CREATE TABLE entry_note(
  entry_id INTEGER NOT NULL,
  ordinal INTEGER NOT NULL CHECK(ordinal > 0),
  label TEXT,
  body_json TEXT NOT NULL,
  plain_text TEXT NOT NULL,
  source_page INTEGER,
  content_sha256 TEXT NOT NULL,
  PRIMARY KEY(entry_id, ordinal),
  FOREIGN KEY(entry_id) REFERENCES tafsir_entry(entry_id) ON DELETE CASCADE
);
CREATE TABLE coverage(
  surah INTEGER NOT NULL CHECK(surah BETWEEN 1 AND 114),
  ayah_start INTEGER NOT NULL CHECK(ayah_start > 0),
  ayah_end INTEGER NOT NULL CHECK(ayah_end >= ayah_start),
  status TEXT NOT NULL,
  note TEXT,
  PRIMARY KEY(surah, ayah_start, ayah_end)
);
CREATE INDEX idx_entry_verse_lookup ON entry_verse_map(surah, ayah, ordinal);
CREATE INDEX idx_entry_range ON tafsir_entry(surah, verse_start, verse_end, segment_no);
"""


def sha_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def require_sha256(value: object, field: str) -> str:
    text = str(value or "").strip().lower()
    if len(text) != 64 or any(ch not in "0123456789abcdef" for ch in text):
        raise ValueError(f"{field} must be a lowercase 64-character SHA-256")
    return text


def metadata_text(value: object) -> str:
    """Store booleans as JSON lowercase true/false so runtime checks are deterministic."""
    if isinstance(value, (dict, list, bool)):
        return json.dumps(value, ensure_ascii=False, separators=(",", ":"))
    return str(value)


def canonical_runs(runs: object) -> tuple[str, str]:
    if not isinstance(runs, list):
        raise ValueError("body_runs/note runs must be a list")
    cleaned: list[dict[str, str]] = []
    plain: list[str] = []
    for run in runs:
        if not isinstance(run, dict):
            raise ValueError("Every styled run must be an object")
        style = str(run.get("style", ""))
        text = str(run.get("text", ""))
        if style not in ALLOWED_STYLES:
            raise ValueError(f"Unsupported run style: {style}")
        if text:
            cleaned.append({"style": style, "text": text})
            plain.append(text)
    if not cleaned:
        raise ValueError("Empty commentary/notes are forbidden")
    body_json = json.dumps(cleaned, ensure_ascii=False, separators=(",", ":"))
    plain_text = "".join(plain)
    folded = plain_text.casefold()
    for marker in FORBIDDEN_PAYLOAD_MARKERS:
        if marker.casefold() in folded:
            raise ValueError(f"Forbidden third-party contamination marker: {marker}")
    if ARABIC_SCRIPT.search(plain_text):
        raise ValueError(
            "Arabic-script source text is forbidden in Qurtubi/Qushayri English payloads"
        )
    return body_json, plain_text


def validate_metadata(metadata: dict, allow_unreleased: bool) -> None:
    missing = sorted(REQUIRED_METADATA - set(metadata))
    if missing:
        raise ValueError(f"Missing metadata fields: {', '.join(missing)}")
    if metadata["schema_version"] != SCHEMA_VERSION:
        raise ValueError(f"schema_version must be {SCHEMA_VERSION!r}")
    if metadata["edition_id"] not in ALLOWED_EDITIONS:
        raise ValueError(f"Unsupported edition_id: {metadata['edition_id']!r}")
    if not str(metadata["source_title"]).strip():
        raise ValueError("source_title is required")
    if not str(metadata["translator"]).strip():
        raise ValueError("translator is required")
    require_sha256(metadata["source_sha256"], "source_sha256")
    expected = int(metadata["expected_mapped_verse_count"])
    if expected <= 0:
        raise ValueError("expected_mapped_verse_count must be positive")
    if metadata.get("content_language") != "en":
        raise ValueError("New tafsir payload content_language must be 'en'")
    if metadata.get("arabic_source_text_included") is not False:
        raise ValueError("Arabic source text must not be included in new tafsir payloads")
    if not allow_unreleased:
        if metadata["rights_status"] not in ALLOWED_RIGHTS:
            raise ValueError("Redistribution rights are not cleared")
        if metadata["source_audit_status"] != "verified":
            raise ValueError("Source audit is not verified")
        if metadata["content_audit_status"] != "verified":
            raise ValueError("Content audit is not verified")


def validate_page_range(source_entry: dict, source_id: str) -> tuple[int | None, int | None]:
    page_start = source_entry.get("source_page_start")
    page_end = source_entry.get("source_page_end")
    page_start = None if page_start is None else int(page_start)
    page_end = None if page_end is None else int(page_end)
    if page_start is not None and page_start <= 0:
        raise ValueError(f"Invalid source_page_start for {source_id}")
    if page_end is not None and page_end <= 0:
        raise ValueError(f"Invalid source_page_end for {source_id}")
    if page_start is not None and page_end is not None and page_end < page_start:
        raise ValueError(f"Reversed source page range for {source_id}")
    return page_start, page_end


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument(
        "--allow-unreleased",
        action="store_true",
        help="Build a local audit DB even when rights/content gates are still pending.",
    )
    args = parser.parse_args()

    document = json.loads(args.input.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise SystemExit("Top-level tafsir export must be an object")
    metadata = document.get("metadata") or {}
    entries = document.get("entries") or []
    coverage = document.get("coverage") or []
    if not isinstance(metadata, dict):
        raise SystemExit("metadata must be an object")
    if not isinstance(entries, list) or not entries:
        raise SystemExit("No reviewed tafsir entries supplied")
    if not isinstance(coverage, list) or not coverage:
        raise SystemExit("Coverage metadata is required")
    validate_metadata(metadata, args.allow_unreleased)

    if args.output.exists():
        args.output.unlink()
    con = sqlite3.connect(args.output)
    try:
        con.executescript(SCHEMA)
        for key, value in sorted(metadata.items()):
            con.execute(
                "INSERT INTO source_metadata(key,value) VALUES(?,?)",
                (str(key), metadata_text(value)),
            )

        source_ids: set[str] = set()
        structural_keys: set[tuple[int, int, int, int]] = set()
        mapped_verses: set[tuple[int, int]] = set()

        for source_entry in entries:
            if not isinstance(source_entry, dict):
                raise ValueError("Every entry must be an object")
            source_id = str(source_entry.get("source_entry_id", "")).strip()
            if not source_id:
                raise ValueError("source_entry_id is required")
            if source_id in source_ids:
                raise ValueError(f"Duplicate source_entry_id: {source_id}")
            source_ids.add(source_id)

            entry_type = str(source_entry.get("entry_type", ""))
            if entry_type not in ALLOWED_TYPES:
                raise ValueError(f"Unsupported entry_type: {entry_type}")
            surah = int(source_entry["surah"])
            if surah not in range(1, 115):
                raise ValueError(f"Invalid surah for {source_id}: {surah}")

            start = source_entry.get("verse_start")
            end = source_entry.get("verse_end")
            start = None if start is None else int(start)
            end = None if end is None else int(end)
            verse_entry = entry_type in {"VERSE_COMMENTARY", "VERSE_RANGE_COMMENTARY"}
            if verse_entry:
                if start is None or end is None or start < 1 or end < start:
                    raise ValueError(f"Invalid verse range for {source_id}")
                if entry_type == "VERSE_COMMENTARY" and start != end:
                    raise ValueError(f"VERSE_COMMENTARY must map one verse: {source_id}")
                if entry_type == "VERSE_RANGE_COMMENTARY" and end <= start:
                    raise ValueError(f"VERSE_RANGE_COMMENTARY must span >1 verse: {source_id}")
            elif start is not None or end is not None:
                raise ValueError(
                    f"{entry_type} must stay structurally separate from verse taps: {source_id}"
                )

            segment_no = int(source_entry.get("segment_no", 1))
            if segment_no <= 0:
                raise ValueError(f"segment_no must be positive for {source_id}")
            if verse_entry:
                structural_key = (surah, int(start), int(end), segment_no)
                if structural_key in structural_keys:
                    raise ValueError(
                        "Duplicate verse range/segment. Assign source-order segment_no: "
                        f"{source_id} {structural_key}"
                    )
                structural_keys.add(structural_key)

            page_start, page_end = validate_page_range(source_entry, source_id)
            body_json, plain = canonical_runs(source_entry["body_runs"])
            cur = con.execute(
                """INSERT INTO tafsir_entry(
                    source_entry_id,entry_type,surah,verse_start,verse_end,segment_no,
                    section_title,body_json,plain_text,source_page_start,source_page_end,
                    content_sha256
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)""",
                (
                    source_id,
                    entry_type,
                    surah,
                    start,
                    end,
                    segment_no,
                    source_entry.get("section_title"),
                    body_json,
                    plain,
                    page_start,
                    page_end,
                    sha_text(body_json),
                ),
            )
            entry_id = int(cur.lastrowid)

            if verse_entry:
                assert start is not None and end is not None
                for ordinal, ayah in enumerate(range(start, end + 1), start=1):
                    con.execute(
                        "INSERT INTO entry_verse_map(surah,ayah,entry_id,ordinal) VALUES(?,?,?,?)",
                        (surah, ayah, entry_id, ordinal),
                    )
                    mapped_verses.add((surah, ayah))

            notes = source_entry.get("notes") or []
            if not isinstance(notes, list):
                raise ValueError(f"notes must be a list for {source_id}")
            for ordinal, note in enumerate(notes, start=1):
                if not isinstance(note, dict):
                    raise ValueError(f"Invalid note object for {source_id}")
                note_json, note_plain = canonical_runs(note["runs"])
                note_page = note.get("source_page")
                note_page = None if note_page is None else int(note_page)
                if note_page is not None and note_page <= 0:
                    raise ValueError(f"Invalid note source_page for {source_id}")
                label = note.get("label")
                if label is not None and not str(label).strip():
                    label = None
                con.execute(
                    """INSERT INTO entry_note(
                        entry_id,ordinal,label,body_json,plain_text,source_page,content_sha256
                    ) VALUES(?,?,?,?,?,?,?)""",
                    (
                        entry_id,
                        ordinal,
                        None if label is None else str(label),
                        note_json,
                        note_plain,
                        note_page,
                        sha_text(note_json),
                    ),
                )

        coverage_ranges: list[tuple[int, int, int]] = []
        for row in coverage:
            if not isinstance(row, dict):
                raise ValueError("Every coverage row must be an object")
            surah = int(row["surah"])
            start = int(row["ayah_start"])
            end = int(row["ayah_end"])
            status = str(row.get("status", "available"))
            if surah not in range(1, 115) or start < 1 or end < start:
                raise ValueError(f"Invalid coverage range: {row}")
            if status not in ALLOWED_COVERAGE_STATUS:
                raise ValueError(f"Unsupported coverage status: {status}")
            coverage_ranges.append((surah, start, end))
            con.execute(
                "INSERT INTO coverage(surah,ayah_start,ayah_end,status,note) VALUES(?,?,?,?,?)",
                (surah, start, end, status, row.get("note")),
            )

        expected_mapped = int(metadata["expected_mapped_verse_count"])
        if len(mapped_verses) != expected_mapped:
            raise ValueError(
                f"Mapped verse count mismatch: expected {expected_mapped}, got {len(mapped_verses)}"
            )
        uncovered = [
            (surah, ayah)
            for surah, ayah in sorted(mapped_verses)
            if not any(
                cov_surah == surah and start <= ayah <= end
                for cov_surah, start, end in coverage_ranges
            )
        ]
        if uncovered:
            raise ValueError(f"Mapped verses outside declared coverage: {uncovered[:10]}")

        con.commit()
        if con.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise ValueError("SQLite quick_check failed")
        if con.execute("PRAGMA foreign_key_check").fetchall():
            raise ValueError("SQLite foreign_key_check failed")
    except Exception:
        con.close()
        args.output.unlink(missing_ok=True)
        raise
    else:
        con.close()

    print(
        json.dumps(
            {
                "schema_version": SCHEMA_VERSION,
                "edition_id": metadata["edition_id"],
                "entries": len(entries),
                "mapped_verses": len(mapped_verses),
                "sha256": hashlib.sha256(args.output.read_bytes()).hexdigest(),
                "output": str(args.output),
                "distribution_mode": "audit_only" if args.allow_unreleased else "release_eligible",
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()

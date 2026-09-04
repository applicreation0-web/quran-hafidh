#!/usr/bin/env python3
"""Build a reviewed tafsir-v2 SQLite database from a source-specific JSON export.

This builder is intentionally source-agnostic. Qushayri/Qurtubi extraction must happen in
separate source-specific tooling and be human/source verified before its JSON is accepted.
No AI summary or translation is generated here.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
from pathlib import Path

SCHEMA = """
PRAGMA foreign_keys=ON;
CREATE TABLE source_metadata(key TEXT PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE tafsir_entry(
  entry_id INTEGER PRIMARY KEY,
  entry_type TEXT NOT NULL,
  surah INTEGER NOT NULL,
  verse_start INTEGER,
  verse_end INTEGER,
  segment_no INTEGER NOT NULL DEFAULT 1,
  section_title TEXT,
  body_json TEXT NOT NULL,
  plain_text TEXT NOT NULL,
  source_page_start INTEGER,
  source_page_end INTEGER,
  content_sha256 TEXT NOT NULL
);
CREATE TABLE entry_verse_map(
  surah INTEGER NOT NULL,
  ayah INTEGER NOT NULL,
  entry_id INTEGER NOT NULL,
  ordinal INTEGER NOT NULL,
  PRIMARY KEY(surah, ayah, entry_id),
  FOREIGN KEY(entry_id) REFERENCES tafsir_entry(entry_id) ON DELETE CASCADE
);
CREATE TABLE entry_note(
  entry_id INTEGER NOT NULL,
  ordinal INTEGER NOT NULL,
  label TEXT,
  body_json TEXT NOT NULL,
  plain_text TEXT NOT NULL,
  source_page INTEGER,
  content_sha256 TEXT NOT NULL,
  PRIMARY KEY(entry_id, ordinal),
  FOREIGN KEY(entry_id) REFERENCES tafsir_entry(entry_id) ON DELETE CASCADE
);
CREATE TABLE coverage(
  surah INTEGER NOT NULL,
  ayah_start INTEGER NOT NULL,
  ayah_end INTEGER NOT NULL,
  status TEXT NOT NULL,
  note TEXT,
  PRIMARY KEY(surah, ayah_start, ayah_end)
);
CREATE INDEX idx_entry_verse_lookup ON entry_verse_map(surah, ayah, ordinal);
CREATE INDEX idx_entry_range ON tafsir_entry(surah, verse_start, verse_end, segment_no);
"""

ALLOWED_STYLES = {"regular", "italic", "bold", "bold_italic", "note_ref"}
ALLOWED_TYPES = {
    "SURA_INTRODUCTION",
    "BASMALA_COMMENTARY",
    "VERSE_COMMENTARY",
    "VERSE_RANGE_COMMENTARY",
}


def canonical_runs(runs: list[dict]) -> tuple[str, str]:
    cleaned: list[dict] = []
    plain: list[str] = []
    for run in runs:
        style = str(run.get("style", ""))
        text = str(run.get("text", ""))
        if style not in ALLOWED_STYLES:
            raise ValueError(f"Unsupported run style: {style}")
        if text:
            cleaned.append({"style": style, "text": text})
            plain.append(text)
    if not cleaned:
        raise ValueError("Empty commentary/notes are forbidden")
    return json.dumps(cleaned, ensure_ascii=False, separators=(",", ":")), "".join(plain)


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    document = json.loads(args.input.read_text(encoding="utf-8"))
    metadata = document.get("metadata") or {}
    entries = document.get("entries") or []
    coverage = document.get("coverage") or []
    if not metadata.get("edition_id"):
        raise SystemExit("metadata.edition_id is required")
    if not entries:
        raise SystemExit("No reviewed tafsir entries supplied")

    if args.output.exists():
        args.output.unlink()
    con = sqlite3.connect(args.output)
    try:
        con.executescript(SCHEMA)
        for key, value in sorted(metadata.items()):
            con.execute(
                "INSERT INTO source_metadata(key,value) VALUES(?,?)",
                (str(key), json.dumps(value, ensure_ascii=False) if isinstance(value, (dict, list)) else str(value)),
            )

        entry_ids: set[str] = set()
        for source_entry in entries:
            source_id = str(source_entry["source_entry_id"])
            if source_id in entry_ids:
                raise ValueError(f"Duplicate source_entry_id: {source_id}")
            entry_ids.add(source_id)
            entry_type = str(source_entry["entry_type"])
            if entry_type not in ALLOWED_TYPES:
                raise ValueError(f"Unsupported entry_type: {entry_type}")
            surah = int(source_entry["surah"])
            start = source_entry.get("verse_start")
            end = source_entry.get("verse_end")
            start = None if start is None else int(start)
            end = None if end is None else int(end)
            if entry_type in {"VERSE_COMMENTARY", "VERSE_RANGE_COMMENTARY"}:
                if start is None or end is None or start < 1 or end < start:
                    raise ValueError(f"Invalid verse range for {source_id}")
            body_json, plain = canonical_runs(source_entry["body_runs"])
            cur = con.execute(
                """INSERT INTO tafsir_entry(
                    entry_type,surah,verse_start,verse_end,segment_no,section_title,
                    body_json,plain_text,source_page_start,source_page_end,content_sha256
                ) VALUES(?,?,?,?,?,?,?,?,?,?,?)""",
                (
                    entry_type,
                    surah,
                    start,
                    end,
                    int(source_entry.get("segment_no", 1)),
                    source_entry.get("section_title"),
                    body_json,
                    plain,
                    source_entry.get("source_page_start"),
                    source_entry.get("source_page_end"),
                    sha(plain),
                ),
            )
            entry_id = int(cur.lastrowid)
            if start is not None and end is not None:
                for ordinal, ayah in enumerate(range(start, end + 1), start=1):
                    con.execute(
                        "INSERT INTO entry_verse_map(surah,ayah,entry_id,ordinal) VALUES(?,?,?,?)",
                        (surah, ayah, entry_id, ordinal),
                    )
            for ordinal, note in enumerate(source_entry.get("notes") or [], start=1):
                note_json, note_plain = canonical_runs(note["runs"])
                con.execute(
                    """INSERT INTO entry_note(
                        entry_id,ordinal,label,body_json,plain_text,source_page,content_sha256
                    ) VALUES(?,?,?,?,?,?,?)""",
                    (
                        entry_id,
                        ordinal,
                        note.get("label"),
                        note_json,
                        note_plain,
                        note.get("source_page"),
                        sha(note_plain),
                    ),
                )

        for row in coverage:
            con.execute(
                "INSERT INTO coverage(surah,ayah_start,ayah_end,status,note) VALUES(?,?,?,?,?)",
                (
                    int(row["surah"]),
                    int(row["ayah_start"]),
                    int(row["ayah_end"]),
                    str(row.get("status", "available")),
                    row.get("note"),
                ),
            )
        con.commit()
        if con.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise ValueError("SQLite quick_check failed")
        if con.execute("PRAGMA foreign_key_check").fetchall():
            raise ValueError("SQLite foreign_key_check failed")
    finally:
        con.close()

    print(json.dumps({
        "edition_id": metadata["edition_id"],
        "entries": len(entries),
        "sha256": hashlib.sha256(args.output.read_bytes()).hexdigest(),
        "output": str(args.output),
    }, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

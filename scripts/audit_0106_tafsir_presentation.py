#!/usr/bin/env python3
"""Contradictory presentation audit for Quran Safeguard 0.10.6 Tafsir.

This gate inspects the packaged SQLite payloads, not merely source code. It
checks exactly the presentation decisions approved for 0.10.6 while preserving
Qur'an-reference labels and all classical commentary wording.
"""
from __future__ import annotations

import argparse
import base64
import gzip
import re
import sqlite3
import tempfile
from pathlib import Path

QUSHAYRI_PARTS = [f"qushayri_en.sqlite.gz.b64.part{i:02d}" for i in range(2)]
QURTUBI_PARTS = [f"qurtubi_en.sqlite.gz.b64.part{i:02d}" for i in range(4)]
QURTUBI_MARKER = re.compile(r"\b(\d{1,3})\.?\s+(?=(?:…\s*)?(?:[^\W\d_]|[\"‘“]))")


def decode(assets: Path, parts: list[str]) -> bytes:
    missing = [name for name in parts if not (assets / name).is_file()]
    if missing:
        raise SystemExit(f"Missing Tafsir assets: {missing}")
    encoded = "".join((assets / name).read_text(encoding="ascii") for name in parts)
    return gzip.decompress(base64.b64decode(encoded, validate=True))


def open_db(blob: bytes):
    tmp = tempfile.NamedTemporaryFile(suffix=".sqlite")
    tmp.write(blob)
    tmp.flush()
    return tmp, sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)


def metadata(con) -> dict[str, str]:
    return dict(con.execute("SELECT key,value FROM source_metadata ORDER BY key"))


def audit_qurtubi(assets: Path) -> None:
    tmp, con = open_db(decode(assets, QURTUBI_PARTS))
    try:
        meta = metadata(con)
        if meta.get("presentation_revision") != "0106-qurtubi-hide-verse-labels-v1":
            raise SystemExit("Qurtubi 0.10.6 presentation revision missing")
        if meta.get("verse_marker_display_policy") != "indexed-source-verse-labels-hidden-in-translation":
            raise SystemExit("Qurtubi verse-marker display policy missing")
        rows = list(
            con.execute(
                "SELECT id,surah,verse_start,verse_end,verse_translation,commentary "
                "FROM tafsir_entry ORDER BY id"
            )
        )
        if len(rows) != 432:
            raise SystemExit(f"Qurtubi row count changed: {len(rows)}")
        suspicious = []
        for row_id, surah, start, end, translation, commentary in rows:
            in_range = [
                int(match.group(1))
                for match in QURTUBI_MARKER.finditer(translation)
                if start <= int(match.group(1)) <= end
            ]
            if in_range:
                suspicious.append((row_id, surah, start, end, in_range, translation[:140]))
        if suspicious:
            raise SystemExit(f"Qurtubi indexed verse labels still visible: {suspicious[:8]}")

        row = con.execute(
            "SELECT verse_translation,commentary FROM tafsir_entry "
            "WHERE surah=2 AND verse_start<=21 AND verse_end>=21 ORDER BY id LIMIT 1"
        ).fetchone()
        if not row:
            raise SystemExit("Qurtubi 2:21 missing")
        if re.match(r"^\s*21\.?\s+", row[0]):
            raise SystemExit("Qurtubi 2:21 still displays the redundant 21 label")
        if not row[0].lstrip().startswith("Mankind!"):
            raise SystemExit(f"Qurtubi 2:21 translation changed unexpectedly: {row[0][:100]!r}")
        genuine_refs = con.execute(
            "SELECT COUNT(*) FROM tafsir_entry WHERE commentary GLOB '*[0-9]:[0-9]*'"
        ).fetchone()[0]
        if genuine_refs < 1:
            raise SystemExit("Qurtubi genuine Qur'an references disappeared from commentary")
    finally:
        con.close()
        tmp.close()


def audit_qushayri(assets: Path) -> None:
    tmp, con = open_db(decode(assets, QUSHAYRI_PARTS))
    try:
        meta = metadata(con)
        expected_meta = {
            "presentation_revision": "0106-qushayri-source-semantics-v1",
            "verified_note_call_count": "928",
            "note_call_display_policy": "remove-only-source-verified-unexposed-footnote-calls",
            "poetry_index_entry_count": "121",
            "poetry_index_occurrence_count": "126",
            "poetry_unique_line_count": "542",
            "poetry_semantics": "source-poetry-index-v1",
            "paragraph_policy": "source-geometry-no-pdf-block-breaks",
            "semantic_run_table": "tafsir_run",
        }
        for key, expected in expected_meta.items():
            if meta.get(key) != expected:
                raise SystemExit(
                    f"Qushayri 0.10.6 metadata {key}={meta.get(key)!r}, expected {expected!r}"
                )
        if con.execute("SELECT COUNT(*) FROM tafsir_entry").fetchone()[0] != 720:
            raise SystemExit("Qushayri row count changed")
        if con.execute("SELECT COUNT(*) FROM tafsir_run").fetchone()[0] < 720:
            raise SystemExit("Qushayri semantic run table unexpectedly sparse")
        invalid_styles = con.execute(
            "SELECT COUNT(*) FROM tafsir_run WHERE style NOT IN ('REGULAR','POETRY')"
        ).fetchone()[0]
        if invalid_styles:
            raise SystemExit("Qushayri unknown semantic run style found")
        poetry_runs = con.execute(
            "SELECT COUNT(*) FROM tafsir_run WHERE style='POETRY'"
        ).fetchone()[0]
        if poetry_runs < 1:
            raise SystemExit("Qushayri Poetry Index produced no POETRY runtime runs")

        for entry_id, commentary in con.execute(
            "SELECT id,commentary FROM tafsir_entry ORDER BY id"
        ):
            rebuilt = "".join(
                text
                for (text,) in con.execute(
                    "SELECT text FROM tafsir_run WHERE entry_id=? ORDER BY run_no",
                    (entry_id,),
                )
            )
            if rebuilt != commentary:
                raise SystemExit(f"Qushayri semantic runs diverge from entry {entry_id}")

        quran_refs = con.execute(
            "SELECT COUNT(*) FROM tafsir_entry "
            "WHERE verse_translation LIKE '%[2:11]%' OR verse_translation LIKE '%[2:12]%'"
        ).fetchone()[0]
        if quran_refs < 1:
            raise SystemExit("Qushayri useful [2:11]/[2:12] Qur'an references were removed")

        row = con.execute(
            "SELECT id,commentary FROM tafsir_entry "
            "WHERE surah=2 AND verse_start<=10 AND verse_end>=10 ORDER BY id LIMIT 1"
        ).fetchone()
        if not row:
            raise SystemExit("Qushayri 2:10 missing")
        entry_id, commentary = row
        forbidden = ["(shirk)46", "neglectful.47", "dhimmīs.48", "tribulation.49"]
        survived = [token for token in forbidden if token in commentary]
        if survived:
            raise SystemExit(f"Qushayri unexposed source note calls survived: {survived}")

        expected_poem = (
            "We have not been firm\n"
            "but justice will be firm with us without bending.\n\n"
            "If we had been sincere (khalaṣnā),\n"
            "we would have been saved (takhallaṣnā) from tribulation."
        )
        poetry_texts = [
            text
            for style, text in con.execute(
                "SELECT style,text FROM tafsir_run WHERE entry_id=? ORDER BY run_no",
                (entry_id,),
            )
            if style == "POETRY"
        ]
        matching = [text for text in poetry_texts if "We have not been firm" in text]
        if len(matching) != 1:
            raise SystemExit(f"Qushayri 2:10 indexed poem run count={len(matching)}")
        if expected_poem not in matching[0]:
            raise SystemExit(
                "Qushayri 2:10 poem line/stanza spacing is not source-faithful: "
                f"{matching[0]!r}"
            )
        if "We have not been firm\n\nbut justice" in commentary:
            raise SystemExit("Qushayri false PDF-block blank line survived inside poem")
    finally:
        con.close()
        tmp.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("assets_dir", type=Path)
    args = parser.parse_args()
    assets = args.assets_dir.resolve()
    audit_qurtubi(assets)
    audit_qushayri(assets)
    print("0.10.6 Tafsir presentation contradictory audit PASS")
    print("- Qurtubi: source verse labels hidden only from displayed translations; commentary references retained")
    print("- Qushayri: useful Qur'an refs retained; 928 source-verified unexposed note calls removed")
    print("- Qushayri: 121 Poetry Index entries / 126 occurrences mapped; semantic POETRY runs active")
    print("- Qushayri 2:10: four poetry lines with one source stanza break; no PDF-block fake blank line")


if __name__ == "__main__":
    main()

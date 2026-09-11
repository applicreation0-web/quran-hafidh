#!/usr/bin/env python3
"""Fail-closed audit of Quran Hifz Qurtubi/Qushayri packaged corpora. Stdlib only."""
from __future__ import annotations

import base64
import gzip
import sqlite3
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/plus/assets/tafsir"

SPECS = {
    "qurtubi": {
        "parts": 4,
        "entries": 432,
        "presentation_revision": "0106-qurtubi-hide-verse-labels-v1",
    },
    "qushayri": {
        "parts": 2,
        "entries": 720,
        "presentation_revision": "0106-qushayri-source-semantics-v1",
    },
}


def metadata(db: sqlite3.Connection) -> dict[str, str]:
    return dict(db.execute("SELECT key,value FROM source_metadata"))


def load_packaged_db(name: str, part_count: int) -> bytes:
    parts = [ASSETS / f"{name}_en.sqlite.gz.b64.part{i:02d}" for i in range(part_count)]
    missing = [p.name for p in parts if not p.is_file()]
    assert not missing, f"{name}: missing packaged assets {missing}"
    encoded = "".join(p.read_text(encoding="ascii") for p in parts)
    return gzip.decompress(base64.b64decode(encoded, validate=True))


def audit(name: str, spec: dict[str, object]) -> None:
    raw = load_packaged_db(name, int(spec["parts"]))
    with tempfile.NamedTemporaryFile(suffix=".sqlite") as tmp:
        tmp.write(raw)
        tmp.flush()
        db = sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)
        try:
            assert db.execute("PRAGMA quick_check").fetchone()[0] == "ok", f"{name}: quick_check failed"
            meta = metadata(db)
            expected_entries = int(spec["entries"])
            assert meta.get("schema_version") == "2", f"{name}: schema_version={meta.get('schema_version')!r}"
            assert meta.get("edition_id") == name, f"{name}: edition_id={meta.get('edition_id')!r}"
            assert meta.get("arabic_included") == "false", f"{name}: arabic_included must be false"
            assert meta.get("presentation_revision") == spec["presentation_revision"], (
                f"{name}: presentation revision drift"
            )
            assert int(meta.get("entry_count", "-1")) == expected_entries, f"{name}: metadata entry_count drift"
            rows = db.execute("SELECT COUNT(*) FROM tafsir_entry").fetchone()[0]
            assert rows == expected_entries, f"{name}: tafsir_entry rows={rows} != {expected_entries}"

            if name == "qushayri":
                expected_meta = {
                    "semantic_run_table": "tafsir_run",
                    "verified_note_call_count": "928",
                    "poetry_index_entry_count": "121",
                    "poetry_index_occurrence_count": "126",
                    "poetry_unique_line_count": "542",
                }
                for key, expected in expected_meta.items():
                    assert meta.get(key) == expected, f"qushayri: {key}={meta.get(key)!r} != {expected!r}"
                tables = {
                    row[0]
                    for row in db.execute("SELECT name FROM sqlite_master WHERE type='table'")
                }
                assert "tafsir_run" in tables, "qushayri: tafsir_run missing"
                run_count = db.execute("SELECT COUNT(*) FROM tafsir_run").fetchone()[0]
                assert run_count > 0, "qushayri: tafsir_run empty"

            # Prove the runtime lookup shape expected by TafsirRepository is valid.
            sample = db.execute(
                "SELECT surah,verse_start,verse_end,segment_no,verse_translation,commentary "
                "FROM tafsir_entry ORDER BY id LIMIT 1"
            ).fetchone()
            assert sample is not None and len(sample) == 6, f"{name}: runtime lookup schema mismatch"
        finally:
            db.close()
    print(f"{name}: PASS ({spec['entries']} entries, {spec['parts']} packaged parts)")


def main() -> None:
    leaked = list(ASSETS.glob("*.pdf"))
    assert not leaked, f"source PDFs must not be bundled: {leaked}"
    for name, spec in SPECS.items():
        audit(name, spec)
    print("QURAN_HIFZ_MULTITAFSIR_AUDIT=PASS")


if __name__ == "__main__":
    main()

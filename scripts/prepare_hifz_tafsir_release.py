#!/usr/bin/env python3
"""Normalize audited Tafsir build inputs into raw gzip parts for Quran Hifz.

The source corpora are not edited in place. The generated APK assets get a single
.gz.part packaging format and explicit local personal-use metadata.
"""
from __future__ import annotations

import argparse
import base64
import gzip
import sqlite3
import tempfile
from pathlib import Path

RIGHTS = {
    "personal_use_only": "true",
    "redistribution_approved": "false",
    "rights_note": "Personal-use local corpus; redistribution is not approved by Quran Hifz. Preserve source and translator attribution.",
}

CORPORA = (
    ("al_jalalayn_en.sqlite", 4, False, 6236, "verse_commentary"),
    ("qurtubi_en.sqlite", 4, True, 432, "tafsir_entry"),
    ("qushayri_en.sqlite", 2, True, 720, "tafsir_entry"),
)


def read_source(root: Path, name: str, count: int, encoded: bool) -> bytes:
    suffix = ".gz.b64.part" if encoded else ".gz.part"
    payload = b"".join((root / f"{name}{suffix}{i:02d}").read_bytes() for i in range(count))
    if encoded:
        payload = base64.b64decode(payload)
    return gzip.decompress(payload)


def source_present(root: Path, name: str, count: int, encoded: bool) -> bool:
    suffix = ".gz.b64.part" if encoded else ".gz.part"
    return all((root / f"{name}{suffix}{i:02d}").exists() for i in range(count))


def prepare_database(raw: bytes, expected: int, table: str) -> bytes:
    with tempfile.NamedTemporaryFile(suffix=".sqlite") as tmp:
        tmp.write(raw)
        tmp.flush()
        db = sqlite3.connect(tmp.name)
        try:
            check = db.execute("PRAGMA quick_check").fetchone()[0]
            if check != "ok":
                raise RuntimeError(f"SQLite quick_check failed: {check}")
            actual = db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
            if actual != expected:
                raise RuntimeError(f"Unexpected {table} count: {actual} != {expected}")
            for key, value in RIGHTS.items():
                db.execute("INSERT OR REPLACE INTO source_metadata(key,value) VALUES(?,?)", (key, value))
            db.commit()
        finally:
            db.close()
        return Path(tmp.name).read_bytes()


def write_parts(root: Path, name: str, data: bytes, count: int) -> None:
    compressed = gzip.compress(data, compresslevel=9, mtime=0)
    chunk = (len(compressed) + count - 1) // count
    for i in range(count):
        start = i * chunk
        end = min(len(compressed), (i + 1) * chunk)
        (root / f"{name}.gz.part{i:02d}").write_bytes(compressed[start:end])


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source")
    parser.add_argument("output")
    args = parser.parse_args()
    source = Path(args.source)
    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    for stale in output.glob("*"):
        if stale.is_file():
            stale.unlink()

    available = [c for c in CORPORA if source_present(source, c[0], c[1], c[2])]
    missing = [c[0] for c in CORPORA if c not in available]
    if missing:
        print(f"HIFZ_TAFSIR_OPTIONAL_CORPORA_MISSING: {', '.join(missing)}")

    for name, parts, encoded, expected, table in available:
        raw = read_source(source, name, parts, encoded)
        prepared = prepare_database(raw, expected, table)
        write_parts(output, name, prepared, parts)

    # Re-open generated corpora and verify both metadata and row counts.
    for name, parts, _encoded, expected, table in available:
        compressed = b"".join((output / f"{name}.gz.part{i:02d}").read_bytes() for i in range(parts))
        raw = gzip.decompress(compressed)
        with tempfile.NamedTemporaryFile(suffix=".sqlite") as tmp:
            tmp.write(raw); tmp.flush()
            db = sqlite3.connect(tmp.name)
            try:
                meta = dict(db.execute("SELECT key,value FROM source_metadata"))
                if any(meta.get(k) != v for k, v in RIGHTS.items()):
                    raise RuntimeError(f"Rights metadata mismatch for {name}")
                if db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0] != expected:
                    raise RuntimeError(f"Row-count mismatch for {name}")
            finally:
                db.close()
    print("HIFZ_TAFSIR_RELEASE_ASSETS_OK")


if __name__ == "__main__":
    main()

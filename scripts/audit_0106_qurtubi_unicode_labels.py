#!/usr/bin/env python3
"""Strict packaged-payload audit for Qurtubi 0.10.6 display labels.

Only verse-number labels that fall inside the row's indexed source range are
considered suspicious. Ordinary numbers and commentary references are ignored.
"""
from __future__ import annotations

import argparse
import base64
import gzip
import re
import sqlite3
import tempfile
from pathlib import Path

PARTS = [f"qurtubi_en.sqlite.gz.b64.part{i:02d}" for i in range(4)]
NUMBER = re.compile(r"\b(\d{1,3})\.?\s+")
OPENING = {'"', '‘', '“'}


def decode(assets: Path) -> bytes:
    encoded = "".join((assets / name).read_text(encoding="ascii") for name in PARTS)
    return gzip.decompress(base64.b64decode(encoded, validate=True))


def candidate_markers(text: str):
    for match in NUMBER.finditer(text):
        if match.end() >= len(text):
            continue
        nxt = text[match.end()]
        if nxt.isalpha() or nxt in OPENING:
            yield int(match.group(1)), match.start(), match.end()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("assets_dir", type=Path)
    args = parser.parse_args()

    blob = decode(args.assets_dir.resolve())
    tmp = tempfile.NamedTemporaryFile(suffix=".sqlite")
    tmp.write(blob)
    tmp.flush()
    con = sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)
    try:
        meta = dict(con.execute("SELECT key,value FROM source_metadata"))
        if meta.get("presentation_revision") != "0106-qurtubi-hide-verse-labels-v1":
            raise SystemExit("Qurtubi 0.10.6 presentation revision missing")

        rows = list(con.execute(
            "SELECT id,surah,verse_start,verse_end,verse_translation FROM tafsir_entry ORDER BY id"
        ))
        if len(rows) != 432:
            raise SystemExit(f"Qurtubi entry count changed: {len(rows)}")

        surviving = []
        for row_id, surah, start, end, translation in rows:
            for number, pos, _ in candidate_markers(translation):
                if start <= number <= end:
                    surviving.append(
                        (row_id, surah, start, end, number, translation[max(0,pos-20):pos+100])
                    )
        if surviving:
            raise SystemExit(
                f"Qurtubi indexed display verse labels survived: {surviving[:12]}"
            )

        checks = {
            (2, 21): "Mankind!",
            (2, 158): "Ṣafā",
        }
        for (surah, ayah), expected_start in checks.items():
            row = con.execute(
                "SELECT verse_translation FROM tafsir_entry "
                "WHERE surah=? AND verse_start<=? AND verse_end>=? ORDER BY id LIMIT 1",
                (surah, ayah, ayah),
            ).fetchone()
            if not row:
                raise SystemExit(f"Qurtubi {surah}:{ayah} missing")
            text = row[0].lstrip()
            if not text.startswith(expected_start):
                raise SystemExit(
                    f"Qurtubi {surah}:{ayah} translation changed or label survived: {text[:120]!r}"
                )

        coverage = {1: 7, 2: 286, 3: 200, 4: 22}
        for surah, last in coverage.items():
            for ayah in range(1, last + 1):
                count = con.execute(
                    "SELECT COUNT(*) FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=?",
                    (surah, ayah, ayah),
                ).fetchone()[0]
                if count < 1:
                    raise SystemExit(f"Qurtubi coverage gap at {surah}:{ayah}")

        print("Qurtubi 0.10.6 Unicode verse-label audit PASS")
        print("- 432 entries retained; coverage unchanged through 4:22")
        print("- indexed source verse labels absent from displayed translations")
        print("- ASCII and transliterated starts verified (2:21 Mankind / 2:158 Ṣafā)")
        print("- ordinary numbers and commentary references were not globally filtered")
    finally:
        con.close()
        tmp.close()


if __name__ == "__main__":
    main()

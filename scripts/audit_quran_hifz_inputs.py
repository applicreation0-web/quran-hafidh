#!/usr/bin/env python3
"""Fail-fast audit of the immutable Quran Hifz build inputs. Stdlib only."""
from __future__ import annotations
import gzip
import hashlib
import json
import re
import sqlite3
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG_DIR = ROOT / "third_party/quran-svg/mushafs/hafs/kfqc/svg-br"
GEO_DIR = ROOT / "third_party/quran-svg/mushafs/hafs/kfqc/json"
TAFSIR_DIR = ROOT / "app/src/plus/assets/tafsir"
EXPECTED_TAFSIR_SHA256 = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
EXPECTED_VERSES = 6236


def assert_exact_pages(directory: Path, suffix: str) -> None:
    expected = [f"{i:03d}{suffix}" for i in range(1, 605)]
    actual = sorted(p.name for p in directory.iterdir() if p.is_file() and re.fullmatch(rf"\d{{3}}{re.escape(suffix)}", p.name))
    assert actual == expected, f"{directory}: expected exact 001..604{suffix}, got {len(actual)} files"


def polygon_numbers(raw: str) -> list[float]:
    nums = [float(v) for v in re.findall(r"-?\d+(?:\.\d+)?", raw or "")]
    assert len(nums) >= 6 and len(nums) % 2 == 0, f"invalid polygon coordinate count: {raw[:80]!r}"
    return nums


def audit_geometry() -> None:
    assert_exact_pages(SVG_DIR, ".svg.br")
    assert_exact_pages(GEO_DIR, ".json")
    unique_verses: set[tuple[int, int]] = set()
    entries = 0
    continuation_entries = 0
    for page in range(1, 605):
        data = json.loads((GEO_DIR / f"{page:03d}.json").read_text(encoding="utf-8"))
        assert isinstance(data, list) and data, f"page {page}: empty geometry"
        for item in data:
            surah = int(item["surahNumber"])
            ayah = int(item["ayahNumber"])
            assert 1 <= surah <= 114 and ayah >= 1, f"page {page}: invalid verse {surah}:{ayah}"
            polygon_numbers(str(item["polygon"]))
            unique_verses.add((surah, ayah))
            entries += 1
            continuation_entries += int(bool(item.get("continuation", False)))
    assert len(unique_verses) == EXPECTED_VERSES, f"geometry verse coverage {len(unique_verses)} != {EXPECTED_VERSES}"
    print(f"geometry: 604 pages, {entries} entries, {len(unique_verses)} unique verses, {continuation_entries} continuations")


def audit_tafsir() -> None:
    parts = [TAFSIR_DIR / f"al_jalalayn_en.sqlite.gz.part{i:02d}" for i in range(4)]
    assert all(p.is_file() for p in parts), "Tafsir part set incomplete"
    compressed = b"".join(p.read_bytes() for p in parts)
    raw = gzip.decompress(compressed)
    sha = hashlib.sha256(raw).hexdigest()
    assert sha == EXPECTED_TAFSIR_SHA256, f"Tafsir SHA-256 mismatch: {sha}"
    with tempfile.NamedTemporaryFile(suffix=".sqlite") as tmp:
        tmp.write(raw)
        tmp.flush()
        db = sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)
        try:
            row = db.execute("SELECT value FROM source_metadata WHERE key='verse_count'").fetchone()
            assert row and int(row[0]) == EXPECTED_VERSES, f"Tafsir metadata verse_count={row!r}"
            commentary = db.execute("SELECT COUNT(*) FROM verse_commentary").fetchone()[0]
            assert commentary > 0, "Tafsir commentary table is empty"
        finally:
            db.close()
    print(f"tafsir: sha256={sha}, metadata verse_count={EXPECTED_VERSES}")


def main() -> None:
    audit_geometry()
    audit_tafsir()
    print("QURAN_HIFZ_INPUT_AUDIT=PASS")


if __name__ == "__main__":
    main()

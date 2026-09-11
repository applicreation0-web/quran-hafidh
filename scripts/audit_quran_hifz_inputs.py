#!/usr/bin/env python3
"""Fail-fast audit of the immutable Quran Hifz build inputs. Stdlib only."""
from __future__ import annotations
import gzip
import hashlib
import json
import re
import sqlite3
import statistics
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG_DIR = ROOT / "third_party/quran-svg/mushafs/hafs/kfqc/svg-br"
GEO_DIR = ROOT / "third_party/quran-svg/mushafs/hafs/kfqc/json"
TAFSIR_DIR = ROOT / "app/src/plus/assets/tafsir"
EXPECTED_TAFSIR_SHA256 = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
EXPECTED_VERSES = 6236
EXPECTED_QURAN_TEXT_ROWS = 8820
STANDARD_BOUNDARIES = [
    0.00, 41.25, 77.00, 113.25, 148.75, 184.50, 220.25, 256.50,
    292.25, 327.75, 363.75, 399.25, 435.25, 470.75, 507.00, 547.44,
]


def assert_exact_pages(directory: Path, suffix: str) -> None:
    expected = [f"{i:03d}{suffix}" for i in range(1, 605)]
    actual = sorted(p.name for p in directory.iterdir() if p.is_file() and re.fullmatch(rf"\d{{3}}{re.escape(suffix)}", p.name))
    assert actual == expected, f"{directory}: expected exact 001..604{suffix}, got {len(actual)} files"


def polygon_numbers(raw: str) -> list[float]:
    nums = [float(v) for v in re.findall(r"-?\d+(?:\.\d+)?", raw or "")]
    assert len(nums) >= 6 and len(nums) % 2 == 0, f"invalid polygon coordinate count: {raw[:80]!r}"
    return nums


def polygon_boxes(raw: str) -> list[tuple[float, float, float, float]]:
    """Parse the same M/L/Z polygon subset accepted by GeometryRepository."""
    tokens = re.findall(r"[MmLlZz]|-?\d+(?:\.\d+)?", raw or "")
    if not any(t.upper() in {"M", "L", "Z"} for t in tokens):
        nums = polygon_numbers(raw)
        points = list(zip(nums[0::2], nums[1::2]))
        xs = [p[0] for p in points]
        ys = [p[1] for p in points]
        return [(min(xs), min(ys), max(xs), max(ys))]

    boxes: list[tuple[float, float, float, float]] = []
    points: list[tuple[float, float]] = []
    i = 0
    while i < len(tokens):
        token = tokens[i].upper()
        i += 1
        if token == "M":
            if len(points) >= 3:
                xs = [p[0] for p in points]
                ys = [p[1] for p in points]
                boxes.append((min(xs), min(ys), max(xs), max(ys)))
            points = [(float(tokens[i]), float(tokens[i + 1]))]
            i += 2
        elif token == "L":
            points.append((float(tokens[i]), float(tokens[i + 1])))
            i += 2
        elif token == "Z":
            if len(points) >= 3:
                xs = [p[0] for p in points]
                ys = [p[1] for p in points]
                boxes.append((min(xs), min(ys), max(xs), max(ys)))
            points = []
        else:
            raise AssertionError(f"unsupported polygon token {token!r}")
    if len(points) >= 3:
        xs = [p[0] for p in points]
        ys = [p[1] for p in points]
        boxes.append((min(xs), min(ys), max(xs), max(ys)))
    assert boxes, "polygon has no closed geometry"
    return boxes


def standard_page_edges(data: list[dict]) -> list[float]:
    polygon_edges: list[float] = []
    for item in data:
        for _, top, _, bottom in polygon_boxes(str(item["polygon"])):
            if bottom > top:
                polygon_edges.extend((top, bottom))
    assert polygon_edges

    observed: list[float | None] = []
    for target in STANDARD_BOUNDARIES:
        near = [y for y in polygon_edges if abs(y - target) <= 12.0]
        observed.append(statistics.median(near) if near else None)
    if observed[0] is None:
        observed[0] = 0.0
    if observed[-1] is None:
        observed[-1] = 550.0

    offsets = [None if value is None else value - STANDARD_BOUNDARIES[i] for i, value in enumerate(observed)]
    known = [i for i, value in enumerate(offsets) if value is not None]
    out: list[float] = []
    for i, base in enumerate(STANDARD_BOUNDARIES):
        if offsets[i] is not None:
            offset = float(offsets[i])
        else:
            lo = max(k for k in known if k < i)
            hi = min(k for k in known if k > i)
            t = (i - lo) / (hi - lo)
            offset = float(offsets[lo]) * (1.0 - t) + float(offsets[hi]) * t
        out.append(base + offset)
    for i in range(1, len(out)):
        if out[i] <= out[i - 1]:
            out[i] = out[i - 1] + 1.0
    return out


def opening_page_edges(data: list[dict], row_count: int) -> list[float]:
    markers = [float(item["y"]) for item in data if item.get("y") is not None]
    assert markers and row_count >= 2
    low, high = min(markers), max(markers)
    assert high > low
    step = (high - low) / (row_count - 1)
    centers = [low + step * i for i in range(row_count)]
    return [centers[0] - step * 0.5] + [
        (centers[i - 1] + centers[i]) * 0.5 for i in range(1, row_count)
    ] + [centers[-1] + step * 0.5]


def derive_quran_rows(page: int, data: list[dict]) -> dict[int, set[tuple[int, int]]]:
    if page == 1:
        edges, physical_start = opening_page_edges(data, 7), 2
    elif page == 2:
        edges, physical_start = opening_page_edges(data, 6), 3
    else:
        edges, physical_start = standard_page_edges(data), 1

    rows: dict[int, set[tuple[int, int]]] = {}
    for index, (top, bottom) in enumerate(zip(edges, edges[1:])):
        line_height = bottom - top
        assert line_height > 0
        verses: set[tuple[int, int]] = set()
        for item in data:
            verse = (int(item["surahNumber"]), int(item["ayahNumber"]))
            for _, p_top, _, p_bottom in polygon_boxes(str(item["polygon"])):
                overlap = min(bottom, p_bottom) - max(top, p_top)
                threshold = max(1.5, min(line_height, p_bottom - p_top) * 0.35)
                if overlap >= threshold:
                    verses.add(verse)
                    break
        if verses:
            rows[physical_start + index] = verses
    return rows


def assert_line_golden(rows_by_page: dict[int, dict[int, set[tuple[int, int]]]]) -> None:
    expected = {
        1: {
            2: {(1, 1)}, 3: {(1, 2)}, 4: {(1, 3), (1, 4)},
            5: {(1, 5), (1, 6)}, 6: {(1, 6), (1, 7)}, 7: {(1, 7)}, 8: {(1, 7)},
        },
        2: {
            3: {(2, 1), (2, 2)}, 4: {(2, 2), (2, 3)}, 5: {(2, 3), (2, 4)},
            6: {(2, 4)}, 7: {(2, 5)}, 8: {(2, 5)},
        },
        3: {
            1: {(2, 6)}, 2: {(2, 6), (2, 7)}, 3: {(2, 7), (2, 8)}, 4: {(2, 8)},
            5: {(2, 9)}, 6: {(2, 9), (2, 10)}, 7: {(2, 10), (2, 11)},
            8: {(2, 11), (2, 12)}, 9: {(2, 12), (2, 13)}, 10: {(2, 13)},
            11: {(2, 13), (2, 14)}, 12: {(2, 14)}, 13: {(2, 14), (2, 15)},
            14: {(2, 15), (2, 16)}, 15: {(2, 16)},
        },
        48: {line: {(2, 282)} for line in range(1, 16)},
        100: {
            1: {(4, 135)}, 2: {(4, 135)}, 3: {(4, 135)}, 4: {(4, 135), (4, 136)},
            5: {(4, 136)}, 6: {(4, 136)}, 7: {(4, 136)}, 8: {(4, 136), (4, 137)},
            9: {(4, 137)}, 10: {(4, 137), (4, 138), (4, 139)}, 11: {(4, 139)},
            12: {(4, 139), (4, 140)}, 13: {(4, 140)}, 14: {(4, 140)}, 15: {(4, 140)},
        },
        604: {
            3: {(112, 1), (112, 2), (112, 3)}, 4: {(112, 4)},
            7: {(113, 1), (113, 2), (113, 3)}, 8: {(113, 3), (113, 4)}, 9: {(113, 5)},
            12: {(114, 1), (114, 2), (114, 3)}, 13: {(114, 3), (114, 4), (114, 5)},
            14: {(114, 5)}, 15: {(114, 6)},
        },
    }
    for page, golden in expected.items():
        assert rows_by_page[page] == golden, f"page {page}: physical Quran-line regression"


def audit_geometry() -> None:
    assert_exact_pages(SVG_DIR, ".svg.br")
    assert_exact_pages(GEO_DIR, ".json")
    unique_verses: set[tuple[int, int]] = set()
    line_verse_coverage: set[tuple[int, int]] = set()
    rows_by_page: dict[int, dict[int, set[tuple[int, int]]]] = {}
    entries = 0
    continuation_entries = 0
    quran_text_rows = 0
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

        rows = derive_quran_rows(page, data)
        assert rows, f"page {page}: no Quran text rows"
        if page > 2:
            assert all(1 <= line <= 15 for line in rows), f"page {page}: row outside 1..15"
        rows_by_page[page] = rows
        quran_text_rows += len(rows)
        for verses in rows.values():
            line_verse_coverage.update(verses)

    assert len(unique_verses) == EXPECTED_VERSES, f"geometry verse coverage {len(unique_verses)} != {EXPECTED_VERSES}"
    assert line_verse_coverage == unique_verses, "physical line grid does not cover every Quran verse"
    assert quran_text_rows == EXPECTED_QURAN_TEXT_ROWS, (
        f"physical Quran text rows {quran_text_rows} != {EXPECTED_QURAN_TEXT_ROWS}"
    )
    assert_line_golden(rows_by_page)
    print(
        f"geometry: 604 pages, {entries} entries, {len(unique_verses)} unique verses, "
        f"{quran_text_rows} Quran text rows, {continuation_entries} continuations"
    )


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
            assert commentary == EXPECTED_VERSES, f"Tafsir commentary rows={commentary}"
        finally:
            db.close()
    print(f"tafsir: sha256={sha}, metadata verse_count={EXPECTED_VERSES}")


def main() -> None:
    audit_geometry()
    audit_tafsir()
    print("QURAN_HIFZ_INPUT_AUDIT=PASS")


if __name__ == "__main__":
    main()

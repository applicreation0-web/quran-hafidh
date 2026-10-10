#!/usr/bin/env python3
"""Safely extract *coordinate-only* Quranic recall cues from authenticated V2.1.

A migration tool, not an Android engine. Its output contains no Quran word text,
tafsir commentary, Arabic/French editorial titles, or old tafsir source names.
Legacy SP identifiers are retained only as compatibility aliases; historical
interval end-points are retained to keep the existing Hifz session APIs stable.

Input is intentionally historical; the release APK MUST NOT bundle its inputs.
"""
from __future__ import annotations

import hashlib
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

EXPECTED_SOURCE_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
GLOBAL_COUNT = 1256
CUE_COUNT = 1243
RECORD_COUNT = 1644
WORD_COUNT = 77432
INTERVAL = re.compile(r"^(\d+):(\d+)[–-](\d+)(?:\s+.*)?$")

def ensure(ok: bool, reason: str) -> None:
    if not ok:
        raise SystemExit("Quran cue extraction rejected: " + reason)

def positive(row: dict, key: str) -> int:
    n = row.get(key)
    ensure(type(n) is int and n > 0, f"invalid {key} on {row.get('passage_global_id')}")
    return n

def trueish(v: object) -> bool:
    return v is True or v == 1 or (isinstance(v, str) and v.strip().upper() in ("YES", "TRUE", "1"))

def canonical_range(row: dict) -> tuple[int, int, int]:
    # Historical grouping consulted only during conversion; NOT emitted.
    pid = row["passage_global_id"]
    if pid in ("SP1255", "SP1256"):
        return (114, 1, 6)  # audited historical last-surah correction
    m = INTERVAL.match(str(row.get("tafsir_munir_grouping", "")))
    ensure(m is not None, f"missing verified legacy interval for {pid}")
    surah, first, last = map(int, m.groups()[:3])
    ensure(1 <= surah <= 114 and 1 <= first <= last <= 286,
           f"invalid historical Quran coordinates for {pid}")
    return surah, first, last

def extract(source: Path, word_dir: Path) -> dict:
    raw = source.read_bytes()
    ensure(hashlib.sha256(raw).hexdigest() == EXPECTED_SOURCE_SHA256,
           "frozen V2.1 input SHA mismatch")
    data = json.loads(raw.decode("utf-8"))
    ensure(data.get("schema_version") == "V2.1" and data.get("boundaries_changed") is False,
           "unverified source schema")
    globals_ = data.get("global_passages", [])
    records = data.get("page_passage_records", [])
    ensure(len(globals_) == GLOBAL_COUNT and len(records) == RECORD_COUNT,
           "historical source record counts changed")

    # Validate actual quran-ws geometry on all pages before emitting coordinates.
    pages: dict[int, list[str]] = {}
    for suffix in ("001-150", "151-300", "301-450", "451-604"):
        obj = json.loads((word_dir / f"word-boxes-{suffix}.json").read_text("utf-8"))
        ensure(obj.get("schema") == "quran-haafidh-word-boxes-v1"
               and obj.get("source_release") == "v1.1.2"
               and obj.get("box_space") == "viewBox 0 0 345 550", "wrong word geometry source")
        for p, rows in obj["pages"].items():
            page = int(p)
            ensure(page not in pages and 1 <= page <= 604, "duplicated or invalid geometry page")
            ensure(all(isinstance(row, list) and len(row) == 5 for row in rows),
                   "invalid word geometry row")
            pages[page] = [row[0] for row in rows]
    ensure(set(pages) == set(range(1, 605)), "Mushaf word geometry pages incomplete")
    total_words = sum(map(len, pages.values()))
    ensure(total_words == WORD_COUNT, f"unexpected Quran word box count: {total_words}")

    groups: dict[tuple[int,int,int], dict] = {}
    representative: dict[str, dict] = {}
    legacy_id_to_range: dict[str, tuple[int,int,int]] = {}
    for number, row in enumerate(globals_, 1):
        pid = row.get("passage_global_id")
        ensure(pid == f"SP{number:04d}", "legacy ID set or order changed")
        ensure(row.get("minimality_verified_v2_1") == "AUDITED_V2_1",
               f"unaudited historical source {pid}")
        surah, first, last = canonical_range(row)
        ensure(positive(row, "surah_start") == surah
               and first <= positive(row, "ayah_start") <= last
               and positive(row, "surah_end") == surah
               and first <= positive(row, "ayah_end") <= last,
               f"historical segment escapes its Quran coordinates: {pid}")
        key = (surah, first, last)
        legacy_id_to_range[pid] = key
        if key not in groups:
            groups[key] = {
                "id": pid, "surah": surah,
                "start_ayah": first, "end_ayah": last,
                "start_page": positive(row, "starts_on_page"),
                "end_page": positive(row, "ends_on_page"),
                "aliases": [], "page_occurrences": [],
            }
            representative[pid] = groups[key]
        c = groups[key]
        c["aliases"].append(pid)
        c["start_page"] = min(c["start_page"], positive(row, "starts_on_page"))
        c["end_page"] = max(c["end_page"], positive(row, "ends_on_page"))
    ensure(len(groups) == CUE_COUNT, f"expected {CUE_COUNT} real Quranic cue positions")
    ensure(len(legacy_id_to_range) == GLOBAL_COUNT, "legacy alias set incomplete")

    on_page: dict[str, dict] = {}
    page_coverage: set[int] = set()
    seen_records: set[str] = set()
    seen_page_group: set[tuple[int,str]] = set()
    for row in records:
        pid = row["passage_global_id"]
        ensure(pid in legacy_id_to_range, f"unknown old ID {pid}")
        page = positive(row, "page")
        ensure(1 <= page <= 604, f"invalid page {page}")
        page_coverage.add(page)
        seen_records.add(pid)
        c = groups[legacy_id_to_range[pid]]
        ensure(c["start_page"] <= page <= c["end_page"], "page outside verified interval")
        page_key = page, c["id"]
        if page_key not in seen_page_group:
            seen_page_group.add(page_key)
            c["page_occurrences"].append({
                "page": page, "index": positive(row, "passage_index_on_page")})
        if pid == c["id"] and trueish(row.get("anchor_is_on_current_page")):
            ensure(pid not in on_page, f"duplicate representative cue {pid}")
            ensure(page == c["start_page"], f"non-start-page anchor {pid}")
            ensure(row.get("minimality_verified_v2_1") == "AUDITED_V2_1",
                   f"unaudited cue occurrence {pid}")
            word_count = positive(row, "anchor_word_count_v2_1")
            keys = pages[page]
            start_key = f"{c['surah']}:{c['start_ayah']}:1"
            ensure(start_key in keys, f"start word absent from canonical geometry {pid}")
            at = keys.index(start_key)
            ensure(at + word_count <= len(keys), f"cue overflows source page {pid}")
            selected = keys[at:at + word_count]
            ensure(len(selected) == word_count and len(set(selected)) == word_count,
                   f"inconsistent word sequence {pid}")
            on_page[pid] = {
                "word_count": word_count,
                "start_line": positive(row, "start_line"),
                "first_word_id": positive(row, "first_word_id"),
                "last_word_id": positive(row, "last_word_id"),
                "first_word_position": positive(row, "first_word_position"),
                "last_word_position": positive(row, "last_word_position"),
            }
    ensure(page_coverage == set(range(1, 605)), "historical page coverage changed")
    ensure(len(seen_records) == GLOBAL_COUNT, "legacy passage-to-page coverage changed")
    ensure(len(on_page) == CUE_COUNT, f"only {len(on_page)} exact start-cues found")

    ordered = sorted(groups.values(), key=lambda c: (c["surah"], c["start_ayah"]))
    for c in ordered:
        ensure(c["id"] in on_page, f"missing cue for {c['id']}")
        c.update(on_page[c["id"]])
        c["page_occurrences"].sort(key=lambda x: x["page"])
        ensure(c["page_occurrences"][0]["page"] == c["start_page"],
               f"canonical start page missing for {c['id']}")
    # Historical source passes contain adjacent Quran intervals. Check boundaries,
    # without creating or altering any segmentation.
    per_surah: dict[int, list[dict]] = defaultdict(list)
    for c in ordered:
        per_surah[c["surah"]].append(c)
    ensure(set(per_surah) == set(range(1, 115)), "canonical surah coverage incomplete")
    for surah, items in per_surah.items():
        ensure(items[0]["start_ayah"] == 1, f"surah {surah} does not start at ayah 1")
        for previous, current in zip(items, items[1:]):
            ensure(current["start_ayah"] == previous["end_ayah"] + 1,
                   f"cue coordinate gap/overlap in surah {surah}")
    result = {
        "schema_version": "QURANIC_CUES_COORDINATES_V1",
        "source_sha256": EXPECTED_SOURCE_SHA256,
        "cue_count": CUE_COUNT,
        "legacy_alias_count": GLOBAL_COUNT,
        "page_count": 604,
        "word_box_count_verified": WORD_COUNT,
        "cues": ordered,
    }
    encoded = json.dumps(result, ensure_ascii=False, separators=(",", ":"))
    ensure(not any(keyword in encoded.lower() for keyword in (
        "munir", "islahi", "tafsir_", "title_", "arabic", "fr_v2", "commentary")),
        "editorial data leaked into Quran coordinates asset")
    return result

def main() -> None:
    ensure(len(sys.argv) == 4,
           "usage: extract_quranic_cues_v1.py V21_JSON WORD_GEOMETRY_DIR OUTPUT_JSON")
    result = extract(Path(sys.argv[1]), Path(sys.argv[2]))
    output = Path(sys.argv[3])
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, separators=(",", ":")) + "\n",
                      encoding="utf-8")
    print(f"PASS: {len(result['cues'])} coordinate-only cues, "
          f"{result['legacy_alias_count']} old-ID aliases, "
          f"{result['word_box_count_verified']} verified real Quran words; "
          f"SHA256={hashlib.sha256(output.read_bytes()).hexdigest()}")

if __name__ == "__main__":
    main()

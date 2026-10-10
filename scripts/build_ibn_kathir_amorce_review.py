#!/usr/bin/env python3
"""Build a STRICT Quran-coordinate *review queue* for Ibn Kathir groups.

Not a mnemonic engine; it NEVER guesses a memorable length or semantic cue.
All begin/end coordinates come from the already SHA-fenced 1,903-group index.
All Mushaf positions come from the pinned 604-page, 77,432-word geometry.
No tafsir commentary or previous Al-Munir segmentation is used.
"""
from __future__ import annotations

import argparse
import base64
import hashlib
import json
import re
import zlib
from pathlib import Path

GROUPS = 1903
VERSES = 6236
PAGES = 604
WORDS = 77432
CHUNKS = ("001-150", "151-300", "301-450", "451-604")

def require(condition, reason):
    if not condition:
        raise ValueError("Ibn Kathir cue evidence invalid: " + reason)

def read_groups(java: Path):
    source = java.read_text(encoding="utf-8")
    found = re.search(r'BOUNDARIES_ZLIB_BASE64\s*=\s*"([A-Za-z0-9+/=]+)"', source)
    expected = re.search(r'ENDPOINTS_SHA256\s*=\s*"([0-9a-f]{64})"', source)
    require(found is not None and expected is not None, "source index/header not found")
    endpoints = zlib.decompress(base64.b64decode(found.group(1), validate=True))
    digest = hashlib.sha256(endpoints).hexdigest()
    require(digest == expected.group(1), "documentary index SHA-256 mismatch")
    lines = endpoints.decode("utf-8").strip().splitlines()
    require(len(lines) == 114, "expected 114 surah index rows")
    groups = []
    for surah, line in enumerate(lines, start=1):
        k, sep, last = line.partition(":")
        require(sep == ":" and k == str(surah), f"out-of-order surah {surah}")
        first = 1
        for token in last.split(","):
            end = int(token)
            require(first <= end <= 286, f"invalid verse bounds {surah}:{first}-{end}")
            groups.append((surah, first, end))
            first = end + 1
    require(len(groups) == GROUPS, "index contains unexpected group count")
    require(sum(end - start + 1 for _, start, end in groups) == VERSES,
            "index has gaps or overlaps")
    return groups, digest

def geometry_index(folder: Path):
    first_positions = {}
    verse_last_page = {}
    unique_words = set()
    seen_pages = set()
    total = 0
    for chunk in CHUNKS:
        path = folder / f"word-boxes-{chunk}.json"
        data = json.loads(path.read_text(encoding="utf-8"))
        require(data.get("schema") == "quran-haafidh-word-boxes-v1"
                and data.get("source_release") == "v1.1.2"
                and data.get("box_space") == "viewBox 0 0 345 550",
                "unexpected geometry source " + path.name)
        for p, boxes in data["pages"].items():
            page = int(p)
            require(1 <= page <= PAGES and page not in seen_pages,
                    f"duplicate/invalid Mushaf page {page}")
            seen_pages.add(page)
            for box in boxes:
                require(isinstance(box, list) and len(box) == 5,
                        f"invalid word box on page {page}")
                key = box[0]
                parts = key.split(":")
                require(len(parts) == 3 and all(v.isdecimal() for v in parts),
                        f"invalid Quranic word key {key!r}")
                require(key not in unique_words, f"duplicate Quranic word {key}")
                unique_words.add(key)
                total += 1
                s, a, word = map(int, parts)
                require(s >= 1 and s <= 114 and a >= 1 and word >= 1,
                        f"invalid word coordinates {key}")
                if word == 1:
                    require((s, a) not in first_positions,
                            f"duplicated first Quran word {key}")
                    first_positions[(s, a)] = page
                verse_last_page[(s, a)] = max(page, verse_last_page.get((s, a), 0))
    require(seen_pages == set(range(1, PAGES + 1)), "604-page geometry incomplete")
    require(total == WORDS, f"expected {WORDS} Quran word boxes, got {total}")
    require(len(first_positions) == VERSES, "first words missing from canonical Quran verses")
    return first_positions, verse_last_page

def build(index_path: Path, words_folder: Path):
    groups, boundary_sha = read_groups(index_path)
    first_word_page, end_word_page = geometry_index(words_folder)
    review = []
    for surah, start, end in groups:
        page = first_word_page.get((surah, start))
        last = end_word_page.get((surah, end))
        require(page is not None and last is not None and 1 <= page <= last <= PAGES,
                f"missing exact Quran geometry at {surah}:{start}-{end}")
        identifier = f"IKEN{surah:03d}_{start:03d}_{end:03d}"
        review.append({
            "id": identifier,
            "surah": surah,
            "start_ayah": start,
            "end_ayah": end,
            "start_page": page,
            "end_page": last,
            "first_quran_word_key": f"{surah}:{start}:1",
            "verified_boundary": True,
            "verified_start_geometry": True,
            "candidate_anchor_word_count": None,
            "selection_status": "NOT_REVIEWED",
        })
    require(len({r["id"] for r in review}) == GROUPS, "duplicate Ibn Kathir IDs")
    return {
        "schema_version": "IK_QURANIC_AMORCE_REVIEW_V1",
        "runtime_ready": False,
        "source_index_sha256": boundary_sha,
        "source_geometry": "quran-ws-v1.1.2",
        "groups": GROUPS,
        "covered_verses": VERSES,
        "verified_word_boxes": WORDS,
        "verified_604_pages": True,
        "selected_anchors": 0,
        "comment": "Only group boundaries and exact first-word positions are verified; anchor words/lengths need independent minimality review.",
        "candidates": review,
    }

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("group_index_java", type=Path)
    parser.add_argument("word_geometry_dir", type=Path)
    parser.add_argument("output_json", type=Path)
    args = parser.parse_args()
    doc = build(args.group_index_java, args.word_geometry_dir)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")) + "\n",
                                encoding="utf-8")
    require(all(c["selection_status"] == "NOT_REVIEWED"
                and c["candidate_anchor_word_count"] is None for c in doc["candidates"]),
            "unverified keys must never be marked ready")
    print(f"PASS: {GROUPS} indexed Ibn Kathir blocks on the verified QCF word geometry; "
          "0 invented semantic keys; runtime NOT READY.")

if __name__ == "__main__":
    main()

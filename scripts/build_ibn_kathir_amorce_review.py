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
QURAN_TEXT_GIT_BLOB = "fcc214311897921d08bbc56879b82aa38328a5f3"

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

def load_verified_verse_text(path: Path, groups):
    # The locally pinned Tanzil text is for editorial checking ONLY.
    # It must NEVER be treated as an exact KFQC word-to-box alignment.
    raw = path.read_bytes()
    git_sha = hashlib.sha1(f"blob {len(raw)}\0".encode("ascii") + raw).hexdigest()
    require(git_sha == QURAN_TEXT_GIT_BLOB,
            "Tanzil reference text changed without Quranic-source audit")
    lines = raw.decode("utf-8-sig").splitlines()
    require(len(lines) == VERSES and all(x.strip() for x in lines),
            "expected 6236 nonempty ayah source lines")
    last_by_surah = {}
    for surah, start, end in groups:
        last_by_surah[surah] = end
    require(len(last_by_surah) == 114 and sum(last_by_surah.values()) == VERSES,
            "source text does not match canonical 114-surah verse lengths")
    # In this source the basmala is prefixed to ayah 1 on 112 surahs
    # although KFQC does not count it as that ayah's first words.
    # Do NOT permit "bismillah" to masquerade as the Ibn Kathir recall cue.
    basmala = lines[0]
    special_basmala = basmala.replace("بِسۡمِ", "بِّسۡمِ", 1)
    require(special_basmala != basmala, "unexpected Tanzil basmala spelling")
    text_by_ayah = {}
    stripped_preambles = 0
    at = 0
    for surah in range(1, 115):
        for ayah in range(1, last_by_surah[surah] + 1):
            line = lines[at]
            at += 1
            if ayah == 1 and surah not in (1, 9):
                found = next((prefix for prefix in (basmala, special_basmala)
                              if line.startswith(prefix + " ")), None)
                require(found is not None, f"unrecognized basmala prelude in {surah}:1")
                line = line[len(found):].strip()
                stripped_preambles += 1
            require(bool(line), f"missing canonical Quran text at {surah}:{ayah}")
            text_by_ayah[(surah, ayah)] = line
    require(at == VERSES and stripped_preambles == 112,
            "incorrect canonical text/basmala segmentation")
    require(text_by_ayah[(50, 1)].startswith("قۤۚ "),
            "Qaf 50:1 must begin with Qaf, not the surah-level basmala")
    require(text_by_ayah[(9, 1)] == lines[sum(last_by_surah[s] for s in range(1,9))],
            "At-Tawbah's first ayah must remain unprefixed")
    return text_by_ayah, git_sha


def build(index_path: Path, words_folder: Path, verse_text_path: Path):
    groups, boundary_sha = read_groups(index_path)
    first_word_page, end_word_page = geometry_index(words_folder)
    text_by_ayah, text_sha = load_verified_verse_text(verse_text_path, groups)
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
            "full_start_ayah_text_for_review": text_by_ayah[(surah, start)],
            "review_text_warning": "TANZIL_LINE_NOT_KFQC_WORD_ALIGNED",
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
        "reference_verse_text_source": "scripts/data/quran-uthmani-tanzil.txt",
        "reference_verse_text_git_blob": text_sha,
        "reference_verse_text_is_kfqc_word_aligned": False,
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
    parser.add_argument("verse_text_for_review", type=Path)
    parser.add_argument("output_json", type=Path)
    args = parser.parse_args()
    doc = build(args.group_index_java, args.word_geometry_dir, args.verse_text_for_review)
    args.output_json.parent.mkdir(parents=True, exist_ok=True)
    args.output_json.write_text(json.dumps(doc, ensure_ascii=False, separators=(",", ":")) + "\n",
                                encoding="utf-8")
    require(all(c["selection_status"] == "NOT_REVIEWED"
                and c["candidate_anchor_word_count"] is None
                and c["full_start_ayah_text_for_review"].strip()
                and c["review_text_warning"] == "TANZIL_LINE_NOT_KFQC_WORD_ALIGNED"
                for c in doc["candidates"]),
            "unverified keys must never be marked ready")
    print(f"PASS: {GROUPS} indexed Ibn Kathir blocks on the verified QCF word geometry; "
          "0 invented semantic keys; runtime NOT READY.")

if __name__ == "__main__":
    main()

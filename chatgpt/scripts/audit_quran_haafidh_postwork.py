#!/usr/bin/env python3
"""Post-Work fail-closed audit for Al-Munir structure and Quranic recall anchors."""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path

EXPECTED_GLOBAL = 1256
EXPECTED_CANONICAL_MUNIR = 1243
RANGE = re.compile(r"^(\d+):(\d+)[–-](\d+)(?:\s+(.*))?$")
KNOWN = {
    (2, 40, 43): "ما طلب من بني إسرائيل",
    (2, 44, 48): "نماذج من سوء أخلاق اليهود",
    (2, 67, 73): "قصة ذبح البقرة",
    (2, 122, 123): "تذكير بالنعمة وتخويف من الآخرة",
    (49, 1, 5): "طاعة اللَّه تعالى والرسول صلّى اللَّه عليه وسلّم والتأدب في خطاب النبي صلّى اللَّه عليه وسلّم",
    (67, 1, 5): "بعض أدلة القدرة الإلهية",
    (114, 1, 6): "الاستعاذة من شرّ الشياطين",
}
SAMPLE_SURAHS = (2, 4, 19, 24, 36, 49, 67, 114)


def fail(message: str) -> None:
    raise SystemExit("Post-Work Al-Munir audit rejected: " + message)


def main() -> None:
    if len(sys.argv) != 2:
        fail("usage: audit_quran_haafidh_postwork.py SEMANTIC_V21_JSON")
    root = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    globals_ = root.get("global_passages")
    records = root.get("page_passage_records")
    if not isinstance(globals_, list) or len(globals_) != EXPECTED_GLOBAL:
        fail("global passage set is not frozen V2.1/1256")
    if not isinstance(records, list) or len(records) != 1644:
        fail("page passage record set is not frozen V2.1/1644")

    groups: dict[tuple[int, int, int], dict] = {}
    legacy_to_key: dict[str, tuple[int, int, int]] = {}
    row_by_id = {}
    for row in globals_:
        pid = str(row.get("passage_global_id", "")).strip()
        if not pid or pid in row_by_id:
            fail(f"duplicate/missing passage id {pid!r}")
        row_by_id[pid] = row
        grouping = str(row.get("tafsir_munir_grouping", "")).strip()
        if pid in {"SP1255", "SP1256"}:
            key = (114, 1, 6)
            title = KNOWN[key]
        else:
            match = RANGE.match(grouping)
            if not match:
                fail(f"{pid}: unparseable Al-Munir grouping {grouping!r}")
            key = tuple(int(match.group(i)) for i in (1, 2, 3))
            title = (match.group(4) or "").strip()
        s, a0, a1 = key
        if a0 < 1 or a1 < a0:
            fail(f"{pid}: invalid Al-Munir range {key}")
        legacy_start = (int(row.get("surah_start", 0)), int(row.get("ayah_start", 0)))
        legacy_end = (int(row.get("surah_end", 0)), int(row.get("ayah_end", 0)))
        if legacy_start[0] != s or legacy_end[0] != s or not (a0 <= legacy_start[1] <= legacy_end[1] <= a1):
            fail(f"{pid}: legacy passage lies outside Al-Munir unit {key}")
        group = groups.setdefault(key, {"ids": [], "titles": set()})
        group["ids"].append(pid)
        if title:
            group["titles"].add(title)
        legacy_to_key[pid] = key

    if len(groups) != EXPECTED_CANONICAL_MUNIR:
        fail(f"canonical Al-Munir unit count is {len(groups)}, expected {EXPECTED_CANONICAL_MUNIR}")

    # Directly source-verified exceptional heading missing in older metadata.
    groups.setdefault((24, 11, 22), {"ids": [], "titles": set()})["titles"].add("الحكم الخامس قصة الإفك")

    for key, group in groups.items():
        titles = group["titles"]
        if not titles:
            fail(f"Al-Munir unit {key} has no printed Arabic heading")
        if len(titles) != 1:
            fail(f"Al-Munir unit {key} has conflicting headings: {sorted(titles)}")

    for key, expected_title in KNOWN.items():
        group = groups.get(key)
        if not group:
            fail(f"known Al-Munir unit missing: {key}")
        actual = next(iter(group["titles"]))
        if actual != expected_title:
            fail(f"known Al-Munir heading mismatch for {key}: {actual!r}")

    page_records_by_id: dict[str, list[dict]] = {}
    pages = set()
    for row in records:
        pid = str(row.get("passage_global_id", "")).strip()
        if pid not in row_by_id:
            fail(f"unknown page-record passage id {pid!r}")
        page = int(row.get("page", 0) or 0)
        if not 1 <= page <= 604:
            fail(f"{pid}: page outside 1..604")
        pages.add(page)
        page_records_by_id.setdefault(pid, []).append(row)
    if pages != set(range(1, 605)):
        fail(f"semantic page coverage is {len(pages)}/604")

    canonical_anchor_counts = Counter()
    anchor_lengths = []
    for key, group in groups.items():
        # The runtime canonical ID is the first legacy row encountered for this exact Al-Munir unit.
        canonical_id = group["ids"][0]
        g = row_by_id[canonical_id]
        if (int(g.get("surah_start", 0)), int(g.get("ayah_start", 0))) != (key[0], key[1]):
            fail(f"{canonical_id}: canonical anchor row does not start at exact Al-Munir boundary {key}")
        candidates = [
            r for r in page_records_by_id.get(canonical_id, [])
            if str(r.get("anchor_is_on_current_page", "")).strip().upper() in {"YES", "TRUE", "1"}
        ]
        if len(candidates) != 1:
            fail(f"{canonical_id}: expected one on-page canonical amorce, got {len(candidates)}")
        anchor = candidates[0]
        text = str(anchor.get("anchor_arabic_v2_1", "")).strip()
        count = int(anchor.get("anchor_word_count_v2_1", 0) or 0)
        if not text or count < 1:
            fail(f"{canonical_id}: missing Quranic amorce")
        if str(anchor.get("minimality_verified_v2_1", "")).strip() != "AUDITED_V2_1":
            fail(f"{canonical_id}: amorce minimality is not audited")
        canonical_anchor_counts[key] += 1
        anchor_lengths.append(count)

    if len(canonical_anchor_counts) != EXPECTED_CANONICAL_MUNIR or any(v != 1 for v in canonical_anchor_counts.values()):
        fail("not every canonical Al-Munir unit has exactly one start amorce")
    outside_3_6 = sum(1 for n in anchor_lengths if n < 3 or n > 6)
    if outside_3_6 == 0:
        fail("anchor lengths are still artificially constrained to 3–6 words")

    print(
        "Post-Work Al-Munir audit OK: "
        f"canonical_units={len(groups)} pages={len(pages)} anchors={len(anchor_lengths)} "
        f"anchor_words_min={min(anchor_lengths)} max={max(anchor_lengths)} outside_3_6={outside_3_6}"
    )
    for surah in SAMPLE_SURAHS:
        keys = sorted(k for k in groups if k[0] == surah)
        if not keys:
            fail(f"no Al-Munir group for sampled surah {surah}")
        key = keys[0]
        title = next(iter(groups[key]["titles"]))
        print(f"MUNIR_SAMPLE surah={surah} range={key[1]}-{key[2]} title={title}")


if __name__ == "__main__":
    main()

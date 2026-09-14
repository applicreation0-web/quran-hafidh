#!/usr/bin/env python3
"""Adversarial checks against the exact generated Medina Mushaf geometry used by Hifz."""

from __future__ import annotations

import json
from collections import defaultdict
from pathlib import Path

GEOMETRY = Path("app/src/main/assets/reader109/geometry.json")
REPORT = Path("geometry-report.json")


def verse_key(value: str) -> tuple[int, int]:
    surah, ayah = value.split(":")
    return int(surah), int(ayah)


def main() -> None:
    root = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    assert root.get("schema") == 1
    pages = root["pages"]
    assert len(pages) == 604

    verse_lines: dict[str, set[str]] = defaultdict(set)
    verse_pages: dict[str, set[int]] = defaultdict(set)
    all_line_ids: set[str] = set()

    for page_text, page in pages.items():
        page_number = int(page_text)
        lines = page["lines"]
        for line in lines:
            line_id = line["id"]
            assert line_id not in all_line_ids, f"duplicate geometry line id: {line_id}"
            all_line_ids.add(line_id)
            assert int(line["page"]) == page_number
            for verse in line["verses"]:
                verse_key(verse)
                verse_lines[verse].add(line_id)
                verse_pages[verse].add(page_number)

    multi_page_verses = {verse: sorted(ps) for verse, ps in verse_pages.items() if len(ps) > 1}
    assert not multi_page_verses, f"verses span multiple Mushaf pages: {multi_page_verses}"

    # Explicit 10.10 boundary case: Al-Hujurat starts in the middle of a Mushaf page.
    start = "49:1"
    assert start in verse_pages and verse_pages[start]
    start_page = min(verse_pages[start])
    page_verses = {
        verse
        for line in pages[str(start_page)]["lines"]
        for verse in line["verses"]
    }
    assert any(verse_key(v) < verse_key(start) for v in page_verses), (
        "49:1 no longer proves a mid-page start in the generated corpus"
    )
    assert start in page_verses

    # Filtering proof: off-target verses on the same physical page must never become
    # active merely because the whole page is displayed.
    target_start = verse_key("49:1")
    target_end = verse_key("49:18")
    active_lines = []
    off_target_visible = set()
    for line in pages[str(start_page)]["lines"]:
        target_verses = [
            v for v in line["verses"]
            if target_start <= verse_key(v) <= target_end
        ]
        if target_verses:
            active_lines.append({"id": line["id"], "target": target_verses})
        off_target_visible.update(
            v for v in line["verses"] if verse_key(v) < target_start
        )
    assert active_lines
    assert off_target_visible, "mid-page preceding verses unexpectedly absent"
    assert all(
        all(target_start <= verse_key(v) <= target_end for v in item["target"])
        for item in active_lines
    )

    # The long-ayah regression anchor must be present in the real corpus. The synthetic
    # 15-line test exercises the exact 5+5+5 policy even if this edition maps 2:282 to a
    # different number of physical geometry lines.
    long_anchor = "2:282"
    assert long_anchor in verse_lines and verse_lines[long_anchor]

    ranked = sorted(
        ((len(lines), verse, sorted(verse_pages[verse])) for verse, lines in verse_lines.items()),
        reverse=True,
    )
    longest_count, longest_verse, longest_pages = ranked[0]

    report = {
        "schema": 1,
        "page_count": len(pages),
        "unique_line_count": len(all_line_ids),
        "multi_page_verse_count": len(multi_page_verses),
        "hujurat_start_page": start_page,
        "hujurat_preceding_visible_verses": sorted(off_target_visible, key=verse_key),
        "hujurat_active_line_count": len(active_lines),
        "2_282_real_line_count": len(verse_lines[long_anchor]),
        "2_282_pages": sorted(verse_pages[long_anchor]),
        "longest_verse_by_geometry_lines": longest_verse,
        "longest_verse_line_count": longest_count,
        "longest_verse_pages": longest_pages,
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, sort_keys=True))


if __name__ == "__main__":
    main()

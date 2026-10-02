#!/usr/bin/env python3
"""Validate and import the audited semantic-passages corpus into Quran Haafidh.

This script is intentionally conservative:
- it never invents semantic boundaries;
- it never derives approximate word boxes from Mushaf line cells;
- it rejects the obsolete fixed 3–6-word anchor rule;
- it writes a runtime asset only after structural checks pass.

Exact visual_ranges are optional for normal Lecture markers. Révision active keeps its legacy
first/last-line landmarks on any page whose anchors lack exact visual ranges.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ASSET_RELATIVE = Path("hifz-app/src/main/assets/semantic/semantic_passages_v2_1.json")
REPORT_RELATIVE = Path("semantic-import-report.json")


def yes(value: Any) -> bool:
    if isinstance(value, bool):
        return value
    if isinstance(value, (int, float)):
        return value != 0
    return str(value or "").strip().upper() in {"YES", "TRUE", "1"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit("semantic import rejected: " + message)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def normalize_root(raw: Any) -> dict[str, Any]:
    require(isinstance(raw, dict), "root must be a JSON object")
    records = raw.get("page_passage_records")
    require(isinstance(records, list) and records, "page_passage_records missing or empty")
    return raw


def validate_record(row: dict[str, Any], index: int) -> None:
    prefix = f"record {index}"
    page = int(row.get("page", 0) or 0)
    require(1 <= page <= 604, f"{prefix}: page outside 1..604")
    passage_id = str(row.get("passage_global_id", "")).strip()
    require(bool(passage_id), f"{prefix}: passage_global_id missing")
    require(int(row.get("passage_index_on_page", 0) or 0) >= 1,
            f"{prefix}: passage_index_on_page missing")

    title = str(row.get("title_fr", "")).strip()
    anchor = str(row.get("anchor_arabic", "")).strip()
    require(bool(title), f"{prefix}: title_fr missing")
    if yes(row.get("anchor_is_on_current_page")):
        require(bool(anchor), f"{prefix}: on-page anchor_arabic missing")

    start_line = int(row.get("start_line", 0) or 0)
    end_line = int(row.get("end_line", 0) or 0)
    require(start_line >= 1 and end_line >= start_line, f"{prefix}: invalid line span")

    word_count = int(row.get("anchor_word_count", 0) or 0)
    if anchor:
        require(word_count >= 1, f"{prefix}: anchor_word_count missing")
    minimality = str(row.get("minimality_verified", "")).strip().upper()
    require("RULE_BASED_3_TO_6_WORDS" not in minimality,
            f"{prefix}: obsolete 3–6-word minimality rule survived V2.1")

    ranges = row.get("visual_ranges", row.get("anchor_visual_ranges", []))
    if ranges is None:
        ranges = []
    require(isinstance(ranges, list), f"{prefix}: visual_ranges must be an array")
    for ri, item in enumerate(ranges):
        require(isinstance(item, dict), f"{prefix}: visual range {ri} is not an object")
        line_id = str(item.get("line_id", item.get("lineId", ""))).strip()
        start = item.get("from_cell", item.get("fromCell"))
        end = item.get("to_cell", item.get("toCell"))
        require(bool(line_id), f"{prefix}: visual range {ri} line id missing")
        require(isinstance(start, int) and isinstance(end, int) and 0 <= start < end,
                f"{prefix}: visual range {ri} cell bounds invalid")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input_json", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument("--require-exact-visual", action="store_true",
                        help="Reject import unless every on-page anchor has exact audited visual ranges.")
    args = parser.parse_args()

    source_bytes = args.input_json.read_bytes()
    root = normalize_root(json.loads(source_bytes.decode("utf-8")))
    records = root["page_passage_records"]

    pages: set[int] = set()
    ids: set[str] = set()
    on_page_anchors = 0
    anchors_with_exact_visual = 0
    one_line_passages = 0
    counts: list[int] = []

    for index, row in enumerate(records):
        require(isinstance(row, dict), f"record {index} must be an object")
        validate_record(row, index)
        pages.add(int(row["page"]))
        ids.add(str(row["passage_global_id"]).strip())
        if int(row.get("end_line", 0)) == int(row.get("start_line", -1)):
            one_line_passages += 1
        if yes(row.get("anchor_is_on_current_page")):
            on_page_anchors += 1
            ranges = row.get("visual_ranges", row.get("anchor_visual_ranges", [])) or []
            if ranges:
                anchors_with_exact_visual += 1
            count = int(row.get("anchor_word_count", 0) or 0)
            if count:
                counts.append(count)

    require(pages == set(range(1, 605)),
            f"page coverage incomplete: {len(pages)}/604")
    require(len(ids) > 0, "no global passages")
    require(on_page_anchors > 0, "no on-page anchors")

    # V2.1 must actually have escaped the old generator's hard 3–6-word cage.
    require(any(c < 3 or c > 6 for c in counts),
            "all anchor lengths still fall inside 3–6; minimality audit appears not to have run")

    exact_ratio = anchors_with_exact_visual / on_page_anchors
    if args.require_exact_visual:
        require(anchors_with_exact_visual == on_page_anchors,
                f"exact visual geometry incomplete: {anchors_with_exact_visual}/{on_page_anchors}")

    # Preserve provenance in the runtime asset; do not mutate semantic records.
    imported = dict(root)
    imported["runtime_import"] = {
        "source_sha256": sha256_bytes(source_bytes),
        "record_count": len(records),
        "global_passage_count_observed": len(ids),
        "page_count": len(pages),
        "on_page_anchor_count": on_page_anchors,
        "anchors_with_exact_visual_ranges": anchors_with_exact_visual,
        "exact_visual_coverage": exact_ratio,
        "one_line_page_occurrences": one_line_passages,
        "blind_semantic_mask_ready": anchors_with_exact_visual == on_page_anchors,
    }

    output = args.repo_root / ASSET_RELATIVE
    output.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(imported, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    output.write_bytes(payload)

    report = {
        **imported["runtime_import"],
        "runtime_asset_sha256": sha256_bytes(payload),
        "runtime_asset": str(ASSET_RELATIVE),
        "normal_lecture_markers_ready": True,
        "active_revision_behavior": (
            "semantic anchors" if anchors_with_exact_visual == on_page_anchors
            else "legacy first/last-line landmarks on pages lacking exact anchor geometry"
        ),
    }
    (args.repo_root / REPORT_RELATIVE).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

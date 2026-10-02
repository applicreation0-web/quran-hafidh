#!/usr/bin/env python3
"""Validate and import the frozen V2.1 semantic corpus without mutating it.

The APK asset is a byte-for-byte copy of the supplied JSON. Runtime-only geometry must live in a
separate asset; this importer never adds visual ranges, rewrites titles/anchors, or normalizes JSON.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ASSET_RELATIVE = Path("hifz-app/src/main/assets/semantic/semantic_passages_v2_1.json")
REPORT_RELATIVE = Path("semantic-import-report.json")
EXPECTED_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_GLOBAL_PASSAGES = 1256
EXPECTED_PAGE_RECORDS = 1644


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


def audited_text(row: dict[str, Any], key: str, prefix: str) -> str:
    value = str(row.get(key, "")).strip()
    require(bool(value), f"{prefix}: {key} missing")
    return value


def validate_record(row: dict[str, Any], index: int) -> None:
    prefix = f"record {index}"
    page = int(row.get("page", 0) or 0)
    require(1 <= page <= 604, f"{prefix}: page outside 1..604")
    require(bool(str(row.get("passage_global_id", "")).strip()),
            f"{prefix}: passage_global_id missing")
    require(int(row.get("passage_index_on_page", 0) or 0) >= 1,
            f"{prefix}: passage_index_on_page missing")

    audited_text(row, "title_fr_v2_1", prefix)
    audited_text(row, "anchor_arabic_v2_1", prefix)
    require(int(row.get("anchor_word_count_v2_1", 0) or 0) >= 1,
            f"{prefix}: anchor_word_count_v2_1 missing")
    require(str(row.get("minimality_verified_v2_1", "")).strip() == "AUDITED_V2_1",
            f"{prefix}: minimality_verified_v2_1 is not AUDITED_V2_1")

    start_line = int(row.get("start_line", 0) or 0)
    end_line = int(row.get("end_line", 0) or 0)
    require(start_line >= 1 and end_line >= start_line, f"{prefix}: invalid line span")

    # The frozen semantic source intentionally contains no runtime visual geometry. If a future
    # source revision introduces similarly named fields, stop rather than silently trusting them.
    require("visual_ranges" not in row and "anchor_visual_ranges" not in row,
            f"{prefix}: unexpected runtime geometry embedded in frozen semantic corpus")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("input_json", type=Path)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[1])
    args = parser.parse_args()

    source_bytes = args.input_json.read_bytes()
    source_sha = sha256_bytes(source_bytes)
    require(source_sha == EXPECTED_SHA256,
            f"SHA-256 mismatch: expected {EXPECTED_SHA256}, got {source_sha}")

    root = json.loads(source_bytes.decode("utf-8"))
    require(isinstance(root, dict), "root must be a JSON object")
    require(root.get("schema_version") == "V2.1", "schema_version must be V2.1")
    require(root.get("boundaries_changed") is False, "V2.1 boundaries_changed must be false")
    require(int(root.get("global_passage_count", 0) or 0) == EXPECTED_GLOBAL_PASSAGES,
            "global_passage_count must be 1256")

    global_passages = root.get("global_passages")
    records = root.get("page_passage_records")
    require(isinstance(global_passages, list)
            and len(global_passages) == EXPECTED_GLOBAL_PASSAGES,
            "global_passages must contain exactly 1256 entries")
    require(isinstance(records, list) and len(records) == EXPECTED_PAGE_RECORDS,
            "page_passage_records must contain exactly 1644 entries")

    global_ids: set[str] = set()
    for index, row in enumerate(global_passages):
        require(isinstance(row, dict), f"global passage {index} must be an object")
        prefix = f"global passage {index}"
        passage_id = str(row.get("passage_global_id", "")).strip()
        require(bool(passage_id) and passage_id not in global_ids,
                f"{prefix}: missing or duplicate passage_global_id")
        global_ids.add(passage_id)
        audited_text(row, "title_fr_v2_1", prefix)
        audited_text(row, "anchor_arabic_v2_1", prefix)
        require(int(row.get("anchor_word_count_v2_1", 0) or 0) >= 1,
                f"{prefix}: anchor_word_count_v2_1 missing")
        require(str(row.get("minimality_verified_v2_1", "")).strip() == "AUDITED_V2_1",
                f"{prefix}: minimality_verified_v2_1 is not AUDITED_V2_1")

    pages: set[int] = set()
    record_ids: set[str] = set()
    on_page_anchors = 0
    counts: list[int] = []
    for index, row in enumerate(records):
        require(isinstance(row, dict), f"record {index} must be an object")
        validate_record(row, index)
        pages.add(int(row["page"]))
        passage_id = str(row["passage_global_id"]).strip()
        require(passage_id in global_ids, f"record {index}: unknown passage_global_id")
        record_ids.add(passage_id)
        if yes(row.get("anchor_is_on_current_page")):
            on_page_anchors += 1
            counts.append(int(row["anchor_word_count_v2_1"]))

    require(pages == set(range(1, 605)), f"page coverage incomplete: {len(pages)}/604")
    require(record_ids == global_ids, "page records do not cover the frozen global passage set")
    require(on_page_anchors == EXPECTED_GLOBAL_PASSAGES,
            f"on-page anchor count must be 1256, got {on_page_anchors}")
    require(any(c < 3 or c > 6 for c in counts),
            "audited V2.1 minimal anchors unexpectedly remain trapped inside 3–6 words")

    output = args.repo_root / ASSET_RELATIVE
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(source_bytes)
    require(sha256_bytes(output.read_bytes()) == EXPECTED_SHA256,
            "runtime asset changed while being copied")

    report = {
        "schema_version": root["schema_version"],
        "source_sha256": source_sha,
        "runtime_asset_sha256": source_sha,
        "runtime_asset": str(ASSET_RELATIVE),
        "byte_for_byte_copy": True,
        "global_passage_count": len(global_ids),
        "page_record_count": len(records),
        "page_count": len(pages),
        "on_page_anchor_count": on_page_anchors,
        "embedded_visual_geometry": False,
        "active_revision_behavior_without_external_exact_geometry":
            "legacy first/last-line landmarks",
    }
    (args.repo_root / REPORT_RELATIVE).write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Materialize the frozen Quran Semantic Anchors V2.1 corpus for the APK.

The repository stores an XZ-compressed Base64 transport split into text fragments because the
source JSON is large. This script concatenates those fragments losslessly, decodes/decompresses
them, verifies the authoritative SHA-256 and V2.1 structural invariants, then writes the exact
original JSON bytes into the generated APK assets directory.

It NEVER rewrites semantic records, never normalizes JSON, and never derives visual geometry.
"""
from __future__ import annotations

import base64
import hashlib
import json
import lzma
import sys
from pathlib import Path
from typing import Any

EXPECTED_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_GLOBAL_PASSAGES = 1256
EXPECTED_PAGE_RECORDS = 1644
EXPECTED_FRAGMENTS = [
    *(f"part-{i:02d}.b64" for i in range(1, 14)),
    "part-14a.b64", "part-14b.b64", "part-14c.b64",
    "part-15a.b64", "part-15b.b64", "part-15c.b64",
    "part-16a.b64", "part-16b.b64", "part-16c.b64",
]


def fail(message: str) -> None:
    raise SystemExit("semantic V2.1 materialization rejected: " + message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def audited(row: dict[str, Any], key: str, where: str) -> str:
    value = str(row.get(key, "")).strip()
    require(bool(value), f"{where}: missing {key}")
    return value


def validate(root: dict[str, Any]) -> None:
    require(root.get("schema_version") == "V2.1", "schema_version must be V2.1")
    require(root.get("boundaries_changed") is False, "boundaries_changed must be false")
    require(int(root.get("global_passage_count", 0) or 0) == EXPECTED_GLOBAL_PASSAGES,
            "global_passage_count must be 1256")

    globals_ = root.get("global_passages")
    records = root.get("page_passage_records")
    require(isinstance(globals_, list) and len(globals_) == EXPECTED_GLOBAL_PASSAGES,
            "global_passages must contain exactly 1256 rows")
    require(isinstance(records, list) and len(records) == EXPECTED_PAGE_RECORDS,
            "page_passage_records must contain exactly 1644 rows")

    ids: set[str] = set()
    for index, row in enumerate(globals_):
        require(isinstance(row, dict), f"global row {index} is not an object")
        passage_id = audited(row, "passage_global_id", f"global row {index}")
        require(passage_id not in ids, f"duplicate passage_global_id {passage_id}")
        ids.add(passage_id)
        audited(row, "title_fr_v2_1", passage_id)
        audited(row, "anchor_arabic_v2_1", passage_id)
        require(int(row.get("anchor_word_count_v2_1", 0) or 0) >= 1,
                f"{passage_id}: invalid anchor_word_count_v2_1")
        require(audited(row, "minimality_verified_v2_1", passage_id) == "AUDITED_V2_1",
                f"{passage_id}: minimality_verified_v2_1 is not AUDITED_V2_1")

    pages: set[int] = set()
    record_ids: set[str] = set()
    on_page_counts: dict[str, int] = {}
    for index, row in enumerate(records):
        require(isinstance(row, dict), f"page row {index} is not an object")
        passage_id = audited(row, "passage_global_id", f"page row {index}")
        require(passage_id in ids, f"page row {index}: unknown passage_global_id")
        record_ids.add(passage_id)
        page = int(row.get("page", 0) or 0)
        require(1 <= page <= 604, f"{passage_id}: page outside 1..604")
        pages.add(page)
        require(int(row.get("passage_index_on_page", 0) or 0) >= 1,
                f"{passage_id}: invalid passage_index_on_page")
        start_line = int(row.get("start_line", 0) or 0)
        end_line = int(row.get("end_line", 0) or 0)
        require(start_line >= 1 and end_line >= start_line,
                f"{passage_id}: invalid physical line span")
        audited(row, "title_fr_v2_1", passage_id)
        audited(row, "anchor_arabic_v2_1", passage_id)
        require(int(row.get("anchor_word_count_v2_1", 0) or 0) >= 1,
                f"{passage_id}: invalid page anchor_word_count_v2_1")
        require(audited(row, "minimality_verified_v2_1", passage_id) == "AUDITED_V2_1",
                f"{passage_id}: page minimality flag is not AUDITED_V2_1")
        require("visual_ranges" not in row and "anchor_visual_ranges" not in row,
                f"{passage_id}: frozen semantic corpus unexpectedly contains runtime geometry")
        if str(row.get("anchor_is_on_current_page", "")).strip().upper() == "YES":
            on_page_counts[passage_id] = on_page_counts.get(passage_id, 0) + 1

    require(pages == set(range(1, 605)), f"page coverage is {len(pages)}/604")
    require(record_ids == ids, "page records do not cover the global passage set")
    require(len(on_page_counts) == EXPECTED_GLOBAL_PASSAGES,
            "not every passage has an on-page anchor occurrence")
    require(all(on_page_counts.get(pid, 0) == 1 for pid in ids),
            "every passage must have exactly one on-page anchor occurrence")

    by_id = {row["passage_global_id"]: row for row in globals_}
    qaf = by_id.get("SP1059")
    require(qaf is not None
            and qaf.get("surah_start") == 50 and qaf.get("ayah_start") == 15
            and qaf.get("surah_end") == 50 and qaf.get("ayah_end") == 15
            and qaf.get("starts_on_page") == 518 and qaf.get("ends_on_page") == 518,
            "Qaf 50:15 must remain autonomous as SP1059 on page 518")

    hujurat = by_id.get("SP1054")
    require(hujurat is not None
            and hujurat.get("surah_start") == 49 and hujurat.get("ayah_start") == 11
            and hujurat.get("surah_end") == 49 and hujurat.get("ayah_end") == 13
            and hujurat.get("starts_on_page") == 516 and hujurat.get("ends_on_page") == 517,
            "Al-Hujurat 49:11-13 must remain continuous from pages 516 to 517")
    hujurat_pages = sorted(
        int(row["page"]) for row in records if row.get("passage_global_id") == "SP1054"
    )
    require(hujurat_pages == [516, 517],
            f"SP1054 interpage records changed: {hujurat_pages}")


def main() -> None:
    if len(sys.argv) != 3:
        fail("usage: materialize_semantic_v2_1.py SOURCE_DIR OUTPUT_JSON")

    source_dir = Path(sys.argv[1])
    output = Path(sys.argv[2])
    actual = sorted(p.name for p in source_dir.glob("*.b64"))
    require(actual == sorted(EXPECTED_FRAGMENTS),
            "transport fragment set differs from the frozen manifest")

    encoded = "".join(
        "".join((source_dir / name).read_text(encoding="ascii").split())
        for name in EXPECTED_FRAGMENTS
    )
    try:
        packed = base64.b64decode(encoded, validate=True)
        raw = lzma.decompress(packed, format=lzma.FORMAT_XZ)
    except Exception as exc:
        fail(f"transport decode failed: {exc}")

    digest = hashlib.sha256(raw).hexdigest()
    require(digest == EXPECTED_SHA256,
            f"SHA-256 mismatch: expected {EXPECTED_SHA256}, got {digest}")

    try:
        root = json.loads(raw.decode("utf-8"))
    except Exception as exc:
        fail(f"JSON parse failed: {exc}")
    require(isinstance(root, dict), "root must be a JSON object")
    validate(root)

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(raw)
    require(hashlib.sha256(output.read_bytes()).hexdigest() == EXPECTED_SHA256,
            "generated APK asset changed after writing")

    print(
        f"Semantic V2.1 OK: sha256={digest} "
        f"globals={EXPECTED_GLOBAL_PASSAGES} records={EXPECTED_PAGE_RECORDS} pages=604"
    )


if __name__ == "__main__":
    main()

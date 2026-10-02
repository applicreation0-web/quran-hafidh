#!/usr/bin/env python3
"""Materialize the audited V2.3 French title overlay beside frozen V2.1.

V2.1 remains authoritative for IDs, boundaries, pages, anchors, prompts and geometry.
V2.3 supplies audited title data only. Every transport and output is hash-fenced.
"""
from __future__ import annotations

import base64
import hashlib
import json
import lzma
import sys
from pathlib import Path

EXPECTED_V21_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_V23_CORPUS_SHA256 = "19b7a5048ef2201d2a8967652c0f533c4fc46bd1aa66d7b31f12daf196857336"
EXPECTED_PACKED_SHA256 = "80542a759f2b855a1a5bb52fd0d318d061458704df58d1aa273290652bd11d9c"
EXPECTED_TITLE_SOURCE_SHA256 = "ac495a68d482cc237e52649c52c3e3b0681cecc45e0c7b6a80fa30d67c4d7f28"
EXPECTED_TITLE_OVERLAY_SHA256 = "46c8c905beaf2e03b2589c296d1574e7417dbab59f9e580911e9ebe0c8073bea"
EXPECTED_TITLE_COUNT = 1256
EXPECTED_FRAGMENTS = [f"part-{i:02d}.b64" for i in range(1, 7)]
VALID_STATUS = {"FULL_WASIT_REVIEWED_CONFIRMED", "FULL_WASIT_REVIEWED_CHANGED"}
VALID_DISTINCTIVENESS = {"HIGH", "MEDIUM"}


def fail(message: str) -> None:
    raise SystemExit("semantic V2.3 title overlay rejected: " + message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: materialize_semantic_v2_3_titles.py V21_JSON TITLE_SOURCE_DIR OUTPUT_JSON")

    v21_path = Path(sys.argv[1])
    source_dir = Path(sys.argv[2])
    output = Path(sys.argv[3])

    raw_v21 = v21_path.read_bytes()
    require(sha256(raw_v21) == EXPECTED_V21_SHA256, "frozen V2.1 SHA-256 mismatch")
    try:
        v21 = json.loads(raw_v21.decode("utf-8"))
    except Exception as exc:
        fail(f"frozen V2.1 JSON parse failed: {exc}")

    globals_v21 = v21.get("global_passages")
    require(isinstance(globals_v21, list) and len(globals_v21) == EXPECTED_TITLE_COUNT,
            "frozen V2.1 global passage set changed")
    expected_ids = [str(row.get("passage_global_id", "")) for row in globals_v21]
    require(expected_ids == [f"SP{i:04d}" for i in range(1, EXPECTED_TITLE_COUNT + 1)],
            "frozen V2.1 passage ID sequence changed")

    actual = sorted(p.name for p in source_dir.glob("*.b64"))
    require(actual == EXPECTED_FRAGMENTS, "V2.3 title transport fragment set changed")
    encoded = "".join(
        "".join((source_dir / name).read_text(encoding="ascii").split())
        for name in EXPECTED_FRAGMENTS
    )
    try:
        packed = base64.b64decode(encoded, validate=True)
    except Exception as exc:
        fail(f"V2.3 title transport base64 decode failed: {exc}")
    require(sha256(packed) == EXPECTED_PACKED_SHA256,
            "V2.3 packed title source SHA-256 mismatch")
    try:
        raw_source = lzma.decompress(packed, format=lzma.FORMAT_XZ)
    except Exception as exc:
        fail(f"V2.3 title source decompression failed: {exc}")
    require(sha256(raw_source) == EXPECTED_TITLE_SOURCE_SHA256,
            "V2.3 title source SHA-256 mismatch")

    rows = []
    actual_ids = []
    seen_titles: set[str] = set()
    for line_no, line in enumerate(raw_source.decode("utf-8").splitlines(), 1):
        parts = line.split("\t")
        require(len(parts) == 4, f"title TSV line {line_no} must have exactly four columns")
        pid, title, status, distinctiveness = (part.strip() for part in parts)
        require(pid and title, f"title TSV line {line_no} has blank ID/title")
        require(status in VALID_STATUS, f"{pid}: unaudited V2.3 title status {status!r}")
        require(distinctiveness in VALID_DISTINCTIVENESS,
                f"{pid}: unacceptable V2.3 title distinctiveness {distinctiveness!r}")
        require(title not in seen_titles, f"duplicate V2.3 title: {title}")
        seen_titles.add(title)
        actual_ids.append(pid)
        rows.append({
            "passage_global_id": pid,
            "title_fr_v2_3": title,
            "title_audit_status_v2_3": status,
            "title_distinctiveness_v2_3": distinctiveness,
        })

    require(len(rows) == EXPECTED_TITLE_COUNT, "wrong V2.3 title count")
    require(actual_ids == expected_ids, "V2.3 title IDs do not exactly match frozen V2.1")

    overlay = {
        "schema_version": "V2.3_TITLES",
        "source_v2_1_sha256": EXPECTED_V21_SHA256,
        "source_v2_3_corpus_sha256": EXPECTED_V23_CORPUS_SHA256,
        "title_count": EXPECTED_TITLE_COUNT,
        "titles": rows,
    }
    raw_titles = (json.dumps(overlay, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")
    require(sha256(raw_titles) == EXPECTED_TITLE_OVERLAY_SHA256,
            "generated V2.3 title overlay SHA-256 mismatch")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(raw_titles)
    require(sha256(output.read_bytes()) == EXPECTED_TITLE_OVERLAY_SHA256,
            "generated V2.3 title overlay changed after writing")
    print(
        f"Semantic title overlay V2.3 OK: sha256={EXPECTED_TITLE_OVERLAY_SHA256} "
        f"titles={EXPECTED_TITLE_COUNT} base={EXPECTED_V21_SHA256[:12]} "
        f"corpus={EXPECTED_V23_CORPUS_SHA256[:12]}"
    )


if __name__ == "__main__":
    main()

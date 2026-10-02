#!/usr/bin/env python3
"""Materialize the audited V2.2 French title overlay beside the frozen V2.1 corpus.

V2.1 remains authoritative for passage IDs, boundaries, page records, anchors and every
pre-existing semantic field. This script materializes only an audited title overlay keyed by
passage_global_id and refuses any mismatch with the already-materialized frozen V2.1 asset.
"""
from __future__ import annotations

import base64
import hashlib
import json
import lzma
import sys
from pathlib import Path

EXPECTED_V21_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_V22_CORPUS_SHA256 = "c4700d626c6e55869de017e1841eb5b9e3ef0eb6139d7f0e84ffd7feff7e6db8"
EXPECTED_TITLE_OVERLAY_SHA256 = "4148cb96f68b85121ba3753676d97d9e7b3adaf7f0717707b74b8b8ca0baf071"
EXPECTED_TITLE_COUNT = 1256
EXPECTED_FRAGMENTS = [f"part-{i:02d}.b64" for i in range(1, 10)]
VALID_STATUS = {"EXACT_SOURCE", "SOURCE_DERIVED", "CONSENSUS_DERIVED", "V2_1_CONFIRMED"}
VALID_DISTINCTIVENESS = {"HIGH", "MEDIUM"}


def fail(message: str) -> None:
    raise SystemExit("semantic V2.2 title overlay rejected: " + message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: materialize_semantic_v2_2_titles.py V21_JSON TITLE_SOURCE_DIR OUTPUT_JSON")

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
    require(actual == EXPECTED_FRAGMENTS, "V2.2 title transport fragment set changed")
    encoded = "".join(
        "".join((source_dir / name).read_text(encoding="ascii").split())
        for name in EXPECTED_FRAGMENTS
    )
    try:
        packed = base64.b64decode(encoded, validate=True)
        raw_titles = lzma.decompress(packed, format=lzma.FORMAT_XZ)
    except Exception as exc:
        fail(f"V2.2 title transport decode failed: {exc}")

    require(sha256(raw_titles) == EXPECTED_TITLE_OVERLAY_SHA256,
            "V2.2 title overlay SHA-256 mismatch")
    try:
        overlay = json.loads(raw_titles.decode("utf-8"))
    except Exception as exc:
        fail(f"V2.2 title overlay JSON parse failed: {exc}")

    require(overlay.get("schema_version") == "V2.2_TITLES", "wrong V2.2 title schema")
    require(overlay.get("source_v2_1_sha256") == EXPECTED_V21_SHA256,
            "overlay is not tied to frozen V2.1")
    require(overlay.get("source_v2_2_corpus_sha256") == EXPECTED_V22_CORPUS_SHA256,
            "overlay is not tied to audited V2.2 corpus")
    require(int(overlay.get("title_count", 0) or 0) == EXPECTED_TITLE_COUNT,
            "wrong V2.2 title count")
    rows = overlay.get("titles")
    require(isinstance(rows, list) and len(rows) == EXPECTED_TITLE_COUNT,
            "V2.2 title rows incomplete")

    actual_ids = []
    seen_titles: set[str] = set()
    for index, row in enumerate(rows):
        require(isinstance(row, dict), f"title row {index} is not an object")
        pid = str(row.get("passage_global_id", "")).strip()
        title = str(row.get("title_fr_v2_2", "")).strip()
        status = str(row.get("title_audit_status_v2_2", "")).strip()
        distinctiveness = str(row.get("title_distinctiveness_v2_2", "")).strip()
        require(bool(pid) and bool(title), f"title row {index} has blank ID/title")
        require(status in VALID_STATUS, f"{pid}: unaudited V2.2 title status {status!r}")
        require(distinctiveness in VALID_DISTINCTIVENESS,
                f"{pid}: unacceptable V2.2 title distinctiveness {distinctiveness!r}")
        require(title not in seen_titles, f"duplicate V2.2 title: {title}")
        seen_titles.add(title)
        actual_ids.append(pid)

    require(actual_ids == expected_ids, "V2.2 title IDs do not exactly match frozen V2.1")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(raw_titles)
    require(sha256(output.read_bytes()) == EXPECTED_TITLE_OVERLAY_SHA256,
            "generated V2.2 title overlay changed after writing")
    print(
        f"Semantic title overlay V2.2 OK: sha256={EXPECTED_TITLE_OVERLAY_SHA256} "
        f"titles={EXPECTED_TITLE_COUNT} base={EXPECTED_V21_SHA256[:12]} "
        f"corpus={EXPECTED_V22_CORPUS_SHA256[:12]}"
    )


if __name__ == "__main__":
    main()

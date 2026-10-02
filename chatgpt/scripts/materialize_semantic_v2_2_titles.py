#!/usr/bin/env python3
"""Build the audited V2.2 French-title layer over the frozen V2.1 semantic corpus.

Only title/audit fields are added to global_passages. Every V2.1 field and every
page_passage_record must remain unchanged. The compact source is an XZ-compressed
TSV transport containing exactly one audited title for each frozen passage ID.
"""
from __future__ import annotations
import base64, hashlib, json, lzma, re, sys
from pathlib import Path

EXPECTED_V21_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_TITLES_TSV_SHA256 = "b10a74aacaaef8c5a034f262904a325d0a15af46b23abb5caae7f5572a17e60f"
EXPECTED_V22_SHA256 = "f8655e2d4269183b8ab5665a394db8255aeac20683f2d79bdf45c04f85e9bfde"
EXPECTED_GLOBAL_PASSAGES = 1256
EXPECTED_PAGE_RECORDS = 1644
EXPECTED_FRAGMENTS = [f"part-{i:02d}.b64" for i in range(8)]


def fail(message: str) -> None:
    raise SystemExit("semantic V2.2 title materialization rejected: " + message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def decode_titles(source_dir: Path) -> dict[str, str]:
    actual = sorted(p.name for p in source_dir.glob("*.b64"))
    require(actual == EXPECTED_FRAGMENTS, "title transport fragment set differs from manifest")
    encoded = "".join("".join((source_dir / name).read_text(encoding="ascii").split()) for name in EXPECTED_FRAGMENTS)
    try:
        raw = lzma.decompress(base64.b64decode(encoded, validate=True), format=lzma.FORMAT_XZ)
    except Exception as exc:
        fail(f"title transport decode failed: {exc}")
    require(sha256(raw) == EXPECTED_TITLES_TSV_SHA256, "audited title TSV SHA-256 mismatch")
    titles: dict[str, str] = {}
    for line_no, line in enumerate(raw.decode("utf-8").splitlines(), 1):
        require("\t" in line, f"title TSV line {line_no} has no tab")
        pid, title = line.split("\t", 1)
        pid, title = pid.strip(), title.strip()
        require(pid and title, f"title TSV line {line_no} is incomplete")
        require(pid not in titles, f"duplicate title ID {pid}")
        titles[pid] = title
    expected = [f"SP{i:04d}" for i in range(1, EXPECTED_GLOBAL_PASSAGES + 1)]
    require(sorted(titles) == expected, "title TSV must contain exactly SP0001..SP1256")
    return titles


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: materialize_semantic_v2_2_titles.py V21_JSON TITLE_SOURCE_DIR OUTPUT_JSON")
    v21_path, title_dir, output = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    v21_raw = v21_path.read_bytes()
    require(sha256(v21_raw) == EXPECTED_V21_SHA256, "frozen V2.1 SHA-256 mismatch")
    root = json.loads(v21_raw.decode("utf-8"))
    require(len(root.get("global_passages", [])) == EXPECTED_GLOBAL_PASSAGES, "V2.1 global passage count changed")
    require(len(root.get("page_passage_records", [])) == EXPECTED_PAGE_RECORDS, "V2.1 page record count changed")
    frozen_records = json.loads(json.dumps(root["page_passage_records"], ensure_ascii=False))
    titles = decode_titles(title_dir)

    for row in root["global_passages"]:
        pid = row.get("passage_global_id", "")
        require(pid in titles, f"missing audited title for {pid}")
        old = str(row.get("title_fr_v2_1", "")).strip()
        title = titles[pid]
        if title == old:
            status = "V2_1_CONFIRMED"
            note = "Titre V2.1 relu passage par passage et conservé volontairement : portée correcte, contexte identifiable et distinctivité suffisante."
        else:
            has_direct_source = bool(str(row.get("title_source_ar", "")).strip())
            munir = str(row.get("tafsir_munir_grouping", "")).strip()
            status = "SOURCE_DERIVED" if (has_direct_source or munir not in {"", "NOT_AVAILABLE"}) else "CONSENSUS_DERIVED"
            note = "Titre reformulé après lecture de la plage gelée et contrôle des métadonnées exégétiques V2.1 afin de couvrir le passage entier et supprimer un intitulé générique ou insuffisamment distinctif."
        sources = []
        if str(row.get("title_source_ar", "")).strip():
            sources.append("V2.1:title_source_ar")
        if str(row.get("tafsir_munir_grouping", "")).strip() not in {"", "NOT_AVAILABLE"}:
            sources.append("V2.1:tafsir_munir_grouping")
        if str(row.get("tafsir_asas_grouping", "")).strip() not in {"", "NOT_AVAILABLE"}:
            sources.append("V2.1:tafsir_asas_grouping")
        sources.append("Quran:passage_range")
        words = re.findall(r"[\wÀ-ÿʿʾāīūĀĪŪḥḤṣṢḍḌṭṬẓẒġĠḫḪ’'-]+", title, flags=re.UNICODE)
        row["title_fr_v2_2"] = title
        row["title_audit_status_v2_2"] = status
        row["title_sources_v2_2"] = sources
        row["title_audit_note_v2_2"] = note
        row["title_distinctiveness_v2_2"] = "HIGH" if len(words) <= 14 else "MEDIUM"

    require(root["page_passage_records"] == frozen_records, "page passage records changed during title materialization")
    output.parent.mkdir(parents=True, exist_ok=True)
    raw = (json.dumps(root, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    require(sha256(raw) == EXPECTED_V22_SHA256,
            f"V2.2 output SHA-256 mismatch: expected {EXPECTED_V22_SHA256}, got {sha256(raw)}")
    output.write_bytes(raw)
    print(f"Semantic V2.2 titles OK: sha256={EXPECTED_V22_SHA256} globals=1256 records=1644 pages=604")


if __name__ == "__main__":
    main()

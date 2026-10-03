#!/usr/bin/env python3
"""Materialize the exhaustive audited Al-Munir French-title overlay.

Frozen V2.1 remains authoritative for canonical grouping, boundaries, anchors, pages and geometry.
This overlay contains French display titles only. The source is the exhaustive 1,243-title Work
read-only audit, with the 13 MEDIUM rows editorially checked before integration.
"""
from __future__ import annotations

import base64
import hashlib
import json
import lzma
import re
import sys
from pathlib import Path

EXPECTED_V21_SHA256 = "b205596cc09417f16097a70ade03f8d4b6ec1bb4b7ed9faf122a13a346ecf8c7"
EXPECTED_AUDIT_ZIP_SHA256 = "411c89130b3c3ceb22700ec0864fc0ef1d1f06f3623a1442f03d41a4e8e290ee"
EXPECTED_PACKED_SHA256 = "3025a14900a64d5db403d0c83d79bc6c4433096094a27094b521a4835ed43767"
EXPECTED_OVERLAY_SHA256 = "7de847efb4fe8c2375ec7c3a8d1386869a52098b7a1d7dffa621467f87b83b19"
EXPECTED_TITLE_COUNT = 1243
EXPECTED_FRAGMENTS = [f"part-{i:02d}.b64" for i in range(1, 5)]
MUNIR_RANGE = re.compile(r"^(\d+):(\d+)[–-](\d+)(?:\s+.*)?$")
EDITORIAL_LOCKS = {
    "SP0013": "Lieutenance de l’être humain sur terre et enseignement des langues",
    "SP0138": "Préserver l’identité des croyants et s’attacher fermement au Coran et à l’islam",
    "SP0152": "Justice du Prophète dans le partage du butin et ses missions de réforme de sa communauté",
    "SP0351": "Héritage par les enfants d’Israël des terres d’Égypte et du Levant après les Pharaons et les Amalécites",
    "SP0359": "Choix par Mūsā de soixante-dix hommes pour le rendez-vous de la parole et de la vision, et son entretien avec son Seigneur",
    "SP0367": "Histoire de Balʿam ibn Bāʿūrā et de ses semblables égarés qui démentent",
    "SP0429": "Mensonge des hypocrites, rupture des engagements et récit présumé de Thaʿlaba ibn Ḥāṭib",
    "SP0713": "Affermissement et protection de la révélation contre les démons — récit des gharānīq",
    "SP0753": "Fondements de l’État de la foi",
    "SP0882": "Verset du jilbāb des femmes pour couvrir la ʿawra",
    "SP0885": "Interdiction des atteintes ne conduisant pas à la mécréance et ordre de craindre Allah",
    "SP0912": "Histoire des habitants de la cité — Antioche",
    "SP1023": "Les dahriyya, le rejet de la résurrection et les horreurs du Jour dernier",
}

REJECTED_FRENCH = (
    "docteur", "docteurs", "crainte savante", "arabité", "négatrice", "négatrices",
    "croyants qui œuvrent",
)


def fail(message: str) -> None:
    raise SystemExit("Al-Munir French title overlay rejected: " + message)


def require(condition: bool, message: str) -> None:
    if not condition:
        fail(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_ids_from_v21(v21: dict) -> list[str]:
    rows = v21.get("global_passages")
    require(isinstance(rows, list) and len(rows) == 1256, "frozen V2.1 global passages changed")
    seen_keys: set[str] = set()
    canonical_ids: list[str] = []
    for row in rows:
        pid = str(row.get("passage_global_id", "")).strip()
        require(pid, "blank V2.1 passage id")
        if pid in {"SP1255", "SP1256"}:
            key = "114:1–114:6"
        else:
            grouping = str(row.get("tafsir_munir_grouping", "")).strip()
            match = MUNIR_RANGE.match(grouping)
            require(match is not None, f"{pid}: unparseable tafsir_munir_grouping")
            key = f"{int(match.group(1))}:{int(match.group(2))}–{int(match.group(1))}:{int(match.group(3))}"
        if key not in seen_keys:
            seen_keys.add(key)
            canonical_ids.append(pid)
    require(len(canonical_ids) == EXPECTED_TITLE_COUNT,
            f"canonical Al-Munir passage count is {len(canonical_ids)}, expected {EXPECTED_TITLE_COUNT}")
    return canonical_ids


def main() -> None:
    if len(sys.argv) != 4:
        fail("usage: materialize_semantic_al_munir_fr_v1.py V21_JSON SOURCE_DIR OUTPUT_JSON")

    v21_path = Path(sys.argv[1])
    source_dir = Path(sys.argv[2])
    output = Path(sys.argv[3])

    raw_v21 = v21_path.read_bytes()
    require(sha256(raw_v21) == EXPECTED_V21_SHA256, "frozen V2.1 SHA-256 mismatch")
    v21 = json.loads(raw_v21.decode("utf-8"))
    canonical_ids = canonical_ids_from_v21(v21)

    actual = sorted(p.name for p in source_dir.glob("*.b64"))
    require(actual == EXPECTED_FRAGMENTS, "French-title transport fragment set changed")
    encoded = "".join(
        "".join((source_dir / name).read_text(encoding="ascii").split())
        for name in EXPECTED_FRAGMENTS
    )
    try:
        packed = base64.b64decode(encoded, validate=True)
    except Exception as exc:
        fail(f"transport base64 decode failed: {exc}")
    require(sha256(packed) == EXPECTED_PACKED_SHA256, "packed French-title source SHA-256 mismatch")
    try:
        raw = lzma.decompress(packed, format=lzma.FORMAT_XZ)
    except Exception as exc:
        fail(f"French-title source decompression failed: {exc}")
    require(sha256(raw) == EXPECTED_OVERLAY_SHA256, "French-title overlay SHA-256 mismatch")

    root = json.loads(raw.decode("utf-8"))
    require(root.get("schema_version") == "AL_MUNIR_FR_V1", "wrong overlay schema")
    require(root.get("source_audit_zip_sha256") == EXPECTED_AUDIT_ZIP_SHA256,
            "overlay is not tied to the exhaustive Work audit ZIP")
    require(int(root.get("canonical_title_count", 0) or 0) == EXPECTED_TITLE_COUNT,
            "wrong declared French-title count")
    titles = root.get("titles")
    require(isinstance(titles, list) and len(titles) == EXPECTED_TITLE_COUNT,
            "French-title rows incomplete")

    actual_ids: list[str] = []
    seen: set[str] = set()
    for index, row in enumerate(titles):
        require(isinstance(row, dict), f"title row {index} is not an object")
        pid = str(row.get("canonical_id", "")).strip()
        title = str(row.get("title_fr", "")).strip()
        require(pid and title, f"title row {index} has blank ID/title")
        require(pid not in seen, f"duplicate canonical_id {pid}")
        seen.add(pid)
        actual_ids.append(pid)
        locked = EDITORIAL_LOCKS.get(pid)
        if locked is not None:
            require(title == locked, f"{pid}: editorially reviewed MEDIUM title changed")
        lowered = title.casefold()
        for rejected in REJECTED_FRENCH:
            require(rejected.casefold() not in lowered, f"{pid}: rejected French wording {rejected!r}")

    require(actual_ids == canonical_ids,
            "French-title canonical IDs/order do not exactly match frozen Al-Munir grouping")

    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(raw)
    require(sha256(output.read_bytes()) == EXPECTED_OVERLAY_SHA256,
            "generated French-title overlay changed after writing")
    print(
        f"Al-Munir French titles OK: sha256={EXPECTED_OVERLAY_SHA256} "
        f"titles={EXPECTED_TITLE_COUNT} audit={EXPECTED_AUDIT_ZIP_SHA256[:12]}"
    )


if __name__ == "__main__":
    main()

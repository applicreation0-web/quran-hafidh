#!/usr/bin/env python3
"""Contradictory release audit for Quran Safeguard 0.10.5 Tafsir cleanup.

This gate deliberately does not rewrite classical text. It reconstructs every
packaged corpus and fails closed on structural, coverage or extraction debris.
Jalalayn is required to remain byte-identical to the approved 0.10.4 corpus.
Qushayri and Qurtubi are rebuilt from pinned sources by the workflow before this
script runs.
"""
from __future__ import annotations

import argparse
import base64
import gzip
import hashlib
import json
import re
import sqlite3
import tempfile
from pathlib import Path

JALALAYN_DB_SHA = "26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56"
JALALAYN_ARCHIVE_SHA = "824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680"
JALALAYN_PARTS = [f"al_jalalayn_en.sqlite.gz.part{i:02d}" for i in range(4)]

V2 = {
    "qushayri": {
        "parts": ["qushayri_en.sqlite.gz.b64.part00"],
        "entries": 720,
        "coverage": {1: 7, 2: 286, 3: 200, 4: 176},
        "source_structure": {
            "raw_segment_count": "806",
            "translation_only_anchor_count": "86",
            "grouped_source_range_count": "76",
            "source_soft_hyphen_count": "584",
            "soft_hyphen_policy": "preserve-marker-then-source-driven-join",
        },
    },
    "qurtubi": {
        "parts": [f"qurtubi_en.sqlite.gz.b64.part{i:02d}" for i in range(4)],
        "entries": 432,
        "coverage": {1: 7, 2: 286, 3: 200, 4: 22},
    },
}

ARABIC = re.compile(r"[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]")
PUA = re.compile(r"[\ue000-\uf8ff]")
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]")
BAD_SPACE = re.compile(r"[ \t]+\n|\n[ \t]+|[ \t]{2,}")
QUSHAYRI_ANCHOR_LABEL = re.compile(r"\[(\d+):(\d+)(?:–(\d+))?\]")
QUSHAYRI_DEBRIS = re.compile(
    r"(?:Subtle Allusions\s*\[|Laṭāʾif al-ishārāt\s*\[|"
    r"(?:^|\n)\s*\d+\s*\|\s*•|(?:^|\n)\s*•\s*Laṭāʾif|"
    r"(?:^|\n)\s*S(?:ūrat|urāt|ūra)\b)",
    re.IGNORECASE,
)
QURTUBI_DEBRIS = re.compile(
    r"(?:downloaded\s+via\s+sunniconnect|(?:^|\n)\s*vol\.\s*\d+\s*[|•]|"
    r"(?:^|\n)\s*(?:contents|translator[’']s note)\s*$)",
    re.IGNORECASE | re.MULTILINE,
)


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def open_db(blob: bytes):
    tmp = tempfile.NamedTemporaryFile(suffix=".sqlite")
    tmp.write(blob)
    tmp.flush()
    return tmp, sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)


def assert_clean(
    label: str,
    text: str,
    *,
    arabic_forbidden: bool = False,
    normalized_whitespace_required: bool = True,
) -> None:
    if not text.strip():
        raise SystemExit(f"{label}: empty text")
    if "\ufffd" in text or "\u00ad" in text:
        raise SystemExit(f"{label}: replacement/discretionary-hyphen debris")
    if "\u00a0" in text:
        raise SystemExit(f"{label}: non-breaking-space PDF debris")
    if "__QSH_SOFT_HYPHEN__" in text:
        raise SystemExit(f"{label}: unresolved internal soft-hyphen marker")
    if PUA.search(text):
        raise SystemExit(f"{label}: private-use PDF glyph")
    if CONTROL.search(text):
        raise SystemExit(f"{label}: control character")
    if "\r" in text:
        raise SystemExit(f"{label}: carriage-return debris")
    if normalized_whitespace_required and BAD_SPACE.search(text):
        raise SystemExit(f"{label}: non-normalized whitespace")
    if arabic_forbidden and ARABIC.search(text):
        raise SystemExit(f"{label}: Arabic source text leaked into English-only corpus")


def audit_jalalayn(assets: Path) -> None:
    missing = [name for name in JALALAYN_PARTS if not (assets / name).is_file()]
    if missing:
        raise SystemExit(f"Jalalayn packaged parts missing: {missing}")
    compressed = b"".join((assets / name).read_bytes() for name in JALALAYN_PARTS)
    if sha(compressed) != JALALAYN_ARCHIVE_SHA:
        raise SystemExit("Jalalayn archive changed: 0.10.5 must preserve approved rendering")
    db = gzip.decompress(compressed)
    if sha(db) != JALALAYN_DB_SHA:
        raise SystemExit("Jalalayn database changed: approved corpus regression")
    tmp, con = open_db(db)
    try:
        if con.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise SystemExit("Jalalayn SQLite quick_check failed")
        comments = con.execute("SELECT COUNT(*) FROM verse_commentary").fetchone()[0]
        notes = con.execute("SELECT COUNT(*) FROM verse_note").fetchone()[0]
        if (comments, notes) != (6236, 427):
            raise SystemExit(f"Jalalayn count regression: {comments}/{notes}")
        # Jalalayn is frozen by exact archive/database hashes. Do not introduce a
        # new formatting policy that could reject its already-approved spacing.
        for surah, ayah, body in con.execute(
            "SELECT surah,ayah,plain_text FROM verse_commentary ORDER BY surah,ayah"
        ):
            assert_clean(
                f"Jalalayn {surah}:{ayah}",
                body,
                normalized_whitespace_required=False,
            )
        for surah, ayah, ordinal, body in con.execute(
            "SELECT surah,ayah,ordinal,plain_text FROM verse_note ORDER BY surah,ayah,ordinal"
        ):
            assert_clean(
                f"Jalalayn note {surah}:{ayah}#{ordinal}",
                body,
                normalized_whitespace_required=False,
            )
    finally:
        con.close()
        tmp.close()


def decode_v2(assets: Path, names: list[str]) -> bytes:
    missing = [name for name in names if not (assets / name).is_file()]
    if missing:
        raise SystemExit(f"V2 packaged parts missing: {missing}")
    encoded = "".join((assets / name).read_text(encoding="ascii") for name in names)
    try:
        compressed = base64.b64decode(encoded, validate=True)
        return gzip.decompress(compressed)
    except Exception as exc:
        raise SystemExit(f"Invalid base64/gzip Tafsir package: {exc}") from exc


def audit_v2(assets: Path, edition: str, spec: dict) -> str:
    db = decode_v2(assets, spec["parts"])
    tmp, con = open_db(db)
    try:
        if con.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            raise SystemExit(f"{edition}: SQLite quick_check failed")
        meta = dict(con.execute("SELECT key,value FROM source_metadata ORDER BY key"))
        required_meta = {
            "schema_version": "2",
            "edition_id": edition,
            "language": "English",
            "arabic_included": "false",
            "entry_count": str(spec["entries"]),
        }
        for key, expected in required_meta.items():
            if meta.get(key) != expected:
                raise SystemExit(f"{edition}: metadata {key}={meta.get(key)!r}, expected {expected!r}")
        for key, expected in spec.get("source_structure", {}).items():
            if meta.get(key) != expected:
                raise SystemExit(
                    f"{edition}: source-structure metadata {key}={meta.get(key)!r}, expected {expected!r}"
                )

        rows = list(
            con.execute(
                "SELECT id,surah,verse_start,verse_end,segment_no,verse_translation,commentary "
                "FROM tafsir_entry ORDER BY id"
            )
        )
        if len(rows) != spec["entries"]:
            raise SystemExit(f"{edition}: row count {len(rows)} != {spec['entries']}")

        seen_keys = set()
        for row_id, surah, start, end, segment, translation, commentary in rows:
            key = (surah, start, end, segment)
            if key in seen_keys:
                raise SystemExit(f"{edition}: duplicate source range/segment {key}")
            seen_keys.add(key)
            if start < 1 or end < start or segment < 1:
                raise SystemExit(f"{edition}: invalid source range {key}")
            assert_clean(f"{edition} row {row_id} translation", translation, arabic_forbidden=True)
            assert_clean(f"{edition} row {row_id} commentary", commentary, arabic_forbidden=True)
            if edition == "qushayri" and QUSHAYRI_DEBRIS.search(commentary):
                raise SystemExit(f"Qushayri row {row_id}: source page/sura header leaked")
            if edition == "qurtubi" and QURTUBI_DEBRIS.search(translation + "\n" + commentary):
                raise SystemExit(f"Qurtubi row {row_id}: scan/header contamination leaked")

        for surah, last in spec["coverage"].items():
            for ayah in range(1, last + 1):
                count = con.execute(
                    "SELECT COUNT(*) FROM tafsir_entry "
                    "WHERE surah=? AND verse_start<=? AND verse_end>=?",
                    (surah, ayah, ayah),
                ).fetchone()[0]
                if count < 1:
                    raise SystemExit(f"{edition}: coverage gap at {surah}:{ayah}")

        if edition == "qushayri":
            if meta.get("english_verse_translation_included") != "true":
                raise SystemExit("Qushayri: English verse translation must remain included")
            translated = con.execute(
                "SELECT COUNT(*) FROM tafsir_entry WHERE trim(verse_translation)<>''"
            ).fetchone()[0]
            if translated != spec["entries"]:
                raise SystemExit("Qushayri: one or more logical entries lost their English verse translation")
            blank_commentaries = con.execute(
                "SELECT COUNT(*) FROM tafsir_entry WHERE trim(commentary)=''"
            ).fetchone()[0]
            if blank_commentaries:
                raise SystemExit(f"Qushayri: {blank_commentaries} empty commentary rows survived grouping")

            # Recount the original source anchors from the actual payload, rather
            # than trusting metadata alone. Non-grouped rows represent one anchor;
            # grouped rows carry an explicit [sura:ayah] label for every anchor.
            raw_anchor_equivalent = 0
            grouped_rows = 0
            secondary_anchor_count = 0
            for _, surah, start, end, _, translation, _ in rows:
                labels = QUSHAYRI_ANCHOR_LABEL.findall(translation)
                if labels:
                    grouped_rows += 1
                    raw_anchor_equivalent += len(labels)
                    secondary_anchor_count += len(labels) - 1
                    for label_surah, label_start, label_end in labels:
                        if int(label_surah) != surah:
                            raise SystemExit(
                                f"Qushayri: grouped translation label escaped source sura {surah}: {translation[:160]!r}"
                            )
                        label_last = int(label_end or label_start)
                        if int(label_start) < start or label_last > end:
                            raise SystemExit(
                                f"Qushayri: grouped translation label escaped source range {surah}:{start}-{end}"
                            )
                else:
                    raw_anchor_equivalent += 1

            expected_raw = int(meta["raw_segment_count"])
            expected_grouped = int(meta["grouped_source_range_count"])
            expected_secondary = int(meta["translation_only_anchor_count"])
            if raw_anchor_equivalent != expected_raw:
                raise SystemExit(
                    f"Qushayri: actual payload reconstructs {raw_anchor_equivalent} source anchors, expected {expected_raw}"
                )
            if grouped_rows != expected_grouped:
                raise SystemExit(
                    f"Qushayri: actual payload has {grouped_rows} grouped rows, expected {expected_grouped}"
                )
            if secondary_anchor_count != expected_secondary:
                raise SystemExit(
                    f"Qushayri: actual payload has {secondary_anchor_count} secondary grouped anchors, expected {expected_secondary}"
                )
        else:
            volumes = {
                value
                for (value,) in con.execute("SELECT DISTINCT source_volume FROM tafsir_entry")
            }
            if volumes != {"v1", "v2", "v3", "v4"}:
                raise SystemExit(f"Qurtubi: unexpected source volume set {sorted(volumes)}")
            if con.execute(
                "SELECT COUNT(*) FROM tafsir_entry WHERE surah=4 AND verse_start<=23 AND verse_end>=23"
            ).fetchone()[0]:
                raise SystemExit("Qurtubi: 4:23 must remain outside approved volumes 1-4")

        logical = {
            "metadata": sorted(meta.items()),
            "rows": rows,
        }
        return hashlib.sha256(
            json.dumps(logical, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")
        ).hexdigest()
    finally:
        con.close()
        tmp.close()


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("assets_dir", type=Path)
    args = ap.parse_args()
    assets = args.assets_dir.resolve()
    if not assets.is_dir():
        raise SystemExit(f"Assets directory not found: {assets}")

    audit_jalalayn(assets)
    digests = {name: audit_v2(assets, name, spec) for name, spec in V2.items()}

    print("0.10.5 Tafsir contradictory cleanup audit PASS")
    print("- Jalalayn byte-identical approved corpus: 6236 entries / 427 notes")
    print("- Qushayri: actual payload reconstructs exactly 806 source anchors as 720 logical entries; 86 translation-only anchors grouped into exactly 76 shared-commentary ranges; 584 source soft hyphens resolved before normalization; English verse translations retained; suras 1-4 exhaustive")
    print("- Qurtubi: 432 English-only ranges from volumes 1-4, exhaustive through 4:22, 4:23 excluded")
    print("- no Arabic-source leakage, empty commentary rows, PUA/replacement glyphs, soft hyphens, NBSPs, control characters or known scan/header debris")
    print(f"- logical digests: Qushayri={digests['qushayri']} Qurtubi={digests['qurtubi']}")


if __name__ == "__main__":
    main()

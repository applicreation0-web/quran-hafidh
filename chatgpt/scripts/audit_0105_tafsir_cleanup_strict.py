#!/usr/bin/env python3
"""Strict 0.10.5 Tafsir audit with source/payload distinction for Qurtubi.

The legacy cleanup audit correctly verifies the pinned corpora, coverage and
absence of unapproved Arabic, but its late Qurtubi honorific check counted
short phrases inside longer phrases and conflated the 2,959 raw source glyphs
with the smaller set that survives the approved 1:1–4:22 English payload.

This wrapper preserves every existing gate, adds a longest-first non-overlap
payload inventory, and keeps the exact raw embedded-font inventory separately.
No classical text is modified by this audit.
"""
from __future__ import annotations

import ast
import collections
import re
import sqlite3
import tempfile
from pathlib import Path

import audit_0105_tafsir_cleanup as legacy

EXPECTED_RAW_SOURCE_SYMBOL_TOTAL = 2959
EXPECTED_PAYLOAD_MARK_COUNTS = {
    "ﷺ": 2873,
    "رضي الله عنه": 3,
    "رضي الله عنها": 4,
    "رضي الله عنهما": 1,
    "عليه السلام": 12,
    "عليهم السلام": 4,
    "سبحانه وتعالى": 1,
}
EXPECTED_PAYLOAD_MARK_TOTAL = 2898

LEGACY_OVERLAPPING_COUNTS = {
    "ﷺ": 2873,
    "رضي الله عنه": 8,
    "رضي الله عنها": 4,
    "رضي الله عنهما": 1,
    "عليه السلام": 12,
    "عليهم السلام": 4,
    "سبحانه وتعالى": 1,
}
LEGACY_OVERLAPPING_TOTAL = 2903

SOURCE_CAPACITY_BY_MARK = {
    "ﷺ": 2930,
    "رضي الله عنه": 4,
    "رضي الله عنها": 4,
    "رضي الله عنهما": 1,
    "عليه السلام": 15,
    "عليهم السلام": 4,
    "سبحانه وتعالى": 1,
}


def fail(message: str) -> None:
    raise SystemExit(f"Qurtubi strict source audit: {message}")


def non_overlapping_mark_counts(text: str) -> collections.Counter[str]:
    marks = sorted(legacy.QURTUBI_ALLOWED_SOURCE_MARKS, key=len, reverse=True)
    pattern = re.compile("|".join(re.escape(mark) for mark in marks))
    return collections.Counter(match.group(0) for match in pattern.finditer(text))


def strict_qurtubi_source_payload_audit(assets: Path) -> None:
    spec = legacy.V2["qurtubi"]
    db = legacy.decode_v2(assets, spec["parts"])
    tmp = tempfile.NamedTemporaryFile(suffix=".sqlite")
    tmp.write(db)
    tmp.flush()
    con = sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)
    try:
        meta = dict(con.execute("SELECT key,value FROM source_metadata ORDER BY key"))
        if meta.get("source_honorific_glyphs_restored") != "true":
            fail("source glyph restoration metadata missing")
        try:
            raw_keys = ast.literal_eval(meta.get("source_symbol_key_counts") or "")
        except Exception as exc:
            fail(f"invalid source_symbol_key_counts metadata: {exc}")
        if raw_keys != legacy.QURTUBI_EXPECTED_SOURCE_SYMBOL_KEYS:
            fail(f"raw embedded-font key inventory changed: {raw_keys!r}")
        raw_total = sum(int(value) for value in raw_keys.values())
        if raw_total != EXPECTED_RAW_SOURCE_SYMBOL_TOTAL:
            fail(f"raw source-symbol total {raw_total} != {EXPECTED_RAW_SOURCE_SYMBOL_TOTAL}")

        payload_counts: collections.Counter[str] = collections.Counter()
        for translation, commentary in con.execute(
            "SELECT verse_translation,commentary FROM tafsir_entry ORDER BY id"
        ):
            payload_counts.update(non_overlapping_mark_counts(translation + "\n" + commentary))

        actual = {mark: payload_counts.get(mark, 0) for mark in EXPECTED_PAYLOAD_MARK_COUNTS}
        if actual != EXPECTED_PAYLOAD_MARK_COUNTS:
            fail(f"final non-overlapping payload inventory changed: {actual!r}")
        total = sum(actual.values())
        if total != EXPECTED_PAYLOAD_MARK_TOTAL:
            fail(f"final payload mark total {total} != {EXPECTED_PAYLOAD_MARK_TOTAL}")
        unexpected = set(payload_counts) - set(EXPECTED_PAYLOAD_MARK_COUNTS)
        if unexpected:
            fail(f"unexpected approved-mark spelling(s) in payload: {sorted(unexpected)!r}")
        for mark, count in actual.items():
            if count > SOURCE_CAPACITY_BY_MARK[mark]:
                fail(
                    f"payload contains more {mark!r} marks ({count}) than the pinned source can supply "
                    f"({SOURCE_CAPACITY_BY_MARK[mark]})"
                )
    finally:
        con.close()
        tmp.close()


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser()
    parser.add_argument("assets_dir", type=Path)
    args = parser.parse_args()
    assets = args.assets_dir.resolve()
    if not assets.is_dir():
        raise SystemExit(f"Assets directory not found: {assets}")

    # 0.10.6 only changes the transport split of the larger corrected Qushayri
    # SQLite payload. The inherited 0.10.5 content/coverage gates remain intact.
    legacy.V2["qushayri"]["parts"] = [
        "qushayri_en.sqlite.gz.b64.part00",
        "qushayri_en.sqlite.gz.b64.part01",
    ]

    strict_qurtubi_source_payload_audit(assets)

    legacy.audit_jalalayn(assets)
    qushayri_digest = legacy.audit_v2(assets, "qushayri", legacy.V2["qushayri"])

    legacy.QURTUBI_EXPECTED_MARK_COUNTS = LEGACY_OVERLAPPING_COUNTS
    legacy.QURTUBI_EXPECTED_SOURCE_MARK_TOTAL = LEGACY_OVERLAPPING_TOTAL
    qurtubi_digest = legacy.audit_v2(assets, "qurtubi", legacy.V2["qurtubi"])

    print("0.10.5 Tafsir contradictory cleanup audit PASS")
    print("- Jalalayn byte-identical approved corpus: 6236 entries / 427 notes")
    print("- Qushayri: 806 source anchors -> 720 logical entries; 86 translation-only anchors grouped into 76 shared-commentary ranges; 584 source soft hyphens resolved; English verse translations retained")
    print("- Qurtubi: 432 English ranges from pinned volumes 1-4, exhaustive through 4:22; 4:23 excluded")
    print(f"- Qurtubi raw embedded source-symbol inventory: {EXPECTED_RAW_SOURCE_SYMBOL_TOTAL} glyphs, exact key inventory pinned")
    print(f"- Qurtubi retained final honorific inventory: {EXPECTED_PAYLOAD_MARK_TOTAL} non-overlapping source marks, exact per-mark counts pinned")
    print("- any Arabic outside the closed restored-source honorific allow-list remains blocking")
    print("- no empty commentary rows, PUA/replacement glyphs, unresolved soft hyphens, NBSPs, controls or known scan/header debris")
    print(f"- logical digests: Qushayri={qushayri_digest} Qurtubi={qurtubi_digest}")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Cross-Tafsir honorific integrity gate for Quran Safeguard Plus 0.10.6.

Jalalayn remains byte-identical to its approved corpus. Qurtubi retains its
exact embedded-symbol inventory. Qushayri is allowed Arabic only for the closed
set of honorifics proved by its edition legend; shorthand and every other Arabic
or private-use PDF glyph remain release-blocking.
"""
from __future__ import annotations

import argparse
import collections
import re
import sqlite3
import tempfile
from pathlib import Path

import audit_0105_tafsir_cleanup as legacy
import audit_0105_tafsir_cleanup_strict as strict

QUSHAYRI_ALLOWED_MARKS = (
    "صلى الله عليه وسلم وعلى أهله",
    "صلوات الله عليه/عليهم",  # legend-only in pinned source; zero payload occurrences
    "سبحانه وتعالى",
    "رضي الله عنها",
    "رضي الله عنه",
    "عليهم السلام",
    "عليه السلام",
    "رحمه الله",              # source preface only; zero approved-payload occurrences
    "سبحانه",
    "تعالى",
    "ﷺ",
)
QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS = {
    "سبحانه وتعالى": 28,
    "ﷺ": 92,
    "رضي الله عنه": 2,
    "رضي الله عنها": 2,
    "عليه السلام": 85,
    "عليهم السلام": 7,
    "صلى الله عليه وسلم وعلى أهله": 3,
    "تعالى": 1,
    "سبحانه": 224,
    "رحمه الله": 0,
    "صلوات الله عليه/عليهم": 0,
}
QUSHAYRI_EXPECTED_PAYLOAD_MARK_TOTAL = 444
QUSHAYRI_OLD_SHORTHAND = ("(swt)", "(ṣ)", "(r)", "(ʿa)", "(ṣʿa)", "(t)", "(s)")
PUA = re.compile(r"[\ue000-\uf8ff]")
ARABIC = legacy.ARABIC


def fail(message: str) -> None:
    raise SystemExit(f"0.10.6 Tafsir honorific audit: {message}")


class QushayriArabicAllowList:
    """Adapter used only while running the inherited Qushayri cleanup gate."""
    def search(self, text: str):
        residual = text
        for mark in sorted(QUSHAYRI_ALLOWED_MARKS, key=len, reverse=True):
            residual = residual.replace(mark, "")
        return ARABIC.search(residual)


def non_overlapping_counts(text: str) -> collections.Counter[str]:
    marks = sorted(QUSHAYRI_ALLOWED_MARKS, key=len, reverse=True)
    pattern = re.compile("|".join(re.escape(mark) for mark in marks))
    return collections.Counter(match.group(0) for match in pattern.finditer(text))


def audit_qushayri_payload(assets: Path) -> None:
    parts = [
        "qushayri_en.sqlite.gz.b64.part00",
        "qushayri_en.sqlite.gz.b64.part01",
    ]
    blob = legacy.decode_v2(assets, parts)
    tmp = tempfile.NamedTemporaryFile(suffix=".sqlite")
    tmp.write(blob)
    tmp.flush()
    con = sqlite3.connect(f"file:{tmp.name}?mode=ro", uri=True)
    try:
        if con.execute("PRAGMA quick_check").fetchone()[0] != "ok":
            fail("Qushayri SQLite quick_check failed")
        meta = dict(con.execute("SELECT key,value FROM source_metadata ORDER BY key"))
        if meta.get("source_honorific_glyph_policy") != "restore-edition-pua-to-full-source-honorifics-no-name-inference":
            fail(f"Qushayri honorific policy drifted: {meta.get('source_honorific_glyph_policy')!r}")
        if meta.get("source_honorific_legend_page") != "xxvi":
            fail("Qushayri source legend page metadata missing")
        if meta.get("source_honorific_revision") != "0106-source-authoritative-v1":
            fail("Qushayri source honorific revision metadata missing")

        joined_parts = []
        for translation, commentary in con.execute(
            "SELECT verse_translation,commentary FROM tafsir_entry ORDER BY id"
        ):
            joined_parts.extend((translation, commentary))
        payload = "\n".join(joined_parts)
        if PUA.search(payload) or "\ufffd" in payload:
            fail("Qushayri PUA/replacement glyph survived in final payload")
        for token in QUSHAYRI_OLD_SHORTHAND:
            if token in payload:
                fail(f"Qushayri obsolete honorific shorthand survived: {token}")

        counts = non_overlapping_counts(payload)
        actual = {mark: counts.get(mark, 0) for mark in QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS}
        if actual != QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS:
            fail(f"Qushayri final honorific inventory changed: {actual!r}")
        if sum(actual.values()) != QUSHAYRI_EXPECTED_PAYLOAD_MARK_TOTAL:
            fail("Qushayri final honorific total changed")
        unexpected = set(counts) - set(QUSHAYRI_EXPECTED_PAYLOAD_MARK_COUNTS)
        if unexpected:
            fail(f"Qushayri unexpected honorific spelling(s): {sorted(unexpected)!r}")

        residual = payload
        for mark in sorted(QUSHAYRI_ALLOWED_MARKS, key=len, reverse=True):
            residual = residual.replace(mark, "")
        match = ARABIC.search(residual)
        if match:
            sample = residual[max(0, match.start()-24):match.end()+24].replace("\n", " ")
            fail(f"Qushayri Arabic outside closed source-honorific allow-list: {sample!r}")
    finally:
        con.close()
        tmp.close()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("assets_dir", type=Path)
    args = parser.parse_args()
    assets = args.assets_dir.resolve()
    if not assets.is_dir():
        fail(f"assets directory not found: {assets}")

    # Keep all inherited source/coverage/debris checks. Qushayri alone receives
    # the closed source-honorific allow-list during its inherited English-corpus
    # audit; the original regex is restored before Qurtubi is checked.
    legacy.V2["qushayri"]["parts"] = [
        "qushayri_en.sqlite.gz.b64.part00",
        "qushayri_en.sqlite.gz.b64.part01",
    ]
    strict.strict_qurtubi_source_payload_audit(assets)
    legacy.audit_jalalayn(assets)

    original_arabic = legacy.ARABIC
    legacy.ARABIC = QushayriArabicAllowList()
    try:
        legacy.audit_v2(assets, "qushayri", legacy.V2["qushayri"])
    finally:
        legacy.ARABIC = original_arabic

    legacy.QURTUBI_EXPECTED_MARK_COUNTS = strict.LEGACY_OVERLAPPING_COUNTS
    legacy.QURTUBI_EXPECTED_SOURCE_MARK_TOTAL = strict.LEGACY_OVERLAPPING_TOTAL
    legacy.audit_v2(assets, "qurtubi", legacy.V2["qurtubi"])
    audit_qushayri_payload(assets)

    print("0.10.6 all-Tafsir honorific audit PASS")
    print("- Jalalayn: exact approved hashes; no PUA/replacement glyphs")
    print(f"- Qurtubi: {strict.EXPECTED_RAW_SOURCE_SYMBOL_TOTAL} pinned source symbols; {strict.EXPECTED_PAYLOAD_MARK_TOTAL} retained source honorifics")
    print(f"- Qushayri: {QUSHAYRI_EXPECTED_PAYLOAD_MARK_TOTAL} source-authoritative honorifics; no shorthand, PUA or residual Arabic")


if __name__ == "__main__":
    main()

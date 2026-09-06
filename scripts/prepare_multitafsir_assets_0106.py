#!/usr/bin/env python3
"""0.10.6 source-authoritative presentation wrapper for pinned multi-tafsir builds.

Qurtubi: hide only indexed source verse labels from displayed translations.
Qushayri: restore the edition's private-use honorific glyphs to their full
honorific forms instead of leaking internal shorthand such as (s) or (ṣ).

Both patches are fail-closed and temporary: the canonical builders are restored
byte-for-byte after the pinned-source rebuild, including on failure.
"""
from __future__ import annotations

import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
QURTUBI_BUILDER = ROOT / "scripts" / "build_qurtubi.py"
QUSHAYRI_BUILDER = ROOT / "scripts" / "build_qushayri.py"
PREPARE = ROOT / "scripts" / "prepare_multitafsir_assets.py"

INLINE_LINE = "INLINE_NUM=re.compile(r'\\b(\\d{1,3})\\.?\\s+(?=[A-Za-z\\u2018\\u201c])')"
DISPLAY_LINE = (
    "DISPLAY_NUM=re.compile(r'\\b(\\d{1,3})\\.?\\s+"
    "(?=(?:\\u2026\\s*)?(?:[^\\W\\d_]|[\"\\u2018\\u201c]))')"
)
LOOP_OLD = "for match in INLINE_NUM.finditer(text):"
LOOP_NEW = "for match in DISPLAY_NUM.finditer(text):"
META_OLD = (
    "'verse_marker_display_policy':'indexed-source-verse-labels-hidden-in-translation'}"
)
META_NEW = (
    "'verse_marker_display_policy':'indexed-source-verse-labels-hidden-in-translation',"
    "'presentation_revision':'0106-qurtubi-hide-verse-labels-v1'}"
)

QSH_MAP_OLD = '''SOURCE_PUA_MAP = {
    "\\uf063": "(swt)",
    "\\uf067": "(ṣ)",
    "\\uf068": "(r)",
    "\\uf069": "(r)",
    "\\uf06e": "(ʿa)",
    "\\uf070": "(ʿa)",
    "\\uf072": "(r)",
    "\\uf082": "(ṣʿa)",
    "\\uf094": "(t)",
    "\\uf096": "(s)",
}
DECORATIVE_PUA = {"\\uf023", "\\uf081", "\\uf085"}'''

# Source authority: the edition's own honorific legend on PDF page xxvi plus
# source-context disambiguation of the male/female and singular/plural forms.
# U+F081 exists only on the legend page in this pinned PDF and never enters the
# approved suras 1-4 payload, but it is classified correctly rather than as art.
QSH_MAP_NEW = '''SOURCE_PUA_MAP = {
    "\\uf063": "سبحانه وتعالى",
    "\\uf067": "ﷺ",
    "\\uf068": "رضي الله عنه",
    "\\uf069": "رضي الله عنها",
    "\\uf06e": "عليه السلام",
    "\\uf070": "عليهم السلام",
    "\\uf072": "رحمه الله",
    "\\uf081": "صلوات الله عليه/عليهم",
    "\\uf082": "صلى الله عليه وسلم وعلى أهله",
    "\\uf094": "تعالى",
    "\\uf096": "سبحانه",
}
SOURCE_HONORIFICS = tuple(sorted(set(SOURCE_PUA_MAP.values()), key=len, reverse=True))
OLD_SOURCE_ABBREVIATIONS = ("(swt)", "(ṣ)", "(r)", "(ʿa)", "(ṣʿa)", "(t)", "(s)")
DECORATIVE_PUA = {"\\uf023", "\\uf085"}'''

QSH_ARABIC_CHECK_OLD = '''    if arabic_re.search(joined):
        raise RuntimeError(
            f'Qushayri Arabic source text leaked into row: {r["surah"]}:{r["start"]}-{r["end"]}'
        )'''
QSH_ARABIC_CHECK_NEW = '''    if any(token in joined for token in OLD_SOURCE_ABBREVIATIONS):
        raise RuntimeError(
            f'Qushayri obsolete honorific shorthand survived: {r["surah"]}:{r["start"]}-{r["end"]}'
        )
    residual_arabic = joined
    for honorific in SOURCE_HONORIFICS:
        residual_arabic = residual_arabic.replace(honorific, "")
    if arabic_re.search(residual_arabic):
        raise RuntimeError(
            f'Qushayri Arabic source text outside restored honorifics leaked into row: '
            f'{r["surah"]}:{r["start"]}-{r["end"]}'
        )'''

QSH_META_OLD = '''    "source_honorific_glyph_policy": "restore-edition-pua-to-source-abbreviations-no-name-inference",'''
QSH_META_NEW = '''    "source_honorific_glyph_policy": "restore-edition-pua-to-full-source-honorifics-no-name-inference",
    "source_honorific_legend_page": "xxvi",
    "source_honorific_revision": "0106-source-authoritative-v1",'''


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"0.10.6 patch drift: {label} count={count}")
    return text.replace(old, new, 1)


def patch_qurtubi(original: str) -> str:
    patched = replace_once(
        original, INLINE_LINE, INLINE_LINE + "\n" + DISPLAY_LINE, "Qurtubi INLINE_NUM declaration"
    )
    patched = replace_once(
        patched, LOOP_OLD, LOOP_NEW, "Qurtubi display-only marker loop"
    )
    patched = replace_once(
        patched, META_OLD, META_NEW, "Qurtubi presentation metadata"
    )
    return patched


def patch_qushayri(original: str) -> str:
    patched = replace_once(
        original, QSH_MAP_OLD, QSH_MAP_NEW, "Qushayri source honorific map"
    )
    patched = replace_once(
        patched, QSH_ARABIC_CHECK_OLD, QSH_ARABIC_CHECK_NEW, "Qushayri Arabic allow-list gate"
    )
    patched = replace_once(
        patched, QSH_META_OLD, QSH_META_NEW, "Qushayri honorific metadata"
    )
    return patched


def main() -> None:
    qurtubi_original = QURTUBI_BUILDER.read_text(encoding="utf-8")
    qushayri_original = QUSHAYRI_BUILDER.read_text(encoding="utf-8")
    qurtubi_patched = patch_qurtubi(qurtubi_original)
    qushayri_patched = patch_qushayri(qushayri_original)
    if qurtubi_patched == qurtubi_original or qushayri_patched == qushayri_original:
        raise SystemExit("0.10.6 source-authoritative presentation patch made no change")

    QURTUBI_BUILDER.write_text(qurtubi_patched, encoding="utf-8")
    QUSHAYRI_BUILDER.write_text(qushayri_patched, encoding="utf-8")
    try:
        subprocess.run(
            [sys.executable, str(PREPARE), *sys.argv[1:]],
            cwd=ROOT,
            check=True,
        )
    finally:
        QURTUBI_BUILDER.write_text(qurtubi_original, encoding="utf-8")
        QUSHAYRI_BUILDER.write_text(qushayri_original, encoding="utf-8")

    if QURTUBI_BUILDER.read_text(encoding="utf-8") != qurtubi_original:
        raise SystemExit("Qurtubi canonical builder was not restored after 0.10.6 build")
    if QUSHAYRI_BUILDER.read_text(encoding="utf-8") != qushayri_original:
        raise SystemExit("Qushayri canonical builder was not restored after 0.10.6 build")
    print("Qurtubi 0.10.6 verse-label cleanup applied without source-indexing change")
    print("Qushayri 0.10.6 honorific glyphs restored from the edition legend; no shorthand leakage")


if __name__ == "__main__":
    main()

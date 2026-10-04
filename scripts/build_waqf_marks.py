"""Waqf (pause-mark) index derived from Tanzil's Uthmani text (quranacademy/quran-text mirror,
itself a cleaned derivative of tanzil.net's quran-uthmani.txt — the same text tanzil.net states
"completely matches the Medina Mushaf"). One verse per line, in canonical Quran order; a mark is
a small Arabic Unicode combining character attached to the word it follows.

Known gap: the لا (waqf mamnu', forbidden-stop) mark, U+06D9, is absent from this source — a
documented regex-stripping bug in the common Tanzil-derivative pipeline (confirmed against two
other independent mirrors, both also missing it). The six marks below are otherwise verified
against a real Mushaf page (Al-Hujurat 49:9 on page 516) and match published placements.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'scripts/data/quran-uthmani-tanzil.txt'
OUT = ROOT / 'app/src/main/assets/reader109/waqf.json'

COUNTS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
    112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,
    59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,
    52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,
    21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6,
]
assert len(COUNTS) == 114 and sum(COUNTS) == 6236

MARK_TYPES = {
    'ۘ': 'lazim',     # م   — obligatory stop
    'ۚ': 'jaiz',      # ج   — permissible stop
    'ۖ': 'sili',      # صلى — continuation preferred
    'ۗ': 'qili',      # قلى — stopping preferred
    'ۛ': 'muanaqah',  # ∴   — paired/interchangeable stop
    'ۜ': 'sakta',     # سكتة — brief pause, no breath break
}

lines = SOURCE.read_text(encoding='utf-8').splitlines()
assert len(lines) == 6236, f'expected 6236 verse lines, got {len(lines)}'

waqf = {}
i = 0
for surah, verse_count in enumerate(COUNTS, start=1):
    for ayah in range(1, verse_count + 1):
        words = lines[i].split(' ')
        i += 1
        marks = [{'afterWord': wi, 'type': MARK_TYPES[ch]}
                 for wi, w in enumerate(words) for ch in w if ch in MARK_TYPES]
        if marks:
            waqf[f'{surah}:{ayah}'] = marks

OUT.write_text(json.dumps({'schema': 1, 'source': 'tanzil-uthmani', 'marks': waqf},
                           ensure_ascii=False, separators=(',', ':')))
print('verses with marks:', len(waqf), 'total marks:', sum(len(v) for v in waqf.values()))

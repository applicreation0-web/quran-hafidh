"""Per-verse Arabic text index derived from Tanzil's Uthmani text (see build_waqf_marks.py for
the same source's provenance: quranacademy/quran-text mirror, itself a cleaned derivative of
tanzil.net's quran-uthmani.txt — the same text tanzil.net states "completely matches the Medina
Mushaf"). One verse per line, in canonical Quran order.

Used only for content verification (InkContentVerifier): comparing ML Kit's recognized text
against the real word, never for rendering — the Mushaf page itself is always drawn from the
unmodified KFQC SVG corpus.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'scripts/data/quran-uthmani-tanzil.txt'
OUT = ROOT / 'app/src/main/assets/reader109/verses_text.json'

COUNTS = [
    7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,
    112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,
    59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,
    52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,
    21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6,
]
assert len(COUNTS) == 114 and sum(COUNTS) == 6236

lines = SOURCE.read_text(encoding='utf-8').splitlines()
assert len(lines) == 6236, f'expected 6236 verse lines, got {len(lines)}'

verses = {}
i = 0
for surah, verse_count in enumerate(COUNTS, start=1):
    for ayah in range(1, verse_count + 1):
        verses[f'{surah}:{ayah}'] = lines[i]
        i += 1

OUT.write_text(json.dumps({'schema': 1, 'source': 'tanzil-uthmani', 'verses': verses},
                           ensure_ascii=False, separators=(',', ':')))
print('verses written:', len(verses))

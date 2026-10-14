#!/usr/bin/env python3
"""Audit a public, no-text 1903 Ibn Kathir word-start locator catalogue."""
from pathlib import Path
import csv, hashlib, re, sys
ROOT=Path(__file__).resolve().parents[1]
path=ROOT/'docs/ibn_kathir_public_amorce_keys_v1.csv'
EXPECTED='865dd26e66cc186f7f9a5ec87c9c065597befa350be5e1e0e55efca8494f7dfb'
raw=path.read_bytes()
assert hashlib.sha256(raw).hexdigest()==EXPECTED, 'Public locator catalogue changed'
rows=list(csv.DictReader(raw.decode('utf-8').splitlines()))
assert len(rows)==1903
assert set(rows[0])=={'group_id','surah','start_ayah','end_ayah','start_word_key','title_present'}
ids=set()
by_sura={}
titles=0
for r in rows:
    s,a,b=map(int,(r['surah'],r['start_ayah'],r['end_ayah']))
    assert 1<=s<=114 and 1<=a<=b<=286
    gid=f'IKEN{s:03d}_{a:03d}_{b:03d}'
    assert gid==r['group_id'] and gid not in ids
    assert r['start_word_key']==f'{s}:{a}:1' and r['title_present'] in ('0','1')
    ids.add(gid)
    titles+=int(r['title_present'])
    by_sura.setdefault(s,[]).append((a,b))
assert set(by_sura)==set(range(1,115)) and titles==1708
verses=0
for s,groups in by_sura.items():
    previous=0
    for a,b in groups:
        assert a==previous+1,'Gap or duplicated ayah in chapter '+str(s)
        verses+=b-a+1
        previous=b
assert verses==6236
print('PASS: 1903 public locators, 6236 verses, 114 chapters, 1708 title flags, zero licensed English text')

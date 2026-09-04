#!/usr/bin/env python3
"""Fail closed unless Plus embeds the exact approved three-tafsir corpus."""
from __future__ import annotations
import argparse, base64, gzip, hashlib, re, sqlite3, tempfile, zipfile
from pathlib import Path

JALALAYN_DB_SHA='26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56'
JALALAYN_ARCHIVE_SHA='824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680'
JALALAYN_PARTS=[f'assets/tafsir/al_jalalayn_en.sqlite.gz.part{i:02d}' for i in range(4)]
V2={
 'qushayri':{'parts':[f'assets/tafsir/qushayri_en.sqlite.gz.b64.part{i:02d}' for i in range(1)],'archive_sha':'e38c894b5fe20bb10c8d3a12b5de8c1fc1ed2173709c7ab491faac1cda79f552','db_sha':'f83d181815a2dd19cc3f598048d503022c6d2827ce353149c03797f63158b6d2','entries':809},
 'qurtubi':{'parts':[f'assets/tafsir/qurtubi_en.sqlite.gz.b64.part{i:02d}' for i in range(4)],'archive_sha':'fff869e3504affa565cfbfaf321a22f4ad47f6108da87bc0ebfa287084a7cad9','db_sha':'4f3e890085f8d991818d7b3fe9280d534adcf2242ac1694c9cc985aa842ea87e','entries':432},
}
ARABIC=re.compile(r'[\u0600-\u06ff]')

def sha(data:bytes)->str:return hashlib.sha256(data).hexdigest()
def open_db(blob:bytes):
    tmp=tempfile.NamedTemporaryFile(suffix='.sqlite');tmp.write(blob);tmp.flush()
    return tmp,sqlite3.connect(f'file:{tmp.name}?mode=ro',uri=True)

def audit_jalalayn(z,names):
    missing=[p for p in JALALAYN_PARTS if p not in names]
    if missing:raise SystemExit(f'Jalalayn parts missing: {missing}')
    compressed=b''.join(z.read(p) for p in JALALAYN_PARTS)
    if sha(compressed)!=JALALAYN_ARCHIVE_SHA:raise SystemExit('Jalalayn archive checksum mismatch')
    db=gzip.decompress(compressed)
    if sha(db)!=JALALAYN_DB_SHA:raise SystemExit('Jalalayn DB checksum mismatch')
    tmp,con=open_db(db)
    try:
        if con.execute('PRAGMA quick_check').fetchone()[0]!='ok':raise SystemExit('Jalalayn quick_check failed')
        comments=con.execute('SELECT COUNT(*) FROM verse_commentary').fetchone()[0]
        notes=con.execute('SELECT COUNT(*) FROM verse_note').fetchone()[0]
    finally:con.close();tmp.close()
    if (comments,notes)!=(6236,427):raise SystemExit(f'Unexpected Jalalayn counts: {comments}/{notes}')

def audit_v2(z,names,edition,spec):
    missing=[p for p in spec['parts'] if p not in names]
    if missing:raise SystemExit(f'{edition} parts missing: {missing}')
    encoded=b''.join(z.read(p) for p in spec['parts'])
    try:compressed=base64.b64decode(encoded,validate=True)
    except Exception as exc:raise SystemExit(f'{edition} base64 invalid: {exc}')
    if sha(compressed)!=spec['archive_sha']:raise SystemExit(f'{edition} archive checksum mismatch')
    db=gzip.decompress(compressed)
    if sha(db)!=spec['db_sha']:raise SystemExit(f'{edition} DB checksum mismatch')
    tmp,con=open_db(db)
    try:
        if con.execute('PRAGMA quick_check').fetchone()[0]!='ok':raise SystemExit(f'{edition} quick_check failed')
        meta=dict(con.execute('SELECT key,value FROM source_metadata'))
        if meta.get('schema_version')!='2' or meta.get('edition_id')!=edition:raise SystemExit(f'{edition} metadata mismatch')
        if meta.get('arabic_included')!='false':raise SystemExit(f'{edition} must contain no Arabic source text')
        count=con.execute('SELECT COUNT(*) FROM tafsir_entry').fetchone()[0]
        if count!=spec['entries']:raise SystemExit(f'{edition} entry count mismatch: {count}')
        bad_arabic=0
        for tr,co in con.execute('SELECT verse_translation, commentary FROM tafsir_entry'):
            if ARABIC.search((tr or '')+(co or '')):bad_arabic+=1
        if bad_arabic:raise SystemExit(f'{edition} contains Arabic-script rows: {bad_arabic}')
        if edition=='qushayri':
            if meta.get('english_verse_translation_included')!='true':raise SystemExit('Qushayri English verse translation flag missing')
            for s,a in [(2,68),(4,167),(4,176)]:
                n=con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=?',(s,a,a)).fetchone()[0]
                if n<1:raise SystemExit(f'Qushayri regression missing {s}:{a}')
            if con.execute("SELECT COUNT(*) FROM tafsir_entry WHERE trim(verse_translation)<>''").fetchone()[0]<800:raise SystemExit('Qushayri English verse translations unexpectedly sparse')
            longest=con.execute('SELECT MAX(length(commentary)) FROM tafsir_entry').fetchone()[0]
            if longest < 12000:raise SystemExit(f'Qushayri long commentary unexpectedly truncated: {longest}')
        else:
            if con.execute("SELECT COUNT(*) FROM tafsir_entry WHERE lower(verse_translation||' '||commentary) LIKE '%sunniconnect%'").fetchone()[0]:raise SystemExit('Qurtubi third-party scan contamination detected')
            for s,a in [(1,1),(2,142),(2,254),(3,96),(4,11),(4,22)]:
                n=con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=?',(s,a,a)).fetchone()[0]
                if n<1:raise SystemExit(f'Qurtubi regression missing {s}:{a}')
            if con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=4 AND verse_start<=23 AND verse_end>=23').fetchone()[0]!=0:raise SystemExit('Qurtubi 4:23 must remain unavailable with volumes 1-4')
            r=con.execute('SELECT verse_start,verse_end,length(commentary) FROM tafsir_entry WHERE surah=4 AND verse_start<=12 AND verse_end>=12').fetchone()
            if not r or r[0:2]!=(11,14) or r[2] < 60000:raise SystemExit(f'Qurtubi 4:11-14 range/truncation mismatch: {r}')
            longest=con.execute('SELECT MAX(length(commentary)) FROM tafsir_entry').fetchone()[0]
            if longest < 90000:raise SystemExit(f'Qurtubi long commentary unexpectedly truncated: {longest}')
    finally:con.close();tmp.close()

def main():
    ap=argparse.ArgumentParser();ap.add_argument('apk',type=Path);args=ap.parse_args()
    if not args.apk.is_file():raise SystemExit(f'Plus APK not found: {args.apk}')
    with zipfile.ZipFile(args.apk) as z:
        names=set(z.namelist())
        if any(n.casefold().endswith('.pdf') for n in names):raise SystemExit('Source PDF must never be embedded in Plus')
        audit_jalalayn(z,names)
        for edition,spec in V2.items():audit_v2(z,names,edition,spec)
    print('Verified Plus APK: Jalalayn 6236/427; Qushayri 809; Qurtubi 432; long blocks intact; no PDFs/Arabic scan text')

if __name__=='__main__':main()

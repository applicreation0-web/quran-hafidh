#!/usr/bin/env python3
"""Fail closed unless Plus embeds the approved three-tafsir corpus."""
from __future__ import annotations
import argparse,base64,gzip,hashlib,json,re,sqlite3,tempfile,zipfile
from pathlib import Path

JALALAYN_DB_SHA='26d8715a9bcecda6cb6397f0d8a530cb9404bb69ba66ed5264ed3f5b16d11a56'
JALALAYN_ARCHIVE_SHA='824fa202ad2b47aabdc6910f4792e0c8951a5cc8a641f47a2bab70de73b90680'
JALALAYN_PARTS=[f'assets/tafsir/al_jalalayn_en.sqlite.gz.part{i:02d}' for i in range(4)]
V2={
 'qushayri':{
     'parts':[f'assets/tafsir/qushayri_en.sqlite.gz.b64.part{i:02d}' for i in range(1)],
     'entries':720,
     'coverage':{1:7,2:286,3:200,4:176},
     'source_structure':{
         'raw_segment_count':'806',
         'translation_only_anchor_count':'86',
         'grouped_source_range_count':'76',
     },
 },
 'qurtubi':{'parts':[f'assets/tafsir/qurtubi_en.sqlite.gz.b64.part{i:02d}' for i in range(4)],'entries':432,'coverage':{1:7,2:286,3:200,4:22}},
}
ARABIC=re.compile(r'[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]')
PUA=re.compile(r'[\ue000-\uf8ff]')
QSH_HEADER=re.compile(r'(?:^|\n\n)(?:\d+\s*\|\s*•|•\s*Laṭāʾif|Subtle Allusions\s*\[|Laṭāʾif al-ishārāt\s*\[)')
QSH_SURA_HEADING=re.compile(r'(?:^|\n\n)S(?:ūrat|urāt|ūra)\b',re.I)

def sha(data:bytes)->str:return hashlib.sha256(data).hexdigest()
def open_db(blob:bytes):
    tmp=tempfile.NamedTemporaryFile(suffix='.sqlite');tmp.write(blob);tmp.flush()
    return tmp,sqlite3.connect(f'file:{tmp.name}?mode=ro',uri=True)

def logical_digest(con):
    meta=dict(con.execute('select key,value from source_metadata order by key'))
    columns=[row[1] for row in con.execute('pragma table_info(tafsir_entry)')]
    rows=con.execute('select '+','.join(columns)+' from tafsir_entry order by id').fetchall()
    payload={'metadata':sorted(meta.items()),'columns':columns,'rows':rows}
    return hashlib.sha256(json.dumps(payload,ensure_ascii=False,separators=(',',':'),sort_keys=True).encode('utf-8')).hexdigest()

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
    try:db=gzip.decompress(compressed)
    except Exception as exc:raise SystemExit(f'{edition} gzip invalid: {exc}')
    tmp,con=open_db(db)
    try:
        if con.execute('PRAGMA quick_check').fetchone()[0]!='ok':raise SystemExit(f'{edition} quick_check failed')
        meta=dict(con.execute('SELECT key,value FROM source_metadata'))
        if meta.get('schema_version')!='2' or meta.get('edition_id')!=edition:raise SystemExit(f'{edition} metadata mismatch')
        if meta.get('arabic_included')!='false':raise SystemExit(f'{edition} must contain no Arabic source text')
        if int(meta.get('entry_count','-1'))!=spec['entries']:raise SystemExit(f'{edition} metadata entry count mismatch')
        for key,expected in spec.get('source_structure',{}).items():
            if meta.get(key)!=expected:raise SystemExit(f'{edition} source structure mismatch for {key}: {meta.get(key)!r} != {expected!r}')
        count=con.execute('SELECT COUNT(*) FROM tafsir_entry').fetchone()[0]
        if count!=spec['entries']:raise SystemExit(f'{edition} entry count mismatch: {count}')
        text_rows=list(con.execute('SELECT verse_translation, commentary FROM tafsir_entry'))
        if any(not (tr or '').strip() for tr,_ in text_rows):raise SystemExit(f'{edition} contains blank verse translation rows')
        if any(not (co or '').strip() for _,co in text_rows):raise SystemExit(f'{edition} contains blank commentary rows')
        if any(ARABIC.search((tr or '')+(co or '')) for tr,co in text_rows):raise SystemExit(f'{edition} contains Arabic-script rows')
        if any(PUA.search((tr or '')+(co or '')) for tr,co in text_rows):raise SystemExit(f'{edition} contains private-use PDF glyphs')
        for surah,last_ayah in spec['coverage'].items():
            missing_ayahs=[ayah for ayah in range(1,last_ayah+1) if con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=?',(surah,ayah,ayah)).fetchone()[0]<1]
            if missing_ayahs:raise SystemExit(f'{edition} exhaustive coverage gap in sura {surah}: {missing_ayahs[:25]}')
        if edition=='qushayri':
            if meta.get('english_verse_translation_included')!='true':raise SystemExit('Qushayri English verse translation flag missing')
            for s,a in [(2,11),(2,68),(4,167),(4,176)]:
                n=con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=? AND verse_start<=? AND verse_end>=?',(s,a,a)).fetchone()[0]
                if n<1:raise SystemExit(f'Qushayri regression missing {s}:{a}')
            translated=con.execute("SELECT COUNT(*) FROM tafsir_entry WHERE trim(verse_translation)<>''").fetchone()[0]
            if translated!=spec['entries']:raise SystemExit(f'Qushayri English verse translation missing from {spec["entries"]-translated} logical entry/entries')
            grouped=con.execute("SELECT COUNT(*) FROM tafsir_entry WHERE instr(verse_translation, '[' || surah || ':')>0").fetchone()[0]
            if grouped<int(meta['grouped_source_range_count']):raise SystemExit('Qushayri grouped ranges lost explicit source-anchor labels')
            if any(QSH_HEADER.search(co or '') for _,co in text_rows):raise SystemExit('Qushayri page header leaked into commentary')
            if any(QSH_SURA_HEADING.search(co or '') for _,co in text_rows):raise SystemExit('Qushayri Sura introduction leaked into previous verse')
            longest=con.execute('SELECT MAX(length(commentary)) FROM tafsir_entry').fetchone()[0]
            if longest < 12000:raise SystemExit(f'Qushayri long commentary unexpectedly truncated: {longest}')
        else:
            if con.execute("SELECT COUNT(*) FROM tafsir_entry WHERE lower(verse_translation||' '||commentary) LIKE '%sunniconnect%'").fetchone()[0]:raise SystemExit('Qurtubi third-party scan contamination detected')
            if con.execute('SELECT COUNT(*) FROM tafsir_entry WHERE surah=4 AND verse_start<=23 AND verse_end>=23').fetchone()[0]!=0:raise SystemExit('Qurtubi 4:23 must remain unavailable with volumes 1-4')
            r=con.execute('SELECT verse_start,verse_end,length(commentary) FROM tafsir_entry WHERE surah=4 AND verse_start<=12 AND verse_end>=12').fetchone()
            if not r or r[0:2]!=(11,14) or r[2] < 60000:raise SystemExit(f'Qurtubi 4:11-14 range/truncation mismatch: {r}')
            longest=con.execute('SELECT MAX(length(commentary)) FROM tafsir_entry').fetchone()[0]
            if longest < 90000:raise SystemExit(f'Qurtubi long commentary unexpectedly truncated: {longest}')
        return logical_digest(con)
    finally:con.close();tmp.close()

def main():
    ap=argparse.ArgumentParser();ap.add_argument('apk',type=Path);args=ap.parse_args()
    if not args.apk.is_file():raise SystemExit(f'Plus APK not found: {args.apk}')
    logical={}
    with zipfile.ZipFile(args.apk) as z:
        names=set(z.namelist())
        if any(n.casefold().endswith('.pdf') for n in names):raise SystemExit('Source PDF must never be embedded in Plus')
        audit_jalalayn(z,names)
        for edition,spec in V2.items():logical[edition]=audit_v2(z,names,edition,spec)
    print('Verified Plus APK: Jalalayn 6236/427; Qushayri 806 source anchors -> 720 logical entries with translations preserved; Qurtubi 432; exhaustive coverage; long blocks intact; no PDFs/Arabic/PUA leakage')
    print(f'Logical corpus digests: Qushayri={logical["qushayri"]}; Qurtubi={logical["qurtubi"]}')

if __name__=='__main__':main()

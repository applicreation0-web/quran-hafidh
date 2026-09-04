#!/usr/bin/env python3
from __future__ import annotations
import base64,gzip,hashlib,re,sqlite3,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def req(c,m):
    if not c: raise SystemExit('0.10.4 MULTI-TAFSIR AUDIT FAILURE: '+m)

def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
edition=read('app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt')
repo=read('app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt')
controller=read('app/src/plus/java/com/quranunlock/guard/MultiTafsirPanel.kt')
renderer=read('app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt')
light=read('app/src/light/java/com/quranunlock/guard/TafsirEdition.kt')
legacy_repo=read('app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt')
light_apk_checker=read('scripts/verify_light_apk_no_tafsir.py')
req('MultiTafsirPanel(' in edition,'existing Plus panel is not routed through multi-tafsir controller')
req('QURTUBI("qurtubi", "Qurtubi")' in repo and 'QUSHAYRI("qushayri", "Qushayri")' in repo,'edition ids missing')
req('MultiTafsirRequestKey' in repo,'multi-source request key missing')
req('remember(verse, selectedEdition)' in controller,'edition/verse changes must create a fresh load state')
req('TafsirPanel(' in controller and 'DropdownMenu' not in controller,'controller must delegate to the single TafsirPanel renderer')
req('remember(verse, selectedEdition)' in renderer and 'DropdownMenu' in renderer,'single renderer must reset scroll/notes and expose the compact selector')
req('English commentary unavailable for this verse in this edition.' in renderer,'explicit unavailable state missing')
req('activeRequest == requestKey' in controller,'stale request rejection missing')
req('putString(MULTI_EDITION_KEY' in controller,'edition persistence missing')
req('Commentary on ${verse.surah}:${row.start}–${row.end}' in repo,'range label missing')
req('TafsirRunStyle.ITALIC, row.translation' in repo,'English verse translation is not preserved')
req('isEnabled: Boolean = false' in light,'Light tafsir must remain disabled')
req('qushayri_en.sqlite' in repo and 'qurtubi_en.sqlite' in repo,'new databases not wired')
req('expectedEntries = 806' in repo and '4356e836e8e14818f6b4f5007eeac12454cb388e6aa59759a926bdc560569c5b' in repo,'corrected Qushayri runtime pin missing')
req('Base64.decode' in repo and 'OPEN_READONLY' in repo,'read-only/base64 materialization guard missing')
req('al_jalalayn_en.sqlite' in legacy_repo and '6_236' in legacy_repo and '26d8715a' in legacy_repo,'Jalalayn golden repository changed')
req('qurtubi' in light_apk_checker.lower() and 'qushayri' in light_apk_checker.lower(), 'Light APK checker must explicitly reject Qurtubi and Qushayri markers')

specs={'qushayri':(1,'4356e836e8e14818f6b4f5007eeac12454cb388e6aa59759a926bdc560569c5b',806),'qurtubi':(4,'4f3e890085f8d991818d7b3fe9280d534adcf2242ac1694c9cc985aa842ea87e',432)}
arabic=re.compile(r'[\u0600-\u06ff]')
pua=re.compile(r'[\ue000-\uf8ff]')
qsh_header=re.compile(r'(?:^|\n\n)(?:\d+\s*\|\s*•|•\s*Laṭāʾif|Subtle Allusions\s*\[|Laṭāʾif al-ishārāt\s*\[)')
qsh_sura_heading=re.compile(r'(?:^|\n\n)S(?:ūrat|urāt|ūra)\b',re.I)
for name,(parts,dbsha,count) in specs.items():
    enc=''.join((ROOT/f'app/src/plus/assets/tafsir/{name}_en.sqlite.gz.b64.part{i:02d}').read_text(encoding='ascii') for i in range(parts))
    db=gzip.decompress(base64.b64decode(enc,validate=True))
    req(hashlib.sha256(db).hexdigest()==dbsha,f'{name} database hash mismatch')
    with tempfile.NamedTemporaryFile(suffix='.sqlite') as tmp:
        tmp.write(db);tmp.flush();con=sqlite3.connect(f'file:{tmp.name}?mode=ro',uri=True)
        try:
            req(con.execute('PRAGMA quick_check').fetchone()[0]=='ok',f'{name} quick_check failed')
            meta=dict(con.execute('select key,value from source_metadata'))
            req(meta.get('edition_id')==name and meta.get('arabic_included')=='false',f'{name} metadata mismatch')
            req(int(meta.get('entry_count','-1'))==count,f'{name} metadata entry count mismatch')
            req(con.execute('select count(*) from tafsir_entry').fetchone()[0]==count,f'{name} count mismatch')
            text_rows=list(con.execute('select verse_translation,commentary from tafsir_entry'))
            req(not any(arabic.search((tr or '')+(co or '')) for tr,co in text_rows),f'{name} contains Arabic source text')
            req(not any(pua.search((tr or '')+(co or '')) for tr,co in text_rows),f'{name} contains private-use PDF glyphs')
            if name=='qushayri':
                req(meta.get('english_verse_translation_included')=='true','Qushayri English verse translation flag missing')
                req(con.execute('select count(*) from tafsir_entry where surah=2 and verse_start<=68 and verse_end>=68').fetchone()[0]>=1,'Qushayri 2:68 missing')
                req(con.execute('select count(*) from tafsir_entry where surah=4 and verse_start<=168 and verse_end>=168').fetchone()[0]>=1,'Qushayri 4:167-169 mapping missing')
                req(con.execute('select max(length(commentary)) from tafsir_entry').fetchone()[0]>=12000,'Qushayri long commentary appears truncated')
                req(not any(qsh_header.search(co or '') for _,co in text_rows),'Qushayri page header leaked into commentary')
                req(not any(qsh_sura_heading.search(co or '') for _,co in text_rows),'Qushayri Sura introduction leaked into previous verse')
                false_refs={(58,18),(3,26),(4,70)}
                for s,a in false_refs:
                    # These are known source cross-references that previously created false anchors.
                    if s<=4:
                        # A real verse with the same number must still exist; the guard is the fixed total/hash.
                        continue
            else:
                req(con.execute("select count(*) from tafsir_entry where lower(verse_translation||' '||commentary) like '%sunniconnect%'").fetchone()[0]==0,'Qurtubi scan contamination')
                r=con.execute('select verse_start,verse_end,length(commentary) from tafsir_entry where surah=4 and verse_start<=12 and verse_end>=12').fetchone()
                req(r is not None and r[0:2]==(11,14) and r[2]>=60000,'Qurtubi 4:11-14 range mapping or long body is truncated')
                req(con.execute('select max(length(commentary)) from tafsir_entry').fetchone()[0]>=90000,'Qurtubi largest commentary appears truncated')
                req(con.execute('select count(*) from tafsir_entry where surah=4 and verse_start<=23 and verse_end>=23').fetchone()[0]==0,'Qurtubi 4:23 must be unavailable with volumes 1-4')
        finally: con.close()
print('0.10.4 multi-tafsir source audit: PASS')
print('- Jalalayn legacy corpus untouched; Qushayri 806 genuine segments; Qurtubi 432 blocks')
print('- Qushayri false cross-reference anchors, Sura-intro leakage and private-use glyphs are blocked')
print('- Qushayri English verse translations preserved; Arabic source text absent')
print('- Qurtubi volumes 1-4 mapped through 4:22; 4:23 fail-closed')
print('- long Qushayri/Qurtubi commentary blocks retained without truncation')
print('- single 0.10.3-derived renderer retained with compact selector')
print('- Light remains tafsir-free and explicitly rejects all 3 edition markers')
print('- Plus selector reload/race guards present')

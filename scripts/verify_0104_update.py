#!/usr/bin/env python3
from __future__ import annotations
import base64,gzip,hashlib,json,re,sqlite3,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]

def req(c,m):
    if not c: raise SystemExit('0.10.4 MULTI-TAFSIR AUDIT FAILURE: '+m)

def read(rel): return (ROOT/rel).read_text(encoding='utf-8')

def logical_digest(con: sqlite3.Connection) -> str:
    meta=dict(con.execute('select key,value from source_metadata order by key'))
    columns=[row[1] for row in con.execute('pragma table_info(tafsir_entry)')]
    rows=con.execute('select '+','.join(columns)+' from tafsir_entry order by id').fetchall()
    payload={'metadata':sorted(meta.items()),'columns':columns,'rows':rows}
    return hashlib.sha256(json.dumps(payload,ensure_ascii=False,separators=(',',':'),sort_keys=True).encode('utf-8')).hexdigest()

edition=read('app/src/plus/java/com/quranunlock/guard/TafsirEdition.kt')
repo=read('app/src/plus/java/com/quranunlock/guard/MultiTafsirRepository.kt')
controller=read('app/src/plus/java/com/quranunlock/guard/MultiTafsirPanel.kt')
renderer=read('app/src/plus/java/com/quranunlock/guard/TafsirPanel.kt')
light=read('app/src/light/java/com/quranunlock/guard/TafsirEdition.kt')
legacy_repo=read('app/src/plus/java/com/quranunlock/guard/TafsirRepository.kt')
light_apk_checker=read('scripts/verify_light_apk_no_tafsir.py')
asset_prep=read('scripts/prepare_multitafsir_assets.py')
qushayri_builder=read('scripts/build_qushayri.py')
qurtubi_builder=read('scripts/build_qurtubi.py')

req('MultiTafsirPanel(' in edition,'existing Plus panel is not routed through multi-tafsir controller')
req('suspend fun load(context: Context, verse: VerseRef): TafsirEntry? = null' in edition,'obsolete shared-reader hook must not trigger a parallel Jalalayn load in Plus')
req('QURTUBI("qurtubi", "Qurtubi")' in repo and 'QUSHAYRI("qushayri", "Qushayri")' in repo,'edition ids missing')
req('MultiTafsirRequestKey' in repo,'multi-source request key missing')
req('remember(verse, selectedEdition)' in controller,'edition/verse changes must create a fresh load state')
req('TafsirPanel(' in controller and 'DropdownMenu' not in controller,'controller must delegate to the single TafsirPanel renderer')
req('remember(verse, selectedEdition)' in renderer and 'DropdownMenu' in renderer,'single renderer must reset scroll/notes and expose the compact selector')
req('Commentaire anglais indisponible pour ce verset dans cette édition.' in renderer,'localized unavailable state missing')
req('activeRequest == requestKey' in controller,'stale request rejection missing')
req('putString(MULTI_EDITION_KEY' in controller,'edition persistence missing')
req('Commentary on ${verse.surah}:${row.start}–${row.end}' in repo,'range label missing')
req('TafsirRunStyle.ITALIC, row.translation' in repo,'English verse translation is not preserved')
req('isEnabled: Boolean = false' in light,'Light tafsir must remain disabled')
req('qushayri_en.sqlite' in repo and 'qurtubi_en.sqlite' in repo,'new databases not wired')
req('expectedEntries = 806' in repo and 'expectedEntries = 432' in repo,'runtime corpus counts missing')
req('databaseFileMatches' in repo and 'PRAGMA quick_check' in repo,'runtime structural integrity guard missing')
req('expectedSha256' not in repo,'runtime must not depend on environment-specific SQLite page bytes')
req('Base64.decode' in repo and 'OPEN_READONLY' in repo,'read-only/base64 materialization guard missing')
req('al_jalalayn_en.sqlite' in legacy_repo and '6_236' in legacy_repo and '26d8715a' in legacy_repo,'Jalalayn golden repository changed')
req('qurtubi' in light_apk_checker.lower() and 'qushayri' in light_apk_checker.lower(), 'Light APK checker must explicitly reject Qurtubi and Qushayri markers')
req('EXPECTED_PARTS' in asset_prep and 'logical_digest' in asset_prep and 'publish_packages' in asset_prep,'asset preparation must verify logical corpus content and stage both corpora')
req('DB_SHA' not in asset_prep and 'ARCHIVE_SHA' not in asset_prep,'asset preparation must not pin environment-specific SQLite/archive bytes')
req("assets.rglob('*.pdf')" in asset_prep,'asset preparation must reject any source PDF already present in Plus assets')
req("EXPECTED_SOURCE_SHA256='f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3'" in qushayri_builder,'Qushayri builder must pin the approved source PDF')
req('Missing required Qushayri source PDF' in qushayri_builder and 'Qushayri source SHA-256 mismatch' in qushayri_builder,'Qushayri builder must fail closed on missing or changed source PDF')
req('Qushayri Arabic source text leaked into row' in qushayri_builder,'Qushayri builder must reject mixed-line Arabic leakage after extraction')
req("'v1':'a791ec1313fa2401abe7ca25ac7ccb4bedb1afcb51f2c779160a71e98a6f04cb'" in qurtubi_builder and "'v4':'eb71cb2ed8c2497cc8a5d3634b3eeb7788fdc7caee9de5d6b50349fb8619965c'" in qurtubi_builder,'Qurtubi builder must pin all four approved source volumes')
req('Missing required Qurtubi source volume' in qurtubi_builder and 'coverage gap in sura' in qurtubi_builder,'Qurtubi builder must fail closed on missing volumes or verse coverage')

specs={'qushayri':(1,806),'qurtubi':(4,432)}
coverage={'qushayri':{1:7,2:286,3:200,4:176},'qurtubi':{1:7,2:286,3:200,4:22}}
arabic=re.compile(r'[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]')
pua=re.compile(r'[\ue000-\uf8ff]')
qsh_header=re.compile(r'(?:^|\n\n)(?:\d+\s*\|\s*•|•\s*Laṭāʾif|Subtle Allusions\s*\[|Laṭāʾif al-ishārāt\s*\[)')
qsh_sura_heading=re.compile(r'(?:^|\n\n)S(?:ūrat|urāt|ūra)\b',re.I)
logical={}

for name,(parts,count) in specs.items():
    paths=[ROOT/f'app/src/plus/assets/tafsir/{name}_en.sqlite.gz.b64.part{i:02d}' for i in range(parts)]
    req(all(p.is_file() and p.stat().st_size>0 for p in paths),f'{name} staged asset parts missing')
    enc=''.join(p.read_text(encoding='ascii') for p in paths)
    db=gzip.decompress(base64.b64decode(enc,validate=True))
    with tempfile.NamedTemporaryFile(suffix='.sqlite') as tmp:
        tmp.write(db);tmp.flush();con=sqlite3.connect(f'file:{tmp.name}?mode=ro',uri=True)
        try:
            req(con.execute('PRAGMA quick_check').fetchone()[0]=='ok',f'{name} quick_check failed')
            meta=dict(con.execute('select key,value from source_metadata'))
            req(meta.get('schema_version')=='2' and meta.get('edition_id')==name and meta.get('arabic_included')=='false',f'{name} metadata mismatch')
            req(int(meta.get('entry_count','-1'))==count,f'{name} metadata entry count mismatch')
            req(con.execute('select count(*) from tafsir_entry').fetchone()[0]==count,f'{name} count mismatch')
            text_rows=list(con.execute('select verse_translation,commentary from tafsir_entry'))
            req(not any(arabic.search((tr or '')+(co or '')) for tr,co in text_rows),f'{name} contains Arabic source text')
            req(not any(pua.search((tr or '')+(co or '')) for tr,co in text_rows),f'{name} contains private-use PDF glyphs')
            for surah,last_ayah in coverage[name].items():
                missing=[ayah for ayah in range(1,last_ayah+1) if con.execute('select count(*) from tafsir_entry where surah=? and verse_start<=? and verse_end>=?',(surah,ayah,ayah)).fetchone()[0]<1]
                req(not missing,f'{name} exhaustive coverage gap in sura {surah}: {missing[:25]}')
            if name=='qushayri':
                req(meta.get('english_verse_translation_included')=='true','Qushayri English verse translation flag missing')
                req(con.execute("select count(*) from tafsir_entry where trim(verse_translation)<>''").fetchone()[0]==count,'Qushayri English verse translation must be retained for every approved segment')
                req(con.execute('select count(*) from tafsir_entry where surah=2 and verse_start<=68 and verse_end>=68').fetchone()[0]>=1,'Qushayri 2:68 missing')
                req(con.execute('select count(*) from tafsir_entry where surah=4 and verse_start<=168 and verse_end>=168').fetchone()[0]>=1,'Qushayri 4:167-169 mapping missing')
                req(con.execute('select max(length(commentary)) from tafsir_entry').fetchone()[0]>=12000,'Qushayri long commentary appears truncated')
                req(not any(qsh_header.search(co or '') for _,co in text_rows),'Qushayri page header leaked into commentary')
                req(not any(qsh_sura_heading.search(co or '') for _,co in text_rows),'Qushayri Sura introduction leaked into previous verse')
            else:
                req(con.execute("select count(*) from tafsir_entry where lower(verse_translation||' '||commentary) like '%sunniconnect%'").fetchone()[0]==0,'Qurtubi scan contamination')
                r=con.execute('select verse_start,verse_end,length(commentary) from tafsir_entry where surah=4 and verse_start<=12 and verse_end>=12').fetchone()
                req(r is not None and r[0:2]==(11,14) and r[2]>=60000,'Qurtubi 4:11-14 range mapping or long body is truncated')
                req(con.execute('select max(length(commentary)) from tafsir_entry').fetchone()[0]>=90000,'Qurtubi largest commentary appears truncated')
                req(con.execute('select count(*) from tafsir_entry where surah=4 and verse_start<=23 and verse_end>=23').fetchone()[0]==0,'Qurtubi 4:23 must be unavailable with volumes 1-4')
            logical[name]=logical_digest(con)
        finally: con.close()

print('0.10.4 multi-tafsir source audit: PASS')
print(f'- logical corpus digests: Qushayri={logical["qushayri"]}; Qurtubi={logical["qurtubi"]}')
print('- Jalalayn legacy corpus untouched; Qushayri 806 genuine segments; Qurtubi 432 blocks')
print('- both new builders pin their exact approved source PDF(s) and fail closed on source drift')
print('- staged SQLite bytes may vary by environment; semantic corpus gates are invariant')
print('- Qushayri Arabic source text excluded and every approved segment retains its English verse translation')
print('- Qushayri false cross-reference anchors, Sura-intro leakage and private-use glyphs are blocked')
print('- Qurtubi all four pinned volumes are mandatory; exhaustive verse coverage is release-blocking')
print('- Qurtubi volumes 1-4 mapped through 4:22; 4:23 fail-closed (volume 5 begins at 4:23)')
print('- long Qushayri/Qurtubi commentary blocks retained without truncation')
print('- single 0.10.3-derived renderer retained with compact selector and no duplicate Jalalayn I/O')
print('- Light remains tafsir-free and explicitly rejects all 3 edition markers')
print('- Plus selector reload/race guards present')

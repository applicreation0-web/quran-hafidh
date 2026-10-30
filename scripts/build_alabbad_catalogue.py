#!/usr/bin/env python3
"""Rebuild reviewed al-Abbad catalogue from explicit editorial decisions.

No inferred candidate is promoted.  The decisions file is the editorial input;
this script checks canonical verse/token counts, geometry, source references and
SVG presence and emits the complete audit plus the admitted Android catalogue.
Source HTML is optional, kept outside git; --book-cache enables provenance checks.
"""
from __future__ import annotations
import argparse,csv,hashlib,json,re,unicodedata
from collections import Counter,defaultdict
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
GEOMETRY=ROOT/'hifz-app/src/main/word-source/quran-ws-v1.1.2'
TEXT=ROOT/'scripts/data/quran-uthmani-tanzil.txt'
DECISIONS=ROOT/'research/alabbad_editorial_decisions.json'

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def normalize_name(text):
    return ''.join(c for c in unicodedata.normalize('NFKD',text) if unicodedata.category(c)[0]!='M').replace('ـ','').strip()
def is_word(text):
    return any('\u0621'<=c<='\u064a' or c in 'ٱیک' for c in text)
def load_inputs():
    geometry=defaultdict(dict);hashes={}
    for path in sorted(GEOMETRY.glob('*.json')):
        data=json.loads(path.read_text());assert data['source_release']=='v1.1.2'
        hashes[str(path.relative_to(ROOT))]=sha(path)
        for page,rows in data['pages'].items():
            for key,*box in rows:
                s,a,w=map(int,key.split(':'));assert w not in geometry[(s,a)]
                assert 0<=box[0]<box[2]<=345 and 0<=box[1]<box[3]<=550
                geometry[(s,a)][w]={'page':int(page),'box':box}
    lines=TEXT.read_text().splitlines();assert len(lines)==len(geometry)==6236
    verses={};count_mismatches=[]
    for ref,line in zip(sorted(geometry),lines):
        # U+200A/U+2060 are embedded in Tanzil orthographic words before dagger
        # alif, not word boundaries. Basmala is prepended to surah openings.
        line=line.replace('\u200a','').replace('\u2060','')
        tokens=[w for w in line.split() if is_word(w)]
        if ref[1]==1 and ref[0] not in (1,9):tokens=tokens[4:]
        verses[ref]=tokens
        if len(tokens)!=len(geometry[ref]):count_mismatches.append(':'.join(map(str,ref)))
    # Known orthographic segmentation exception: لو ما / لوما. Never admitted.
    assert count_mismatches==['15:7'],count_mismatches
    return geometry,verses,hashes

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--book-cache',type=Path)
    parser.add_argument('--out',type=Path,default=ROOT/'research');args=parser.parse_args()
    geometry,verses,hashes=load_inputs();decisions=json.loads(DECISIONS.read_text())['decisions']
    assert [r['auditId'] for r in decisions]==list(range(1,293))
    decisions+=json.loads((ROOT/'research/alabbad_supplementary_decisions.json').read_text())['decisions']
    original=list(csv.DictReader((ROOT/'research/mutashabihat_292_dont_1_priorite_utilisateur.csv').open(encoding='utf-8-sig'),delimiter=';'))
    source_info={}
    names=re.findall(r'"([^"]+)"',(ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/QuranSurahNames.java').read_text().split('};')[0])[:114]
    name_map={normalize_name(n):i+1 for i,n in enumerate(names)}
    if args.book_cache:
        from bs4 import BeautifulSoup
        for n in range(1,15):
            path=args.book_cache/f's{n}.html';soup=BeautifulSoup(path.read_text(),'html.parser');chapter={}
            for article in soup.select('article.bk-page'):
                text=' '.join(article.stripped_strings)
                printed=int(re.search(r'ص\s*(\d+)',article['data-loc']).group(1))
                for name,nums in re.findall(r'(?:\[|(?<=﴾)\s*)([^\[\]:﴿﴾]+):\s*([0-9، ,]+)\]',text):
                    s=name_map.get(normalize_name(name))
                    if s:
                        for a in re.findall(r'\d+',nums):chapter.setdefault(f'{s}:{a}',[]).append(printed)
            source_info[f's{n}.html']={'sha256':sha(path),'refs':chapter}
    audit=[];approved=[];seen=set();svg_hashes={}
    for source in decisions:
        raw=original[source['auditId']-1] if source['auditId']<=292 else {}
        row=dict(source);row['originalCandidate']={k:raw.get(k) for k in ('verset_A','verset_B','mot_divergent_A','mot_divergent_B','section_livre')}
        for side in ('first','second'):
            ref=tuple(map(int,row[side].split(':')));assert ref in geometry
            row[side+'VerseWordCount']=len(geometry[ref])
            if side+'WordIndices' not in row:continue
            assert len(verses[ref])==len(geometry[ref]),(row['auditId'],ref)
            assert set(geometry[ref])==set(range(1,len(verses[ref])+1))
            words=[verses[ref][i-1] for i in row[side+'WordIndices']]
            assert words==row[side+'Words'],(row['auditId'],side,words)
            locations=[dict(wordIndex=i,word=verses[ref][i-1],**geometry[ref][i]) for i in row[side+'WordIndices']]
            assert len({x['page'] for x in locations})==1
            row[side+'Page']=locations[0]['page'];row[side+'Geometry']=locations
            lo=max(0,min(row[side+'WordIndices'])-4);hi=min(len(verses[ref]),max(row[side+'WordIndices'])+3)
            row[side+'Context']={'startWordIndex':lo+1,'words':verses[ref][lo:hi]}
            for loc in locations:
                path=ROOT/f"app/src/main/assets/mushaf/hafs/kfqc/svg-br/{loc['page']:03d}.svg.br"
                assert path.is_file(),path
                svg_hashes[str(path.relative_to(ROOT))]=sha(path)
        if row['auditId']!=292 and source_info:
            chapter=row['sourceUrl'].rsplit('/',1)[-1];info=source_info[chapter]
            row['bookChapterSha256']=info['sha256']
            row['printedPagesFirst']=sorted(set(info['refs'].get(row.get('originalFirst',row['first']),[])))
            row['printedPagesSecond']=sorted(set(info['refs'].get(row['second'],[])))
            if row['decision'] in ('retenu','corrigé'):
                assert row['printedPagesFirst'] and row['printedPagesSecond'],(row['auditId'],'source refs missing')
        if row['decision'] in ('retenu','corrigé'):
            key=tuple(sorted((row['first'],row['second'])));assert key not in seen,key;seen.add(key)
            approved.append({k:row[k] for k in ('auditId','first','second','firstPage','secondPage','firstWordIndices','secondWordIndices','firstWords','secondWords','kind','source','sourceUrl','priority')})
        audit.append(row)
    summary=dict(Counter(x['decision'] for x in audit));assert len(audit)==294
    args.out.mkdir(parents=True,exist_ok=True)
    metadata={'schemaVersion':1,'sourceBook':'ʿAbd al-Muḥsin al-ʿAbbād, آيات متشابهات الألفاظ في القرآن الكريم وكيف التمييز بينها','sourceBookUrl':'https://quranpedia.app/books/b10901/index.html','wordIndexBase':1,'geometrySpace':'viewBox 0 0 345 550','counts':summary,'approvedCount':len(approved),'verification':{'textSource':str(TEXT.relative_to(ROOT)),'textSha256':sha(TEXT),'geometrySource':'quran-ws/quran-svg-elements v1.1.2','editorialDecisionsSha256':sha(DECISIONS),'supplementaryDecisionsSha256':sha(ROOT/'research/alabbad_supplementary_decisions.json'),'geometrySha256':hashes,'svgSha256':svg_hashes,'tokenization':'Remove U+200A and U+2060, omit nonletter stop signs, remove prepended four-word basmala except surahs1/9. All admitted verse token counts equal QCF sidecar counts. 15:7 mismatch is excluded from admission.','limitation':'SVG files contain paths, no machine-readable word text. Geometry and canonical textual alignment are verified; this is not a claim of independent visual inspection of every glyph.','sourceProvenanceChecked':bool(source_info)}}
    (args.out/'alabbad_audit_294.json').write_text(json.dumps(dict(metadata,entries=audit),ensure_ascii=False,indent=2)+'\n')
    (args.out/'alabbad_catalogue_approved.json').write_text(json.dumps(dict(metadata,entries=approved),ensure_ascii=False,indent=2)+'\n')
    fields=['auditId','decision','reason','first','second','firstPage','secondPage','firstWordIndices','secondWordIndices','firstWords','secondWords','source','sourceUrl','printedPagesFirst','printedPagesSecond','duplicateOf']
    with (args.out/'alabbad_audit_294.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=fields,delimiter=';',extrasaction='ignore');writer.writeheader()
        for row in audit:writer.writerow({k:json.dumps(v,ensure_ascii=False) if isinstance(v,(dict,list)) else v for k,v in row.items()})
    print(json.dumps({'counts':summary,'approvedCount':len(approved),'sourceProvenanceChecked':bool(source_info)},ensure_ascii=False))
if __name__=='__main__':main()

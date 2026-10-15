#!/usr/bin/env python3
"""Read-only, title-free spatial preparation. Does not modify or invoke the reader."""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
import math
from pathlib import Path

COUNTS = [7,286,200,176,120,165,206,75,129,109,123,111,43,52,99,128,111,110,98,135,112,78,118,64,77,227,93,88,69,60,34,30,73,54,45,83,182,88,75,85,54,53,89,59,37,35,38,29,18,45,60,49,62,55,78,96,29,22,24,13,14,11,11,18,12,12,30,52,52,44,28,28,20,56,40,31,50,40,46,42,29,19,36,25,22,17,19,26,30,20,15,21,11,8,8,19,5,8,8,11,11,8,3,9,5,4,7,3,6,3,5,4,5,6]

def surah_batches(count):
    return [(i,min(i+9,count)) for i in range(1,count+1,10)]

def build_mapping(passages, geometry, chunks, expected_counts=COUNTS, expected_pages=604):
    """Return fresh numerical records plus issues; never mutate any input object."""
    canonical = [(s,a) for s,n in enumerate(expected_counts,1) for a in range(1,n+1)]
    canonical_set=set(canonical)
    selected=[]; ids=set(); clean=[]
    for p in passages:
        q={k:p[k] for k in ('id','surah','start','end')}
        s,a,b=q['surah'],q['start'],q['end']
        if not all(type(x) is int for x in (s,a,b)) or not 1<=s<=len(expected_counts) or not 1<=a<=b<=expected_counts[s-1]:
            raise ValueError('invalid passage range')
        if q['id'] in ids: raise ValueError('duplicate passage id')
        ids.add(q['id']); selected.extend((s,v) for v in range(a,b+1)); clean.append(q)
    if selected != canonical: raise ValueError('passage coverage must be unique, exhaustive and canonically ordered')
    pages=geometry['pages']
    if set(pages)!={str(i) for i in range(1,expected_pages+1)}: raise ValueError('geometry page domain')
    lines=defaultdict(list); issues=[]; words=defaultdict(list); seen_words=set()
    def issue(code,**values):
        value={'code':code,**values};issues.append(value);return value
    for page in range(1,expected_pages+1):
        pg=pages[str(page)]; vb=pg['viewBox']
        if len(vb)!=4 or not all(math.isfinite(x) for x in vb) or vb[2:]!=[345,550]: raise ValueError('geometry viewBox')
        previous=-math.inf
        for i,line in enumerate(pg['lines']):
            if line['page']!=page or line['top']<previous or not vb[1]<=line['top']<line['bottom']<=vb[1]+vb[3]: raise ValueError('geometry line bounds/order')
            previous=line['bottom']
            if any(not vb[0]<=x0<x1<=vb[0]+vb[2] for x0,x1 in line['cells']): raise ValueError('geometry cell bounds')
            for ref in line['verses']:
                lines[ref].append({'page':page,'line_id':line['id'],'line_index_on_page':i,'top':line['top'],'bottom':line['bottom'],'cells':[c[:] for c in line['cells']]})
    word_pages=set()
    for chunk in chunks:
        if chunk.get('source')!='quran-ws/quran-svg-elements' or chunk.get('schema')!='quran-haafidh-word-boxes-v1' or chunk.get('source_release')!='v1.1.2' or chunk.get('box_space')!='viewBox 0 0 345 550': raise ValueError('word source schema/release/space')
        for key,rows in chunk['pages'].items():
            page=int(key)
            if key not in pages or page in word_pages: raise ValueError('word page domain or duplicate page')
            word_pages.add(page); vb=pages[key]['viewBox']; previous_word=None
            for row in rows:
                if len(row)!=5: issue('invalid_word_row',page=page);continue
                wid,*box=row
                try: s,a,w=map(int,wid.split(':'))
                except (ValueError,AttributeError): issue('invalid_word_id',page=page);continue
                ref=f'{s}:{a}'
                if previous_word is not None and (s,a,w)<=previous_word: issue('source_word_order',page=page,word_id=wid)
                previous_word=(s,a,w)
                if wid in seen_words: issue('duplicate_word',page=page,word_id=wid);continue
                seen_words.add(wid)
                if not all(type(x) in (int,float) and math.isfinite(x) for x in box) or not (0<=box[0]<box[2]<=345 and 0<=box[1]<box[3]<=550):
                    issue('invalid_box',page=page,word_id=wid);continue
                if (s,a) not in canonical_set or w<1: issue('noncanonical_word',page=page,word_id=wid);continue
                svg=[box[0]+vb[0],box[1]+vb[1],box[2]+vb[0],box[3]+vb[1]]
                # Source bands are selection bands, not glyph contours: center membership
                # verifies line identity without fabricating a contour or demanding full containment.
                cy=(svg[1]+svg[3])/2
                matches=[l for l in lines[ref] if l['page']==page and l['top']<=cy<l['bottom']]
                line_id=matches[0]['line_id'] if len(matches)==1 else None
                if line_id is None: issue('unverified_line',page=page,word_id=wid)
                words[ref].append({'word_id':wid,'word_index':w,'page':page,'line_id':line_id,'box_normalized':box,'box_svg':svg})
    for page in sorted(set(range(1,expected_pages+1))-word_pages): issue('missing_word_page',page=page)
    result=[]
    for p in clean:
        local=[]; ayat=[]
        for a in range(p['start'],p['end']+1):
            ref=f"{p['surah']}:{a}"; ls=lines[ref]; ws=sorted(words[ref],key=lambda w:w['word_index'])
            if not ls: local.append(issue('missing_verse_geometry',verse=ref))
            if not ws: local.append(issue('missing_verse_words',verse=ref))
            if ws and [w['word_index'] for w in ws]!=list(range(1,ws[-1]['word_index']+1)): local.append(issue('word_sequence_gap',verse=ref))
            if ws and [w['page'] for w in ws]!=sorted(w['page'] for w in ws): local.append(issue('word_page_order',verse=ref))
            gp=sorted({l['page'] for l in ls}); wp=sorted({w['page'] for w in ws})
            if wp!=gp: local.append(issue('word_geometry_page_mismatch',verse=ref,geometry_pages=gp,word_pages=wp))
            if sorted(geometry.get('verses',{}).get(ref,[]))!=gp: local.append(issue('verse_page_index_mismatch',verse=ref))
            ayat.append({'ayah':a,'pages':gp,'lines':ls,'words':ws})
        candidates=[w for w in ayat[0]['words'] if w['word_index']==1 and w['line_id'] is not None]
        first=candidates[0] if len(candidates)==1 else None
        if first is None: local.append(issue('missing_first_word',passage_id=p['id']))
        relevant={f"{p['surah']}:{a}" for a in range(p['start'],p['end']+1)}
        local.extend(i for i in issues if i not in local and i.get('word_id','').rsplit(':',1)[0] in relevant)
        pp=sorted({page for a in ayat for page in a['pages']})
        result.append({**p,'pages':pp,'first_word':first,'ayat':ayat,'issues':local})
    report={'passages':len(result),'covered_ayat':len(selected),'canonical_ayat':len(canonical),'pages':expected_pages,'word_boxes':sum(map(len,words.values())),'first_word_markers':sum(p['first_word'] is not None for p in result),'multi_page_passages':sum(len(p['pages'])>1 for p in result),'issue_count':len(issues),'issue_counts':dict(Counter(i['code'] for i in issues)),'issues':issues,'status':'PREPARATION_ONLY','editorial_audit':'OPEN','thumbnails':'NOT_EXECUTED','boox_visual_test':'NOT_EXECUTED','cp03':'NOT_COMPLETE'}
    return result,report

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[1])
    parser.add_argument('--passages',type=Path,required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args();repo=args.repo.resolve();out=args.out.resolve()
    paths=[args.passages.resolve(),repo/'app/src/main/assets/reader109/geometry.json',*sorted((repo/'hifz-app/src/main/word-source/quran-ws-v1.1.2').glob('word-boxes-*.json'))]
    if len(paths)!=6: raise ValueError('expected four word geometry chunks')
    # Refuse any output location that could overwrite source assets or input.
    if out==repo or repo in out.parents or any(out==p or out in p.parents for p in paths): raise ValueError('output must be outside repository and inputs')
    hashes={str(p):digest(p) for p in paths}
    data=json.loads(paths[0].read_text());geometry=json.loads(paths[1].read_text());chunks=[json.loads(p.read_text()) for p in paths[2:]]
    passages,report=build_mapping(data['passages'],geometry,chunks)
    sources=[{'source_file':p.name,'sha256':hashes[str(p)]} for p in paths]
    out.mkdir(parents=True,exist_ok=True)
    def write(name,value):
        dest=out/name;tmp=out/(name+'.tmp');tmp.write_text(json.dumps(value,ensure_ascii=True,separators=(',',':'))+'\n');tmp.replace(dest)
    batches=[]
    for start,end in surah_batches(114):
        chosen=[p for p in passages if start<=p['surah']<=end];pages=sorted({n for p in chosen for n in p['pages']})
        name=f'qcf-surahs-{start:03d}-{end:03d}.json'
        write(name,{'schema':'qcf-spatial-preparation-v1','status':'PREPARATION_ONLY','surah_start':start,'surah_end':end,'sources':sources,'word_box_space':[0,0,345,550],'page_geometry':{str(n):geometry['pages'][str(n)] for n in pages},'passages':chosen})
        batches.append({'file':name,'sha256':digest(out/name),'passages':len(chosen),'surahs':end-start+1})
        print(f'WROTE {name}: {len(chosen)} passages',flush=True)
    after={str(p):digest(p) for p in paths}
    if hashes!=after: raise RuntimeError('source inputs changed during export')
    report.update({'sources':sources,'batches':batches,'source_bytes_unchanged':True,'coordinate_transform':'box_svg = box_normalized + [viewBox.x, viewBox.y, viewBox.x, viewBox.y]; no scaling','line_verification':'word vertical center inside native geometry band for same verse and page; no estimated line on ambiguity'})
    write('coverage-report.json',report)
    (out/'README.md').write_text('# QCF spatial preparation\n\nPREPARATION ONLY. Editorial corpus audit OPEN; CP03 NOT_COMPLETE. No app, Android, renderer, geometry or Hifz engine changes. No release.\n\nTwelve numerical JSON lots contain at most ten surahs each. A canonical passage appears once and carries every ayah, all touched pages, native source line/cell geometry, normalized authentic word boxes, and exact first word index 1 where verified. No editorial titles or Arabic text are exported. Source SHA-256 values and output hashes are in coverage-report.json.\n\nGeometry uses the original per-page SVG viewBox; word boxes use 0 0 345 550. Native SVG boxes are obtained only by adding that viewBox origin, matching existing reader.js wordBoxInSvgSpace. No scaling. Line verification uses vertical center membership in the source verse band; bands are not glyph contours. Any unresolved box or line is flagged, never estimated. Complete box counts rely on the pinned source (a missing final word cannot be detected without an independent canonical word-count source).\n\nThumbnails NOT_EXECUTED pending the actual renderer. BOOX visual testing NOT_EXECUTED. These files are data links, not proof of device rendering or editorial acceptance.\n')
    print(json.dumps({k:v for k,v in report.items() if k not in ('issues','sources','batches')},sort_keys=True))

if __name__=='__main__': main()

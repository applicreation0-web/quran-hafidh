#!/usr/bin/env python3
"""Validate approved runtime data against independent corpus and geometry inputs.

No religious judgement is inferred here. Quarantined/excluded rows must stay absent.
Glyph paths cannot independently establish word text; see editorial audit limitation.
"""
import gzip,hashlib,json,pathlib,sqlite3,tempfile,xml.etree.ElementTree as ET
import brotli
from build_alabbad_catalogue import load_inputs
ROOT=pathlib.Path(__file__).resolve().parents[1]

def main():
    approved=json.loads((ROOT/'research/alabbad_catalogue_approved.json').read_text())
    audit=json.loads((ROOT/'research/alabbad_audit_294.json').read_text())
    assert approved['verification']['sourceProvenanceChecked'] is True
    assert [e['auditId'] for e in audit['entries']]==list(range(1,295))
    admitted={e['auditId']:e for e in audit['entries'] if e['decision'] in ('retenu','corrigé')}
    assert len(admitted)==approved['approvedCount']==len(approved['entries'])
    geometry,text,_=load_inputs()
    svg={};by_page={p:set() for p in range(1,605)};endpoints=set()
    pinned=approved['verification']['svgSha256']
    # Every canonical page must remain authentic and proportional, including unindexed pages.
    for page in range(1,605):
        path=ROOT/f'app/src/main/assets/mushaf/hafs/kfqc/svg-br/{page:03d}.svg.br'
        raw=path.read_bytes();root=ET.fromstring(brotli.decompress(raw))
        view=list(map(float,root.attrib['viewBox'].replace(',',' ').split()))
        assert view[2:]==[345,550],(page,view)
        svg[page]={f"{e.attrib['surah']}:{e.attrib['ayah']}" for e in root.iter()
                   if 'surah' in e.attrib and 'ayah' in e.attrib and 'ayahPolygon' in e.attrib.get('class','')}
        assert svg[page],('missing original polygons',page)
        relative=str(path.relative_to(ROOT))
        if relative in pinned:assert hashlib.sha256(raw).hexdigest()==pinned[relative],page
    for entry in approved['entries']:
        row=admitted[entry['auditId']]
        for side in ('first','second'):
            ref=entry[side];r=tuple(map(int,ref.split(':')));page=entry[side+'Page'];indices=entry[side+'WordIndices']
            assert ref in svg[page],(entry['auditId'],ref,'missing exact ayah polygon')
            assert indices and len(indices)==len(set(indices))
            assert [text[r][i-1] for i in indices]==entry[side+'Words']
            locations=[dict(wordIndex=i,word=text[r][i-1],**geometry[r][i]) for i in indices]
            assert locations==row[side+'Geometry']
            assert all(loc['page']==page for loc in locations)
            endpoints.add(r);by_page[page].add(entry['auditId'])
    # Both sides of every comparison resolve the original offline commentary, no substitute text.
    parts=ROOT/'app/src/plus/assets/tafsir'
    raw=gzip.decompress(b''.join((parts/f'al_jalalayn_en.sqlite.gz.part{i:02d}').read_bytes() for i in range(4)))
    with tempfile.NamedTemporaryFile(suffix='.sqlite') as tmp:
        tmp.write(raw);tmp.flush()
        db=sqlite3.connect(tmp.name)
        assert db.execute('PRAGMA quick_check').fetchone()[0]=='ok'
        for s,a in endpoints:
            body=db.execute('SELECT body_json FROM verse_commentary WHERE surah=? AND ayah=?',(s,a)).fetchone()
            assert body and json.loads(body[0]),('missing Jalalayn',s,a)
        db.close()
    print(json.dumps({'approvedPairs':len(admitted),'auditedDecisions':len(audit['entries']),
        'qcfPagesChecked':604,'pagesWithComparisons':sum(bool(v) for v in by_page.values()),
        'maximumComparisonsOnPage':max(map(len,by_page.values())),
        'originalJalalaynEndpointsChecked':len(endpoints),
        'excludedOrQuarantined':len(audit['entries'])-len(admitted)},ensure_ascii=False))

if __name__=='__main__':main()

#!/usr/bin/env python3
"""Compile approved reference data only; never infer an editorial decision."""
import argparse,json,pathlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
SOURCE=ROOT/'research/alabbad_catalogue_approved.json'
JAVA=ROOT/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java'
def materialize():
    data=json.loads(SOURCE.read_text());entries=data['entries']
    assert len(entries)==data['approvedCount'] and entries
    output=['    // BEGIN GENERATED APPROVED CATALOGUE',
            '    static final List<Entry> CATALOGUE = Collections.unmodifiableList(Arrays.asList(']
    q=lambda s:json.dumps(s,ensure_ascii=False)
    seen=set()
    for i,e in enumerate(entries):
        a=list(map(int,e['first'].split(':')));b=list(map(int,e['second'].split(':')))
        pair=tuple(sorted((e['first'],e['second'])));assert pair not in seen and a!=b;seen.add(pair)
        assert e['firstWordIndices'] and e['secondWordIndices']
        assert (e['auditId']==292 and e['source']=='Ajout explicite de l’utilisateur') or e['sourceUrl'].startswith('https://quranpedia.app/books/b10901/')
        words=lambda key:'new int[]{'+','.join(map(str,e[key]))+'}'
        values=[*map(str,a+b),q(e['kind']),q(e['first']),q(e['second']),q(''),
                q(e['source']+' · '+str(e['auditId'])),q(e['sourceUrl']),
                words('firstWordIndices'),words('secondWordIndices'),str(e['firstPage']),str(e['secondPage']),str(e['auditId'])]
        output.append('        new Entry('+','.join(values)+')'+(',' if i+1<len(entries) else ''))
    output.extend(['    ));','    /** Legacy internal name retained for existing callers; contains the complete approved catalogue. */',
                   '    static final List<Entry> PILOT=CATALOGUE;',
                   '    // END GENERATED APPROVED CATALOGUE'])
    source=JAVA.read_text();start=source.index('    // BEGIN GENERATED APPROVED CATALOGUE');end=source.index('    // END GENERATED APPROVED CATALOGUE',start)+len('    // END GENERATED APPROVED CATALOGUE')
    return source[:start]+'\n'.join(output)+source[end:]
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
    expected=materialize()
    if args.check:
        assert JAVA.read_text()==expected,'Generated Java differs from approved catalogue; rebuild required'
    else:JAVA.write_text(expected)
    print('PASS: approved catalogue Java',json.loads(SOURCE.read_text())['approvedCount'])

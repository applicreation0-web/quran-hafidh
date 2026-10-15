#!/usr/bin/env python3
"""Read-only metadata reconciliation for the private Maududi EPUB and hOCR.

Neither English commentary text nor titles are copied into output. A matching
OCR label is not editorial, legal, edition, or printed-scan approval.
"""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
from html.parser import HTMLParser
import json
from pathlib import Path
import posixpath
import re
import xml.etree.ElementTree as ET
from zipfile import ZipFile

EPUB_SHA = '8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba'
BOOK_URL = 'https://archive.org/details/towards-understanding-quran-maududi/page/n{}/mode/1up'

def reader_page_for_epub_member(page):
    """BookReader displayed page n(K) maps to EPUB/page_(K+1).html."""
    if type(page) is not int or not 1 <= page <= 3116:
        raise ValueError('invalid EPUB page for reader')
    return page - 1
REF = re.compile(r'[\[(](\d{1,3})\s*:\s*(\d{1,3})[\])]')
HEADING = re.compile(r'^(\d{1,3})[.,]?\s+(?:Sura(?:h)?\s+|(?=An Nahl\b))', re.I)
PAGE = re.compile(r'EPUB/page_(\d+)\.html')

class BodyText(HTMLParser):
    def __init__(self):
        super().__init__(); self.active=False;self.parts=[]
    def handle_starttag(self, tag, attrs):
        if tag.lower()=='body':self.active=True
    def handle_endtag(self,tag):
        if tag.lower()=='body':self.active=False
    def handle_data(self,text):
        if self.active:self.parts.append(text)

def read_index(epub_path):
    """Index original EPUB spine with chapter regions and labels; never export prose."""
    p=Path(epub_path)
    if hashlib.sha256(p.read_bytes()).hexdigest()!=EPUB_SHA:
        raise ValueError('EPUB checksum changed; fail closed')
    ns={'c':'urn:oasis:names:tc:opendocument:xmlns:container',
        'o':'http://www.idpf.org/2007/opf'}
    with ZipFile(p) as z:
        if z.testzip() is not None:raise ValueError('EPUB ZIP CRC mismatch')
        container=ET.fromstring(z.read('META-INF/container.xml'))
        opf=container.find('c:rootfiles/c:rootfile',ns).get('full-path')
        doc=ET.fromstring(z.read(opf))
        manifest={x.get('id'):x.get('href') for x in doc.findall('o:manifest/o:item',ns)}
        pages=[];seen=set()
        for node in doc.findall('o:spine/o:itemref',ns):
            href=manifest.get(node.get('idref'))
            if not href:continue
            member=posixpath.normpath(posixpath.join(posixpath.dirname(opf),href))
            m=PAGE.fullmatch(member)
            if not m:continue
            if member in seen:raise ValueError('repeated EPUB page')
            seen.add(member)
            parser=BodyText();parser.feed(z.read(member).decode('utf-8'))
            body=re.sub(r'\s+',' ',' '.join(parser.parts).replace('\u200b','').replace('\u00a0',' ')).strip()
            heading=HEADING.match(re.sub(r'^Page\s+\d+\s+','',body))
            pages.append({'page':int(m[1]), 'heading':int(heading[1]) if heading else None,
                'refs':[(int(a),int(b)) for a,b in REF.findall(body)]})
        allmembers={x for x in z.namelist() if PAGE.fullmatch(x)}
        if allmembers!=seen:raise ValueError('EPUB spine missing numbered pages')
    headings=[(p['heading'],i) for i,p in enumerate(pages) if p['heading'] is not None]
    if [s for s,_ in headings]!=list(range(1,115)):
        raise ValueError('Cannot establish 114 canonical EPUB surah regions')
    if len(pages)!=3117:raise ValueError('Unexpected EPUB page count')
    by_surah={}
    for i,(s,begin) in enumerate(headings):
        end=headings[i+1][1] if i+1<len(headings) else len(pages)
        by_surah[s]=pages[begin:end]
    return by_surah

def classify_case(surah, ayah, pages, hocr, browser=None):
    """Deterministic page candidates, with mandatory scan-level review gate."""
    exact_epub=sorted({p['page'] for p in pages if (surah,ayah) in p['refs']})
    labels=[(a,p['page']) for p in pages for s,a in p['refs'] if s==surah]
    lower=[page for a,page in labels if a<ayah]
    upper=[page for a,page in labels if a>ayah]
    left=max(lower,default=pages[0]['page'])
    right=min((page for page in upper if page>=left),default=pages[-1]['page'])
    if left>right:left,right=pages[0]['page'],pages[-1]['page']
    hits=hocr.get('hits') or []
    hocr_pages=sorted({int(h['page']) for h in hits})
    if hocr_pages:
        candidates=hocr_pages;kind='HOCR_EXACT_REQUIRES_IMAGE';
    elif exact_epub:
        candidates=exact_epub;kind='EPUB_EXACT_REQUIRES_IMAGE'
    else:
        candidates=list(range(left,right+1));kind='EPUB_BRACKET_REQUIRES_IMAGE'
    if not candidates or candidates[0]<1 or candidates[-1]>3116:
        raise ValueError('No usable reader-page candidates')
    if any(p not in [x['page'] for x in pages] for p in candidates) and kind!='HOCR_EXACT_REQUIRES_IMAGE':
        raise ValueError('candidate outside chapter pages')
    visual=(browser or {}).get('status','NOT_EXECUTED')
    if visual not in {'NOT_EXECUTED','VISIBLE_IN_BROWSER_VIEW','NOT_SEEN_IN_BROWSER_VIEW','UNCERTAIN'}:
        raise ValueError('Unknown visual evidence state')
    reader_pages=[reader_page_for_epub_member(page) for page in candidates]
    if visual!='NOT_EXECUTED' and (browser or {}).get('page') not in reader_pages:
        raise ValueError('Visual BookReader page does not match candidate EPUB page')
    return {'surah':surah,'ayah':ayah,'case_id':f'EPUB-LABEL-{surah:03d}-{ayah:03d}',
      'ocr_status':hocr['status'],'index_mode':kind,'candidate_pages':candidates,
      'bookreader_pages':reader_pages,
      'bookreader_page_offset':-1,
      'bookreader_urls':[BOOK_URL.format(x) for x in reader_pages[:12]],
      'epub_exact_pages':exact_epub,'hocr_exact_pages':hocr_pages,
      'browser_visual_state':visual,'browser_page':(browser or {}).get('page'),
      'scan_alignment':'CANDIDATE_ONLY','commentary_body':'NOT_VERIFIED',
      'footnotes':'NOT_VERIFIED','editorial_state':'NOT_APPROVED','repair_applied':False}

def reconcile(epub,hocr_cases,issues,observations):
    chapters=read_index(epub)
    src=json.loads(Path(hocr_cases).read_text())
    hocr={r['reference']:r for r in src['cases']}
    if len(hocr)!=109:raise ValueError('Historical hOCR ledger requires exactly 109 distinct cases')
    with Path(issues).open(newline='',encoding='utf-8') as f:
        raw=list(csv.DictReader(f))
    expected={f"{x['surah']}:{x['ayah']}" for x in raw}
    if len(raw)!=109 or len(expected)!=109 or set(hocr)!=expected:
        raise ValueError('Historical unresolved references changed')
    if len(observations)!=len(set(observations)) or any(x not in expected for x in observations):
        raise ValueError('Unrecognized image observations')
    records=[]
    for x in raw:
        s,a=int(x['surah']),int(x['ayah']);key=f'{s}:{a}'
        record=classify_case(s,a,chapters[s],hocr[key],observations.get(key))
        if hocr[key]['status']=='OUTSIDE_CAPTURE_22_8' and s<=22:
            raise ValueError('hOCR capture classification mismatch')
        records.append(record)
    statuses=Counter(r['index_mode'] for r in records)
    visual=Counter(r['browser_visual_state'] for r in records)
    return {'schema':'mawdudi-ocr-scan-review-v2','epub_sha256':EPUB_SHA,
      'hocr_sha256':src.get('source_hocr_sha256'), 'total':109,
      'page_candidates_le_5':sum(len(r['candidate_pages'])<=5 for r in records),
      'modes':dict(statuses),'visual_evidence':dict(visual),
      'approved_source_text':0,'auto_repairs':0,
      'copyrighted_passages_embedded':False,
      'bookreader_alignment':'EPUB/page_N.html -> BookReader n(N-1), checked on 11 user scan photos',
      'legacy_browser_observations':'RECHECK_REQUIRED',
      'warning':'EPUB indexes differ from BookReader display; non-detection is not proof of absence; OCR hits do not prove translation/notes or license.',
      'cases':records}

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    for v in ('epub','hocr-cases','issues','out'):ap.add_argument('--'+v,type=Path,required=True)
    ap.add_argument('--visual-observations',type=Path)
    args=ap.parse_args()
    observations=json.loads(args.visual_observations.read_text()) if args.visual_observations else {}
    report=reconcile(args.epub,args.hocr_cases,args.issues,observations)
    args.out.parent.mkdir(parents=True,exist_ok=True)
    args.out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='cases'},ensure_ascii=False,indent=2))
if __name__=='__main__':main()

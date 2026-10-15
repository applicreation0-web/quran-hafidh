#!/usr/bin/env python3
"""Read-only quantitative OCR preservation audit for Maududi EPUB vs hOCR MHT.

Output metadata only (counts, per-page token retention, risk level). This audit
cannot establish correctness of doctrinal content, attribution, rights, or notes.
"""
from pathlib import Path
from collections import Counter
import argparse, hashlib, json, re, zipfile, html
from html.parser import HTMLParser

PINNED_EPUB_SHA='8d0a4c5a34bdc8404900b8b61b8a93d633c7984f3497fe74a9d846adfa454bba'
PINNED_HOCR_SHA='ca74b97f3dbc0e93119472c2dbed22dfa8f1d36a391dc4fcaebab9c78a54a921'
PAGE_RE=re.compile(r'<div class="ocr_page" id="page_(\d{6})"')
WORD_RE=re.compile(r'<span class="ocrx_word"[^>]*>(.*?)</span>',re.S)
TAG_RE=re.compile(r'<[^>]+>')
FILTER=re.compile(r'[^\w]',re.U)

class BodyText(HTMLParser):
    def __init__(self):
        super().__init__();self.inside=False;self.parts=[]
    def handle_starttag(self,tag,attrs):
        if tag=='body':self.inside=True
    def handle_endtag(self,tag):
        if tag=='body':self.inside=False
    def handle_data(self,text):
        if self.inside:self.parts.append(text)

def normalize(s):
    w=FILTER.sub('',html.unescape(s).lower())
    return w if len(w)>=3 and not w.isdecimal() else ''

def measure_words(hwords,ewords):
    shared=sum((Counter(hwords)&Counter(ewords)).values())
    retained=round(shared/len(hwords),4) if hwords else None
    missing=len(hwords)-shared
    if len(hwords)<60: risk='LOW_EVIDENCE'
    elif missing>=60 and retained<0.5: risk='CRITICAL_OCR_LOSS_CANDIDATE'
    elif missing>=45 and retained<0.8: risk='HIGH_OCR_LOSS_CANDIDATE'
    elif missing>=20 and retained<0.9: risk='REVIEW_OCR_DIFFERENCE'
    else: risk='NO_MAJOR_DIFFERENCE_DETECTED'
    return {'hocr_tokens':len(hwords),'epub_tokens':len(ewords),'shared_tokens':shared,
        'retention':retained,'missing_hocr_tokens':missing,'risk':risk}

def analyze(epub,hocr):
    epub,hocr=Path(epub),Path(hocr)
    if hashlib.sha256(epub.read_bytes()).hexdigest()!=PINNED_EPUB_SHA:raise ValueError('EPUB SHA mismatch')
    if hashlib.sha256(hocr.read_bytes()).hexdigest()!=PINNED_HOCR_SHA:raise ValueError('hOCR SHA mismatch')
    txt=hocr.read_bytes().decode('utf-8','replace')
    sections=list(PAGE_RE.finditer(txt))
    if len(sections)!=1210:raise ValueError('hOCR page count mismatch')
    records=[]
    with zipfile.ZipFile(epub) as z:
        if z.testzip() is not None:raise ValueError('ZIP CRC failure')
        for i,m in enumerate(sections):
            page=int(m[1]);stop=sections[i+1].start() if i+1<len(sections) else len(txt)
            piece=txt[m.end():stop]
            hwords=[normalize(TAG_RE.sub('',w)) for w in WORD_RE.findall(piece)]
            hwords=[w for w in hwords if w]
            member=f'EPUB/page_{page}.html'
            if member not in z.namelist():raise ValueError('missing EPUB page '+str(page))
            parser=BodyText();parser.feed(z.read(member).decode('utf-8'))
            epubtext=' '.join(parser.parts)
            ewords=[normalize(w) for w in epubtext.split()];ewords=[w for w in ewords if w]
            records.append({'page':page,**measure_words(hwords,ewords)})
    categories=Counter(x['risk'] for x in records)
    high=[x for x in records if x['risk'] in ('CRITICAL_OCR_LOSS_CANDIDATE','HIGH_OCR_LOSS_CANDIDATE')]
    return {'schema':'mawdudi-hocr-epub-retention-v1','epub_sha256':PINNED_EPUB_SHA,
        'hocr_sha256':PINNED_HOCR_SHA,'pages_compared':len(records),'levels':dict(categories),
        'hocr_tokens_considered':sum(x['hocr_tokens'] for x in records),
        'epub_tokens_considered':sum(x['epub_tokens'] for x in records),
        'hocr_tokens_retained_in_epub_multiset':sum(x['shared_tokens'] for x in records),
        'high_risk_page_count':len(high),
        'high_risk_page_indices':[x['page'] for x in high],
        'source_text_approved':False,'source_repaired':False,
        'note':'Per-page bag-of-tokens comparison of OCR derivatives. Differences indicate likely lost OCR material, not an adjudication of exact text or edition. Original scan inspection required.',
        'pages':records}

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--epub',type=Path,required=True)
    ap.add_argument('--hocr',type=Path,required=True);ap.add_argument('--out',type=Path,required=True)
    a=ap.parse_args();d=analyze(a.epub,a.hocr);a.out.write_text(json.dumps(d,indent=2)+'\n')
    print(json.dumps({k:v for k,v in d.items() if k not in ('pages','high_risk_page_indices')},indent=2))
    for p in [50,110,215,218]:
        print(p,next(x for x in d['pages'] if x['page']==p))
if __name__=='__main__':main()

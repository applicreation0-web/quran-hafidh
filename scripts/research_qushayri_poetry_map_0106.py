#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, re
from pathlib import Path
import fitz

SHA = "f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
INDEX_PDF_PAGES = range(517, 521)
PRINTED_TO_PDF_OFFSET = 32


def file_sha(p: Path) -> str:
    h=hashlib.sha256()
    with p.open('rb') as f:
        for c in iter(lambda:f.read(1024*1024),b''): h.update(c)
    return h.hexdigest()


def norm(s: str) -> str:
    s=s.replace('\u00a0',' ').replace('–','-').replace('—','-')
    s=re.sub(r'[“”‘’]',"'",s)
    s=re.sub(r'\s+',' ',s).strip()
    return s


def index_entries(doc):
    raw=[]
    for page_no in INDEX_PDF_PAGES:
        lines=[norm(x) for x in doc[page_no-1].get_text('text').splitlines()]
        lines=[x for x in lines if x and 'Poetry Index' not in x and not re.match(r'^\d+ \|',x) and x != '']
        buf=''
        for line in lines:
            buf=(buf+' '+line).strip() if buf else line
            # Entry ends with a page token: 35, 5, 277, 365-66, 422 n.82, xix, 24
            if re.search(r'(?:\b\d+(?:-\d+)?(?: n\.\d+)?|\b[ivxlcdm]+)(?:,\s*\d+(?:-\d+)?(?: n\.\d+)?)*$',buf,re.I):
                raw.append(buf); buf=''
        if buf: raw.append(buf)
    out=[]
    for text in raw:
        # Remove trailing reference list.
        m=re.search(r'(?P<refs>(?:\b(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))(?:,\s*(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))*)$',text,re.I)
        if not m: continue
        refs=m.group('refs')
        lead=text[:m.start()].rstrip(' ,')
        incipit=lead.split(' - ',1)[0].strip()
        nums=[]
        note_only=[]
        for token in [x.strip() for x in refs.split(',')]:
            if ' n.' in token:
                note_only.append(token); continue
            mm=re.fullmatch(r'(\d+)(?:-(\d+))?',token)
            if mm:
                a=int(mm.group(1)); b=int(mm.group(2) or a)
                nums.extend(range(a,b+1))
        out.append({'raw':text,'incipit':incipit,'printed_pages':nums,'note_refs':note_only})
    return out


def page_lines(page):
    result=[]
    pd=page.get_text('dict')
    for bi,b in enumerate(pd.get('blocks',[])):
        for li,line in enumerate(b.get('lines',[])):
            spans=[s for s in line.get('spans',[]) if s.get('text','').strip()]
            if not spans: continue
            text=''.join(s.get('text','') for s in spans).strip()
            x0,y0,x1,y1=[float(v) for v in line.get('bbox',(0,0,0,0))]
            result.append({'block':bi,'line':li,'text':text,'norm':norm(text),'x0':x0,'y0':y0,'x1':x1,'y1':y1,'fonts':[s.get('font','') for s in spans],'sizes':[float(s.get('size',0)) for s in spans]})
    return result


def similarity(a,b):
    a=norm(a).lower(); b=norm(b).lower()
    if a.startswith(b) or b.startswith(a): return 1.0
    # word-prefix similarity sufficient for index punctuation differences
    aw=a.split(); bw=b.split(); n=min(len(aw),len(bw),8)
    if n==0:return 0.0
    return sum(1 for i in range(n) if aw[i].strip('.,;:?!')==bw[i].strip('.,;:?!'))/n


def main():
    ap=argparse.ArgumentParser(); ap.add_argument('pdf',type=Path); ap.add_argument('--report',type=Path,required=True); args=ap.parse_args()
    if file_sha(args.pdf)!=SHA: raise SystemExit('Pinned Qushayri source mismatch')
    doc=fitz.open(args.pdf); entries=index_entries(doc)
    matches=[]; missing=[]
    for e in entries:
        if not e['printed_pages']: continue
        for printed in e['printed_pages']:
            pdf=printed+PRINTED_TO_PDF_OFFSET
            if pdf<1 or pdf>len(doc):
                missing.append({**e,'printed_page':printed,'reason':'page-out-of-range'}); continue
            lines=page_lines(doc[pdf-1]); scored=[(similarity(l['norm'],e['incipit']),i,l) for i,l in enumerate(lines)]
            score,i,line=max(scored,key=lambda x:x[0],default=(0,-1,None))
            if line is None or score<0.55:
                missing.append({**e,'printed_page':printed,'pdf_page':pdf,'best_score':score,'best_text':line['text'] if line else None}); continue
            window=[]
            for j in range(i,min(len(lines),i+8)):
                l=lines[j]
                if j>i and l['y0']<line['y0']-5: break
                window.append({k:(round(v,2) if isinstance(v,float) else v) for k,v in l.items() if k not in {'norm'}})
            matches.append({**e,'printed_page':printed,'pdf_page':pdf,'score':score,'start_index':i,'start':window[0],'window':window})
    report={'entry_count':len(entries),'numeric_occurrences':sum(len(e['printed_pages']) for e in entries),'matches':matches,'missing':missing}
    args.report.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f"entries={len(entries)} numeric_occurrences={report['numeric_occurrences']} matches={len(matches)} missing={len(missing)}")
    for m in missing[:25]: print('MISSING',m['incipit'],m.get('printed_page'),m.get('best_score'),m.get('best_text'))
    print('\nSAMPLES')
    for m in matches[:15]:
        print('\n',m['incipit'],'printed',m['printed_page'],'pdf',m['pdf_page'],'score',m['score'])
        for l in m['window'][:6]: print(l['block'],l['line'],round(l['x0'],1),round(l['y0'],1),l['text'])

if __name__=='__main__': main()

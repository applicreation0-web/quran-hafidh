#!/usr/bin/env python3
from __future__ import annotations
import argparse, hashlib, json, re, statistics
from pathlib import Path
import fitz

SHA="f5b064cfe8ece67a89aaeea04540a7d79c8ef5a531ce2aff880608621e3e1bd3"
INDEX=range(517,521); OFFSET=32
AR=re.compile(r'[\u0600-\u06ff\u0750-\u077f\u0870-\u089f\u08a0-\u08ff\ufb50-\ufdff\ufe70-\ufeff]')

def sha(p):
 h=hashlib.sha256();
 with open(p,'rb') as f:
  for c in iter(lambda:f.read(1<<20),b''):h.update(c)
 return h.hexdigest()
def norm(s):
 s=s.replace('\u00a0',' ').replace('–','-').replace('—','-');s=re.sub(r'[“”‘’]',"'",s);return re.sub(r'\s+',' ',s).strip()
def mostly_ar(s):
 ch=[c for c in s if c.isalpha()];return bool(ch) and sum(bool(AR.match(c)) for c in ch)/len(ch)>.45
def recs(page):
 out=[]
 for bi,b in enumerate(page.get_text('dict').get('blocks',[])):
  for li,l in enumerate(b.get('lines',[])):
   sp=[x for x in l.get('spans',[]) if x.get('text','').strip()]
   if not sp:continue
   t=''.join(x.get('text','') for x in sp).strip();x0,y0,x1,y1=map(float,l.get('bbox',(0,0,0,0)))
   if not t:continue
   out.append({'block':bi,'line':li,'text':t,'n':norm(t),'x0':x0,'y0':y0,'x1':x1,'y1':y1,'max_size':max(float(x.get('size',0)) for x in sp),'fonts':[x.get('font','') for x in sp]})
 return out
def index_entries(doc):
 raw=[]
 for pn in INDEX:
  lines=[norm(x) for x in doc[pn-1].get_text('text').splitlines()]
  lines=[x for x in lines if x and 'Poetry Index' not in x and not re.match(r'^\d+ \|',x) and x!='']
  buf=''
  for line in lines:
   buf=(buf+' '+line).strip() if buf else line
   if re.search(r'(?:\b\d+(?:-\d+)?(?: n\.\d+)?|\b[ivxlcdm]+)(?:,\s*\d+(?:-\d+)?(?: n\.\d+)?)*$',buf,re.I):raw.append(buf);buf=''
 out=[]
 for text in raw:
  m=re.search(r'(?P<refs>(?:\b(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))(?:,\s*(?:\d+(?:-\d+)?(?: n\.\d+)?|[ivxlcdm]+))*)$',text,re.I)
  if not m:continue
  lead=text[:m.start()].rstrip(' ,'); inc=lead.split(' - ',1)[0].strip();nums=[]
  for tok in [x.strip() for x in m.group('refs').split(',')]:
   if ' n.' in tok:continue
   mm=re.fullmatch(r'(\d+)(?:-(\d+))?',tok)
   if mm:nums.extend(range(int(mm.group(1)),int(mm.group(2) or mm.group(1))+1))
  out.append((inc,nums,text))
 return out
def sim(a,b):
 a=norm(a).lower();b=norm(b).lower()
 if a.startswith(b) or b.startswith(a):return 1.0
 A=a.split();B=b.split();n=min(len(A),len(B),8)
 return sum(1 for i in range(n) if A[i].strip('.,;:?!')==B[i].strip('.,;:?!'))/n if n else 0

def main():
 ap=argparse.ArgumentParser();ap.add_argument('pdf',type=Path);ap.add_argument('--report',type=Path,required=True);args=ap.parse_args()
 if sha(args.pdf)!=SHA:raise SystemExit('source mismatch')
 d=fitz.open(args.pdf);occ=[]
 for inc,pages,raw in index_entries(d):
  for printed in pages:
   pn=printed+OFFSET; rs=recs(d[pn-1]); sc=[(sim(r['n'],inc),i) for i,r in enumerate(rs)];score,i=max(sc)
   if score<.55:raise SystemExit(f'missing {inc} {printed} {score}')
   poem=[];j=i
   while j<len(rs):
    r=rs[j]
    if j>i and (r['x0']<80.0 or r['max_size']<9.8 or mostly_ar(r['text']) or r['y0']<poem[-1]['y0']-2):break
    if r['x0']<80.0 or r['max_size']<9.8 or mostly_ar(r['text']):break
    poem.append(r);j+=1
   nxt=rs[j] if j<len(rs) else None
   gaps=[poem[k+1]['y0']-poem[k]['y0'] for k in range(len(poem)-1)]
   occ.append({'incipit':inc,'printed_page':printed,'pdf_page':pn,'score':score,'lines':poem,'gaps':gaps,'next':nxt,'ends_near_bottom':poem[-1]['y1']>540 if poem else False})
 if len(occ)!=126:raise SystemExit(f'occurrences {len(occ)}')
 bad=[o for o in occ if len(o['lines'])<2]
 if bad:raise SystemExit('short poem '+repr([(x['incipit'],len(x['lines'])) for x in bad]))
 near=[o for o in occ if o['ends_near_bottom']]
 print('occurrences',len(occ),'line_counts',min(len(o['lines']) for o in occ),max(len(o['lines']) for o in occ),statistics.median(len(o['lines']) for o in occ),'near_bottom',len(near))
 print('near bottom',[(o['incipit'],o['printed_page'],len(o['lines']),round(o['lines'][-1]['y1'],1),o['next']['text'][:40] if o['next'] else None) for o in near])
 gs=[g for o in occ for g in o['gaps']];print('gaps min/median/max',min(gs),statistics.median(gs),max(gs))
 # quantify source stanza-gap candidates >15 and <=22
 print('stanza_gap_candidates',sum(15<g<=22 for g in gs),'normal',sum(g<=15 for g in gs),'large',sum(g>22 for g in gs))
 args.report.write_text(json.dumps({'occurrences':occ},ensure_ascii=False,indent=2),encoding='utf-8')

if __name__=='__main__':main()

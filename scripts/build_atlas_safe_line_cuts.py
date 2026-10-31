#!/usr/bin/env python3
"""Measure blank full-width row cuts from the pinned original QCF SVGs.

Does not edit glyph paths, word boxes, or reader geometry. Requires PyMuPDF,
NumPy and system libbrotlidec. Every approved catalogue edge is checked against
all source raster ink, including verse-number markers, with a 0.75-unit margin.
"""
import ctypes,ctypes.util,json,re,fitz,numpy as np,xml.etree.ElementTree as E
from pathlib import Path
R=Path(__file__).resolve().parents[1];G=json.loads((R/'app/src/main/assets/reader109/geometry.json').read_text());W={}
for p in (R/'hifz-app/src/main/word-source/quran-ws-v1.1.2').glob('word-boxes-*.json'):W.update(json.loads(p.read_text())['pages'])
D=ctypes.CDLL(ctypes.util.find_library('brotlidec')); cache={};n=bad=0
import hashlib
from collections import Counter
EVIDENCE=R.parent/'audit-evidence/row-crops'
EVIDENCE.mkdir(parents=True,exist_ok=True)
catalogue=[]
for row in (R/'hifz-app/src/main/java/com/quransafeguard/hifz/preview/AtlasDiscriminantCases.java').read_text().splitlines():
 if not row.strip().startswith('new Entry('):continue
 vals=re.match(r'\s*new Entry\((\d+),(\d+),(\d+),(\d+),',row).groups()
 marks=re.findall(r'new int\[\]\{([^}]+)\}',row)
 audit=int(re.search(r',([0-9]+)\),?$',row).group(1))
 for side,(su,ay,mark) in enumerate([(vals[0],vals[1],marks[0]),(vals[2],vals[3],marks[1])]):
  catalogue.append((su+':'+ay,[int(v) for v in mark.split(',')],audit,side))
checked=0;distribution=Counter();sample_paths=[]
safe={}
for pageKey in sorted(W,key=int):
 page=int(pageKey)
 lines=G['pages'][pageKey]['lines'];oy=G['pages'][pageKey]['viewBox'][1];rs=[[] for l in lines]
 for w in W[pageKey]:
  for i,l in enumerate(lines):
   if l['top']<=(w[2]+w[4])/2+oy<l['bottom']:rs[i].append(w);break
 if any(not r for r in rs):raise ValueError(('empty row',page))
 top=[min(w[2] for w in r) for r in rs];bot=[max(w[4] for w in r) for r in rs]
 data=(R/f'app/src/main/assets/mushaf/hafs/kfqc/svg-br/{page:03d}.svg.br').read_bytes();out=ctypes.create_string_buffer(2000000);sz=ctypes.c_size_t(len(out));assert D.BrotliDecoderDecompress(len(data),data,ctypes.byref(sz),out)==1;s=out.raw[:sz.value].decode();tree=E.fromstring(s)
 for e in list(tree):
  if e.attrib.get('class')=='ayahPolygon':tree.remove(e)
 doc=fitz.open(stream=E.tostring(tree),filetype='svg');pix=doc[0].get_pixmap(matrix=fitz.Matrix(4,4),alpha=True);z=np.frombuffer(pix.samples,dtype=np.uint8).reshape(pix.height,pix.width,4);ink=(z[:,:,3]>0)&(z[:,:,:3].min(axis=2)<180);blank=~ink.any(axis=1)
 cuts=[0]
 for i in range(1,len(rs)):
  lo=max(0,int(np.ceil(bot[i-1]*4)));hi=min(len(blank),int(np.floor(top[i]*4)))
  runs=[];at=lo
  while at<hi:
   if not blank[at]:at+=1;continue
   end=at
   while end<hi and blank[end]:end+=1
   if end-at>=8:runs.append((at,end))
   at=end
  if runs:
   a,b=max(runs,key=lambda x:x[1]-x[0]);cuts.append(round((a+b)/8,4))
  else:cuts.append(None)
 cuts.append(550)
 exact=s.replace('<svg ',"<svg width='345' height='550' ",1)
 safe[pageKey]={'sha256':hashlib.sha256(exact.encode()).hexdigest(),'cuts':cuts}
 for key,marked,audit,side in catalogue:
  if G['verses'][key][0]!=page:continue
  selected=[i for i,r in enumerate(rs) if any(w[0].startswith(key+':') and any(abs(int(w[0].split(':')[2])-v)<=1 for v in marked) for w in r)]
  first,last=min(selected),max(selected)
  while first>0 and cuts[first] is None:first-=1
  underline_bottom=max(min(549,w[4]+1.5)+0.425 for w in W[pageKey] if w[0].startswith(key+':') and int(w[0].split(':')[2]) in marked)
  while last+1<len(rs) and (cuts[last+1] is None or cuts[last+1]<underline_bottom):last+=1
  y0,y1=cuts[first],cuts[last+1]
  for y in [y0,y1]:
   py=int(round(y*4))
   if 0<py<len(blank):assert blank[max(0,py-3):py+4].all(),(key,page,y)
  checked+=1;distribution[last-first+1]+=1
  if audit in (49,292,293):
   source_words=[w for w in W[pageKey] if w[0].startswith(key+':') and any(abs(int(w[0].split(':')[2])-v)<=1 for v in marked)]
   old0=max(0,min(w[2] for w in source_words)-8);old1=min(550,max(w[4] for w in source_words)+8)
   for name,low,high in [('before',old0,old1),('after',y0,y1)]:
    svg=re.sub(r'viewBox="[^\"]+"',f'viewBox="{G["pages"][pageKey]["viewBox"][0]} {oy+low} 345 {high-low}"',s,count=1)
    strokes=''.join(f'<line x1="{w[1]}" y1="{oy+min(549,w[4]+1.5)}" x2="{w[3]}" y2="{oy+min(549,w[4]+1.5)}" stroke="black" stroke-width="0.85"/>' for w in W[pageKey] if w[0].startswith(key+':') and int(w[0].split(':')[2]) in marked)
    svg=svg.replace('</svg>',strokes+'</svg>');image_doc=fitz.open(stream=svg.encode(),filetype='svg');path=EVIDENCE/f'case-{audit}-side-{side}-{name}.png';image_doc[0].get_pixmap(matrix=fitz.Matrix(3,3)).save(path);sample_paths.append(str(path))
 if page%50==0:print(page,flush=True)
asset=R/'hifz-app/src/main/assets/mutashabihat/qcf_safe_line_cuts_v1.json';asset.parent.mkdir(parents=True,exist_ok=True);asset.write_text(json.dumps({'schema':1,'source_commit':'88bf38206b8f4cf6123d2355034953f376d2446d','raster_scale':4,'minimum_blank_band_source_units':2,'method':'original SVG raster at 4x; whole-row word ink gaps; >=2 source-unit blank band','pages':safe},separators=(',',':'))+'\n')
print('WROTE',asset,len(safe),flush=True)

assert checked==len(catalogue),(checked,len(catalogue))
summary={'checked_sides':checked,'safe_cut_boundary_ink_failures':0,'row_count_distribution':dict(sorted(distribution.items())),'asset_sha256':hashlib.sha256(asset.read_bytes()).hexdigest(),'sample_images':sample_paths}
(EVIDENCE/'verification.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary),flush=True)

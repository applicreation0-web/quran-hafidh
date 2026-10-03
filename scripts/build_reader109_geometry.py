"""Read-only geometric index derived from the exact shipped SVG corpus.
Text/glyph paths are never changed. Mask cells are ink groups, not linguistic words.
"""
import ctypes,ctypes.util,json,re,xml.etree.ElementTree as ET
from pathlib import Path
import fitz,numpy as np
from scipy.ndimage import gaussian_filter1d
from scipy.signal import find_peaks
D=ctypes.CDLL(ctypes.util.find_library('brotlidec')); D.BrotliDecoderDecompress.argtypes=[ctypes.c_size_t,ctypes.c_void_p,ctypes.POINTER(ctypes.c_size_t),ctypes.c_void_p]
ROOT=Path(__file__).resolve().parents[1];out=ROOT/'app/src/main/assets/reader109';pages={};verses={};warnings=[]
for p in range(1,605):
 b=(ROOT/f'app/src/main/assets/mushaf/hafs/kfqc/svg-br/{p:03d}.svg.br').read_bytes();buf=ctypes.create_string_buffer(8_000_000);n=ctypes.c_size_t(len(buf));assert D.BrotliDecoderDecompress(len(b),b,ctypes.byref(n),buf)==1
 svg=buf.raw[:n.value].decode();tree=ET.fromstring(svg);vb=list(map(float,tree.attrib['viewBox'].split()));polys=[]
 for e in tree.iter():
  if e.attrib.get('class')=='ayahPolygon':
   key=f"{e.attrib['surah']}:{e.attrib['ayah']}";verses.setdefault(key,[]).append(p);polys.append({'verse':key,'d':e.attrib['d']})
 # Keep only source text layer for measurement; the displayed SVG is unchanged.
 def prune(parent):
  for child in list(parent):
   if child.attrib.get('class') in ['ayahPolygon','ayah_markers']:parent.remove(child)
   else:prune(child)
 prune(tree)
 doc=fitz.open(stream=ET.tostring(tree),filetype='svg');pix=doc[0].get_pixmap(matrix=fitz.Matrix(3,3),alpha=True)
 a=np.frombuffer(pix.samples,dtype=np.uint8).reshape(pix.height,pix.width,4);ink=(a[:,:,3]>90)&(a[:,:,:3].min(axis=2)<100)
 sy=vb[3]/pix.height;sx=vb[2]/pix.width;score=gaussian_filter1d(ink.sum(axis=1).astype(float),max(1,3/sy))
 peaks,_=find_peaks(score,distance=max(1,int(23/sy)),prominence=max(score.max()*.06,1))
 # Remove decorative/header peaks without any source verse polygon at their y.
 def bounds(poly):
  nums=list(map(float,re.findall(r'-?\d+(?:\.\d+)?',poly['d'])));return min(nums[1::2]),max(nums[1::2])
 peaks=[int(y) for y in peaks if any(lo-2<=vb[1]+y*sy<=hi+2 for lo,hi in map(bounds,polys))]
 if not peaks:raise ValueError(p)
 limits=[max(0,peaks[0]-int(18/sy))]+[(a+b)//2 for a,b in zip(peaks,peaks[1:])]+[min(pix.height,peaks[-1]+int(19/sy))]
 lines=[]
 for i,y in enumerate(peaks):
  top,bot=limits[i:i+2];cols=ink[top:bot].sum(axis=0)>0;xs=np.where(cols)[0]
  cells=[]
  if len(xs):
   splits=np.where(np.diff(xs)>max(2,int(2.2/sx)))[0]+1
   for g in np.split(xs,splits):cells.append([round(vb[0]+(g[0]-1)*sx,3),round(vb[0]+(g[-1]+2)*sx,3)])
  yy=vb[1]+y*sy;refs=[]
  for poly in polys:
   # Any area at the line's text baseline; display masks still use exact polygon clips.
   lo,hi=bounds(poly)
   if lo<=yy<=hi:refs.append(poly['verse'])
  if refs:lines.append({'id':f'{p}:{i}', 'page':p,'top':round(vb[1]+top*sy,3),'bottom':round(vb[1]+bot*sy,3),'verses':list(dict.fromkeys(refs)),'cells':cells})
 if p>2 and len(lines)!=15:warnings.append([p,len(lines)])
 pages[str(p)]={'viewBox':vb,'lines':lines}
 if p%100==0:print(p,flush=True)
# Use the existing exact Juz/Hizb source metadata, with verse boundaries.
s=(ROOT/'app/src/main/java/com/quranunlock/guard/QuranStructureMetadata.kt').read_text();divs=re.findall(r'QuranDivision\((\d+), QuranVerseRef\((\d+), (\d+)\), QuranVerseRef\(\d+, \d+\), (\d+),',s)
index={'schema':1,'verses':verses,'pages':pages,'juz':[{'n':int(n),'verse':f'{su}:{ay}','page':int(p)} for n,su,ay,p in divs[:30]],'hizb':[{'n':int(n),'verse':f'{su}:{ay}','page':int(p)} for n,su,ay,p in divs[30:]]}
(out/'geometry.json').write_text(json.dumps(index,separators=(',',':')))
(ROOT/'geometry-report.json').write_text(json.dumps({'pages':604,'verses':len(verses),'lineCountExceptions':warnings,'maskUnit':'source ink groups; no linguistic word coordinates'},indent=2))
print('complete',len(verses),'exceptions',warnings)

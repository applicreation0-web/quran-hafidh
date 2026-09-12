'use strict';
/* Quran Hifz local reader: canonical SVG, deterministic nested masks, no network. */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=boot.selection||[];
let lineIds=boot.lines||[];
let mask=Number(boot.mask||0);
let eink=!!boot.eink;
let audioVerse=null;
const NS='http://www.w3.org/2000/svg';
const mushaf=document.getElementById('mushaf');

function currentSvg(){return mushaf.querySelector('svg')}
function verseOf(p){return p.getAttribute('surah')+':'+p.getAttribute('ayah')}
function selectedPolygons(svg){return [...svg.querySelectorAll('.ayahPolygon')].filter(p=>selected.includes(p.dataset.verse))}

function prepare(){
  const svg=currentSvg();
  if(!svg){N?.error('SVG Mushaf absent');return}
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    const k=verseOf(p);p.dataset.verse=k;
    p.removeAttribute('tabindex');
    p.onclick=e=>{e.stopPropagation();const [s,a]=k.split(':').map(Number);N?.verseTap(s,a)};
  });
  render();
  N?.pageShown(currentPage);
}
document.addEventListener('click',()=>N?.surfaceTap?.());

function insideSelection(polys,x,y){
  const svg=currentSvg();if(!svg)return false;
  const pt=svg.createSVGPoint();pt.x=x;pt.y=y;
  return polys.some(p=>{try{return p.isPointInFill(pt)}catch(_e){return false}});
}

/* Cells whose centre belongs to the selected Quran passage. */
function maskCandidates(lines,polys){
  const byLine=[];
  lines.forEach((line,li)=>{
    const top=Number(line.top),bottom=Number(line.bottom),cells=[];
    (line.cells||[]).forEach((cell,ci)=>{
      const x0=Number(cell[0]),x1=Number(cell[1]);
      if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;
      cells.push({line:li,index:ci,x0,x1,top,bottom});
    });
    cells.sort((a,b)=>a.x0-b.x0);
    if(cells.length)byLine.push({id:String(line.id),top,bottom,cells});
  });
  return byLine;
}

/*
 * One continuous rounded band per physical line. Arabic reading starts on the right, so 25%
 * hides the rightmost quarter, 50% contains that quarter, 75% contains the half, and 100%
 * covers the selected line. The selection clip keeps the band inside the actual selected verses.
 */
function hiddenBandForLine(line,percent){
  const cells=line.cells,n=cells.length;
  if(percent<=0||!n)return null;
  const take=percent>=100?n:Math.max(1,Math.min(n,Math.ceil(n*percent/100)));
  const chosen=cells.slice(n-take); // rightmost contiguous cells: RTL progression
  const left=Math.min(...chosen.map(c=>c.x0));
  const right=Math.max(...chosen.map(c=>c.x1));
  return {
    x:left-2.2,
    y:line.top-1.5,
    width:(right-left)+4.4,
    height:(line.bottom-line.top)+3.0
  };
}

/* Re-copy ayah rosettes above mask so structural markers stay visible. */
function markerLayer(svg,polys){
  const g=document.createElementNS(NS,'g');
  const markers=svg.querySelectorAll('#ayah_markers > g');
  if(!markers.length)return g;
  const rootCtm=svg.getScreenCTM();if(!rootCtm)return g;
  const inv=rootCtm.inverse();
  markers.forEach(m=>{
    const ctm=m.getScreenCTM();if(!ctm)return;
    const full=inv.multiply(ctm),b=m.getBBox();
    const pt=svg.createSVGPoint();pt.x=b.x+b.width/2;pt.y=b.y+b.height/2;
    const c=pt.matrixTransform(full);
    if(!insideSelection(polys,c.x,c.y))return;
    const wrap=document.createElementNS(NS,'g');
    wrap.setAttribute('transform',`matrix(${full.a} ${full.b} ${full.c} ${full.d} ${full.e} ${full.f})`);
    [...m.childNodes].forEach(n=>wrap.appendChild(n.cloneNode(true)));
    g.appendChild(wrap);
  });
  return g;
}

function render(){
  document.body.classList.toggle('eink',eink);
  const svg=currentSvg();if(!svg)return;
  svg.querySelectorAll('.ayahPolygon').forEach(p=>{
    p.classList.toggle('selected',selected.includes(p.dataset.verse));
    p.classList.toggle('audio',audioVerse!==null&&p.dataset.verse===audioVerse);
  });
  svg.querySelectorAll('.masklayer').forEach(n=>n.remove());
  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(!clamped||!pageGeo||!lineIds.length)return;
  const lines=(pageGeo.lines||[]).filter(l=>lineIds.includes(l.id));if(!lines.length)return;
  const polys=selectedPolygons(svg),byLine=maskCandidates(lines,polys);if(!byLine.length)return;

  const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
  const defs=document.createElementNS(NS,'defs'),clip=document.createElementNS(NS,'clipPath');
  clip.id='hifz-selection-clip';
  polys.forEach(p=>{const q=p.cloneNode(false);q.removeAttribute('class');q.removeAttribute('style');clip.appendChild(q)});
  defs.appendChild(clip);layer.appendChild(defs);
  const group=document.createElementNS(NS,'g');
  if(polys.length)group.setAttribute('clip-path','url(#hifz-selection-clip)');

  byLine.forEach(line=>{
    const band=hiddenBandForLine(line,clamped);if(!band)return;
    const el=document.createElementNS(NS,'rect');
    el.setAttribute('class','maskcell');
    el.setAttribute('x',band.x);el.setAttribute('y',band.y);el.setAttribute('width',band.width);el.setAttribute('height',band.height);
    el.setAttribute('rx','4');el.setAttribute('ry','4');
    group.appendChild(el);
  });
  layer.appendChild(group);
  if(polys.length)layer.appendChild(markerLayer(svg,polys));
  svg.appendChild(layer);
}

function revealSelection(visibleFraction){
  const svg=currentSvg();if(!svg||!selected.length)return;
  const nodes=selectedPolygons(svg);if(!nodes.length)return;
  const viewport=Math.max(1,window.innerHeight||document.documentElement.clientHeight||1);
  const fraction=Math.max(.30,Math.min(.70,Number(visibleFraction)||.46));
  document.documentElement.style.setProperty('--reveal-pad',Math.ceil(viewport*(1-fraction))+'px');
  requestAnimationFrame(()=>{
    let top=Infinity,bottom=-Infinity;
    nodes.forEach(p=>{const r=p.getBoundingClientRect();if(r.height>0){top=Math.min(top,r.top);bottom=Math.max(bottom,r.bottom)}});
    if(!Number.isFinite(top))return;
    const limit=viewport*fraction-12,safeTop=viewport*.04,height=bottom-top;
    let delta=0;
    if(bottom>limit)delta=bottom-limit;
    if(top-delta<safeTop)delta=top-safeTop;
    if(height>limit-safeTop)delta=top-safeTop;
    if(delta)window.scrollBy(0,delta);
  });
}
function clearReveal(){document.documentElement.style.setProperty('--reveal-pad','0px');window.scrollTo(0,0)}

window.HifzReader={
  setGeometry(geometry){pageGeo=geometry||null;render()},
  setMask(hidden){mask=Number(hidden||0);render()},
  setSelection(selection,lines){selected=selection||[];lineIds=lines||[];render()},
  setAudioVerse(value){audioVerse=value||null;render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

N?.ready();
prepare();
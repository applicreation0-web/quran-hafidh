'use strict';
/* Quran Hifz local reader: canonical SVG, deterministic nested masks, no network. */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=(boot.selection||[]).map(String);
let lineIds=(boot.lines||[]).map(String);
let mask=Number(boot.mask||0);
let eink=!!boot.eink;
let audioVerse=null;
const NS='http://www.w3.org/2000/svg';
const mushaf=document.getElementById('mushaf');

function currentSvg(){return mushaf.querySelector('svg')}
function verseOf(p){return p.getAttribute('surah')+':'+p.getAttribute('ayah')}
function selectedPolygons(svg){return [...svg.querySelectorAll('.ayahPolygon')].filter(p=>selected.includes(String(p.dataset.verse)))}

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

/* Exact visual width from the right of the selected physical line (RTL). */
function hiddenBandForLine(line,percent){
  const cells=line.cells;
  if(percent<=0||!cells.length)return null;
  const left=Math.min(...cells.map(c=>c.x0));
  const right=Math.max(...cells.map(c=>c.x1));
  const span=Math.max(0,right-left);
  if(!span)return null;
  const fraction=Math.max(0,Math.min(1,Number(percent)/100));
  const hiddenWidth=span*fraction;
  return {
    x:(right-hiddenWidth)-2.2,
    y:line.top-1.5,
    width:hiddenWidth+4.4,
    height:(line.bottom-line.top)+3.0
  };
}

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
    p.classList.toggle('selected',selected.includes(String(p.dataset.verse)));
    p.classList.toggle('audio',audioVerse!==null&&String(p.dataset.verse)===audioVerse);
  });
  svg.querySelectorAll('.masklayer').forEach(n=>n.remove());
  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(!clamped||!pageGeo||!lineIds.length)return;
  const wanted=new Set(lineIds.map(String));
  const lines=(pageGeo.lines||[]).filter(l=>wanted.has(String(l.id)));if(!lines.length)return;
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

/* Move only when the selected passage would actually be hidden by the Tafsir panel. */
function revealSelection(visibleFraction){
  const svg=currentSvg();
  document.documentElement.style.setProperty('--reveal-shift','0px');
  if(!svg||!selected.length)return;
  const nodes=selectedPolygons(svg);if(!nodes.length)return;
  requestAnimationFrame(()=>{
    const viewport=Math.max(1,window.innerHeight||document.documentElement.clientHeight||1);
    const fraction=Math.max(.30,Math.min(.70,Number(visibleFraction)||.46));
    let top=Infinity,bottom=-Infinity;
    nodes.forEach(p=>{const r=p.getBoundingClientRect();if(r.height>0){top=Math.min(top,r.top);bottom=Math.max(bottom,r.bottom)}});
    if(!Number.isFinite(top))return;
    const limit=viewport*fraction-12;
    const safeTop=viewport*.04;
    if(bottom<=limit&&top>=safeTop)return;
    const height=bottom-top;
    let needed=Math.max(0,bottom-limit);
    let maxUp=Math.max(0,top-safeTop);
    if(height>limit-safeTop)needed=maxUp;
    const delta=Math.min(needed,maxUp);
    if(delta>0)document.documentElement.style.setProperty('--reveal-shift',(-delta)+'px');
  });
}
function clearReveal(){document.documentElement.style.setProperty('--reveal-shift','0px')}

window.HifzReader={
  setGeometry(geometry){pageGeo=geometry||null;render()},
  setMask(hidden){mask=Number(hidden||0);render()},
  setSelection(selection,lines){selected=(selection||[]).map(String);lineIds=(lines||[]).map(String);clearReveal();render()},
  setAudioVerse(value){audioVerse=value==null?null:String(value);render()},
  setEink(value){eink=!!value;render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

prepare();
N?.ready();
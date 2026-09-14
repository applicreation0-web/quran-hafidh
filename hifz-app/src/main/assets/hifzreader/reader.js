'use strict';
/* Quran Hifz local reader: canonical SVG, non-deterministic random source-ink masks, no network. */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=(boot.selection||[]).map(String);
let lineIds=(boot.lines||[]).map(String);
let mask=Number(boot.mask||0);
const maskEntropy=String(boot.maskEntropy||'hifz-test');
let eink=!!boot.eink;
let strictLineFocus=!!boot.strictLineFocus;
let audioVerse=null;
let maskOrderSignature='';
let maskOrder=[];
const NS='http://www.w3.org/2000/svg';
const mushaf=document.getElementById('mushaf');

function currentSvg(){return mushaf.querySelector('svg')}
function verseOf(p){return p.getAttribute('surah')+':'+p.getAttribute('ayah')}
function selectedPolygons(svg){return [...svg.querySelectorAll('.ayahPolygon')].filter(p=>selected.includes(String(p.dataset.verse)))}
function shadeVerseSelection(){if(strictLineFocus)return false;return true}

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
  const out=[];
  lines.forEach(line=>{
    const top=Number(line.top),bottom=Number(line.bottom);
    (line.cells||[]).forEach((cell,ci)=>{
      const x0=Number(cell[0]),x1=Number(cell[1]);
      if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;
      out.push({key:String(line.id)+':'+ci,lineId:String(line.id),index:ci,x0,x1,top,bottom});
    });
  });
  return out;
}

function lineFocusLayer(lines){
  const layer=document.createElementNS(NS,'g');
  layer.setAttribute('class','linefocuslayer');
  (lines||[]).forEach(line=>{
    const cells=line.cells||[];if(!cells.length)return;
    let x0=Infinity,x1=-Infinity;
    cells.forEach(cell=>{
      x0=Math.min(x0,Number(cell[0]));
      x1=Math.max(x1,Number(cell[1]));
    });
    const top=Number(line.top),bottom=Number(line.bottom);
    if(!Number.isFinite(x0)||!Number.isFinite(x1)||!Number.isFinite(top)||!Number.isFinite(bottom)||x1<=x0||bottom<=top)return;
    const rect=document.createElementNS(NS,'rect');
    rect.setAttribute('class','linefocuscell');
    rect.setAttribute('x',x0);
    rect.setAttribute('y',top+0.25);
    rect.setAttribute('width',x1-x0);
    rect.setAttribute('height',Math.max(0,bottom-top-0.5));
    rect.setAttribute('rx','1.5');rect.setAttribute('ry','1.5');
    layer.appendChild(rect);
  });
  return layer;
}

function hashSeed(value){
  let h=2166136261>>>0;
  const s=String(value);
  for(let i=0;i<s.length;i++){h^=s.charCodeAt(i);h=Math.imul(h,16777619)>>>0;}
  return h||0x9e3779b9;
}

function seededRandom(seed){
  let x=hashSeed(seed);
  return ()=>{
    x^=(x<<13);x>>>=0;
    x^=(x>>>17);x>>>=0;
    x^=(x<<5);x>>>=0;
    return x/4294967296;
  };
}

function randomOrderKeys(cells,rng){
  const out=(cells||[]).map(c=>String(c.key));
  const draw=typeof rng==='function'?rng:Math.random;
  for(let i=out.length-1;i>0;i--){
    const r=Math.max(0,Math.min(0.999999999999,Number(draw())||0));
    const j=Math.floor(r*(i+1));
    const t=out[i];out[i]=out[j];out[j]=t;
  }
  return out;
}

function currentRandomOrder(cells){
  const keys=(cells||[]).map(c=>String(c.key)).sort();
  const signature=selected.join(',')+'|'+lineIds.join(',')+'|'+keys.join(',');
  if(signature!==maskOrderSignature){
    maskOrderSignature=signature;
    maskOrder=randomOrderKeys(cells,seededRandom(maskEntropy+'|'+signature));
  }
  return maskOrder;
}

/* Random cumulative masking over real source-ink groups. 25% is a fresh draw for a
 * new passage; 50/75/100 extend that same draw. Whitespace is never bridged. */
function randomSegmentsForCells(cells,percent,order){
  const fraction=Math.max(0,Math.min(1,Number(percent)/100));
  if(!fraction||!cells||!cells.length)return [];
  const map=new Map(cells.map(c=>[String(c.key),c]));
  const ordered=(order||[]).map(k=>map.get(String(k))).filter(Boolean);
  const seen=new Set(ordered.map(c=>String(c.key)));
  cells.forEach(c=>{if(!seen.has(String(c.key)))ordered.push(c)});
  const totalWidth=cells.reduce((sum,c)=>sum+Math.max(0,c.x1-c.x0),0);
  if(!totalWidth)return [];
  let remaining=totalWidth*fraction;
  const segments=[];
  for(const cell of ordered){
    if(remaining<=0.0001)break;
    const cellWidth=Math.max(0,cell.x1-cell.x0);if(!cellWidth)continue;
    const hiddenWidth=Math.min(cellWidth,remaining);
    segments.push({key:String(cell.key),x:cell.x1-hiddenWidth,y:cell.top+0.6,width:hiddenWidth,height:Math.max(0,(cell.bottom-cell.top)-1.2)});
    remaining-=hiddenWidth;
  }
  return segments;
}

function markerLayer(svg,polys,lines){
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
    const visible=polys.length
      ? insideSelection(polys,c.x,c.y)
      : (lines||[]).some(line=>c.y>=Number(line.top)&&c.y<=Number(line.bottom));
    if(!visible)return;
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
    p.classList.toggle('selected',shadeVerseSelection()&&selected.includes(String(p.dataset.verse)));
    p.classList.toggle('audio',audioVerse!==null&&String(p.dataset.verse)===audioVerse);
  });
  svg.querySelectorAll('.masklayer,.linefocuslayer').forEach(n=>n.remove());

  const wanted=new Set(lineIds.map(String));
  const lines=pageGeo&&lineIds.length
    ? (pageGeo.lines||[]).filter(l=>wanted.has(String(l.id)))
    : [];
  if(strictLineFocus&&lines.length){
    const focus=lineFocusLayer(lines);
    if(focus.childNodes.length)svg.appendChild(focus);
  }

  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(!clamped||!pageGeo||!lineIds.length||!lines.length)return;
  const polys=selectedPolygons(svg),cells=maskCandidates(lines,polys);if(!cells.length)return;
  const segments=randomSegmentsForCells(cells,clamped,currentRandomOrder(cells));

  const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
  const defs=document.createElementNS(NS,'defs'),clip=document.createElementNS(NS,'clipPath');
  clip.id='hifz-selection-clip';
  polys.forEach(p=>{const q=p.cloneNode(false);q.removeAttribute('class');q.removeAttribute('style');clip.appendChild(q)});
  defs.appendChild(clip);layer.appendChild(defs);
  const group=document.createElementNS(NS,'g');
  if(polys.length)group.setAttribute('clip-path','url(#hifz-selection-clip)');
  segments.forEach(segment=>{
    const el=document.createElementNS(NS,'rect');
    el.setAttribute('class','maskcell');
    el.setAttribute('x',segment.x);el.setAttribute('y',segment.y);
    el.setAttribute('width',segment.width);el.setAttribute('height',segment.height);
    el.setAttribute('rx','2');el.setAttribute('ry','2');
    group.appendChild(el);
  });
  layer.appendChild(group);
  // Verse-number rosettes are deliberately redrawn above the random masks.
  layer.appendChild(markerLayer(svg,polys,lines));
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
  setSelection(selection,lines){
    const nextSelected=(selection||[]).map(String),nextLines=(lines||[]).map(String);
    const changed=nextSelected.join(',')!==selected.join(',')||nextLines.join(',')!==lineIds.join(',');
    selected=nextSelected;lineIds=nextLines;
    if(changed){maskOrderSignature='';maskOrder=[];}
    clearReveal();render();
  },
  setAudioVerse(value){audioVerse=value==null?null:String(value);render()},
  setEink(value){eink=!!value;render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

if(typeof module!=='undefined'&&module.exports)module.exports={randomOrderKeys,randomSegmentsForCells,seededRandom,lineFocusLayer};
prepare();
N?.ready();
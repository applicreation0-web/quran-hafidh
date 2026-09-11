'use strict';
/*
 * Quran Hifz — lecteur local (sélection, masques, révélation au-dessus du panneau).
 * Corrections 2026-09-11 (audit Claude) :
 *  - plus de tabindex sur les polygones : supprime le cadre orange (anneau de focus WebView) ;
 *  - masque calculé uniquement sur les cellules réellement dans les versets sélectionnés ;
 *  - cellules masquées jointives (plus de lamelles de texte visibles entre les mots) ;
 *  - marqueurs de fin de verset toujours visibles au-dessus du masque ;
 *  - revealSelection() crée la marge de défilement nécessaire, clearReveal() la retire.
 */
const N=window.HifzNative;
const boot=window.HIFZ_BOOT||{};
let pageGeo=boot.geometry||null;
let currentPage=Number(boot.page||1);
let selected=boot.selection||[];
let lineIds=boot.lines||[];
let mask=Number(boot.mask||0);
let eink=!!boot.eink;
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

function stableHash(text){
  let h=2166136261>>>0;
  for(let i=0;i<text.length;i++){h^=text.charCodeAt(i);h=Math.imul(h,16777619)>>>0}
  return h;
}

function insideSelection(polys,x,y){
  const svg=currentSvg();if(!svg)return false;
  const pt=svg.createSVGPoint();pt.x=x;pt.y=y;
  return polys.some(p=>{try{return p.isPointInFill(pt)}catch(_e){return false}});
}

/* Cellules (mots) des lignes ciblées dont le centre est dans un verset sélectionné. */
function maskCandidates(lines,polys){
  const out=[];
  lines.forEach((line,li)=>{
    const top=Number(line.top),bottom=Number(line.bottom),cells=line.cells||[];
    cells.forEach((cell,ci)=>{
      const x0=Number(cell[0]),x1=Number(cell[1]);
      if(polys.length&&!insideSelection(polys,(x0+x1)/2,(top+bottom)/2))return;
      out.push({line:li,index:ci,x0,x1,top,bottom,
        order:stableHash(String(line.id)+'#'+ci+'#'+cell[0]+'#'+cell[1])});
    });
  });
  return out;
}

/* Élargit chaque cellule masquée jusqu'au milieu de l'espace avec ses voisines de ligne. */
function widenedRect(cell,lineCells){
  const i=lineCells.indexOf(cell);
  const prev=lineCells[i-1],next=lineCells[i+1];
  const left=prev?Math.min(cell.x0,(prev.x1+cell.x0)/2):cell.x0-3;
  const right=next?Math.max(cell.x1,(cell.x1+next.x0)/2):cell.x1+3;
  return {x:left-0.6,y:cell.top-0.6,width:(right-left)+1.2,height:(cell.bottom-cell.top)+1.2};
}

/* Recopie au-dessus du masque les rosaces de fin de verset situées dans la sélection. */
function markerLayer(svg,polys){
  const g=document.createElementNS(NS,'g');
  const markers=svg.querySelectorAll('#ayah_markers > g');
  if(!markers.length)return g;
  const inv=svg.getScreenCTM()?.inverse();
  if(!inv)return g;
  markers.forEach(m=>{
    const ctm=m.getScreenCTM();if(!ctm)return;
    const full=inv.multiply(ctm);
    const b=m.getBBox();
    const c=new DOMPoint(b.x+b.width/2,b.y+b.height/2).matrixTransform(full);
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
  svg.querySelectorAll('.ayahPolygon').forEach(p=>p.classList.toggle('selected',selected.includes(p.dataset.verse)));
  svg.querySelectorAll('.masklayer').forEach(n=>n.remove());
  const clamped=Math.max(0,Math.min(100,Number(mask)||0));
  if(!clamped||!pageGeo||!lineIds.length)return;
  const lines=(pageGeo.lines||[]).filter(l=>lineIds.includes(l.id));if(!lines.length)return;
  const polys=selectedPolygons(svg);
  const candidates=maskCandidates(lines,polys);if(!candidates.length)return;

  const layer=document.createElementNS(NS,'g');layer.setAttribute('class','masklayer');
  const defs=document.createElementNS(NS,'defs'),clip=document.createElementNS(NS,'clipPath');
  clip.id='hifz-selection-clip';
  polys.forEach(p=>{const q=p.cloneNode(false);q.removeAttribute('class');q.removeAttribute('style');clip.appendChild(q)});
  defs.appendChild(clip);layer.appendChild(defs);
  const group=document.createElementNS(NS,'g');
  if(polys.length)group.setAttribute('clip-path','url(#hifz-selection-clip)');

  const ordered=[...candidates].sort((a,b)=>a.order-b.order||a.top-b.top||a.x0-b.x0);
  const hiddenCount=clamped>=100?ordered.length:Math.max(1,Math.round(ordered.length*clamped/100));
  const hidden=new Set(ordered.slice(0,hiddenCount));
  const byLine=new Map();
  candidates.forEach(c=>{if(!byLine.has(c.line))byLine.set(c.line,[]);byLine.get(c.line).push(c)});
  byLine.forEach(list=>{
    list.sort((a,b)=>a.x0-b.x0);
    list.forEach(cell=>{
      if(!hidden.has(cell))return;
      const r=widenedRect(cell,list),el=document.createElementNS(NS,'rect');
      el.setAttribute('class','maskcell');
      el.setAttribute('x',r.x);el.setAttribute('y',r.y);el.setAttribute('width',r.width);el.setAttribute('height',r.height);
      group.appendChild(el);
    });
  });
  layer.appendChild(group);
  if(polys.length)layer.appendChild(markerLayer(svg,polys));
  svg.appendChild(layer);
}

/* Place la sélection au-dessus d'un panneau couvrant (1 - visibleFraction) de la hauteur. */
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
function clearReveal(){
  document.documentElement.style.setProperty('--reveal-pad','0px');
  window.scrollTo(0,0);
}

window.HifzReader={
  setGeometry(geometry){pageGeo=geometry||null;render()},
  setMask(hidden){mask=Number(hidden||0);render()},
  setSelection(selection,lines){selected=selection||[];lineIds=lines||[];render()},
  revealSelection(visibleFraction){revealSelection(visibleFraction)},
  clearReveal(){clearReveal()},
  page(){return currentPage}
};

N?.ready();
prepare();
